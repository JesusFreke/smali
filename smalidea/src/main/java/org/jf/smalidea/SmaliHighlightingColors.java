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

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;

public class SmaliHighlightingColors {
    public static final TextAttributesKey ACCESS = TextAttributesKey.createTextAttributesKey(
            "ACCESS", DefaultLanguageHighlighterColors.KEYWORD);
    public static final TextAttributesKey ARROW = TextAttributesKey.createTextAttributesKey(
            "ARROW", DefaultLanguageHighlighterColors.PREDEFINED_SYMBOL);
    public static final TextAttributesKey BRACES = TextAttributesKey.createTextAttributesKey(
            "BRACES", DefaultLanguageHighlighterColors.BRACES);
    public static final TextAttributesKey COLON = TextAttributesKey.createTextAttributesKey(
            "COLON", DefaultLanguageHighlighterColors.PREDEFINED_SYMBOL);
    public static final TextAttributesKey COMMA = TextAttributesKey.createTextAttributesKey(
            "COMMA", DefaultLanguageHighlighterColors.COMMA);
    public static final TextAttributesKey COMMENT = TextAttributesKey.createTextAttributesKey(
            "COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);
    public static final TextAttributesKey DIRECTIVE = TextAttributesKey.createTextAttributesKey(
            "DIRECTIVE", DefaultLanguageHighlighterColors.KEYWORD);
    public static final TextAttributesKey DOTDOT = TextAttributesKey.createTextAttributesKey(
            "DOTDOT", DefaultLanguageHighlighterColors.PREDEFINED_SYMBOL);
    public static final TextAttributesKey EQUAL = TextAttributesKey.createTextAttributesKey(
            "EQUAL", DefaultLanguageHighlighterColors.PREDEFINED_SYMBOL);
    public static final TextAttributesKey IDENTIFIER = TextAttributesKey.createTextAttributesKey(
            "IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER);
    public static final TextAttributesKey INSTRUCTION = TextAttributesKey.createTextAttributesKey(
            "INSTRUCTION", DefaultLanguageHighlighterColors.KEYWORD);
    public static final TextAttributesKey LITERAL = TextAttributesKey.createTextAttributesKey(
            "LITERAL", DefaultLanguageHighlighterColors.NUMBER);
    public static final TextAttributesKey NUMBER = TextAttributesKey.createTextAttributesKey(
            "NUMBER", DefaultLanguageHighlighterColors.NUMBER);
    public static final TextAttributesKey ODEX_REFERENCE = TextAttributesKey.createTextAttributesKey(
            "ODEX_REFERENCE", DefaultLanguageHighlighterColors.INSTANCE_METHOD);
    public static final TextAttributesKey PARENS = TextAttributesKey.createTextAttributesKey(
            "PARENS", DefaultLanguageHighlighterColors.PARENTHESES);
    public static final TextAttributesKey REGISTER = TextAttributesKey.createTextAttributesKey(
            "REGISTER", DefaultLanguageHighlighterColors.LOCAL_VARIABLE);
    public static final TextAttributesKey STRING = TextAttributesKey.createTextAttributesKey(
            "STRING", DefaultLanguageHighlighterColors.STRING);
    public static final TextAttributesKey TYPE = TextAttributesKey.createTextAttributesKey(
            "TYPE", DefaultLanguageHighlighterColors.CLASS_REFERENCE);
    public static final TextAttributesKey VERIFICATION_ERROR_TYPE = TextAttributesKey.createTextAttributesKey(
            "VERIFICATION_ERROR_TYPE", DefaultLanguageHighlighterColors.KEYWORD);
}
