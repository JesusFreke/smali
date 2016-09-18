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

package org.jf.dexlib2.dexbacked;

import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile.NotADexFile;
import org.jf.dexlib2.dexbacked.ZipDexContainer.ZipDexFile;
import org.jf.dexlib2.iface.MultiDexContainer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.jf.dexlib2.dexbacked.DexBackedDexFile.verifyMagicAndByteOrder;

/**
 * Represents a zip file that contains dex files (i.e. an apk or jar file)
 */
public class ZipDexContainer implements MultiDexContainer<ZipDexFile>, Closeable {

    private final ZipFile zipFile;
    private final Opcodes opcodes;

    /**
     * Constructs a new ZipDexContainer for the given zip file
     *
     * @param zipFile A Zip
     * @param opcodes The Opcodes instance to use when loading dex files from this container
     */
    public ZipDexContainer(@Nonnull ZipFile zipFile, @Nonnull Opcodes opcodes) {
        this.zipFile = zipFile;
        this.opcodes = opcodes;
    }

    /**
     * Gets a list of the names of dex files in this zip file.
     *
     * @return A list of the names of dex files in this zip file
     */
    @Nonnull @Override public List<String> getDexEntryNames() throws IOException {
        List<String> entryNames = Lists.newArrayList();
        Enumeration<? extends ZipEntry> entriesEnumeration = zipFile.entries();

        while (entriesEnumeration.hasMoreElements()) {
            ZipEntry entry = entriesEnumeration.nextElement();

            if (!isDex(entry)) {
                continue;
            }

            entryNames.add(entry.getName());
        }

        return entryNames;
    }

    /**
     * Loads a dex file from a specific named entry.
     *
     * @param entryName The name of the entry
     * @return A ZipDexFile, or null if there is no entry with the given name
     * @throws NotADexFile If the entry isn't a dex file
     */
    @Nullable @Override public ZipDexFile getEntry(@Nonnull String entryName) throws IOException {
        ZipEntry entry = zipFile.getEntry(entryName);
        if (entry == null) {
            return null;
        }

        return loadEntry(entry);
    }

    @Override public void close() throws IOException {
        zipFile.close();
    }

    public class ZipDexFile extends DexBackedDexFile implements MultiDexContainer.MultiDexFile {

        private final String entryName;

        protected ZipDexFile(@Nonnull Opcodes opcodes, @Nonnull byte[] buf, @Nonnull String entryName) {
            super(opcodes, buf, 0);
            this.entryName = entryName;
        }

        @Nonnull @Override public String getEntryName() {
            return entryName;
        }

        @Nonnull @Override public MultiDexContainer getContainer() {
            return ZipDexContainer.this;
        }
    }

    private boolean isDex(@Nonnull ZipEntry zipEntry) throws IOException {
        InputStream inputStream = zipFile.getInputStream(zipEntry);
        try {
            inputStream.mark(44);
            byte[] partialHeader = new byte[44];
            try {
                ByteStreams.readFully(inputStream, partialHeader);
            } catch (EOFException ex) {
                throw new NotADexFile("File is too short");
            }

            try {
                verifyMagicAndByteOrder(partialHeader, 0);
            } catch (NotADexFile ex) {
                return false;
            }
            return true;
        } finally {
            inputStream.close();
        }
    }

    @Nonnull
    private ZipDexFile loadEntry(@Nonnull ZipEntry zipEntry)
            throws IOException {
        InputStream inputStream = zipFile.getInputStream(zipEntry);
        try {
            byte[] buf = ByteStreams.toByteArray(inputStream);
            return new ZipDexFile(opcodes, buf, zipEntry.getName());
        } finally {
            inputStream.close();
        }
    }
}
