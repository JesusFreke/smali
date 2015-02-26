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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.analysis.reflection.ReflectionClassDef;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.DexBackedOatFile;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.immutable.ImmutableDexFile;
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

public class ClassPath {
    private static final String[] DEX_ODEXDIR = {"", "arm", "arm64", "mips", "x86", "x86_64"};
    private static final String[] DEX_EXTENSIONS = {"", ".odex", ".jar", ".apk", ".zip"};

    @Nonnull private final TypeProto unknownClass;
    @Nonnull private HashMap<String, ClassDef> availableClasses = Maps.newHashMap();
    private int api;
    private boolean isOat = false;

    /**
     * Creates a new ClassPath instance that can load classes from the given dex files
     *
     * @param classPath An array of DexFile objects. When loading a class, these dex files will be searched in order
     */
    public ClassPath(DexFile... classPath) throws IOException {
        this(Lists.newArrayList(classPath), 15, false);
    }

    /**
     * Creates a new ClassPath instance that can load classes from the given dex files
     *
     * @param classPath An iterable of DexFile objects. When loading a class, these dex files will be searched in order
     * @param api API level
     */
    public ClassPath(@Nonnull Iterable<DexFile> classPath, int api, boolean isOat) {
        // add fallbacks for certain special classes that must be present
        Iterable<DexFile> dexFiles = Iterables.concat(classPath, Lists.newArrayList(getBasicClasses()));

        unknownClass = new UnknownClassProto(this);
        loadedClasses.put(unknownClass.getType(), unknownClass);
        this.api = api;
        this.isOat = isOat;

        loadPrimitiveType("Z");
        loadPrimitiveType("B");
        loadPrimitiveType("S");
        loadPrimitiveType("C");
        loadPrimitiveType("I");
        loadPrimitiveType("J");
        loadPrimitiveType("F");
        loadPrimitiveType("D");
        loadPrimitiveType("L");

        for (DexFile dexFile: dexFiles) {
            for (ClassDef classDef: dexFile.getClasses()) {
                ClassDef prev = availableClasses.get(classDef.getType());
                if (prev == null) {
                    availableClasses.put(classDef.getType(), classDef);
                }
            }
        }
    }

    private void loadPrimitiveType(String type) {
        loadedClasses.put(type, new PrimitiveProto(this, type));
    }

    private static DexFile getBasicClasses() {
        // fallbacks for some special classes that we assume are present
        return new ImmutableDexFile(ImmutableSet.of(
                new ReflectionClassDef(Class.class),
                new ReflectionClassDef(Cloneable.class),
                new ReflectionClassDef(Object.class),
                new ReflectionClassDef(Serializable.class),
                new ReflectionClassDef(String.class),
                new ReflectionClassDef(Throwable.class)));
    }

    @Nonnull
    public TypeProto getClass(CharSequence type) {
        return loadedClasses.getUnchecked(type.toString());
    }

    private final CacheLoader<String, TypeProto> classLoader = new CacheLoader<String, TypeProto>() {
        @Override public TypeProto load(String type) throws Exception {
            if (type.charAt(0) == '[') {
                return new ArrayProto(ClassPath.this, type);
            } else {
                return new ClassProto(ClassPath.this, type, ClassPath.this.isOat);
            }
        }
    };

    @Nonnull private LoadingCache<String, TypeProto> loadedClasses = CacheBuilder.newBuilder().build(classLoader);

    @Nonnull
    public ClassDef getClassDef(String type) {
        ClassDef ret = availableClasses.get(type);
        if (ret == null) {
            throw new UnresolvedClassException("Could not resolve class %s", type);
        }
        return ret;
    }

    @Nonnull
    public TypeProto getUnknownClass() {
        return unknownClass;
    }

    public int getApi() {
        return api;
    }

    @Nonnull
    public static ClassPath fromClassPath(Iterable<String> classPathDirs, Iterable<String> classPath, DexFile dexFile,
                                          int api) {

        List<String> skipClassPaths = new ArrayList<String>();
        ArrayList<DexFile> dexFiles = Lists.newArrayList();

        // Load boot.oat first
        try {
            if(api >= 20) {
                List<DexBackedDexFile> oatFiles = loadClassPathOAT(classPathDirs, "boot.oat", api);
                //System.err.printf("Oat files: %d\n", oatFiles.size());

                for(DexBackedDexFile oatFile: oatFiles) {
                    DexBackedOatFile file = (DexBackedOatFile) oatFile;

                    //System.err.printf("Adding to class path: %s\n", file.getName());

                    skipClassPaths.add(file.getName());
                    dexFiles.add(oatFile);
                }
            }
        } catch(ExceptionWithContext ex) {
            // Ignore if we could not loat boot.oat
        }

        for (String classPathEntry: classPath) {
            if (skipClassPaths.contains(classPathEntry))
                continue;

            //System.err.printf("Adding to class path: %s\n", classPathEntry);

            dexFiles.addAll(loadClassPathEntry(classPathDirs, classPathEntry, api));
        }
        if (dexFile instanceof DexBackedOatFile) {
            DexBackedOatFile oatFile = (DexBackedOatFile)dexFile;
            do {
                for(DexBackedDexFile dex: oatFile.getDexes()) {
                    DexBackedOatFile oat = (DexBackedOatFile)dex;
                    //System.err.printf("Adding to class path: %s\n", oat.getName());
                    dexFiles.add(oat);
                }
            } while((oatFile = oatFile.getParent()) != null);
        } else {
            dexFiles.add(dexFile);
        }
        return new ClassPath(dexFiles, api, dexFile instanceof DexBackedOatFile);
    }

    private static final Pattern dalvikCacheOdexPattern = Pattern.compile("@([^@]+)@classes.dex$");

    @Nonnull
    private static List<DexBackedDexFile> loadClassPathOAT(@Nonnull Iterable<String> classPathDirs,
                                              @Nonnull String bootClassPathEntry, int api) {
        for (String classPathDir: classPathDirs) {
            for (String odexDir: DEX_ODEXDIR) {
                File file = new File(classPathDir + "/" + odexDir, bootClassPathEntry);

                if (file.exists() && file.isFile()) {
                    if (!file.canRead()) {
                        System.err.println(String.format(
                                "warning: cannot open %s for reading. Will continue looking.", file.getPath()));
                    } else {
                        try {
                            DexFile dexFile = DexFileFactory.loadDexFile(file, api);
                            if(!(dexFile instanceof DexBackedOatFile))
                                throw new ExceptionWithContext("\"%s\" is not an OAT file", bootClassPathEntry);

                            DexBackedOatFile oatFile = (DexBackedOatFile)dexFile;
                            return oatFile.getDexes();
                        } catch (DexFileFactory.NoClassesDexException ex) {
                            // ignore and continue
                        } catch (Exception ex) {
                            throw ExceptionWithContext.withContext(ex,
                                    "Error while reading boot class path entry \"%s\"", bootClassPathEntry);
                        }
                    }
                }
            }
        }
        throw new ExceptionWithContext("Cannot locate boot class path file %s", bootClassPathEntry);
    }

    @Nonnull
    private static List<DexFile> loadClassPathEntry(@Nonnull Iterable<String> classPathDirs,
                                              @Nonnull String bootClassPathEntry, int api) {
        File rawEntry = new File(bootClassPathEntry);
        // strip off the path - we only care about the filename
        String entryName = rawEntry.getName();

        // if it's a dalvik-cache entry, grab the name of the jar/apk
        if (entryName.endsWith("@classes.dex")) {
            Matcher m = dalvikCacheOdexPattern.matcher(entryName);

            if (!m.find()) {
                throw new ExceptionWithContext(String.format("Cannot parse dependency value %s", bootClassPathEntry));
            }

            entryName = m.group(1);
        }

        int extIndex = entryName.lastIndexOf(".");

        String baseEntryName;
        if (extIndex == -1) {
            baseEntryName = entryName;
        } else {
            baseEntryName = entryName.substring(0, extIndex);
        }

        for (String classPathDir: classPathDirs) {
            for (String odexDir: DEX_ODEXDIR) {
                for (String ext: DEX_EXTENSIONS) {
                    File file = new File(classPathDir + "/" + odexDir, baseEntryName + ext);

                    if (file.exists() && file.isFile()) {
                        if (!file.canRead()) {
                            System.err.println(String.format(
                                    "warning: cannot open %s for reading. Will continue looking.", file.getPath()));
                        } else {
                            List<DexFile> dexFiles = new ArrayList<DexFile>();
                            try {
                                boolean isZipFile = false;

                                ZipFile zipFile = null;
                                try {
                                    zipFile = new ZipFile(file);
                                    // if we get here, it's safe to assume we have a zip file
                                    isZipFile = true;
                                } catch(IOException ex) {
                                } finally {
                                    if (zipFile != null) {
                                        try {
                                            zipFile.close();
                                        } catch (IOException ex) {
                                            // just eat it
                                        }
                                    }
                                }

                                // An apk/jar may have several classes.dex files inside.
                                if(isZipFile) {
                                    for(int i = 1; ; i++) {
                                        String dexName = "classes";
                                        if (i > 1)
                                            dexName += i;

                                        dexName += ".dex";

                                        //System.err.printf("Trying to load %s from %s\n", dexName, baseEntryName + ext);
                                        dexFiles.add(DexFileFactory.loadDexFile(file, dexName, api));
                                    }
                                } else {
                                    dexFiles.add(DexFileFactory.loadDexFile(file, api));
                                }
                            } catch (DexFileFactory.NoClassesDexException ex) {
                                // ignore and continue
                            } catch (Exception ex) {
                                throw ExceptionWithContext.withContext(ex,
                                        "Error while reading boot class path entry \"%s\"", bootClassPathEntry);
                            }

                            return dexFiles;
                        }
                    }
                }
            }
        }
        throw new ExceptionWithContext("Cannot locate boot class path file %s", bootClassPathEntry);
    }
}
