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

package org.jf.dexlib2.writer;

public class DexItemType {
    public static final int HEADER_ITEM = 0x0000;
    public static final int STRING_ID_ITEM = 0x0001;
    public static final int TYPE_ID_ITEM = 0x0002;
    public static final int PROTO_ID_ITEM = 0x0003;
    public static final int FIELD_ID_ITEM = 0x0004;
    public static final int METHOD_ID_ITEM = 0x0005;
    public static final int CLASS_DEF_ITEM = 0x0006;
    public static final int MAP_LIST = 0x1000;
    public static final int TYPE_LIST = 0x1001;
    public static final int ANNOTATION_SET_REF_LIST = 0x1002;
    public static final int ANNOTATION_SET_ITEM = 0x1003;
    public static final int CLASS_DATA_ITEM = 0x2000;
    public static final int CODE_ITEM = 0x2001;
    public static final int STRING_DATA_ITEM = 0x2002;
    public static final int DEBUG_INFO_ITEM = 0x2003;
    public static final int ANNOTATION_ITEM = 0x2004;
    public static final int ENCODED_ARRAY_ITEM = 0x2005;
    public static final int ANNOTATION_DIRECTORY_ITEM = 0x2006;

    public static String getItemTypeName(int itemType) {
        switch (itemType) {
            case HEADER_ITEM: return "HEADER_ITEM";
            case STRING_ID_ITEM: return "STRING_ID_ITEM";
            case TYPE_ID_ITEM: return "TYPE_ID_ITEM";
            case PROTO_ID_ITEM: return "PROTO_ID_ITEM";
            case FIELD_ID_ITEM: return "FIELD_ID_ITEM";
            case METHOD_ID_ITEM: return "METHOD_ID_ITEM";
            case CLASS_DEF_ITEM: return "CLASS_DEF_ITEM";
            case MAP_LIST: return "MAP_LIST";
            case TYPE_LIST: return "TYPE_LIST";
            case ANNOTATION_SET_REF_LIST: return "ANNOTATION_SET_REF_LIST";
            case ANNOTATION_SET_ITEM: return "ANNOTATION_SET_ITEM";
            case CLASS_DATA_ITEM: return "CLASS_DATA_ITEM";
            case CODE_ITEM: return "CODE_ITEM";
            case STRING_DATA_ITEM: return "STRING_DATA_ITEM";
            case DEBUG_INFO_ITEM: return "DEBUG_INFO_ITEM";
            case ANNOTATION_ITEM: return "ANNOTATION_ITEM";
            case ENCODED_ARRAY_ITEM: return "ENCODED_ARRAY_ITEM";
            case ANNOTATION_DIRECTORY_ITEM: return "ANNOTATION_DIRECTORY_ITEM";
            default: return "Unknown dex item type";
        }
    }
}
