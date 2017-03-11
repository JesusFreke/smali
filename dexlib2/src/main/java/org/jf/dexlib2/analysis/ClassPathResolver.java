/*
 * Copyright 2016, Google Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 * Neither the name of Google Inc. nor the names of its
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

import com.beust.jcommander.internal.Sets;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.DexFileFactory.UnsupportedFileTypeException;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.DexBackedOdexFile;
import org.jf.dexlib2.dexbacked.OatFile;
import org.jf.dexlib2.dexbacked.OatFile.OatDexFile;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.iface.MultiDexContainer;
import org.jf.dexlib2.iface.MultiDexContainer.MultiDexFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class ClassPathResolver {
    private final Iterable<String> classPathDirs;
    private final Opcodes opcodes;

    private final Set<File> loadedFiles = Sets.newHashSet();
    private final List<ClassProvider> classProviders = Lists.newArrayList();

    /**
     * Constructs a new ClassPathResolver using a specified list of bootclasspath entries
     *
     * @param bootClassPathDirs A list of directories to search for boot classpath entries. Can be empty if all boot
     *                          classpath entries are specified as local paths
     * @param bootClassPathEntries A list of boot classpath entries to load. These can either be local paths, or
     *                             device paths (e.g. "/system/framework/framework.jar"). The entry will be interpreted
     *                             first as a local path. If not found as a local path, it will be interpreted as a
     *                             partial or absolute device path, and will be searched for in bootClassPathDirs
     * @param extraClassPathEntries A list of additional classpath entries to load. Can be empty. All entries must be
     *                              local paths. Device paths are not supported.
     * @param dexFile The dex file that the classpath will be used to analyze
     * @throws IOException If any IOException occurs
     * @throws ResolveException If any classpath entries cannot be loaded for some reason
     *
     *  If null, a default bootclasspath is used,
     *                             depending on the the file type of dexFile and the api level. If empty, no boot
     *                             classpath entries will be loaded
     */
    public ClassPathResolver(@Nonnull List<String> bootClassPathDirs, @Nonnull List<String> bootClassPathEntries,
                             @Nonnull List<String> extraClassPathEntries, @Nonnull DexFile dexFile)
            throws IOException {
        this(bootClassPathDirs, bootClassPathEntries, extraClassPathEntries, dexFile, true);
    }

    /**
     * Constructs a new ClassPathResolver using a default list of bootclasspath entries
     *
     * @param bootClassPathDirs A list of directories to search for boot classpath entries
     * @param extraClassPathEntries A list of additional classpath entries to load. Can be empty. All entries must be
     *                              local paths. Device paths are not supported.
     * @param dexFile The dex file that the classpath will be used to analyze
     * @throws IOException If any IOException occurs
     * @throws ResolveException If any classpath entries cannot be loaded for some reason
     *
     *  If null, a default bootclasspath is used,
     *                             depending on the the file type of dexFile and the api level. If empty, no boot
     *                             classpath entries will be loaded
     */
    public ClassPathResolver(@Nonnull List<String> bootClassPathDirs, @Nonnull List<String> extraClassPathEntries,
                             @Nonnull DexFile dexFile)
            throws IOException {
        this(bootClassPathDirs, null, extraClassPathEntries, dexFile, true);
    }

    private ClassPathResolver(@Nonnull List<String> bootClassPathDirs, @Nullable List<String> bootClassPathEntries,
                              @Nonnull List<String> extraClassPathEntries, @Nonnull DexFile dexFile, boolean unused)
            throws IOException {
        this.classPathDirs = bootClassPathDirs;
        opcodes = dexFile.getOpcodes();

        if (bootClassPathEntries == null) {
            bootClassPathEntries = getDefaultBootClassPath(dexFile, opcodes.api);
        }

        for (String entry : bootClassPathEntries) {
            try {
                loadLocalOrDeviceBootClassPathEntry(entry);
            } catch (NoDexException ex) {
                if (entry.endsWith(".jar")) {
                    String odexEntry = entry.substring(0, entry.length() - 4) + ".odex";
                    try {
                        loadLocalOrDeviceBootClassPathEntry(odexEntry);
                    } catch (NoDexException ex2) {
                        throw new ResolveException("Neither %s nor %s contain a dex file", entry, odexEntry);
                    } catch (NotFoundException ex2) {
                        throw new ResolveException(ex);
                    }
                } else {
                    throw new ResolveException(ex);
                }
            } catch (NotFoundException ex) {
                if (entry.endsWith(".odex")) {
                    String jarEntry = entry.substring(0, entry.length() - 5) + ".jar";
                    try {
                        loadLocalOrDeviceBootClassPathEntry(jarEntry);
                        } catch (NoDexException ex2) {
                        throw new ResolveException("Neither %s nor %s contain a dex file", entry, jarEntry);
                    } catch (NotFoundException ex2) {
                        throw new ResolveException(ex);
                    }
                } else {
                    throw new ResolveException(ex);
                }
            }
        }

        for (String entry: extraClassPathEntries) {
            // extra classpath entries must be specified using a local path, so we don't need to do the search through
            // bootClassPathDirs
            try {
                loadLocalClassPathEntry(entry);
            } catch (NoDexException ex) {
                throw new ResolveException(ex);
            }
        }

        if (dexFile instanceof MultiDexContainer.MultiDexFile) {
            MultiDexContainer<? extends MultiDexFile> container = ((MultiDexFile)dexFile).getContainer();
            for (String entry: container.getDexEntryNames()) {
                classProviders.add(new DexClassProvider(container.getEntry(entry)));
            }
        } else {
            classProviders.add(new DexClassProvider(dexFile));
        }
    }

    @Nonnull
    public List<ClassProvider> getResolvedClassProviders() {
        return classProviders;
    }

    private boolean loadLocalClassPathEntry(@Nonnull String entry) throws NoDexException, IOException {
        File entryFile = new File(entry);
        if (entryFile.exists() && entryFile.isFile()) {
            try {
                loadEntry(entryFile, true);
                return true;
            } catch (UnsupportedFileTypeException ex) {
                throw new ResolveException(ex, "Couldn't load classpath entry %s", entry);
            }
        }
        return false;
    }

    private void loadLocalOrDeviceBootClassPathEntry(@Nonnull String entry)
            throws IOException, NoDexException, NotFoundException {
        // first, see if the entry is a valid local path
        if (loadLocalClassPathEntry(entry)) {
            return;
        }

        // It's not a local path, so let's try to resolve it as a device path, relative to one of the provided
        // directories
        List<String> pathComponents = splitDevicePath(entry);
        Joiner pathJoiner = Joiner.on(File.pathSeparatorChar);

        for (String directory: classPathDirs) {
            File directoryFile = new File(directory);
            if (!directoryFile.exists()) {
                continue;
            }

            for (int i=0; i<pathComponents.size(); i++) {
                String partialPath = pathJoiner.join(pathComponents.subList(i, pathComponents.size()));
                File entryFile = new File(directoryFile, partialPath);
                if (entryFile.exists() && entryFile.isFile()) {
                    loadEntry(entryFile, true);
                    return;
                }
            }
        }

        throw new NotFoundException("Could not find classpath entry %s", entry);
    }

    private void loadEntry(@Nonnull File entryFile, boolean loadOatDependencies)
            throws IOException, NoDexException {
        if (loadedFiles.contains(entryFile)) {
            return;
        }

        MultiDexContainer<? extends DexBackedDexFile> container;
        try {
            container = DexFileFactory.loadDexContainer(entryFile, opcodes);
        } catch (UnsupportedFileTypeException ex) {
            throw new ResolveException(ex);
        }

        List<String> entryNames = container.getDexEntryNames();

        if (entryNames.size() == 0) {
            throw new NoDexException("%s contains no dex file", entryFile);
        }

        loadedFiles.add(entryFile);

        for (String entryName: entryNames) {
            classProviders.add(new DexClassProvider(container.getEntry(entryName)));
        }

        if (loadOatDependencies && container instanceof OatFile) {
            List<String> oatDependencies = ((OatFile)container).getBootClassPath();
            if (!oatDependencies.isEmpty()) {
                try {
                    loadOatDependencies(entryFile.getParentFile(), oatDependencies);
                } catch (NotFoundException ex) {
                    throw new ResolveException(ex, "Error while loading oat file %s", entryFile);
                } catch (NoDexException ex) {
                    throw new ResolveException(ex, "Error while loading dependencies for oat file %s", entryFile);
                }
            }
        }
    }

    @Nonnull
    private static List<String> splitDevicePath(@Nonnull String path) {
        return Lists.newArrayList(Splitter.on('/').split(path));
    }

    private void loadOatDependencies(@Nonnull File directory, @Nonnull List<String> oatDependencies)
            throws IOException, NoDexException, NotFoundException {
        // We assume that all oat dependencies are located in the same directory as the oat file
        for (String oatDependency: oatDependencies) {
            String oatDependencyName = getFilenameForOatDependency(oatDependency);
            File file = new File(directory, oatDependencyName);
            if (!file.exists()) {
                throw new NotFoundException("Cannot find dependency %s in %s", oatDependencyName, directory);
            }

            loadEntry(file, false);
        }
    }

    @Nonnull
    private String getFilenameForOatDependency(String oatDependency) {
        int index = oatDependency.lastIndexOf('/');

        String dependencyLeaf = oatDependency.substring(index+1);
        if (dependencyLeaf.endsWith(".art")) {
            return dependencyLeaf.substring(0, dependencyLeaf.length() - 4) + ".oat";
        }
        return dependencyLeaf;
    }

    private static class NotFoundException extends Exception {
        public NotFoundException(String message, Object... formatArgs) {
            super(String.format(message, formatArgs));
        }
    }
    
    private static class NoDexException extends Exception {
        public NoDexException(String message, Object... formatArgs) {
            super(String.format(message, formatArgs));
        }
    }

    /**
     * An error that occurred while resolving the classpath
     */
    public static class ResolveException extends RuntimeException {
        public ResolveException (String message, Object... formatArgs) {
            super(String.format(message, formatArgs));
        }

        public ResolveException (Throwable cause) {
            super(cause);
        }

        public ResolveException (Throwable cause, String message, Object... formatArgs) {
            super(String.format(message, formatArgs), cause);
        }
    }

    /**
     * Returns the default boot class path for the given dex file and api level.
     */
    @Nonnull
    private static List<String> getDefaultBootClassPath(@Nonnull DexFile dexFile, int apiLevel) {
        if (dexFile instanceof OatFile.OatDexFile) {
            List<String> bcp = ((OatDexFile)dexFile).getContainer().getBootClassPath();
            if (!bcp.isEmpty()) {
                for (int i=0; i<bcp.size(); i++) {
                    String entry = bcp.get(i);
                    if (entry.endsWith(".art")) {
                        bcp.set(i, entry.substring(0, entry.length() - 4) + ".oat");
                    }
                }
                return bcp;
            }
            return Lists.newArrayList("boot.oat");
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
        } else if (apiLevel <= 23) {
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
        } else /*if (apiLevel <= 24)*/ {
            return Lists.newArrayList(
                    "/system/framework/core-oj.jar",
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
