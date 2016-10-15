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

import org.jf.dexlib2.dexbacked.BaseDexBuffer;
import org.jf.dexlib2.dexbacked.raw.util.DexAnnotator;
import org.jf.dexlib2.util.AnnotatedBytes;
import org.jf.util.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class HeaderItem {
    public static final int ITEM_SIZE = 0x70;

    private static final byte[] MAGIC_VALUE = new byte[] { 0x64, 0x65, 0x78, 0x0a, 0x00, 0x00, 0x00, 0x00 };
    private static final int[] SUPPORTED_DEX_VERSIONS = new int[] { 35, 37 };

    public static final int LITTLE_ENDIAN_TAG = 0x12345678;
    public static final int BIG_ENDIAN_TAG = 0x78563412;

    public static final int CHECKSUM_OFFSET = 8;

    // this is the start of the checksumed data
    public static final int CHECKSUM_DATA_START_OFFSET = 12;
    public static final int SIGNATURE_OFFSET = 12;
    public static final int SIGNATURE_SIZE = 20;

    // this is the start of the sha-1 hashed data
    public static final int SIGNATURE_DATA_START_OFFSET = 32;
    public static final int FILE_SIZE_OFFSET = 32;

    public static final int HEADER_SIZE_OFFSET = 36;

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

    @Nonnull private RawDexFile dexFile;

    public HeaderItem(@Nonnull RawDexFile dexFile) {
        this.dexFile = dexFile;
    }

    public int getChecksum() {
        return dexFile.readSmallUint(CHECKSUM_OFFSET);
    }

    @Nonnull public byte[] getSignature() {
        return dexFile.readByteRange(SIGNATURE_OFFSET, SIGNATURE_SIZE);
    }

    public int getMapOffset() {
        return dexFile.readSmallUint(MAP_OFFSET);
    }

    public int getHeaderSize() {
        return dexFile.readSmallUint(HEADER_SIZE_OFFSET);
    }

    public int getStringCount() {
        return dexFile.readSmallUint(STRING_COUNT_OFFSET);
    }

    public int getStringOffset() {
        return dexFile.readSmallUint(STRING_START_OFFSET);
    }

    public int getTypeCount() {
        return dexFile.readSmallUint(TYPE_COUNT_OFFSET);
    }

    public int getTypeOffset() {
        return dexFile.readSmallUint(TYPE_START_OFFSET);
    }

    public int getProtoCount() {
        return dexFile.readSmallUint(PROTO_COUNT_OFFSET);
    }

    public int getProtoOffset() {
        return dexFile.readSmallUint(PROTO_START_OFFSET);
    }

    public int getFieldCount() {
        return dexFile.readSmallUint(FIELD_COUNT_OFFSET);
    }

    public int getFieldOffset() {
        return dexFile.readSmallUint(FIELD_START_OFFSET);
    }

    public int getMethodCount() {
        return dexFile.readSmallUint(METHOD_COUNT_OFFSET);
    }

    public int getMethodOffset() {
        return dexFile.readSmallUint(METHOD_START_OFFSET);
    }

    public int getClassCount() {
        return dexFile.readSmallUint(CLASS_COUNT_OFFSET);
    }

    public int getClassOffset() {
        return dexFile.readSmallUint(CLASS_START_OFFSET);
    }

    @Nonnull
    public static SectionAnnotator makeAnnotator(@Nonnull DexAnnotator annotator, @Nonnull MapItem mapItem) {
        return new SectionAnnotator(annotator, mapItem) {
            @Nonnull @Override public String getItemName() {
                return "header_item";
            }

            @Override
            protected void annotateItem(@Nonnull AnnotatedBytes out, int itemIndex, @Nullable String itemIdentity) {
                int startOffset = out.getCursor();
                int headerSize;

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
                    out.annotateTo(headerSize, "header padding");
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

    /**
     * Get the highest magic number supported by Android for this api level.
     * @return The dex file magic number
     */
    public static byte[] getMagicForApi(int api) {
        if (api < 24) {
            // Prior to Android N we only support dex version 035.
            return getMagicForDexVersion(35);
        } else {
            // On android N and later we support dex version 037.
            return getMagicForDexVersion(37);
        }
    }

    public static byte[] getMagicForDexVersion(int dexVersion) {
        byte[] magic = MAGIC_VALUE.clone();

        if (dexVersion < 0 || dexVersion > 999) {
            throw new IllegalArgumentException("dexVersion must be within [0, 999]");
        }

        for (int i=6; i>=4; i--) {
            int digit = dexVersion % 10;
            magic[i] = (byte)('0' + digit);
            dexVersion /= 10;
        }

        return magic;
    }

    /**
     * Verifies the magic value at the beginning of a dex file
     *
     * @param buf A byte array containing at least the first 8 bytes of a dex file
     * @param offset The offset within the buffer to the beginning of the dex header
     * @return True if the magic value is valid
     */
    public static boolean verifyMagic(byte[] buf, int offset) {
        if (buf.length - offset < 8) {
            return false;
        }

        for (int i=0; i<4; i++) {
            if (buf[offset + i] != MAGIC_VALUE[i]) {
                return false;
            }
        }
        for (int i=4; i<7; i++) {
            if (buf[offset + i] < '0' ||
                    buf[offset + i] > '9') {
                return false;
            }
        }
        if (buf[offset + 7] != MAGIC_VALUE[7]) {
            return false;
        }

        return true;
    }

    /**
     * Gets the dex version from a dex header
     *
     * @param buf A byte array containing at least the first 7 bytes of a dex file
     * @param offset The offset within the buffer to the beginning of the dex header
     * @return The dex version if the header is valid or -1 if the header is invalid
     */
    public static int getVersion(byte[] buf, int offset) {
        if (!verifyMagic(buf, offset)) {
            return -1;
        }

        return getVersionUnchecked(buf, offset);
    }

    private static int getVersionUnchecked(byte[] buf, int offset) {
        int version = (buf[offset + 4] - '0') * 100;
        version += (buf[offset + 5] - '0') * 10;
        version += buf[offset + 6] - '0';

        return version;
    }

    public static boolean isSupportedDexVersion(int version) {
        for (int i=0; i<SUPPORTED_DEX_VERSIONS.length; i++) {
            if (SUPPORTED_DEX_VERSIONS[i] == version) {
                return true;
            }
        }
        return false;
    }

    public static int getEndian(byte[] buf, int offset) {
        BaseDexBuffer bdb = new BaseDexBuffer(buf);
        return bdb.readInt(offset + ENDIAN_TAG_OFFSET);
    }
}
