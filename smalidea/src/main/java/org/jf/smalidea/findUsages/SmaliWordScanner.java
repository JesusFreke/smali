/*
 * Copyright 2015, Google Inc.
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

package org.jf.smalidea.findUsages;

import com.intellij.lang.cacheBuilder.WordOccurrence;
import com.intellij.lang.cacheBuilder.WordOccurrence.Kind;
import com.intellij.lang.cacheBuilder.WordsScanner;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jf.smalidea.SmaliLexer;
import org.jf.smalidea.SmaliTokens;

public class SmaliWordScanner implements WordsScanner {

    private static final TokenSet MEMBER_NAME_TOKENS = TokenSet.create(
            SmaliTokens.MEMBER_NAME,
            SmaliTokens.SIMPLE_NAME,
            SmaliTokens.ACCESS_SPEC,
            SmaliTokens.VERIFICATION_ERROR_TYPE,
            SmaliTokens.POSITIVE_INTEGER_LITERAL,
            SmaliTokens.NEGATIVE_INTEGER_LITERAL,
            SmaliTokens.FLOAT_LITERAL_OR_ID,
            SmaliTokens.DOUBLE_LITERAL_OR_ID,
            SmaliTokens.BOOL_LITERAL,
            SmaliTokens.NULL_LITERAL,
            SmaliTokens.REGISTER,
            SmaliTokens.PRIMITIVE_TYPE,
            SmaliTokens.VOID_TYPE,
            SmaliTokens.ANNOTATION_VISIBILITY,
            SmaliTokens.INSTRUCTION_FORMAT10t,
            SmaliTokens.INSTRUCTION_FORMAT10x,
            SmaliTokens.INSTRUCTION_FORMAT10x_ODEX,
            SmaliTokens.INSTRUCTION_FORMAT11x,
            SmaliTokens.INSTRUCTION_FORMAT12x_OR_ID,
            SmaliTokens.INSTRUCTION_FORMAT21c_FIELD,
            SmaliTokens.INSTRUCTION_FORMAT21c_FIELD_ODEX,
            SmaliTokens.INSTRUCTION_FORMAT21c_STRING,
            SmaliTokens.INSTRUCTION_FORMAT21c_TYPE,
            SmaliTokens.INSTRUCTION_FORMAT21t,
            SmaliTokens.INSTRUCTION_FORMAT22c_FIELD,
            SmaliTokens.INSTRUCTION_FORMAT22c_FIELD_ODEX,
            SmaliTokens.INSTRUCTION_FORMAT22c_TYPE,
            SmaliTokens.INSTRUCTION_FORMAT22cs_FIELD,
            SmaliTokens.INSTRUCTION_FORMAT22s_OR_ID,
            SmaliTokens.INSTRUCTION_FORMAT22t,
            SmaliTokens.INSTRUCTION_FORMAT23x,
            SmaliTokens.INSTRUCTION_FORMAT31i_OR_ID,
            SmaliTokens.INSTRUCTION_FORMAT31t,
            SmaliTokens.INSTRUCTION_FORMAT35c_METHOD,
            SmaliTokens.INSTRUCTION_FORMAT35c_METHOD_ODEX,
            SmaliTokens.INSTRUCTION_FORMAT35c_TYPE,
            SmaliTokens.INSTRUCTION_FORMAT35mi_METHOD,
            SmaliTokens.INSTRUCTION_FORMAT35ms_METHOD,
            SmaliTokens.INSTRUCTION_FORMAT51l);

    @Override public void processWords(CharSequence fileText, Processor<WordOccurrence> processor) {
        SmaliLexer lexer = new SmaliLexer();
        lexer.start(fileText);

        IElementType type = lexer.getTokenType();
        while (type != null) {
            if (type == SmaliTokens.CLASS_DESCRIPTOR) {
                processClassDescriptor(fileText, lexer.getTokenStart(), lexer.getTokenEnd(), processor);
            } else if (MEMBER_NAME_TOKENS.contains(type)) {
                processor.process(new WordOccurrence(fileText, lexer.getTokenStart(), lexer.getTokenEnd(), Kind.CODE));
            } else if (type == SmaliTokens.PARAM_LIST_OR_ID_PRIMITIVE_TYPE) {
                int tokenStart = lexer.getTokenStart();
                while (type == SmaliTokens.PARAM_LIST_OR_ID_PRIMITIVE_TYPE) {
                    lexer.advance();
                    type = lexer.getTokenType();
                }
                int tokenEnd = lexer.getTokenStart();
                processor.process(new WordOccurrence(fileText, tokenStart, tokenEnd, Kind.CODE));
            }
            lexer.advance();
            type = lexer.getTokenType();
        }
    }

    private void processClassDescriptor(CharSequence fileText, int tokenStart, int tokenEnd,
                                        @NotNull Processor<WordOccurrence> processor) {
        CharSequence tokenText = fileText.subSequence(tokenStart, tokenEnd);

        assert tokenText.charAt(0) == 'L' && tokenText.charAt(tokenText.length()-1) == ';';
        processor.process(new WordOccurrence(fileText, tokenStart, tokenEnd, Kind.CODE));
    }
}
