/*
 * Copyright 2013, Google Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.dexlib2.analysis;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.*;
import com.google.common.io.Files;
import com.google.common.primitives.Ints;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.DexFileFactory.MultipleDexFilesException;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.analysis.reflection.ReflectionClassDef;
import org.jf.dexlib2.dexbacked.DexBackedOdexFile;
import org.jf.dexlib2.dexbacked.OatFile;
import org.jf.dexlib2.dexbacked.OatFile.OatDexFile;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.immutable.ImmutableDexFile;
import org.jf.util.ExceptionWithContext;
import org.jf.util.PathUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

public class ClassPath {
    @Nonnull private final TypeProto unknownClass;
    @Nonnull private List<ClassProvider> classProviders;
    private final boolean checkPackagePrivateAccess;
    public final int oatVersion;

    public static final int NOT_ART = -1;

    /**
     * Creates a new ClassPath instance that can load classes from the given providers
     *
     * @param classProviders An iterable of ClassProviders. When loading a class, these providers will be searched in
     *                       order
     */
    public ClassPath(ClassProvider... classProviders) throws IOException {
        this(Arrays.asList(classProviders), false, NOT_ART);
    }

    /**
     * Creates a new ClassPath instance that can load classes from the given providers
     *
     * @param classProviders An iterable of ClassProviders. When loading a class, these providers will be searched in
     *                       order
     * @param checkPackagePrivateAccess Whether checkPackagePrivateAccess is needed, enabled for ONLY early API 17 by
     *                                  default
     * @param oatVersion The applicable oat version, or NOT_ART
     */
    public ClassPath(@Nonnull Iterable<? extends ClassProvider> classProviders, boolean checkPackagePrivateAccess,
                     int oatVersion) {
        // add fallbacks for certain special classes that must be present
        unknownClass = new UnknownClassProto(this);
        loadedClasses.put(unknownClass.getType(), unknownClass);
        this.checkPackagePrivateAccess = checkPackagePrivateAccess;
        this.oatVersion = oatVersion;

        loadPrimitiveType("Z");
        loadPrimitiveType("B");
        loadPrimitiveType("S");
        loadPrimitiveType("C");
        loadPrimitiveType("I");
        loadPrimitiveType("J");
        loadPrimitiveType("F");
        loadPrimitiveType("D");
        loadPrimitiveType("L");

        this.classProviders = Lists.newArrayList(classProviders);
        this.classProviders.add(getBasicClasses());
    }

    private void loadPrimitiveType(String type) {
        loadedClasses.put(type, new PrimitiveProto(this, type));
    }

    private static ClassProvider getBasicClasses() {
        // fallbacks for some special classes that we assume are present
        return new DexClassProvider(new ImmutableDexFile(Opcodes.forApi(19), ImmutableSet.of(
                new ReflectionClassDef(Class.class),
                new ReflectionClassDef(Cloneable.class),
                new ReflectionClassDef(Object.class),
                new ReflectionClassDef(Serializable.class),
                new ReflectionClassDef(String.class),
                new ReflectionClassDef(Throwable.class))));
    }

    public boolean isArt() {
        return oatVersion != NOT_ART;
    }

    @Nonnull
    public TypeProto getClass(@Nonnull CharSequence type) {
        return loadedClasses.getUnchecked(type.toString());
    }

    private final CacheLoader<String, TypeProto> classLoader = new CacheLoader<String, TypeProto>() {
        @Override public TypeProto load(String type) throws Exception {
            if (type.charAt(0) == '[') {
                return new ArrayProto(ClassPath.this, type);
            } else {
                return new ClassProto(ClassPath.this, type);
            }
        }
    };

    @Nonnull private LoadingCache<String, TypeProto> loadedClasses = CacheBuilder.newBuilder().build(classLoader);

    @Nonnull
    public ClassDef getClassDef(String type) {
        for (ClassProvider provider: classProviders) {
            ClassDef classDef = provider.getClassDef(type);
            if (classDef != null) {
                return classDef;
            }
        }
        throw new UnresolvedClassException("Could not resolve class %s", type);
    }

    @Nonnull
    public TypeProto getUnknownClass() {
        return unknownClass;
    }

    public boolean shouldCheckPackagePrivateAccess() {
        return checkPackagePrivateAccess;
    }


    /**
     * Creates a ClassPath given a set of user inputs
     *
     * This performs all the magic in finding the right defaults based on the values provided and what type of dex
     * file we have. E.g. choosing the right default bootclasspath if needed, actually locating the files on the
     * filesystem, etc.
     *
     * This is meant to be as forgiving as possible and to generally "do the right thing" based on the given inputs.
     *
     * @param classPathDirs A list of directories to search for class path entries in. Be sure to include "." to search
     *                      the current working directory, if appropriate.
     * @param bootClassPathEntries A list of boot class path entries to load. This can be just the bare filenames,
     *                             relative paths, absolute paths based on the local directory structure, absolute paths
     *                             based on the device directory structure, etc. It can contain paths to
     *                             jar/dex/oat/odex files, or just bare filenames with no extension, etc.
     *                             If non-null and blank, then no entries will be loaded other than dexFile
     *                             If null, it will attempt to use the correct defaults based on the inputs.
     * @param extraClassPathEntries Additional class path entries. The same sorts of naming mechanisms as for
     *                              bootClassPathEntries are allowed
     * @param checkPackagePrivateAccess Whether checkPackagePrivateAccess is needed, enabled for ONLY early API 17 by
     *                                  default
     * @param dexFile The dex file that will be analyzed. It can be a dex, odex or oat file.
     * @param api The api level of the device that these dex files come from.
     * @param experimental Whether to allow experimental opcodes
     *
     * @return A ClassPath object
     */
    @Nonnull
    public static ClassPath loadClassPath(@Nonnull Iterable<String> classPathDirs,
                                          @Nullable Iterable<String> bootClassPathEntries,
                                          @Nonnull Iterable<String> extraClassPathEntries, @Nonnull DexFile dexFile,
                                          int api, boolean experimental, boolean checkPackagePrivateAccess)
            throws IOException {
        List<ClassProvider> classProviders = Lists.newArrayList();
        if (bootClassPathEntries == null) {
            bootClassPathEntries = getDefaultDeviceBootClassPath(dexFile, api);
        }
        if (extraClassPathEntries == null) {
            extraClassPathEntries = ImmutableList.of();
        }
        for (String entry: Iterables.concat(bootClassPathEntries, extraClassPathEntries)) {
            List<File> files = Lists.newArrayList();

            for (String extension: new String[] { null, ".apk", ".jar", ".odex", ".oat", ".dex" }) {
                String searchEntry = entry;
                if (Files.getFileExtension(entry).equals(extension)) {
                    continue;
                }
                if (extension != null) {
                    searchEntry = Files.getNameWithoutExtension(entry) + extension;
                }

                for (String dir: classPathDirs) {
                    files.addAll(findFiles(new File(dir), new File(searchEntry).getName(), 100));
                }
                if (files.size() > 0) {
                    break;
                }
            }

            if (files.size() == 0) {
                throw new FileNotFoundException(String.format("Classpath entry %s could not be found", entry));
            }

            File bestMatch = Collections.max(files, new ClassPathEntryComparator(entry));
            try {
                DexFile entryDexFile = DexFileFactory.loadDexFile(bestMatch, api, experimental);
                classProviders.add(new DexClassProvider(entryDexFile));
            } catch (MultipleDexFilesException ex) {
                for (DexFile entryDexFile: ex.oatFile.getDexFiles()) {
                    classProviders.add(new DexClassProvider(entryDexFile));
                }
            }
        }

        int oatVersion = -1;
        if (dexFile instanceof OatDexFile) {
            oatVersion = ((OatDexFile)dexFile).getOatVersion();
        }
        classProviders.add(new DexClassProvider(dexFile));

        return new ClassPath(classProviders, checkPackagePrivateAccess, oatVersion);
    }

    private static class ClassPathEntryComparator implements Comparator<File> {
        @Nonnull  private List<String> reversePathComponents;

        public ClassPathEntryComparator(@Nonnull String entry) {
            // TODO: will PathUtil.getPathComponents work for unix-style paths while on windows?
            this.reversePathComponents = Lists.reverse(PathUtil.getPathComponents(new File(entry)));
        }

        @Override public int compare(File file1, File file2) {
            int comparison = Ints.compare(countMatchingComponents(file1), countMatchingComponents(file2));
            if (comparison != 0) {
                // the path that matches the entry being searched for wins
                return comparison;
            }

            comparison = Ints.compare(PathUtil.getPathComponents(file1).size(),
                    PathUtil.getPathComponents(file2).size());
            if (comparison != 0) {
                // the path "higher up" (with fewer directories) wins
                return comparison * -1;
            }

            // otherwise.. just return the first one alphabetically.
            return file1.compareTo(file2);
        }

        private int countMatchingComponents(File file) {
            for (int i=0; i<reversePathComponents.size(); i++) {
                if (file == null) {
                    return i;
                }
                if (!file.getName().equals(reversePathComponents.get(i))) {
                    return i;
                }
                file = file.getParentFile();
            }
            return reversePathComponents.size();
        }
    }

    /**
     * Ye olde recursive file search.
     *
     * Searches for all files (not directories!) named "name".
     *
     * It attempts to detect filesystem loops via File.getCanonicalPath, and will not recurse path maxDepth directories.
     */
    @Nonnull
    private static List<File> findFiles(@Nonnull File dir, @Nonnull String name, int maxDepth) throws IOException {
        List<File> files = Lists.newArrayList();
        Set<String> visitedPaths = Sets.newHashSet();

        if (!dir.exists()) {
            throw new IllegalArgumentException(String.format("Directory %s does not exist", dir.getPath()));
        }
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException(String.format("%s is not a directory", dir.getPath()));
        }

        findFiles(files, visitedPaths, dir, name, maxDepth);
        return files;
    }

    private static void findFiles(@Nonnull  List<File> result, @Nonnull Set<String> visitedPaths, @Nonnull File dir,
                                  @Nonnull String name, int maxDepth) throws IOException {
        if (maxDepth < 0 || !visitedPaths.add(dir.getCanonicalPath())) {
            return;
        }

        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child: children) {
            if (child.isDirectory()) {
                findFiles(result, visitedPaths, child, name, maxDepth-1);
            } else {
                if (name.equals(child.getName())) {
                    try {
                        DexFileFactory.loadDexFile(child, 15);
                    } catch (ExceptionWithContext ex) {
                        if (!(ex instanceof MultipleDexFilesException)) {
                            // Don't add it to the results if it can't be loaded
                            continue;
                        }
                    }
                    result.add(child);
                }
            }
        }
    }

    private final Supplier<OdexedFieldInstructionMapper> fieldInstructionMapperSupplier = Suppliers.memoize(
            new Supplier<OdexedFieldInstructionMapper>() {
                @Override public OdexedFieldInstructionMapper get() {
                    return new OdexedFieldInstructionMapper(isArt());
                }
            });

    @Nonnull
    public OdexedFieldInstructionMapper getFieldInstructionMapper() {
        return fieldInstructionMapperSupplier.get();
    }

    /**
     * Returns the default boot class path for the given api. This is boot class path that is used for "stock"
     * (i.e nexus) images for the given api level, but may not be correct for devices with heavily modified firmware.
     */
    @Nonnull
    private static List<String> getDefaultDeviceBootClassPath(DexFile dexFile, int apiLevel) {
        if (dexFile instanceof OatFile.OatDexFile) {
            if (((OatFile.OatDexFile) dexFile).getOatVersion() >= 74) {
                return ((OatFile.OatDexFile) dexFile).getOatFile().getBootClassPath();
            } else {
                return Lists.newArrayList("boot.oat");
            }
        }

        if (dexFile instanceof DexBackedOdexFile) {
            return ((DexBackedOdexFile)dexFile).getDependencies();
        }

        if (apiLevel <= 8) {
            return Lists.newArrayList(
                    "/system/framework/core.jar",
                    "/system/framework/ext.jar",
                    "/system/framework/framework.jar",
                    "/system/framework/android.policy.jar",
                    "/system/framework/services.jar");
        } else if (apiLevel <= 11) {
            return Lists.newArrayList(
                    "/system/framework/core.jar",
                    "/system/framework/bouncycastle.jar",
                    "/system/framework/ext.jar",
                    "/system/framework/framework.jar",
                    "/system/framework/android.policy.jar",
                    "/system/framework/services.jar",
                    "/system/framework/core-junit.jar");
        } else if (apiLevel <= 13) {
            return Lists.newArrayList(
                    "/system/framework/core.jar",
                    "/system/framework/apache-xml.jar",
                    "/system/framework/bouncycastle.jar",
                    "/system/framework/ext.jar",
                    "/system/framework/framework.jar",
                    "/system/framework/android.policy.jar",
                    "/system/framework/services.jar",
                    "/system/framework/core-junit.jar");
        } else if (apiLevel <= 15) {
            return Lists.newArrayList(
                    "/system/framework/core.jar",
                    "/system/framework/core-junit.jar",
                    "/system/framework/bouncycastle.jar",
                    "/system/framework/ext.jar",
                    "/system/framework/framework.jar",
                    "/system/framework/android.policy.jar",
                    "/system/framework/services.jar",
                    "/system/framework/apache-xml.jar",
                    "/system/framework/filterfw.jar");
        } else if (apiLevel <= 17) {
            // this is correct as of api 17/4.2.2
            return Lists.newArrayList(
                    "/system/framework/core.jar",
                    "/system/framework/core-junit.jar",
                    "/system/framework/bouncycastle.jar",
                    "/system/framework/ext.jar",
                    "/system/framework/framework.jar",
                    "/system/framework/telephony-common.jar",
                    "/system/framework/mms-common.jar",
                    "/system/framework/android.policy.jar",
                    "/system/framework/services.jar",
                    "/system/framework/apache-xml.jar");
        } else if (apiLevel <= 18) {
            return Lists.newArrayList(
                    "/system/framework/core.jar",
                    "/system/framework/core-junit.jar",
                    "/system/framework/bouncycastle.jar",
                    "/system/framework/ext.jar",
                    "/system/framework/framework.jar",
                    "/system/framework/telephony-common.jar",
                    "/system/framework/voip-common.jar",
                    "/system/framework/mms-common.jar",
                    "/system/framework/android.policy.jar",
                    "/system/framework/services.jar",
                    "/system/framework/apache-xml.jar");
        } else if (apiLevel <= 19) {
            return Lists.newArrayList(
                    "/system/framework/core.jar",
                    "/system/framework/conscrypt.jar",
                    "/system/framework/core-junit.jar",
                    "/system/framework/bouncycastle.jar",
                    "/system/framework/ext.jar",
                    "/system/framework/framework.jar",
                    "/system/framework/framework2.jar",
                    "/system/framework/telephony-common.jar",
                    "/system/framework/voip-common.jar",
                    "/system/framework/mms-common.jar",
                    "/system/framework/android.policy.jar",
                    "/system/framework/services.jar",
                    "/system/framework/apache-xml.jar",
                    "/system/framework/webviewchromium.jar");
        } else if (apiLevel <= 22) {
            return Lists.newArrayList(
                    "/system/framework/core-libart.jar",
                    "/system/framework/conscrypt.jar",
                    "/system/framework/okhttp.jar",
                    "/system/framework/core-junit.jar",
                    "/system/framework/bouncycastle.jar",
                    "/system/framework/ext.jar",
                    "/system/framework/framework.jar",
                    "/system/framework/telephony-common.jar",
                    "/system/framework/voip-common.jar",
                    "/system/framework/ims-common.jar",
                    "/system/framework/mms-common.jar",
                    "/system/framework/android.policy.jar",
                    "/system/framework/apache-xml.jar");
        } else /*if (apiLevel <= 23)*/ {
            return Lists.newArrayList(
                    "/system/framework/core-libart.jar",
                    "/system/framework/conscrypt.jar",
                    "/system/framework/okhttp.jar",
                    "/system/framework/core-junit.jar",
                    "/system/framework/bouncycastle.jar",
                    "/system/framework/ext.jar",
                    "/system/framework/framework.jar",
                    "/system/framework/telephony-common.jar",
                    "/system/framework/voip-common.jar",
                    "/system/framework/ims-common.jar",
                    "/system/framework/apache-xml.jar",
                    "/system/framework/org.apache.http.legacy.boot.jar");
        }
    }
}
