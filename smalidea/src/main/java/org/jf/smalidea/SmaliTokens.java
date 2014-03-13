/*
 * Copyright 2014, Google Inc.
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

package org.jf.smalidea;

import com.intellij.psi.tree.IElementType;
import org.jf.smali.smaliParser;

import java.lang.reflect.Field;

public class SmaliTokens {
    private static final IElementType[] ELEMENT_TYPES;

    public static IElementType getElementType(int tokenType) {
        return ELEMENT_TYPES[tokenType];
    }

    public static IElementType ACCESS_SPEC;
    public static IElementType ANNOTATION_DIRECTIVE;
    public static IElementType ANNOTATION_VISIBILITY;
    public static IElementType ARRAY_DATA_DIRECTIVE;
    public static IElementType ARRAY_DESCRIPTOR;
    public static IElementType ARROW;
    public static IElementType BOOL_LITERAL;
    public static IElementType BYTE_LITERAL;
    public static IElementType CATCH_DIRECTIVE;
    public static IElementType CATCHALL_DIRECTIVE;
    public static IElementType CHAR_LITERAL;
    public static IElementType CLASS_DESCRIPTOR;
    public static IElementType CLASS_DIRECTIVE;
    public static IElementType CLOSE_BRACE;
    public static IElementType CLOSE_PAREN;
    public static IElementType COLON;
    public static IElementType COMMA;
    public static IElementType DOTDOT;
    public static IElementType DOUBLE_LITERAL;
    public static IElementType DOUBLE_LITERAL_OR_ID;
    public static IElementType END_ANNOTATION_DIRECTIVE;
    public static IElementType END_ARRAY_DATA_DIRECTIVE;
    public static IElementType END_FIELD_DIRECTIVE;
    public static IElementType END_LOCAL_DIRECTIVE;
    public static IElementType END_METHOD_DIRECTIVE;
    public static IElementType END_PACKED_SWITCH_DIRECTIVE;
    public static IElementType END_PARAMETER_DIRECTIVE;
    public static IElementType END_SPARSE_SWITCH_DIRECTIVE;
    public static IElementType END_SUBANNOTATION_DIRECTIVE;
    public static IElementType ENUM_DIRECTIVE;
    public static IElementType EPILOGUE_DIRECTIVE;
    public static IElementType EQUAL;
    public static IElementType FIELD_DIRECTIVE;
    public static IElementType FIELD_OFFSET;
    public static IElementType FLOAT_LITERAL;
    public static IElementType FLOAT_LITERAL_OR_ID;
    public static IElementType IMPLEMENTS_DIRECTIVE;
    public static IElementType INLINE_INDEX;
    public static IElementType INSTRUCTION_FORMAT10t;
    public static IElementType INSTRUCTION_FORMAT10x;
    public static IElementType INSTRUCTION_FORMAT10x_ODEX;
    public static IElementType INSTRUCTION_FORMAT11n;
    public static IElementType INSTRUCTION_FORMAT11x;
    public static IElementType INSTRUCTION_FORMAT12x;
    public static IElementType INSTRUCTION_FORMAT12x_OR_ID;
    public static IElementType INSTRUCTION_FORMAT20bc;
    public static IElementType INSTRUCTION_FORMAT20t;
    public static IElementType INSTRUCTION_FORMAT21c_FIELD;
    public static IElementType INSTRUCTION_FORMAT21c_FIELD_ODEX;
    public static IElementType INSTRUCTION_FORMAT21c_STRING;
    public static IElementType INSTRUCTION_FORMAT21c_TYPE;
    public static IElementType INSTRUCTION_FORMAT21ih;
    public static IElementType INSTRUCTION_FORMAT21lh;
    public static IElementType INSTRUCTION_FORMAT21s;
    public static IElementType INSTRUCTION_FORMAT21t;
    public static IElementType INSTRUCTION_FORMAT22b;
    public static IElementType INSTRUCTION_FORMAT22c_FIELD;
    public static IElementType INSTRUCTION_FORMAT22c_FIELD_ODEX;
    public static IElementType INSTRUCTION_FORMAT22c_TYPE;
    public static IElementType INSTRUCTION_FORMAT22cs_FIELD;
    public static IElementType INSTRUCTION_FORMAT22s;
    public static IElementType INSTRUCTION_FORMAT22s_OR_ID;
    public static IElementType INSTRUCTION_FORMAT22t;
    public static IElementType INSTRUCTION_FORMAT22x;
    public static IElementType INSTRUCTION_FORMAT23x;
    public static IElementType INSTRUCTION_FORMAT30t;
    public static IElementType INSTRUCTION_FORMAT31c;
    public static IElementType INSTRUCTION_FORMAT31i;
    public static IElementType INSTRUCTION_FORMAT31i_OR_ID;
    public static IElementType INSTRUCTION_FORMAT31t;
    public static IElementType INSTRUCTION_FORMAT32x;
    public static IElementType INSTRUCTION_FORMAT35c_METHOD;
    public static IElementType INSTRUCTION_FORMAT35c_METHOD_ODEX;
    public static IElementType INSTRUCTION_FORMAT35c_TYPE;
    public static IElementType INSTRUCTION_FORMAT35mi_METHOD;
    public static IElementType INSTRUCTION_FORMAT35ms_METHOD;
    public static IElementType INSTRUCTION_FORMAT3rc_METHOD;
    public static IElementType INSTRUCTION_FORMAT3rc_METHOD_ODEX;
    public static IElementType INSTRUCTION_FORMAT3rc_TYPE;
    public static IElementType INSTRUCTION_FORMAT3rmi_METHOD;
    public static IElementType INSTRUCTION_FORMAT3rms_METHOD;
    public static IElementType INSTRUCTION_FORMAT51l;
    public static IElementType LINE_COMMENT;
    public static IElementType LINE_DIRECTIVE;
    public static IElementType LOCAL_DIRECTIVE;
    public static IElementType LOCALS_DIRECTIVE;
    public static IElementType LONG_LITERAL;
    public static IElementType METHOD_DIRECTIVE;
    public static IElementType MEMBER_NAME;
    public static IElementType NEGATIVE_INTEGER_LITERAL;
    public static IElementType NULL_LITERAL;
    public static IElementType OPEN_BRACE;
    public static IElementType OPEN_PAREN;
    public static IElementType PACKED_SWITCH_DIRECTIVE;
    public static IElementType PARAM_LIST_END;
    public static IElementType PARAM_LIST_START;
    public static IElementType PARAM_LIST_OR_ID_END;
    public static IElementType PARAM_LIST_OR_ID_START;
    public static IElementType PARAMETER_DIRECTIVE;
    public static IElementType POSITIVE_INTEGER_LITERAL;
    public static IElementType PRIMITIVE_TYPE;
    public static IElementType PROLOGUE_DIRECTIVE;
    public static IElementType REGISTER;
    public static IElementType REGISTERS_DIRECTIVE;
    public static IElementType RESTART_LOCAL_DIRECTIVE;
    public static IElementType SHORT_LITERAL;
    public static IElementType SIMPLE_NAME;
    public static IElementType SOURCE_DIRECTIVE;
    public static IElementType SPARSE_SWITCH_DIRECTIVE;
    public static IElementType STRING_LITERAL;
    public static IElementType SUBANNOTATION_DIRECTIVE;
    public static IElementType SUPER_DIRECTIVE;
    public static IElementType VERIFICATION_ERROR_TYPE;
    public static IElementType VOID_TYPE;
    public static IElementType VTABLE_INDEX;

    static {
        int tokenCount = smaliParser.tokenNames.length;
        ELEMENT_TYPES = new IElementType[tokenCount];

        for (int tokenId=0; tokenId<tokenCount; tokenId++) {
            String tokenName = smaliParser.tokenNames[tokenId];
            Field field;

            try {
                field = SmaliTokens.class.getField(tokenName);
            } catch (NoSuchFieldException ex) {
                continue;
            }

            IElementType elementType = new IElementType(tokenName, SmaliLanguage.INSTANCE);
            ELEMENT_TYPES[tokenId] = elementType;

            try {
                field.set(null, elementType);
            } catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
