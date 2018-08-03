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

import com.intellij.lexer.LexerBase;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.antlr.runtime.CommonToken;
import org.jetbrains.annotations.NotNull;
import org.jf.smali.smaliFlexLexer;
import org.jf.smali.smaliParser;
import org.jf.smali.util.BlankReader;

public class SmaliLexer extends LexerBase {
    private final smaliFlexLexer lexer = new smaliFlexLexer(BlankReader.INSTANCE);
    private CommonToken token = null;
    private int state = 0;
    private int endOffset;
    private CharSequence text;

    public SmaliLexer() {
        super();
        lexer.setSuppressErrors(true);
    }

    @Override public void start(@NotNull CharSequence buffer, int startOffset, int endOffset, int initialState) {
        text = buffer;
        lexer.reset(buffer, startOffset, endOffset, initialState);
        this.endOffset = endOffset;
        this.token = null;
        this.state = 0;
    }

    @NotNull @Override public CharSequence getTokenSequence() {
        return getTokenText();
    }

    @NotNull @Override public String getTokenText() {
        ensureToken();
        return token.getText();
    }

    @Override
    public int getState() {
        ensureToken();
        return state;
    }

    @Override
    public IElementType getTokenType() {
        ensureToken();
        return mapTokenTypeToElementType(token.getType());
    }

    private IElementType mapTokenTypeToElementType(int tokenType) {
        if (tokenType == smaliParser.WHITE_SPACE) {
            return TokenType.WHITE_SPACE;
        }
        if (tokenType == smaliParser.INVALID_TOKEN) {
            return TokenType.BAD_CHARACTER;
        }
        if (tokenType == smaliParser.EOF) {
            return null;
        }
        return SmaliTokens.getElementType(tokenType);
    }

    @Override
    public int getTokenStart() {
        ensureToken();
        return token.getStartIndex();
    }

    @Override
    public int getTokenEnd() {
        ensureToken();
        return token.getStopIndex()+1;
    }

    @Override
    public void advance() {
        token = null;
        state = 0;
    }

    @NotNull @Override public CharSequence getBufferSequence() {
        return text;
    }

    @Override
    public int getBufferEnd() {
        return endOffset;
    }

    private void ensureToken() {
        if (token == null) {
            token = (CommonToken)lexer.nextToken();
            state = lexer.yystate();
        }
        assert token != null;
    }
}
