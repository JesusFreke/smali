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

import com.google.common.collect.Lists;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.light.LightMethodBuilder;
import com.intellij.util.ArrayUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jf.smalidea.SmaliLanguage;
import org.jf.smalidea.psi.SmaliCompositeElementFactory;
import org.jf.smalidea.psi.SmaliElementTypes;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class SmaliMethodReference extends SmaliCompositeElement implements PsiReference {
    public static final SmaliCompositeElementFactory FACTORY = new SmaliCompositeElementFactory() {
        @Override public SmaliCompositeElement createElement() {
            return new SmaliMethodReference();
        }
    };

    @Override public String getName() {
        PsiElement memberName = getMemberName();
        if (memberName == null) {
            return null;
        }
        return memberName.getText();
    }

    public SmaliMethodReference() {
        super(SmaliElementTypes.METHOD_REFERENCE);
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

    @Nullable
    public PsiClass getContainingClass() {
        SmaliClassTypeElement containingClassReference = getContainingType();
        if (containingClassReference == null) {
            return null;
        }
        PsiClass containingClass = containingClassReference.resolve();
        if (containingClass == null) {
            return null;
        }

        return containingClass;
    }

    @Nullable
    public SmaliClassTypeElement getContainingType() {
        return findChildByClass(SmaliClassTypeElement.class);
    }

    @Nullable
    public SmaliMemberName getMemberName() {
        return findChildByClass(SmaliMemberName.class);
    }

    @Nonnull
    public List<PsiType> getParameterTypes() {
        SmaliMethodReferenceParamList paramList = findChildByClass(SmaliMethodReferenceParamList.class);
        if (paramList == null) {
            return Lists.newArrayList();
        }

        SmaliTypeElement[] parameterElements = paramList.getParameterTypes();

        List<PsiType> types = new ArrayList<PsiType>(parameterElements.length);
        for (SmaliTypeElement parameterElement: parameterElements) {
            types.add(parameterElement.getType());
        }
        return types;
    }

    @Nullable
    public SmaliTypeElement getReturnType() {
        SmaliTypeElement[] types = findChildrenByClass(SmaliTypeElement.class);
        if (types.length < 2) {
            return null;
        }
        return types[1];
    }

    @Nullable @Override public PsiElement resolve() {
        PsiClass containingClass = getContainingClass();
        if (containingClass == null) {
            return null;
        }

        SmaliMemberName memberName = getMemberName();
        if (memberName == null) {
            return null;
        }

        LightMethodBuilder pattern = new LightMethodBuilder(getManager(), SmaliLanguage.INSTANCE, memberName.getText());

        for (PsiType type: getParameterTypes()) {
            pattern.addParameter("", type);
        }

        SmaliTypeElement returnTypeElement = getReturnType();
        if (returnTypeElement == null) {
            return null;
        }

        pattern.setMethodReturnType(returnTypeElement.getType());

        // TODO: what about static constructor?
        pattern.setConstructor(memberName.getText().equals("<init>"));

        return containingClass.findMethodBySignature(pattern, true);
    }

    @NotNull @Override public String getCanonicalText() {
        return getText();
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

    @Override public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
        SmaliMemberName memberName = getMemberName();
        if (memberName == null) {
            throw new IncorrectOperationException();
        }
        memberName.setName(newElementName);
        return this;
    }

    @Override public PsiElement bindToElement(@NotNull PsiElement element) throws IncorrectOperationException {
        //TODO: implement this
        throw new IncorrectOperationException();
    }
}
