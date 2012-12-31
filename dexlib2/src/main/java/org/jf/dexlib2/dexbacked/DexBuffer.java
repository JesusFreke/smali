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

import org.jf.util.ExceptionWithContext;
import org.jf.util.Utf8Utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;

public class DexBuffer extends BaseDexBuffer {
    public final int stringCount;
    public final int stringStartOffset;
    public final int typeCount;
    public final int typeStartOffset;
    public final int protoCount;
    public final int protoStartOffset;
    public final int fieldCount;
    public final int fieldStartOffset;
    public final int methodCount;
    public final int methodStartOffset;
    public final int classCount;
    public final int classStartOffset;

    @Nonnull private final String[] stringCache;

    private static final byte[][] MAGIC_VALUES= new byte[][] {
            new byte[]{0x64, 0x65, 0x78, 0x0a, 0x30, 0x33, 0x35, 0x00},
            new byte[]{0x64, 0x65, 0x78, 0x0a, 0x30, 0x33, 0x36, 0x00}};

    private static final int LITTLE_ENDIAN_TAG = 0x12345678;
    private static final int BIG_ENDIAN_TAG = 0x78563412;

    private static final int CHECKSUM_OFFSET = 8;
    private static final int SIGNATURE_OFFSET = 12;
    private static final int ENDIAN_TAG_OFFSET = 40;
    private static final int MAP_OFFSET = 52;
    private static final int STRING_COUNT_OFFSET = 56;
    private static final int STRING_START_OFFSET = 60;
    private static final int TYPE_COUNT_OFFSET = 64;
    private static final int TYPE_START_OFFSET = 68;
    private static final int PROTO_COUNT_OFFSET = 72;
    private static final int PROTO_START_OFFSET = 76;
    private static final int FIELD_COUNT_OFFSET = 80;
    private static final int FIELD_START_OFFSET = 84;
    private static final int METHOD_COUNT_OFFSET = 88;
    private static final int METHOD_START_OFFSET = 92;
    private static final int CLASS_COUNT_OFFSET = 96;
    private static final int CLASS_START_OFFSET = 100;

    private static final int SIGNATURE_SIZE = 20;

    private static final int STRING_ID_ITEM_SIZE = 4;
    private static final int TYPE_ID_ITEM_SIZE = 4;
    private static final int PROTO_ID_ITEM_SIZE = 12;
    private static final int FIELD_ID_ITEM_SIZE = 8;
    private static final int METHOD_ID_ITEM_SIZE = 8;
    private static final int CLASS_DEF_ITEM_SIZE = 32;
    public static final int MAP_ITEM_SIZE = 12;

    public static final int FIELD_CLASS_IDX_OFFSET = 0;
    public static final int FIELD_TYPE_IDX_OFFSET = 2;
    public static final int FIELD_NAME_IDX_OFFSET = 4;

    public static final int METHOD_CLASS_IDX_OFFSET = 0;
    public static final int METHOD_PROTO_IDX_OFFSET = 2;
    public static final int METHOD_NAME_IDX_OFFSET = 4;

    public static final int PROTO_RETURN_TYPE_IDX_OFFSET = 4;
    public static final int PROTO_PARAM_LIST_OFF_OFFSET = 8;

    public static final int TYPE_LIST_SIZE_OFFSET = 0;
    public static final int TYPE_LIST_LIST_OFFSET = 4;

    public DexBuffer(@Nonnull byte[] buf) {
        super(buf);

        verifyMagic();
        verifyEndian();
        stringCount = readSmallUint(STRING_COUNT_OFFSET);
        stringStartOffset = readSmallUint(STRING_START_OFFSET);
        typeCount = readSmallUint(TYPE_COUNT_OFFSET);
        typeStartOffset = readSmallUint(TYPE_START_OFFSET);
        protoCount = readSmallUint(PROTO_COUNT_OFFSET);
        protoStartOffset = readSmallUint(PROTO_START_OFFSET);
        fieldCount = readSmallUint(FIELD_COUNT_OFFSET);
        fieldStartOffset = readSmallUint(FIELD_START_OFFSET);
        methodCount = readSmallUint(METHOD_COUNT_OFFSET);
        methodStartOffset = readSmallUint(METHOD_START_OFFSET);
        classCount = readSmallUint(CLASS_COUNT_OFFSET);
        classStartOffset = readSmallUint(CLASS_START_OFFSET);

        stringCache = new String[stringCount];
    }

    private void verifyMagic() {
        outer: for (byte[] magic: MAGIC_VALUES) {
            for (int i=0; i<magic.length; i++) {
                if (buf[i] != magic[i]) {
                    continue outer;
                }
            }
            return;
        }
        StringBuilder sb = new StringBuilder("Invalid magic value:");
        for (int i=0; i<8; i++) {
            sb.append(String.format(" %02x", buf[i]));
        }
        throw new ExceptionWithContext(sb.toString());
    }

    private void verifyEndian() {
        int endian = readInt(ENDIAN_TAG_OFFSET);
        if (endian == BIG_ENDIAN_TAG) {
            throw new ExceptionWithContext("dexlib does not currently support big endian dex files.");
        } else if (endian != LITTLE_ENDIAN_TAG) {
            StringBuilder sb = new StringBuilder("Invalid endian tag:");
            for (int i=0; i<4; i++) {
                sb.append(String.format(" %02x", buf[ENDIAN_TAG_OFFSET+i]));
            }
            throw new ExceptionWithContext(sb.toString());
        }
    }

    public int getChecksum() {
        return readInt(CHECKSUM_OFFSET);
    }

    public byte[] getSignature() {
        return Arrays.copyOfRange(this.buf, SIGNATURE_OFFSET, SIGNATURE_OFFSET + SIGNATURE_SIZE);
    }

    public int getMapOffset() {
        return readSmallUint(MAP_OFFSET);
    }

    public int getStringIdItemOffset(int stringIndex) {
        if (stringIndex < 0 || stringIndex >= stringCount) {
            throw new ExceptionWithContext("String index out of bounds: %d", stringIndex);
        }
        return stringStartOffset + stringIndex*STRING_ID_ITEM_SIZE;
    }

    public int getTypeIdItemOffset(int typeIndex) {
        if (typeIndex < 0 || typeIndex >= typeCount) {
            throw new ExceptionWithContext("Type index out of bounds: %d", typeIndex);
        }
        return typeStartOffset + typeIndex*TYPE_ID_ITEM_SIZE;
    }

    public int getFieldIdItemOffset(int fieldIndex) {
        if (fieldIndex < 0 || fieldIndex >= fieldCount) {
            throw new ExceptionWithContext("Field index out of bounds: %d", fieldIndex);
        }
        return fieldStartOffset + fieldIndex*FIELD_ID_ITEM_SIZE;
    }

    public int getMethodIdItemOffset(int methodIndex) {
        if (methodIndex < 0 || methodIndex >= methodCount) {
            throw new ExceptionWithContext("Method index out of bounds: %d", methodIndex);
        }
        return methodStartOffset + methodIndex*METHOD_ID_ITEM_SIZE;
    }

    public int getProtoIdItemOffset(int protoIndex) {
        if (protoIndex < 0 || protoIndex >= protoCount) {
            throw new ExceptionWithContext("Proto index out of bounds: %d", protoIndex);
        }
        return protoStartOffset + protoIndex*PROTO_ID_ITEM_SIZE;
    }

    public int getClassDefItemOffset(int classIndex) {
        if (classIndex < 0 || classIndex >= classCount) {
            throw new ExceptionWithContext("Class index out of bounds: %d", classIndex);
        }
        return classStartOffset + classIndex*CLASS_DEF_ITEM_SIZE;
    }

    public int getClassCount() {
        return classCount;
    }

    @Nonnull
    public String getString(int stringIndex) {
        String ret = stringCache[stringIndex];
        if (ret == null) {
            int stringOffset = getStringIdItemOffset(stringIndex);
            int stringDataOffset = readSmallUint(stringOffset);
            DexReader reader = readerAt(stringDataOffset);
            int utf16Length = reader.readSmallUleb128();
            ret = Utf8Utils.utf8BytesWithUtf16LengthToString(buf, reader.getOffset(), utf16Length);
            stringCache[stringIndex] = ret;
        }
        return ret;
    }

    @Nullable
    public String getOptionalType(int typeIndex) {
        if (typeIndex == -1) {
            return null;
        }
        return getType(typeIndex);
    }

    @Nullable
    public String getOptionalString(int stringIndex) {
        if (stringIndex == -1) {
            return null;
        }
        return getString(stringIndex);
    }

    @Nonnull
    public String getType(int typeIndex) {
        int typeOffset = getTypeIdItemOffset(typeIndex);
        int stringIndex = readSmallUint(typeOffset);
        return getString(stringIndex);
    }

    @Override
    @Nonnull
    public DexReader readerAt(int offset) {
        return new DexReader(this, offset);
    }
}
