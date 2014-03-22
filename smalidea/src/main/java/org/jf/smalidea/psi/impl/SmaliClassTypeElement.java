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
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jf.smalidea.psi.SmaliCompositeElementFactory;
import org.jf.smalidea.psi.SmaliElementTypes;
import org.jf.smalidea.util.NameUtils;

public class SmaliClassTypeElement extends SmaliCompositeElement implements PsiTypeElement, PsiReference {
    public static final SmaliCompositeElementFactory FACTORY = new SmaliCompositeElementFactory() {
        @Override public SmaliCompositeElement createElement() {
            return new SmaliClassTypeElement();
        }
    };

    @Nullable private SmaliClassType classType = null;

    public SmaliClassTypeElement() {
        super(SmaliElementTypes.CLASS_TYPE);
    }

    /**
     * @return the fully qualified java-style name of the class in this .class statement
     */
    @NotNull
    public String getJavaType() {
        return NameUtils.smaliToJavaType(getText());
    }

    @NotNull @Override public SmaliClassType getType() {
        if (classType == null) {
            classType = new SmaliClassType(this);
        }
        return classType;
    }

    @Override public String getName() {
        return NameUtils.shortNameFromQualifiedName(getCanonicalText());
    }

    @Nullable @Override public PsiJavaCodeReferenceElement getInnermostComponentReferenceElement() {
        // Not applicable for smali
        return null;
    }

    @Override public PsiElement getElement() {
        return this;
    }

    @Override public PsiReference getReference() {
        return this;
    }

    @Override public TextRange getRangeInElement() {
        return new TextRange(0, getTextLength());
    }

    @Nullable @Override public PsiElement resolve() {
        JavaPsiFacade facade = JavaPsiFacade.getInstance(getProject());
        return facade.findClass(getCanonicalText(), getResolveScope());
    }

    @NotNull @Override public String getCanonicalText() {
        return NameUtils.smaliToJavaType(getText());
    }

    @Override public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
        //TODO: implement this
        throw new IncorrectOperationException();
    }

    @Override public PsiElement bindToElement(@NotNull PsiElement element) throws IncorrectOperationException {
        //TODO: implement this
        throw new IncorrectOperationException();
    }

    @Override public boolean isReferenceTo(PsiElement element) {
        if (!(element instanceof PsiClassType)) {
            return false;
        }
        return element.getManager().areElementsEquivalent(element, resolve());
    }

    @NotNull @Override public Object[] getVariants() {
        // TODO: implement this?
        return new Object[0];
    }

    @Override public boolean isSoft() {
        return false;
    }

    // Annotations on types are for JSR 308. Not applicable to smali.

    @NotNull @Override public PsiAnnotation[] getAnnotations() {
        return new PsiAnnotation[0];
    }

    @NotNull @Override public PsiAnnotation[] getApplicableAnnotations() {
        return new PsiAnnotation[0];
    }

    @Nullable @Override public PsiAnnotation findAnnotation(@NotNull @NonNls String qualifiedName) {
        return null;
    }

    @NotNull @Override public PsiAnnotation addAnnotation(@NotNull @NonNls String qualifiedName) {
        throw new UnsupportedOperationException();
    }
}
