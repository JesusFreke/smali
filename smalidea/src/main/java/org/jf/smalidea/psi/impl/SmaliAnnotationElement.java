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

package org.jf.smalidea.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameValuePair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jf.smalidea.SmaliTokens;
import org.jf.smalidea.psi.SmaliCompositeElementFactory;
import org.jf.smalidea.psi.SmaliElementTypes;

public class SmaliAnnotationElement extends SmaliCompositeElement implements PsiNameValuePair {
    // TODO: consider making this a stub

    public static final SmaliCompositeElementFactory FACTORY = new SmaliCompositeElementFactory() {
        @Override public SmaliCompositeElement createElement() {
            return new SmaliAnnotationElement();
        }
    };

    public SmaliAnnotationElement() {
        super(SmaliElementTypes.ANNOTATION_ELEMENT);
    }

    @Override public String getName() {
        SmaliAnnotationElementName identifier = getNameIdentifier();
        if (identifier != null) {
            return identifier.getName();
        }
        return null;
    }

    @Nullable @Override public SmaliAnnotationElementName getNameIdentifier() {
        return findChildByClass(SmaliAnnotationElementName.class);
    }

    @Nullable @Override public PsiAnnotationMemberValue getValue() {
        ASTNode equalNode = findChildByType(SmaliTokens.EQUAL);
        if (equalNode == null) {
            return null;
        }

        PsiElement nextElement = equalNode.getPsi().getNextSibling();
        while (nextElement != null) {
            if (nextElement instanceof PsiAnnotationMemberValue) {
                return (PsiAnnotationMemberValue)nextElement;
            }
            nextElement = nextElement.getNextSibling();
        }
        return null;
    }

    @NotNull @Override public PsiAnnotationMemberValue setValue(@NotNull PsiAnnotationMemberValue newValue) {
        // TODO: implement this
        throw new UnsupportedOperationException();
    }

    @Nullable @Override public String getLiteralValue() {
        // Not applicable for smali
        return null;
    }
}
