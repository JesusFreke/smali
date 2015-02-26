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

package org.jf.dexlib2.dexbacked.raw;

import com.google.common.io.ByteStreams;

import java.io.InputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import org.jf.dexlib2.dexbacked.BaseDexBuffer;

public class OatHeaderItem {
    public static final byte[][] MAGIC_VALUES= new byte[][] {
            new byte[] {0x7f, 0x45, 0x4c, 0x46}, // "\x7fELF"
    };
    public static final byte[] OAT_MAGIC_VALUE = {0x6f, 0x61, 0x74, 0x0a}; // "oat\n"

    public static final int MAGIC_OFFSET = 0;
    public static final int MAGIC_LENGTH = 4;

    public static final int[] OAT_HEADER_SIZE = {
        0x50,   // L Preview
        0x54    // L Final
    };
    public static final int OAT_HEADER_OFFSET_NUMDEXES = 0x14;

    public static boolean verifyMagic(byte[] buf) {
        if (buf.length < MAGIC_LENGTH) {
            return false;
        }

        boolean matches = true;
        for (int i=0; i<MAGIC_VALUES.length; i++) {
            byte[] expected = MAGIC_VALUES[i];
            matches = true;
            for (int j=0; j < MAGIC_LENGTH; j++) {
                if (buf[j] != expected[j]) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return true;
            }
        }

        return false;
    }

    public static int getDexOffset(byte[] buf) {
        int offset = MAGIC_LENGTH;
        while(offset < buf.length - MAGIC_LENGTH) {
            if(HeaderItem.verifyMagic(buf, offset)) {
                return offset;
            }

            offset++;
        }

        return 0;
    }

    public static List<DexItem> getDexes(byte[] buf) {
        List<DexItem> dexes = new ArrayList<DexItem>();

        int offset = 0x1000;
        for(int i = 0; i < OAT_MAGIC_VALUE.length; i++) {
            if(buf[offset + i] != OAT_MAGIC_VALUE[i])
                return dexes;
        }

        int version = Integer.parseInt(new String(buf, offset + 5, 2));
        int headerIndex = (version == 31)?0:1;
        int numDexes = readUint(buf, offset + OAT_HEADER_OFFSET_NUMDEXES); 
        int keyValueSize = readUint(buf, offset + OAT_HEADER_SIZE[headerIndex] - 4);

        //System.err.printf("Found oat: %d, numdexes: %d, keyValueSize: %d\n", version, numDexes, keyValueSize);

        offset += OAT_HEADER_SIZE[headerIndex] + keyValueSize;

        //System.err.printf("Reading dexes at offset: %08X\n", offset);

        for(int i = 0; i < numDexes; i++) {
            int fileNameSize = readUint(buf, offset);
            offset += 4;
            String name = new String(buf, offset, fileNameSize);
            offset += fileNameSize;
            offset += 4;    // Skip checksum
            int dexOffset = 0x1000 + readUint(buf, offset);
            offset += 4;
            int dexSize = readUint(buf, dexOffset + HeaderItem.FILE_SIZE_OFFSET);

            int classDefsSize = readUint(buf, dexOffset + HeaderItem.CLASS_COUNT_OFFSET);
            offset += classDefsSize * 4;

            //System.err.printf("Found dex: %s at offset %08x, next offset: %08x\n", name, dexOffset, offset);

            dexes.add(new DexItem(name, dexOffset, dexSize));
        }

        return dexes;
    }

    private static int readUint(byte[] buf, int offset) {
        return (buf[offset] & 0xff) |
                ((buf[offset+1] & 0xff) << 8) |
                ((buf[offset+2] & 0xff) << 16) |
                ((buf[offset+3]) << 24);
    }

    public static class DexItem {
        public String name;
        public int offset;
        public int size;

        public DexItem(String name, int offset, int size) {
            this.name = name;
            this.offset = offset;
            this.size = size;
        }
    }
}
