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
import com.intellij.psi.impl.source.codeStyle.CodeEditUtil;
import com.intellij.psi.infos.CandidateInfo;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jf.smalidea.psi.SmaliCompositeElementFactory;
import org.jf.smalidea.psi.SmaliElementTypes;
import org.jf.smalidea.psi.leaf.SmaliClassDescriptor;
import org.jf.smalidea.util.NameUtils;

public class SmaliClassTypeElement extends SmaliTypeElement implements PsiJavaCodeReferenceElement {
    public static final SmaliClassTypeElement[] EMPTY_ARRAY = new SmaliClassTypeElement[0];

    public static final SmaliCompositeElementFactory FACTORY = new SmaliCompositeElementFactory() {
        @Override public SmaliCompositeElement createElement() {
            return new SmaliClassTypeElement();
        }
    };

    @Nullable private SmaliClassType classType = null;

    public SmaliClassTypeElement() {
        super(SmaliElementTypes.CLASS_TYPE);
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

    @Nullable @Override public SmaliClassTypeElement getInnermostComponentReferenceElement() {
        return this;
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

    @Nullable @Override public PsiClass resolve() {
        return NameUtils.resolveSmaliType(this, getText());
    }

    @NotNull @Override public String getCanonicalText() {
        return getQualifiedName();
    }

    @Override public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
        SmaliClassDescriptor descriptor = getReferenceNameElement();
        if (descriptor == null) {
            throw new IncorrectOperationException();
        }

        SmaliClassDescriptor newDescriptor = new SmaliClassDescriptor(NameUtils.javaToSmaliType(newElementName));
        CodeEditUtil.setNodeGenerated(newDescriptor, true);

        this.replaceChild(descriptor, newDescriptor);
        return this;
    }

    @Override public PsiElement bindToElement(@NotNull PsiElement element) throws IncorrectOperationException {
        if (element instanceof PsiClass) {
            handleElementRename(((PsiClass) element).getQualifiedName());
            return this;
        }
        throw new IncorrectOperationException();
    }

    @Override public boolean isReferenceTo(PsiElement element) {
        if (!(element instanceof PsiClass)) {
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

    // ***************************************************************************
    // Below are the PsiJavaCodeReferenceElement-specific methods

    @Override public void processVariants(@NotNull PsiScopeProcessor processor) {
        // TODO: maybe just do nothing?
        throw new UnsupportedOperationException();
    }

    @Nullable @Override public SmaliClassDescriptor getReferenceNameElement() {
        return findChildByClass(SmaliClassDescriptor.class);
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
        PsiClass psiClass = resolve();
        if (psiClass != null) {
            return psiClass.getQualifiedName();
        }
        return NameUtils.smaliToJavaType(getText());
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
        return null;
    }

    @Nullable @Override public String getReferenceName() {
        return getName();
    }
}
