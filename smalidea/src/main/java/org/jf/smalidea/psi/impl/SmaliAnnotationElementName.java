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

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.ArrayUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jf.smalidea.psi.SmaliCompositeElementFactory;
import org.jf.smalidea.psi.SmaliElementTypes;

public class SmaliAnnotationElementName extends SmaliCompositeElement implements PsiIdentifier, PsiReference {
    public static final SmaliCompositeElementFactory FACTORY = new SmaliCompositeElementFactory() {
        @Override public SmaliCompositeElement createElement() {
            return new SmaliAnnotationElementName();
        }
    };

    public SmaliAnnotationElementName() {
        super(SmaliElementTypes.ANNOTATION_ELEMENT_NAME);
    }

    @Override public IElementType getTokenType() {
        return getElementType();
    }

    @Override public String getName() {
        return getText();
    }

    @Nullable
    public SmaliAnnotation getContainingAnnotation() {
        return findAncestorByClass(SmaliAnnotation.class);
    }

    @Override public PsiElement bindToElement(PsiElement element) throws IncorrectOperationException {
        //TODO: implement this if needed
        throw new IncorrectOperationException();
    }

    @Override public PsiElement getElement() {
        return this;
    }

    @Override public TextRange getRangeInElement() {
        return new TextRange(0, getTextLength());
    }

    @Nullable @Override public PsiElement resolve() {
        SmaliAnnotation smaliAnnotation = getContainingAnnotation();
        if (smaliAnnotation == null) {
            return null;
        }

        String annotationType = smaliAnnotation.getQualifiedName();
        if (annotationType == null) {
            return null;
        }

        JavaPsiFacade facade = JavaPsiFacade.getInstance(getProject());
        PsiClass annotationClass = facade.findClass(annotationType, getResolveScope());
        if (annotationClass == null) {
            return null;
        }

        for (PsiMethod method : annotationClass.findMethodsByName(getName(), true)) {
            if (method.getParameterList().getParametersCount() == 0) {
                return method;
            }
        }
        return null;
    }

    @NotNull @Override public String getCanonicalText() {
        // TODO: return a full method reference here?
        String name = getName();
        if (name == null) {
            return "";
        }
        return name;
    }

    @Override public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
        //TODO: implement this
        throw new IncorrectOperationException();
    }

    @Override public boolean isReferenceTo(PsiElement element) {
        return resolve() == element;
    }

    @NotNull @Override public Object[] getVariants() {
        return ArrayUtil.EMPTY_OBJECT_ARRAY;
    }

    @Override public boolean isSoft() {
        return false;
    }

    @Override public PsiReference getReference() {
        return this;
    }
}
