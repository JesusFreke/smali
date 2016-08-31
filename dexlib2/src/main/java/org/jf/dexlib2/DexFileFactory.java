/*
 * Copyright 2012, Google Inc.
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

package org.jf.dexlib2;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.DexBackedDexFile.NotADexFile;
import org.jf.dexlib2.dexbacked.DexBackedOdexFile;
import org.jf.dexlib2.dexbacked.OatFile;
import org.jf.dexlib2.dexbacked.OatFile.NotAnOatFileException;
import org.jf.dexlib2.dexbacked.OatFile.OatDexFile;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.writer.pool.DexPool;
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class DexFileFactory {
    @Nonnull
    public static DexBackedDexFile loadDexFile(@Nonnull String path) throws IOException {
        return loadDexFile(new File(path), Opcodes.forApi(15));
    }

    @Nonnull
    public static DexBackedDexFile loadDexFile(@Nonnull String path, @Nonnull Opcodes opcodes) throws IOException {
        return loadDexFile(new File(path), opcodes);
    }

    @Nonnull
    public static DexBackedDexFile loadDexFile(@Nonnull File file) throws IOException {
        return loadDexFile(file, Opcodes.forApi(15));
    }

    /**
     * Loads a dex/apk/odex/oat file.
     *
     * For oat files with multiple dex files, the first will be opened. For zip/apk files, the "classes.dex" entry
     * will be opened.
     *
     * @param file The file to open
     * @param opcodes The set of opcodes to use
     * @return A DexBackedDexFile for the given file
     *
     * @throws UnsupportedOatVersionException If file refers to an unsupported oat file
     * @throws DexFileNotFoundException If file does not exist, if file is a zip file but does not have a "classes.dex"
     * entry, or if file is an oat file that has no dex entries.
     * @throws UnsupportedFileTypeException If file is not a valid dex/zip/odex/oat file, or if the "classes.dex" entry
     * in a zip file is not a valid dex file
     */
    @Nonnull
    public static DexBackedDexFile loadDexFile(@Nonnull File file, @Nonnull Opcodes opcodes) throws IOException {
        if (!file.exists()) {
            throw new DexFileNotFoundException("%s does not exist", file.getName());
        }

        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(file);
        } catch (IOException ex) {
            // ignore and continue
        }

        if (zipFile != null) {
            try {
                return new ZipDexEntryFinder(zipFile, opcodes).findEntry("classes.dex", true);
            } finally {
                zipFile.close();
            }
        }

        InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
        try {
            try {
                return DexBackedDexFile.fromInputStream(opcodes, inputStream);
            } catch (DexBackedDexFile.NotADexFile ex) {
                // just eat it
            }

            try {
                return DexBackedOdexFile.fromInputStream(opcodes, inputStream);
            } catch (DexBackedOdexFile.NotAnOdexFile ex) {
                // just eat it
            }

            // Note: DexBackedDexFile.fromInputStream and DexBackedOdexFile.fromInputStream will reset inputStream
            // back to the same position, if they fails

            OatFile oatFile = null;
            try {
                oatFile = OatFile.fromInputStream(inputStream);
            } catch (NotAnOatFileException ex) {
                // just eat it
            }

            if (oatFile != null) {
                if (oatFile.isSupportedVersion() == OatFile.UNSUPPORTED) {
                    throw new UnsupportedOatVersionException(oatFile);
                }

                List<OatDexFile> oatDexFiles = oatFile.getDexFiles();

                if (oatDexFiles.size() == 0) {
                    throw new DexFileNotFoundException("Oat file %s contains no dex files", file.getName());
                }

                return oatDexFiles.get(0);
            }
        } finally {
            inputStream.close();
        }

        throw new UnsupportedFileTypeException("%s is not an apk, dex, odex or oat file.", file.getPath());
    }

    /**
     * Loads a dex entry from a container format (zip/oat)
     *
     * This has two modes of operation, depending on the exactMatch parameter. When exactMatch is true, it will only
     * load an entry whose name exactly matches that provided by the dexEntry parameter.
     *
     * When exactMatch is false, then it will search for any entry that dexEntry is a path suffix of. "path suffix"
     * meaning all the path components in dexEntry must fully match the corresponding path components in the entry name,
     * but some path components at the beginning of entry name can be missing.
     *
     * For example, if an oat file contains a "/system/framework/framework.jar:classes2.dex" entry, then the following
     * will match (not an exhaustive list):
     *
     * "/system/framework/framework.jar:classes2.dex"
     * "system/framework/framework.jar:classes2.dex"
     * "framework/framework.jar:classes2.dex"
     * "framework.jar:classes2.dex"
     * "classes2.dex"
     *
     * Note that partial path components specifically don't match. So something like "work/framework.jar:classes2.dex"
     * would not match.
     *
     * If dexEntry contains an initial slash, it will be ignored for purposes of this suffix match -- but not when
     * performing an exact match.
     *
     * If multiple entries match the given dexEntry, a MultipleMatchingDexEntriesException will be thrown
     *
     * @param file The container file. This must be either a zip (apk) file or an oat file.
     * @param dexEntry The name of the entry to load. This can either be the exact entry name, if exactMatch is true,
     *                 or it can be a path suffix.
     * @param exactMatch If true, dexE
     * @param opcodes The set of opcodes to use
     * @return A DexBackedDexFile for the given entry
     *
     * @throws UnsupportedOatVersionException If file refers to an unsupported oat file
     * @throws DexFileNotFoundException If the file does not exist, or if no matching entry could be found
     * @throws UnsupportedFileTypeException If file is not a valid zip/oat file, or if the matching entry is not a
     * valid dex file
     * @throws MultipleMatchingDexEntriesException If multiple entries match the given dexEntry
     */
    public static DexBackedDexFile loadDexEntry(@Nonnull File file, @Nonnull String dexEntry,
                                                boolean exactMatch, @Nonnull Opcodes opcodes) throws IOException {
        if (!file.exists()) {
            throw new DexFileNotFoundException("Container file %s does not exist", file.getName());
        }

        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(file);
        } catch (IOException ex) {
            // ignore and continue
        }

        if (zipFile != null) {
            try {
                return new ZipDexEntryFinder(zipFile, opcodes).findEntry(dexEntry, exactMatch);
            } finally {
                zipFile.close();
            }
        }

        InputStream inputStream = new BufferedInputStream(new FileInputStream(file));
        try {
            OatFile oatFile = null;
            try {
                oatFile = OatFile.fromInputStream(inputStream);
            } catch (NotAnOatFileException ex) {
                // just eat it
            }

            if (oatFile != null) {
                if (oatFile.isSupportedVersion() == OatFile.UNSUPPORTED) {
                    throw new UnsupportedOatVersionException(oatFile);
                }

                List<OatDexFile> oatDexFiles = oatFile.getDexFiles();

                if (oatDexFiles.size() == 0) {
                    throw new DexFileNotFoundException("Oat file %s contains no dex files", file.getName());
                }

                return new OatDexEntryFinder(file.getPath(), oatFile).findEntry(dexEntry, exactMatch);
            }
        } finally {
            inputStream.close();
        }

        throw new UnsupportedFileTypeException("%s is not an apk or oat file.", file.getPath());
    }

    /**
     * Writes a DexFile out to disk
     *
     * @param path The path to write the dex file to
     * @param dexFile a Dexfile to write
     * @throws IOException
     */
    public static void writeDexFile(@Nonnull String path, @Nonnull DexFile dexFile) throws IOException {
        DexPool.writeTo(path, dexFile);
    }

    private DexFileFactory() {}

    public static class DexFileNotFoundException extends ExceptionWithContext {
        public DexFileNotFoundException(@Nullable String message, Object... formatArgs) {
            super(message, formatArgs);
        }
    }

    public static class UnsupportedOatVersionException extends ExceptionWithContext {
        @Nonnull public final OatFile oatFile;

        public UnsupportedOatVersionException(@Nonnull OatFile oatFile) {
            super("Unsupported oat version: %d", oatFile.getOatVersion());
            this.oatFile = oatFile;
        }
    }

    public static class MultipleMatchingDexEntriesException extends ExceptionWithContext {
        public MultipleMatchingDexEntriesException(@Nonnull String message, Object... formatArgs) {
            super(String.format(message, formatArgs));
        }
    }

    public static class UnsupportedFileTypeException extends ExceptionWithContext {
        public UnsupportedFileTypeException(@Nonnull String message, Object... formatArgs) {
            super(String.format(message, formatArgs));
        }
    }

    /**
     * Matches two entries fully, ignoring any initial slash, if any
     */
    private static boolean fullEntryMatch(@Nonnull String entry, @Nonnull String targetEntry) {
        if (entry.equals(targetEntry)) {
            return true;
        }

        if (entry.charAt(0) == '/') {
            entry = entry.substring(1);
        }

        if (targetEntry.charAt(0) == '/') {
            targetEntry = targetEntry.substring(1);
        }

        return entry.equals(targetEntry);
    }

    /**
     * Performs a partial match against entry and targetEntry.
     *
     * This is considered a partial match if targetEntry is a suffix of entry, and if the suffix starts
     * on a path "part" (ignoring the initial separator, if any). Both '/' and ':' are considered separators for this.
     *
     * So entry="/blah/blah/something.dex" and targetEntry="lah/something.dex" shouldn't match, but
     * both targetEntry="blah/something.dex" and "/blah/something.dex" should match.
     */
    private static boolean partialEntryMatch(String entry, String targetEntry) {
        if (entry.equals(targetEntry)) {
            return true;
        }

        if (!entry.endsWith(targetEntry)) {
            return false;
        }

        // Make sure the first matching part is a full entry. We don't want to match "/blah/blah/something.dex" with
        // "lah/something.dex", but both "/blah/something.dex" and "blah/something.dex" should match
        char precedingChar = entry.charAt(entry.length() - targetEntry.length() - 1);
        char firstTargetChar = targetEntry.charAt(0);
        // This is a device path, so we should always use the linux separator '/', rather than the current platform's
        // separator
        return firstTargetChar == ':' || firstTargetChar == '/' || precedingChar == ':' || precedingChar == '/';
    }

    protected abstract static class DexEntryFinder {
        @Nullable
        protected abstract DexBackedDexFile getEntry(@Nonnull String entry) throws IOException;

        @Nonnull
        protected abstract List<String> getEntryNames();

        @Nonnull
        protected abstract String getFilename();

        @Nonnull
        public DexBackedDexFile findEntry(@Nonnull String targetEntry, boolean exactMatch) throws IOException {
            if (exactMatch) {
                DexBackedDexFile dexFile = getEntry(targetEntry);
                if (dexFile == null) {
                    if (getEntryNames().contains(targetEntry)) {
                        throw new UnsupportedFileTypeException("Entry %s in %s is not a dex file", targetEntry,
                                getFilename());
                    } else {
                        throw new DexFileNotFoundException("Could not find %s in %s.", targetEntry, getFilename());
                    }
                }
                return dexFile;
            }

            // find all full and partial matches
            List<String> fullMatches = Lists.newArrayList();
            List<DexBackedDexFile> fullEntries = Lists.newArrayList();
            List<String> partialMatches = Lists.newArrayList();
            List<DexBackedDexFile> partialEntries = Lists.newArrayList();
            for (String entry: getEntryNames()) {
                if (fullEntryMatch(entry, targetEntry)) {
                    // We want to grab all full matches, regardless of whether they're actually a dex file.
                    fullMatches.add(entry);
                    fullEntries.add(getEntry(entry));
                } else if (partialEntryMatch(entry, targetEntry)) {
                    DexBackedDexFile dexFile = getEntry(entry);
                    // We only want to grab a partial match if it is actually a dex file.
                    if (dexFile != null) {
                        partialMatches.add(entry);
                        partialEntries.add(dexFile);
                    }
                }
            }

            // full matches always take priority
            if (fullEntries.size() == 1) {
                DexBackedDexFile dexFile = fullEntries.get(0);
                if (dexFile == null) {
                    throw new UnsupportedFileTypeException("Entry %s in %s is not a dex file",
                            fullMatches.get(0), getFilename());
                }
                return dexFile;
            }
            if (fullEntries.size() > 1) {
                // This should be quite rare. This would only happen if an oat file has two entries that differ
                // only by an initial path separator. e.g. "/blah/blah.dex" and "blah/blah.dex"
                throw new MultipleMatchingDexEntriesException(String.format(
                        "Multiple entries in %s match %s: %s", getFilename(), targetEntry,
                        Joiner.on(", ").join(fullMatches)));
            }

            if (partialEntries.size() == 0) {
                throw new DexFileNotFoundException("Could not find a dex entry in %s matching %s",
                        getFilename(), targetEntry);
            }
            if (partialEntries.size() > 1) {
                throw new MultipleMatchingDexEntriesException(String.format(
                        "Multiple dex entries in %s match %s: %s", getFilename(), targetEntry,
                        Joiner.on(", ").join(partialMatches)));
            }
            return partialEntries.get(0);
        }
    }

    private static class ZipDexEntryFinder extends DexEntryFinder {
        @Nonnull private final ZipFile zipFile;
        @Nonnull private final Opcodes opcodes;

        public ZipDexEntryFinder(@Nonnull ZipFile zipFile, @Nonnull Opcodes opcodes) {
            this.zipFile = zipFile;
            this.opcodes = opcodes;
        }

        @Nullable @Override protected DexBackedDexFile getEntry(@Nonnull String entry) throws IOException {
            ZipEntry zipEntry = zipFile.getEntry(entry);

            InputStream stream = null;
            try {
                stream = zipFile.getInputStream(zipEntry);
                return DexBackedDexFile.fromInputStream(opcodes, stream);
            } catch (NotADexFile ex) {
                return null;
            } finally {
                if (stream != null) {
                    stream.close();
                }
            }
        }

        @Nonnull @Override protected List<String> getEntryNames() {
            List<String> entries = Lists.newArrayList();
            Enumeration<? extends ZipEntry> entriesEnumeration = zipFile.entries();

            while (entriesEnumeration.hasMoreElements()) {
                ZipEntry entry = entriesEnumeration.nextElement();
                entries.add(entry.getName());
            }

            return entries;
        }

        @Nonnull @Override protected String getFilename() {
            return zipFile.getName();
        }
    }

    private static class OatDexEntryFinder extends DexEntryFinder {
        @Nonnull private final String fileName;
        @Nonnull private final OatFile oatFile;

        public OatDexEntryFinder(@Nonnull String fileName, @Nonnull OatFile oatFile) {
            this.fileName = fileName;
            this.oatFile = oatFile;
        }

        @Nullable @Override protected DexBackedDexFile getEntry(@Nonnull String entry) throws IOException {
            for (OatDexFile dexFile: oatFile.getDexFiles()) {
                if (dexFile.filename.equals(entry)) {
                    return dexFile;
                }
            }
            return null;
        }

        @Nonnull @Override protected List<String> getEntryNames() {
            List<String> entries = Lists.newArrayList();

            for (OatDexFile oatDexFile: oatFile.getDexFiles()) {
                entries.add(oatDexFile.filename);
            }

            return entries;
        }

        @Nonnull @Override protected String getFilename() {
            return fileName;
        }
    }
}
