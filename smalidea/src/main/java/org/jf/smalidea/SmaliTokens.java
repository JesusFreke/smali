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

import com.google.common.collect.Maps;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jf.smali.smaliParser;

import java.lang.reflect.Field;
import java.util.Map;

public class SmaliTokens {
    private static final IElementType[] ELEMENT_TYPES;

    public static IElementType getElementType(int tokenType) {
        return ELEMENT_TYPES[tokenType];
    }

    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType ACCESS_SPEC;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType ANNOTATION_DIRECTIVE;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType ANNOTATION_VISIBILITY;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType ARRAY_DATA_DIRECTIVE;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType ARRAY_TYPE_PREFIX;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType ARROW;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType BOOL_LITERAL;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType BYTE_LITERAL;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType CATCH_DIRECTIVE;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType CATCHALL_DIRECTIVE;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType CHAR_LITERAL;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType CLASS_DESCRIPTOR;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType CLASS_DIRECTIVE;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType CLOSE_BRACE;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType CLOSE_PAREN;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType COLON;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType COMMA;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType DOTDOT;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType DOUBLE_LITERAL;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType DOUBLE_LITERAL_OR_ID;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType END_ANNOTATION_DIRECTIVE;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType END_ARRAY_DATA_DIRECTIVE;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType END_FIELD_DIRECTIVE;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType END_LOCAL_DIRECTIVE;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType END_METHOD_DIRECTIVE;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType END_PACKED_SWITCH_DIRECTIVE;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType END_PARAMETER_DIRECTIVE;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType END_SPARSE_SWITCH_DIRECTIVE;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType END_SUBANNOTATION_DIRECTIVE;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType ENUM_DIRECTIVE;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType EPILOGUE_DIRECTIVE;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType EQUAL;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType FIELD_DIRECTIVE;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType FIELD_OFFSET;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType FLOAT_LITERAL;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType FLOAT_LITERAL_OR_ID;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType IMPLEMENTS_DIRECTIVE;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType INLINE_INDEX;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType INSTRUCTION_FORMAT10t;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType INSTRUCTION_FORMAT10x;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType INSTRUCTION_FORMAT10x_ODEX;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType INSTRUCTION_FORMAT11n;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType INSTRUCTION_FORMAT11x;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType INSTRUCTION_FORMAT12x;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType INSTRUCTION_FORMAT12x_OR_ID;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType INSTRUCTION_FORMAT20bc;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType INSTRUCTION_FORMAT20t;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType INSTRUCTION_FORMAT21c_FIELD;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType INSTRUCTION_FORMAT21c_FIELD_ODEX;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType INSTRUCTION_FORMAT21c_STRING;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType INSTRUCTION_FORMAT21c_TYPE;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType INSTRUCTION_FORMAT21ih;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType INSTRUCTION_FORMAT21lh;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType INSTRUCTION_FORMAT21s;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType INSTRUCTION_FORMAT21t;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType INSTRUCTION_FORMAT22b;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType INSTRUCTION_FORMAT22c_FIELD;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType INSTRUCTION_FORMAT22c_FIELD_ODEX;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType INSTRUCTION_FORMAT22c_TYPE;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType INSTRUCTION_FORMAT22cs_FIELD;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType INSTRUCTION_FORMAT22s;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType INSTRUCTION_FORMAT22s_OR_ID;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType INSTRUCTION_FORMAT22t;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType INSTRUCTION_FORMAT22x;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType INSTRUCTION_FORMAT23x;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType INSTRUCTION_FORMAT30t;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType INSTRUCTION_FORMAT31c;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType INSTRUCTION_FORMAT31i;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType INSTRUCTION_FORMAT31i_OR_ID;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType INSTRUCTION_FORMAT31t;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType INSTRUCTION_FORMAT32x;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType INSTRUCTION_FORMAT35c_METHOD;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType INSTRUCTION_FORMAT35c_METHOD_ODEX;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType INSTRUCTION_FORMAT35c_TYPE;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType INSTRUCTION_FORMAT35mi_METHOD;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType INSTRUCTION_FORMAT35ms_METHOD;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType INSTRUCTION_FORMAT3rc_METHOD;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType INSTRUCTION_FORMAT3rc_METHOD_ODEX;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType INSTRUCTION_FORMAT3rc_TYPE;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType INSTRUCTION_FORMAT3rmi_METHOD;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType INSTRUCTION_FORMAT3rms_METHOD;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType INSTRUCTION_FORMAT51l;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType LINE_COMMENT;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType LINE_DIRECTIVE;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType LOCAL_DIRECTIVE;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType LOCALS_DIRECTIVE;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType LONG_LITERAL;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType METHOD_DIRECTIVE;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType MEMBER_NAME;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType NEGATIVE_INTEGER_LITERAL;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType NULL_LITERAL;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType OPEN_BRACE;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType OPEN_PAREN;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType PACKED_SWITCH_DIRECTIVE;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType PARAM_LIST_OR_ID_PRIMITIVE_TYPE;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType PARAMETER_DIRECTIVE;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType POSITIVE_INTEGER_LITERAL;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType PRIMITIVE_TYPE;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType PROLOGUE_DIRECTIVE;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType REGISTER;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType REGISTERS_DIRECTIVE;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType RESTART_LOCAL_DIRECTIVE;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType SHORT_LITERAL;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType SIMPLE_NAME;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType SOURCE_DIRECTIVE;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType SPARSE_SWITCH_DIRECTIVE;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType STRING_LITERAL;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType SUBANNOTATION_DIRECTIVE;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType SUPER_DIRECTIVE;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType VERIFICATION_ERROR_TYPE;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType VOID_TYPE;
    @SuppressWarnings({"UnusedDeclaration"}) public static IElementType VTABLE_INDEX;

    public static final TokenSet INSTRUCTION_TOKENS;

    static {
        Map<String, TextAttributesKey> tokenColors = Maps.newHashMap();

        tokenColors.put("ACCESS_SPEC", SmaliHighlightingColors.ACCESS);
        tokenColors.put("ANNOTATION_DIRECTIVE", SmaliHighlightingColors.DIRECTIVE);
        tokenColors.put("ANNOTATION_VISIBILITY", SmaliHighlightingColors.ACCESS);
        tokenColors.put("ARRAY_DATA_DIRECTIVE", SmaliHighlightingColors.DIRECTIVE);
        tokenColors.put("ARRAY_TYPE_PREFIX", SmaliHighlightingColors.TYPE);
        tokenColors.put("ARROW", SmaliHighlightingColors.ARROW);
        tokenColors.put("BOOL_LITERAL", SmaliHighlightingColors.LITERAL);
        tokenColors.put("BYTE_LITERAL", SmaliHighlightingColors.NUMBER);
        tokenColors.put("CATCH_DIRECTIVE", SmaliHighlightingColors.DIRECTIVE);
        tokenColors.put("CATCHALL_DIRECTIVE", SmaliHighlightingColors.DIRECTIVE);
        tokenColors.put("CHAR_LITERAL", SmaliHighlightingColors.STRING);
        tokenColors.put("CLASS_DESCRIPTOR", SmaliHighlightingColors.TYPE);
        tokenColors.put("CLASS_DIRECTIVE", SmaliHighlightingColors.DIRECTIVE);
        tokenColors.put("CLOSE_BRACE", SmaliHighlightingColors.BRACES);
        tokenColors.put("CLOSE_PAREN", SmaliHighlightingColors.PARENS);
        tokenColors.put("COLON", SmaliHighlightingColors.COLON);
        tokenColors.put("COMMA", SmaliHighlightingColors.COMMA);
        tokenColors.put("DOTDOT", SmaliHighlightingColors.DOTDOT);
        tokenColors.put("DOUBLE_LITERAL", SmaliHighlightingColors.NUMBER);
        tokenColors.put("DOUBLE_LITERAL_OR_ID", SmaliHighlightingColors.NUMBER);
        tokenColors.put("END_ANNOTATION_DIRECTIVE", SmaliHighlightingColors.DIRECTIVE);
        tokenColors.put("END_ARRAY_DATA_DIRECTIVE", SmaliHighlightingColors.DIRECTIVE);
        tokenColors.put("END_FIELD_DIRECTIVE", SmaliHighlightingColors.DIRECTIVE);
        tokenColors.put("END_LOCAL_DIRECTIVE", SmaliHighlightingColors.DIRECTIVE);
        tokenColors.put("END_METHOD_DIRECTIVE", SmaliHighlightingColors.DIRECTIVE);
        tokenColors.put("END_PACKED_SWITCH_DIRECTIVE", SmaliHighlightingColors.DIRECTIVE);
        tokenColors.put("END_PARAMETER_DIRECTIVE", SmaliHighlightingColors.DIRECTIVE);
        tokenColors.put("END_SPARSE_SWITCH_DIRECTIVE", SmaliHighlightingColors.DIRECTIVE);
        tokenColors.put("END_SUBANNOTATION_DIRECTIVE", SmaliHighlightingColors.DIRECTIVE);
        tokenColors.put("ENUM_DIRECTIVE", SmaliHighlightingColors.DIRECTIVE);
        tokenColors.put("EPILOGUE_DIRECTIVE", SmaliHighlightingColors.DIRECTIVE);
        tokenColors.put("EQUAL", SmaliHighlightingColors.EQUAL);
        tokenColors.put("FIELD_DIRECTIVE", SmaliHighlightingColors.DIRECTIVE);
        tokenColors.put("FIELD_OFFSET", SmaliHighlightingColors.ODEX_REFERENCE);
        tokenColors.put("FLOAT_LITERAL", SmaliHighlightingColors.NUMBER);
        tokenColors.put("FLOAT_LITERAL_OR_ID", SmaliHighlightingColors.NUMBER);
        tokenColors.put("IMPLEMENTS_DIRECTIVE", SmaliHighlightingColors.DIRECTIVE);
        tokenColors.put("INLINE_INDEX", SmaliHighlightingColors.ODEX_REFERENCE);
        tokenColors.put("INSTRUCTION_FORMAT10t", SmaliHighlightingColors.INSTRUCTION);
        tokenColors.put("INSTRUCTION_FORMAT10x", SmaliHighlightingColors.INSTRUCTION);
        tokenColors.put("INSTRUCTION_FORMAT10x_ODEX", SmaliHighlightingColors.INSTRUCTION);
        tokenColors.put("INSTRUCTION_FORMAT11n", SmaliHighlightingColors.INSTRUCTION);
        tokenColors.put("INSTRUCTION_FORMAT11x", SmaliHighlightingColors.INSTRUCTION);
        tokenColors.put("INSTRUCTION_FORMAT12x", SmaliHighlightingColors.INSTRUCTION);
        tokenColors.put("INSTRUCTION_FORMAT12x_OR_ID", SmaliHighlightingColors.INSTRUCTION);
        tokenColors.put("INSTRUCTION_FORMAT20bc", SmaliHighlightingColors.INSTRUCTION);
        tokenColors.put("INSTRUCTION_FORMAT20t", SmaliHighlightingColors.INSTRUCTION);
        tokenColors.put("INSTRUCTION_FORMAT21c_FIELD", SmaliHighlightingColors.INSTRUCTION);
        tokenColors.put("INSTRUCTION_FORMAT21c_FIELD_ODEX", SmaliHighlightingColors.INSTRUCTION);
        tokenColors.put("INSTRUCTION_FORMAT21c_STRING", SmaliHighlightingColors.INSTRUCTION);
        tokenColors.put("INSTRUCTION_FORMAT21c_TYPE", SmaliHighlightingColors.INSTRUCTION);
        tokenColors.put("INSTRUCTION_FORMAT21ih", SmaliHighlightingColors.INSTRUCTION);
        tokenColors.put("INSTRUCTION_FORMAT21lh", SmaliHighlightingColors.INSTRUCTION);
        tokenColors.put("INSTRUCTION_FORMAT21s", SmaliHighlightingColors.INSTRUCTION);
        tokenColors.put("INSTRUCTION_FORMAT21t", SmaliHighlightingColors.INSTRUCTION);
        tokenColors.put("INSTRUCTION_FORMAT22b", SmaliHighlightingColors.INSTRUCTION);
        tokenColors.put("INSTRUCTION_FORMAT22c_FIELD", SmaliHighlightingColors.INSTRUCTION);
        tokenColors.put("INSTRUCTION_FORMAT22c_FIELD_ODEX", SmaliHighlightingColors.INSTRUCTION);
        tokenColors.put("INSTRUCTION_FORMAT22c_TYPE", SmaliHighlightingColors.INSTRUCTION);
        tokenColors.put("INSTRUCTION_FORMAT22cs_FIELD", SmaliHighlightingColors.INSTRUCTION);
        tokenColors.put("INSTRUCTION_FORMAT22s", SmaliHighlightingColors.INSTRUCTION);
        tokenColors.put("INSTRUCTION_FORMAT22s_OR_ID", SmaliHighlightingColors.INSTRUCTION);
        tokenColors.put("INSTRUCTION_FORMAT22t", SmaliHighlightingColors.INSTRUCTION);
        tokenColors.put("INSTRUCTION_FORMAT22x", SmaliHighlightingColors.INSTRUCTION);
        tokenColors.put("INSTRUCTION_FORMAT23x", SmaliHighlightingColors.INSTRUCTION);
        tokenColors.put("INSTRUCTION_FORMAT30t", SmaliHighlightingColors.INSTRUCTION);
        tokenColors.put("INSTRUCTION_FORMAT31c", SmaliHighlightingColors.INSTRUCTION);
        tokenColors.put("INSTRUCTION_FORMAT31i", SmaliHighlightingColors.INSTRUCTION);
        tokenColors.put("INSTRUCTION_FORMAT31i_OR_ID", SmaliHighlightingColors.INSTRUCTION);
        tokenColors.put("INSTRUCTION_FORMAT31t", SmaliHighlightingColors.INSTRUCTION);
        tokenColors.put("INSTRUCTION_FORMAT32x", SmaliHighlightingColors.INSTRUCTION);
        tokenColors.put("INSTRUCTION_FORMAT35c_METHOD", SmaliHighlightingColors.INSTRUCTION);
        tokenColors.put("INSTRUCTION_FORMAT35c_METHOD_ODEX", SmaliHighlightingColors.INSTRUCTION);
        tokenColors.put("INSTRUCTION_FORMAT35c_TYPE", SmaliHighlightingColors.INSTRUCTION);
        tokenColors.put("INSTRUCTION_FORMAT35mi_METHOD", SmaliHighlightingColors.INSTRUCTION);
        tokenColors.put("INSTRUCTION_FORMAT35ms_METHOD", SmaliHighlightingColors.INSTRUCTION);
        tokenColors.put("INSTRUCTION_FORMAT3rc_METHOD", SmaliHighlightingColors.INSTRUCTION);
        tokenColors.put("INSTRUCTION_FORMAT3rc_METHOD_ODEX", SmaliHighlightingColors.INSTRUCTION);
        tokenColors.put("INSTRUCTION_FORMAT3rc_TYPE", SmaliHighlightingColors.INSTRUCTION);
        tokenColors.put("INSTRUCTION_FORMAT3rmi_METHOD", SmaliHighlightingColors.INSTRUCTION);
        tokenColors.put("INSTRUCTION_FORMAT3rms_METHOD", SmaliHighlightingColors.INSTRUCTION);
        tokenColors.put("INSTRUCTION_FORMAT51l", SmaliHighlightingColors.INSTRUCTION);
        tokenColors.put("LINE_COMMENT", SmaliHighlightingColors.COMMENT);
        tokenColors.put("LINE_DIRECTIVE", SmaliHighlightingColors.DIRECTIVE);
        tokenColors.put("LOCAL_DIRECTIVE", SmaliHighlightingColors.DIRECTIVE);
        tokenColors.put("LOCALS_DIRECTIVE", SmaliHighlightingColors.DIRECTIVE);
        tokenColors.put("LONG_LITERAL", SmaliHighlightingColors.NUMBER);
        tokenColors.put("MEMBER_NAME", SmaliHighlightingColors.IDENTIFIER);
        tokenColors.put("METHOD_DIRECTIVE", SmaliHighlightingColors.DIRECTIVE);
        tokenColors.put("NEGATIVE_INTEGER_LITERAL", SmaliHighlightingColors.NUMBER);
        tokenColors.put("NULL_LITERAL", SmaliHighlightingColors.LITERAL);
        tokenColors.put("OPEN_BRACE", SmaliHighlightingColors.BRACES);
        tokenColors.put("OPEN_PAREN", SmaliHighlightingColors.PARENS);
        tokenColors.put("PACKED_SWITCH_DIRECTIVE", SmaliHighlightingColors.DIRECTIVE);
        tokenColors.put("PARAM_LIST_OR_ID_PRIMITIVE_TYPE", SmaliHighlightingColors.TYPE);
        tokenColors.put("PARAMETER_DIRECTIVE", SmaliHighlightingColors.DIRECTIVE);
        tokenColors.put("POSITIVE_INTEGER_LITERAL", SmaliHighlightingColors.NUMBER);
        tokenColors.put("PRIMITIVE_TYPE", SmaliHighlightingColors.TYPE);
        tokenColors.put("PROLOGUE_DIRECTIVE", SmaliHighlightingColors.DIRECTIVE);
        tokenColors.put("REGISTER", SmaliHighlightingColors.REGISTER);
        tokenColors.put("REGISTERS_DIRECTIVE", SmaliHighlightingColors.DIRECTIVE);
        tokenColors.put("RESTART_LOCAL_DIRECTIVE", SmaliHighlightingColors.DIRECTIVE);
        tokenColors.put("SHORT_LITERAL", SmaliHighlightingColors.NUMBER);
        tokenColors.put("SIMPLE_NAME", SmaliHighlightingColors.IDENTIFIER);
        tokenColors.put("SOURCE_DIRECTIVE", SmaliHighlightingColors.DIRECTIVE);
        tokenColors.put("SPARSE_SWITCH_DIRECTIVE", SmaliHighlightingColors.DIRECTIVE);
        tokenColors.put("STRING_LITERAL", SmaliHighlightingColors.STRING);
        tokenColors.put("SUBANNOTATION_DIRECTIVE", SmaliHighlightingColors.DIRECTIVE);
        tokenColors.put("SUPER_DIRECTIVE", SmaliHighlightingColors.DIRECTIVE);
        tokenColors.put("VERIFICATION_ERROR_TYPE", SmaliHighlightingColors.VERIFICATION_ERROR_TYPE);
        tokenColors.put("VOID_TYPE", SmaliHighlightingColors.TYPE);
        tokenColors.put("VTABLE_INDEX", SmaliHighlightingColors.ODEX_REFERENCE);

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

            TextAttributesKey textAttributesKey = tokenColors.get(tokenName);

            if (textAttributesKey == null) {
                throw new RuntimeException("No color attribute for token " + tokenName);
            }

            SmaliLexicalElementType elementType = new SmaliLexicalElementType(tokenId, tokenName, textAttributesKey);
            ELEMENT_TYPES[tokenId] = elementType;

            try {
                field.set(null, elementType);
            } catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            }
        }

        INSTRUCTION_TOKENS = TokenSet.create(
                INSTRUCTION_FORMAT10t,
                INSTRUCTION_FORMAT10x,
                INSTRUCTION_FORMAT10x_ODEX,
                INSTRUCTION_FORMAT11n,
                INSTRUCTION_FORMAT11x,
                INSTRUCTION_FORMAT12x_OR_ID,
                INSTRUCTION_FORMAT12x,
                INSTRUCTION_FORMAT20bc,
                INSTRUCTION_FORMAT20t,
                INSTRUCTION_FORMAT21c_FIELD,
                INSTRUCTION_FORMAT21c_FIELD_ODEX,
                INSTRUCTION_FORMAT21c_STRING,
                INSTRUCTION_FORMAT21c_TYPE,
                INSTRUCTION_FORMAT21ih,
                INSTRUCTION_FORMAT21lh,
                INSTRUCTION_FORMAT21s,
                INSTRUCTION_FORMAT21t,
                INSTRUCTION_FORMAT22b,
                INSTRUCTION_FORMAT22c_FIELD,
                INSTRUCTION_FORMAT22c_FIELD_ODEX,
                INSTRUCTION_FORMAT22c_TYPE,
                INSTRUCTION_FORMAT22cs_FIELD,
                INSTRUCTION_FORMAT22s_OR_ID,
                INSTRUCTION_FORMAT22s,
                INSTRUCTION_FORMAT22t,
                INSTRUCTION_FORMAT22x,
                INSTRUCTION_FORMAT23x,
                INSTRUCTION_FORMAT30t,
                INSTRUCTION_FORMAT31c,
                INSTRUCTION_FORMAT31i_OR_ID,
                INSTRUCTION_FORMAT31i,
                INSTRUCTION_FORMAT31t,
                INSTRUCTION_FORMAT32x,
                INSTRUCTION_FORMAT35c_METHOD,
                INSTRUCTION_FORMAT35c_METHOD_ODEX,
                INSTRUCTION_FORMAT35c_TYPE,
                INSTRUCTION_FORMAT35mi_METHOD,
                INSTRUCTION_FORMAT35ms_METHOD,
                INSTRUCTION_FORMAT3rc_METHOD,
                INSTRUCTION_FORMAT3rc_METHOD_ODEX,
                INSTRUCTION_FORMAT3rc_TYPE,
                INSTRUCTION_FORMAT3rmi_METHOD,
                INSTRUCTION_FORMAT3rms_METHOD,
                INSTRUCTION_FORMAT51l,
                ARRAY_DATA_DIRECTIVE,
                PACKED_SWITCH_DIRECTIVE,
                SPARSE_SWITCH_DIRECTIVE
        );
    }
}
