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
import com.intellij.psi.impl.light.LightElement;
import com.intellij.psi.infos.CandidateInfo;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jf.smalidea.SmaliLanguage;
import org.jf.smalidea.util.NameUtils;

public class LightSmaliClassTypeElement extends LightElement
        implements PsiTypeElement, PsiReference, PsiJavaCodeReferenceElement {
    @NotNull
    private final String smaliName;

    public LightSmaliClassTypeElement(@NotNull PsiManager manager, @NotNull String smaliName) {
        super(manager, SmaliLanguage.INSTANCE);
        this.smaliName = smaliName;
    }

    @Override public String toString() {
        return "LightSmaliClassTypeElement:" + smaliName;
    }

    @NotNull @Override public PsiType getType() {
        return new SmaliClassType(this);
    }

    @Nullable @Override public LightSmaliClassTypeElement getInnermostComponentReferenceElement() {
        return this;
    }

    @Override public String getText() {
        return smaliName;
    }

    @Override public PsiReference getReference() {
        return this;
    }

    @Override public PsiElement getElement() {
        return this;
    }

    @Override public TextRange getRangeInElement() {
        return new TextRange(0, getTextLength());
    }

    @Nullable @Override public PsiClass resolve() {
        return NameUtils.resolveSmaliType(this, smaliName);
    }

    @NotNull @Override public String getCanonicalText() {
        return NameUtils.resolveSmaliToJavaType(this, smaliName);
    }

    @Override public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
        throw new UnsupportedOperationException();
    }

    @Override public PsiElement bindToElement(@NotNull PsiElement element) throws IncorrectOperationException {
        throw new UnsupportedOperationException();
    }

    @Override public boolean isReferenceTo(PsiElement element) {
        if (!(element instanceof PsiClassType)) {
            return false;
        }
        return element.getManager().areElementsEquivalent(element, resolve());
    }

    @NotNull @Override public Object[] getVariants() {
        throw new RuntimeException("Variants are not available for light references");
    }

    @Override public boolean isSoft() {
        return false;
    }

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

    // ***************************************************************************
    // Below are the PsiJavaCodeReferenceElement-specific methods

    @Override public void processVariants(@NotNull PsiScopeProcessor processor) {
        // TODO: maybe just do nothing?
        throw new UnsupportedOperationException();
    }

    @Nullable @Override public PsiElement getReferenceNameElement() {
        // TODO: implement if needed
        throw new UnsupportedOperationException();
    }

    @Nullable @Override public PsiReferenceParameterList getParameterList() {
        // TODO: (generics) implement this
        return null;
    }

    @NotNull @Override public PsiType[] getTypeParameters() {
        // TODO: (generics) implement this
        return new PsiType[0];
    }

    @Override public boolean isQualified() {
        // TODO: should this return false for classes in the top level package?
        return true;
    }

    @Override public String getQualifiedName() {
        return getCanonicalText();
    }

    @NotNull @Override public JavaResolveResult advancedResolve(boolean incompleteCode) {
        PsiClass element = resolve();
        if (element == null) {
            return JavaResolveResult.EMPTY;
        }
        return new CandidateInfo(element, PsiSubstitutor.EMPTY);
    }

    @NotNull @Override public JavaResolveResult[] multiResolve(boolean incompleteCode) {
        PsiClass element = resolve();
        if (element == null) {
            return JavaResolveResult.EMPTY_ARRAY;
        }
        return new CandidateInfo[] { new CandidateInfo(element, PsiSubstitutor.EMPTY) };
    }

    @Nullable @Override public PsiElement getQualifier() {
        // TODO: implement this if needed
        throw new UnsupportedOperationException();
    }

    @Nullable @Override public String getReferenceName() {
        return getName();
    }
}
