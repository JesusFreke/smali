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

import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.util.AnnotatedBytes;
import org.jf.util.StringUtils;

import javax.annotation.Nonnull;

public class HeaderItem {
    public static final int ITEM_SIZE = 0x70;

    public static final byte[][] MAGIC_VALUES= new byte[][] {
            new byte[]{0x64, 0x65, 0x78, 0x0a, 0x30, 0x33, 0x35, 0x00},
            new byte[]{0x64, 0x65, 0x78, 0x0a, 0x30, 0x33, 0x36, 0x00}};

    public static final int LITTLE_ENDIAN_TAG = 0x12345678;
    public static final int BIG_ENDIAN_TAG = 0x78563412;

    public static final int CHECKSUM_OFFSET = 8;

    public static final int SIGNATURE_OFFSET = 12;
    public static final int ENDIAN_TAG_OFFSET = 40;

    public static final int MAP_OFFSET = 52;

    public static final int STRING_COUNT_OFFSET = 56;
    public static final int STRING_START_OFFSET = 60;

    public static final int TYPE_COUNT_OFFSET = 64;
    public static final int TYPE_START_OFFSET = 68;

    public static final int PROTO_COUNT_OFFSET = 72;
    public static final int PROTO_START_OFFSET = 76;

    public static final int FIELD_COUNT_OFFSET = 80;
    public static final int FIELD_START_OFFSET = 84;

    public static final int METHOD_COUNT_OFFSET = 88;
    public static final int METHOD_START_OFFSET = 92;

    public static final int CLASS_COUNT_OFFSET = 96;
    public static final int CLASS_START_OFFSET = 100;

    public static final int SIGNATURE_SIZE = 20;

    public static Section getSection() {
        return new Section() {
            @Override
            public void annotateSection(@Nonnull AnnotatedBytes out, @Nonnull DexBackedDexFile dexFile, int length) {
                int startOffset = out.getCursor();
                int headerSize;

                out.annotate(0, "-----------------------------");
                out.annotate(0, "header item");
                out.annotate(0, "-----------------------------");
                out.annotate(0, "");

                StringBuilder magicBuilder = new StringBuilder();
                for (int i=0; i<8; i++) {
                    magicBuilder.append((char)dexFile.readUbyte(startOffset + i));
                }

                out.annotate(8, "magic: %s", StringUtils.escapeString(magicBuilder.toString()));
                out.annotate(4, "checksum");
                out.annotate(20, "signature");
                out.annotate(4, "file_size: %d", dexFile.readInt(out.getCursor()));

                headerSize = dexFile.readInt(out.getCursor());
                out.annotate(4, "header_size: %d", headerSize);

                int endianTag = dexFile.readInt(out.getCursor());
                out.annotate(4, "endian_tag: 0x%x (%s)", endianTag, getEndianText(endianTag));

                out.annotate(4, "link_size: %d", dexFile.readInt(out.getCursor()));
                out.annotate(4, "link_offset: 0x%x", dexFile.readInt(out.getCursor()));

                out.annotate(4, "map_off: 0x%x", dexFile.readInt(out.getCursor()));

                out.annotate(4, "string_ids_size: %d", dexFile.readInt(out.getCursor()));
                out.annotate(4, "string_ids_off: 0x%x", dexFile.readInt(out.getCursor()));

                out.annotate(4, "type_ids_size: %d", dexFile.readInt(out.getCursor()));
                out.annotate(4, "type_ids_off: 0x%x", dexFile.readInt(out.getCursor()));

                out.annotate(4, "proto_ids_size: %d", dexFile.readInt(out.getCursor()));
                out.annotate(4, "proto_ids_off: 0x%x", dexFile.readInt(out.getCursor()));

                out.annotate(4, "field_ids_size: %d", dexFile.readInt(out.getCursor()));
                out.annotate(4, "field_ids_off: 0x%x", dexFile.readInt(out.getCursor()));

                out.annotate(4, "method_ids_size: %d", dexFile.readInt(out.getCursor()));
                out.annotate(4, "method_ids_off: 0x%x", dexFile.readInt(out.getCursor()));

                out.annotate(4, "class_defs_size: %d", dexFile.readInt(out.getCursor()));
                out.annotate(4, "class_defs_off: 0x%x", dexFile.readInt(out.getCursor()));

                out.annotate(4, "data_size: %d", dexFile.readInt(out.getCursor()));
                out.annotate(4, "data_off: 0x%x", dexFile.readInt(out.getCursor()));

                if (headerSize > ITEM_SIZE) {
                    out.annotate(headerSize - ITEM_SIZE, "header padding");
                }
            }
        };
    }

    private static String getEndianText(int endianTag) {
        if (endianTag == LITTLE_ENDIAN_TAG) {
            return "Little Endian";
        }
        if (endianTag == BIG_ENDIAN_TAG) {
            return "Big Endian";
        }
        return "Invalid";
    }
}
