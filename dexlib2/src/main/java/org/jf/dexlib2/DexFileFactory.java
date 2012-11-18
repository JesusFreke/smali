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

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.DexBuffer;
import org.jf.dexlib2.iface.DexFile;
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class DexFileFactory {
    @Nonnull
    public static DexFile loadDexFile(String path) throws IOException {
        return loadDexFile(new File(path));
    }

    @Nonnull
    public static DexFile loadDexFile(File dexFile) throws IOException {
        boolean isZipFile = false;
        byte[] dexBytes = null;
        ZipFile zipFile = null;
        boolean zipSuccess = false;
        try {
            zipFile = new ZipFile(dexFile);
            // if we get here, it's safe to assume we have a zip file
            isZipFile = true;

            ZipEntry zipEntry = zipFile.getEntry("classes.dex");
            if (zipEntry == null) {
                throw new ExceptionWithContext("zip file %s does not contain a classes.dex file", dexFile.getName());
            }
            long fileLength = zipEntry.getSize();
            if (fileLength < 40) {
                throw new ExceptionWithContext(
                        "The classes.dex file in %s is too small to be a valid dex file", dexFile.getName());
            } else if (fileLength > Integer.MAX_VALUE) {
                throw new ExceptionWithContext("The classes.dex file in %s is too large to read in", dexFile.getName());
            }
            dexBytes = new byte[(int)fileLength];
            ByteStreams.readFully(zipFile.getInputStream(zipEntry), dexBytes);
            zipSuccess = true;
        } catch (IOException ex) {
            if (isZipFile) {
                throw ex;
            }
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException ex) {
                    // just eat it
                }
            }
        }

        if (!zipSuccess) {
            // nope, it doesn't looks like a zip file. Let's see if it's a dex file
            dexBytes = Files.toByteArray(dexFile);
        }

        DexBuffer dexBuf = new DexBuffer(dexBytes);
        return new DexBackedDexFile(dexBuf);
    }

    public static void writeDexFile(String path, DexFile dexFile) throws IOException {
        org.jf.dexlib2.writer.DexFile.writeTo(path, dexFile);
    }

    private DexFileFactory() {}
}
