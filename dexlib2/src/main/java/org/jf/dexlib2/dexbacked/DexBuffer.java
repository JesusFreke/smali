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

package org.jf.dexlib2.dexbacked;

import org.jf.dexlib2.ReferenceType;
import org.jf.util.ExceptionWithContext;
import org.jf.util.Utf8Utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DexBuffer {
    // TODO: consider using a direct ByteBuffer instead
    @Nonnull /* package private */ final byte[] buf;
    private final int stringCount;
    private final int stringStartOffset;
    private final int typeCount;
    private final int typeStartOffset;
    private final int protoCount;
    private final int protoStartOffset;
    private final int fieldCount;
    private final int fieldStartOffset;
    private final int methodCount;
    private final int methodStartOffset;
    private final int classCount;
    private final int classStartOffset;

    private static final byte[][] MAGIC_VALUES= new byte[][] {
            new byte[]{0x64, 0x65, 0x78, 0x0a, 0x30, 0x33, 0x35, 0x00},
            new byte[]{0x64, 0x65, 0x78, 0x0a, 0x30, 0x33, 0x36, 0x00}};

    private static final int LITTLE_ENDIAN_TAG = 0x12345678;
    private static final int BIG_ENDIAN_TAG = 0x78563412;

    private static final int ENDIAN_TAG_OFFSET = 40;
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

    private static final int STRING_ID_ITEM_SIZE = 4;
    private static final int TYPE_ID_ITEM_SIZE = 4;
    private static final int PROTO_ID_ITEM_SIZE = 12;
    private static final int FIELD_ID_ITEM_SIZE = 8;
    private static final int METHOD_ID_ITEM_SIZE = 8;
    private static final int CLASS_DEF_ITEM_SIZE = 32;

    private static final int FIELD_CLASS_IDX_OFFSET = 0;
    private static final int FIELD_TYPE_IDX_OFFSET = 2;
    private static final int FIELD_NAME_IDX_OFFSET = 4;

    private static final int METHOD_CLASS_IDX_OFFSET = 0;
    private static final int METHOD_PROTO_IDX_OFFSET = 2;
    private static final int METHOD_NAME_IDX_OFFSET = 4;

    private static final int PROTO_RETURN_TYPE_IDX_OFFSET = 4;
    private static final int PROTO_PARAM_LIST_OFF_OFFSET = 8;

    private static final int TYPE_LIST_SIZE_OFFSET = 0;
    private static final int TYPE_LIST_LIST_OFFSET = 4;


    protected DexBuffer(@Nonnull byte[] buf, boolean bare) {
        this.buf = buf;

        if (!bare) {
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
        } else {
            stringCount = 0;
            stringStartOffset = 0;
            typeCount = 0;
            typeStartOffset = 0;
            protoCount = 0;
            protoStartOffset = 0;
            fieldCount = 0;
            fieldStartOffset = 0;
            methodCount = 0;
            methodStartOffset = 0;
            classCount = 0;
            classStartOffset = 0;
        }
    }

    public DexBuffer(@Nonnull byte[] buf) {
        this(buf, false);
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
        int stringOffset = getStringIdItemOffset(stringIndex);
        int stringDataOffset = readSmallUint(stringOffset);
        DexReader reader = readerAt(stringDataOffset);
        int utf16Length = reader.readSmallUleb128();
        return Utf8Utils.utf8BytesWithUtf16LengthToString(buf, reader.getOffset(), utf16Length);
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

    @Nonnull
    public String getField(int fieldIndex) {
        int fieldOffset = getFieldIdItemOffset(fieldIndex);
        String className = getType(readUshort(fieldOffset + FIELD_CLASS_IDX_OFFSET));
        String fieldType = getType(readUshort(fieldOffset + FIELD_TYPE_IDX_OFFSET));
        String fieldName = getString(readUshort(fieldOffset + FIELD_NAME_IDX_OFFSET));

        StringBuilder sb = localStringBuilder.get();
        sb.setLength(0);
        sb.append(className);
        sb.append("->");
        sb.append(fieldName);
        sb.append(":");
        sb.append(fieldType);
        return sb.toString();
    }

    @Nonnull
    public String getMethod(int methodIndex) {
        int methodOffset = getMethodIdItemOffset(methodIndex);
        String className = getType(readUshort(methodOffset + METHOD_CLASS_IDX_OFFSET));
        String methodName = getString(readSmallUint(methodOffset + METHOD_NAME_IDX_OFFSET));

        int protoOffset = getProtoIdItemOffset(readUshort(methodOffset + METHOD_PROTO_IDX_OFFSET));
        String returnType = getType(readSmallUint(protoOffset + PROTO_RETURN_TYPE_IDX_OFFSET));
        int parametersOffset = readSmallUint(protoOffset + PROTO_PARAM_LIST_OFF_OFFSET);

        StringBuilder sb = localStringBuilder.get();
        sb.setLength(0);
        sb.append(className);
        sb.append("->");
        sb.append(methodName);
        sb.append("(");

        if (parametersOffset > 0) {
            int parameterCount = readSmallUint(parametersOffset + TYPE_LIST_SIZE_OFFSET);
            int endOffset = parametersOffset + TYPE_LIST_LIST_OFFSET + parameterCount*2;

            for (int off=parametersOffset+TYPE_LIST_LIST_OFFSET; off<endOffset; off+=2) {
                int parameterTypeIndex = readUshort(off);
                sb.append(getType(parameterTypeIndex));
            }
        }

        sb.append(")");
        sb.append(returnType);
        return sb.toString();
    }

    @Nonnull
    public String getReference(int referenceType, int referenceIndex) {
        switch (referenceType) {
            case ReferenceType.STRING:
                return getString(referenceIndex);
            case ReferenceType.TYPE:
                return getType(referenceIndex);
            case ReferenceType.FIELD:
                return getField(referenceIndex);
            case ReferenceType.METHOD:
                return getMethod(referenceIndex);
            default:
                throw new ExceptionWithContext("Invalid reference type: %d", referenceType);
        }
    }

    public int readSmallUint(int offset) {
        byte[] buf = this.buf;
        int result = (buf[offset] & 0xff) |
                     ((buf[offset+1] & 0xff) << 8) |
                     ((buf[offset+2] & 0xff) << 16) |
                     ((buf[offset+3]) << 24);
        if (result < 0) {
            throw new ExceptionWithContext("Encountered small uint that is out of range at offset 0x%x", offset);
        }
        return result;
    }

    public int readOptionalUint(int offset) {
        byte[] buf = this.buf;
        int result = (buf[offset] & 0xff) |
                ((buf[offset+1] & 0xff) << 8) |
                ((buf[offset+2] & 0xff) << 16) |
                ((buf[offset+3]) << 24);
        if (result < -1) {
            throw new ExceptionWithContext("Encountered optional uint that is out of range at offset 0x%x", offset);
        }
        return result;
    }

    public int readUshort(int offset) {
        byte[] buf = this.buf;
        return (buf[offset] & 0xff) |
               ((buf[offset+1] & 0xff) << 8);
    }

    public int readUbyte(int offset) {
        return buf[offset] & 0xff;
    }

    public long readLong(int offset) {
        // TODO: use | or +?
        byte[] buf = this.buf;
        return (buf[offset] & 0xff) |
               ((buf[offset+1] & 0xff) << 8) |
               ((buf[offset+2] & 0xff) << 16) |
               ((buf[offset+3] & 0xffL) << 24) |
               ((buf[offset+4] & 0xffL) << 32) |
               ((buf[offset+5] & 0xffL) << 40) |
               ((buf[offset+6] & 0xffL) << 48) |
               (((long)buf[offset+7]) << 56);
    }

    public int readInt(int offset) {
        byte[] buf = this.buf;
        return (buf[offset] & 0xff) |
               ((buf[offset+1] & 0xff) << 8) |
               ((buf[offset+2] & 0xff) << 16) |
               (buf[offset+3] << 24);
    }

    public int readShort(int offset) {
        byte[] buf = this.buf;
        return (buf[offset] & 0xff) |
               (buf[offset+1] << 8);
    }

    public int readByte(int offset) {
        return buf[offset];
    }

    @Nonnull
    public DexReader readerAt(int offset) {
        return new DexReader(this, offset);
    }

    private final ThreadLocal<StringBuilder> localStringBuilder = new ThreadLocal<StringBuilder>() {
        @Override protected StringBuilder initialValue() { return new StringBuilder(256); }
    };
}
