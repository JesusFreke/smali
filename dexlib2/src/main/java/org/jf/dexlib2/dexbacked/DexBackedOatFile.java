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

package org.jf.dexlib2.dexbacked;

import com.google.common.io.ByteStreams;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.raw.HeaderItem;
import org.jf.dexlib2.dexbacked.raw.OatHeaderItem;
import org.jf.dexlib2.dexbacked.util.VariableSizeList;
import org.jf.dexlib2.iface.DexFile;

import javax.annotation.Nonnull;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.io.File;
import java.util.List;
import java.util.ArrayList;

public class DexBackedOatFile extends DexBackedDexFile {
    private String name;
    private DexBackedOatFile parent = null;
    private List<DexBackedDexFile> dexes = new ArrayList<DexBackedDexFile>();

    public DexBackedOatFile(@Nonnull Opcodes opcodes, String name, DexBackedOatFile parent, byte[] oatBuf) {
        super(opcodes, oatBuf);

        this.name = name;
        this.parent = parent;
    }
    public DexBackedOatFile(@Nonnull Opcodes opcodes, byte[] oatBuf, List<OatHeaderItem.DexItem> dexes) {
        super(opcodes, oatBuf, dexes.get(0).offset);

        for(OatHeaderItem.DexItem dex: dexes) {
            byte[] dexBuf = new byte[dex.size];
            System.arraycopy(oatBuf, dex.offset, dexBuf, 0, dex.size);

            this.dexes.add(new DexBackedOatFile(opcodes, dex.name, this, dexBuf));
        }
    }

    @Override public boolean isOdexFile() {
        return true;
    }

    public static DexBackedOatFile fromInputStream(@Nonnull Opcodes opcodes, @Nonnull InputStream is)
            throws IOException {
        if (!is.markSupported()) {
            throw new IllegalArgumentException("InputStream must support mark");
        }
        is.mark(OatHeaderItem.MAGIC_LENGTH);
        byte[] partialHeader = new byte[OatHeaderItem.MAGIC_LENGTH];
        try {
            ByteStreams.readFully(is, partialHeader);
        } catch (EOFException ex) {
            throw new NotADexFile("File is too short");
        } finally {
            is.reset();
        }

        verifyMagic(partialHeader);

        is.reset();

        is.mark(is.available() + 1);
        byte[] oatData = new byte[is.available()];
        ByteStreams.readFully(is, oatData);

        return new DexBackedOatFile(opcodes, oatData, OatHeaderItem.getDexes(oatData));
    }

    public List<DexBackedDexFile> getDexes() {
        return this.dexes;
    }

    public String getName() {
        return this.name;
    }

    public DexBackedOatFile getParent() {
        return this.parent;
    }

    public String getSimpleName() {
        String name = new File(this.name).getName();
        if (name.contains(":")) {
            String[] split = name.split(":");

            name = split[1] + "_" + split[0];
        }

        return name;
    }

    private static void verifyMagic(byte[] buf) {
        if (!OatHeaderItem.verifyMagic(buf)) {
            StringBuilder sb = new StringBuilder("Invalid magic value:");
            for (int i=0; i<OatHeaderItem.MAGIC_LENGTH; i++) {
                sb.append(String.format(" %02x", buf[i]));
            }
            throw new NotAnOatFile(sb.toString());
        }
    }

    public static class NotAnOatFile extends RuntimeException {
        public NotAnOatFile() {
        }

        public NotAnOatFile(Throwable cause) {
            super(cause);
        }

        public NotAnOatFile(String message) {
            super(message);
        }

        public NotAnOatFile(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
