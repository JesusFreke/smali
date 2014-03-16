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

package org.jf.smalidea;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import org.antlr.runtime.CommonToken;
import org.antlr.runtime.Token;
import org.antlr.runtime.TokenSource;
import org.antlr.runtime.TokenStream;
import org.jetbrains.annotations.Nullable;
import org.jf.smali.InvalidToken;
import org.jf.smali.smaliParser;

import javax.annotation.Nonnull;
import java.util.ArrayList;

public class PsiBuilderTokenStream implements TokenStream {
    @Nonnull private PsiBuilder psiBuilder;
    @Nullable private CommonToken currentToken = null;
    @Nonnull private ArrayList<Marker> markers = new ArrayList<PsiBuilder.Marker>();

    public PsiBuilderTokenStream(@Nonnull PsiBuilder psiBuilder) {
        this.psiBuilder = psiBuilder;
    }

    @Override public Token LT(int k) {
        if (k == 1) {
            if (currentToken == null) {
                buildCurrentToken();
            }
            return currentToken;
        }
        throw new UnsupportedOperationException();
    }

    @Override public int range() {
        return currentToken==null?0:1;
    }

    @Override public Token get(int i) {
        throw new UnsupportedOperationException();
    }

    @Override public TokenSource getTokenSource() {
        throw new UnsupportedOperationException();
    }

    @Override public String toString(int start, int stop) {
        throw new UnsupportedOperationException();
    }

    @Override public String toString(Token start, Token stop) {
        throw new UnsupportedOperationException();
    }

    @Override public void consume() {
        psiBuilder.advanceLexer();
        buildCurrentToken();
    }

    private void buildCurrentToken() {
        IElementType element = psiBuilder.getTokenType();
        if (element != null) {
            if (element instanceof SmaliLexicalElementType) {
                SmaliLexicalElementType elementType = (SmaliLexicalElementType)element;
                currentToken = new CommonToken(elementType.tokenId, psiBuilder.getTokenText());
            } else if (element == TokenType.BAD_CHARACTER) {
                currentToken = new InvalidToken("", psiBuilder.getTokenText());
            } else {
                throw new UnsupportedOperationException();
            }
        } else {
            currentToken = new CommonToken(Token.EOF);
        }
    }

    @Override public int LA(int i) {
        IElementType elementType = psiBuilder.lookAhead(i-1);
        if (elementType == null) {
            return -1;
        } else if (elementType instanceof SmaliLexicalElementType) {
            return ((SmaliLexicalElementType)elementType).tokenId;
        } else if (elementType == TokenType.BAD_CHARACTER) {
            return smaliParser.INVALID_TOKEN;
        }
        throw new UnsupportedOperationException();
    }

    @Override public int mark() {
        int ret = markers.size();
        markers.add(psiBuilder.mark());
        return ret;
    }

    @Override public int index() {
        return psiBuilder.getCurrentOffset();
    }

    @Override public void rewind(int markerIndex) {
        PsiBuilder.Marker marker = markers.get(markerIndex);
        marker.rollbackTo();
        while (markerIndex < markers.size()) {
            markers.remove(markerIndex);
        }
    }

    @Override public void rewind() {
        rewind(markers.size()-1);
        mark();
    }

    @Override public void release(int markerIndex) {
        while (markerIndex < markers.size()) {
            markers.remove(markerIndex).drop();
        }
    }

    @Override public void seek(int index) {
        if (index < psiBuilder.getCurrentOffset()) {
            throw new UnsupportedOperationException();
        }
        while (index > psiBuilder.getCurrentOffset()) {
            consume();
        }
    }

    @Override public int size() {
        throw new UnsupportedOperationException();
    }

    @Override public String getSourceName() {
        return null;
    }
}
