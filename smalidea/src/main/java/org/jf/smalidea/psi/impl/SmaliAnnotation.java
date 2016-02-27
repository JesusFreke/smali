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
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiImplUtil;
import com.intellij.psi.meta.PsiMetaData;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jf.smalidea.psi.SmaliElementTypes;
import org.jf.smalidea.psi.stub.SmaliAnnotationStub;

public class SmaliAnnotation extends SmaliStubBasedPsiElement<SmaliAnnotationStub> implements PsiAnnotation {
    public SmaliAnnotation(@NotNull SmaliAnnotationStub stub) {
        super(stub, SmaliElementTypes.ANNOTATION);
    }

    public SmaliAnnotation(@NotNull ASTNode node) {
        super(node);
    }

    @NotNull @Override public SmaliAnnotationParameterList getParameterList() {
        SmaliAnnotationParameterList paramList = findChildByClass(SmaliAnnotationParameterList.class);
        // The structure of the parser should ensure the param list is always present, even if there are syntax errors
        assert paramList != null;
        return paramList;
    }

    @Nullable @Override public String getQualifiedName() {
        PsiJavaCodeReferenceElement nameElement = getNameReferenceElement();
        if (nameElement != null) {
            return nameElement.getQualifiedName();
        }
        return null;
    }

    @Nullable public String getSmaliName() {
        SmaliAnnotationStub stub = getStub();
        if (stub != null) {
            return stub.getAnnotationSmaliTypeName();
        }

        SmaliClassTypeElement classType = findChildByClass(SmaliClassTypeElement.class);
        if (classType == null) {
            return null;
        }
        return classType.getSmaliName();
    }

    @Nullable @Override public PsiJavaCodeReferenceElement getNameReferenceElement() {
        SmaliAnnotationStub stub = getStub();
        if (stub != null) {
            String smaliName = stub.getAnnotationSmaliTypeName();
            if (smaliName != null) {
                return new LightSmaliClassTypeElement(getManager(), smaliName);
            }
        }
        return findChildByClass(SmaliClassTypeElement.class);
    }

    @Nullable @Override public PsiAnnotationMemberValue findAttributeValue(@Nullable @NonNls String attributeName) {
        return PsiImplUtil.findAttributeValue(this, attributeName);
    }

    @Nullable @Override
    public PsiAnnotationMemberValue findDeclaredAttributeValue(@Nullable @NonNls String attributeName) {
        return PsiImplUtil.findDeclaredAttributeValue(this, attributeName);
    }

    @Override
    public <T extends PsiAnnotationMemberValue> T setDeclaredAttributeValue(
            @Nullable @NonNls String attributeName, @Nullable T value) {
        // TODO: implement this
        throw new UnsupportedOperationException();
    }

    @Nullable @Override public PsiAnnotationOwner getOwner() {
        return (PsiAnnotationOwner)getStubOrPsiParent();
    }

    @Nullable @Override public PsiMetaData getMetaData() {
        // I have no idea what this is
        return null;
    }
}
