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
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jf.smalidea.SmaliLexer;
import org.jf.smalidea.SmaliTokens;

public class SmaliWordScanner implements WordsScanner {

    @Override public void processWords(CharSequence fileText, Processor<WordOccurrence> processor) {
        SmaliLexer lexer = new SmaliLexer();
        lexer.start(fileText);

        IElementType type = lexer.getTokenType();
        while (type != null) {
            if (type == SmaliTokens.CLASS_DESCRIPTOR) {
                processClassDescriptor(fileText, lexer.getTokenStart(), lexer.getTokenEnd(), processor);
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
