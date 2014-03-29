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
import com.intellij.psi.PsiModifier.ModifierConstant;
import com.intellij.psi.impl.PsiImplUtil;
import com.intellij.psi.impl.PsiSuperMethodImplUtil;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.util.MethodSignature;
import com.intellij.psi.util.MethodSignatureBackedByPsiMethod;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jf.smalidea.psi.SmaliElementTypes;
import org.jf.smalidea.psi.iface.SmaliModifierListOwner;
import org.jf.smalidea.psi.stub.SmaliMethodStub;

import java.util.List;

public class SmaliMethod extends SmaliStubBasedPsiElement<SmaliMethodStub>
        implements PsiMethod, SmaliModifierListOwner {
    public SmaliMethod(@NotNull SmaliMethodStub stub) {
        super(stub, SmaliElementTypes.METHOD);
    }

    public SmaliMethod(@NotNull ASTNode node) {
        super(node);
    }

    @NotNull @Override public String getName() {
        SmaliMethodStub stub = getStub();
        if (stub != null) {
            return stub.getName();
        }
        return getNameIdentifier().getText();
    }

    @Override public boolean hasTypeParameters() {
        // TODO: (generics) implement this
        return false;
    }

    @NotNull
    public SmaliMethodPrototype getMethodPrototype() {
        return getRequiredStubOrPsiChild(SmaliElementTypes.METHOD_PROTOTYPE);
    }

    @NotNull @Override public PsiType getReturnType() {
        SmaliMethodStub stub = getStub();
        if (stub != null) {
            String returnType = stub.getReturnType();
            PsiElementFactory factory = JavaPsiFacade.getInstance(getProject()).getElementFactory();
            return factory.createTypeByFQClassName(returnType, getResolveScope());
        }
        return getReturnTypeElement().getType();
    }

    @NotNull @Override public PsiTypeElement getReturnTypeElement() {
        return getMethodPrototype().getReturnType();
    }

    @NotNull @Override public SmaliMethodParamList getParameterList() {
        return getMethodPrototype().getParameterList();
    }

    @NotNull @Override public PsiReferenceList getThrowsList() {
        // TODO: add a fake reference list for throws
        return null;
    }

    @Nullable @Override public PsiCodeBlock getBody() {
        // not applicable
        return null;
    }

    public int getRegisterCount() {
        SmaliRegistersStatement registersStatement = findChildByClass(SmaliRegistersStatement.class);
        if (registersStatement == null) {
            return 0;
        }
        return registersStatement.getRegisterCount();
    }

    public int getParameterRegisterCount() {
        SmaliModifierList modifierList = getModifierList();
        int parameterRegisterCount = getMethodPrototype().getParameterList().getParameterRegisterCount();
        if (!modifierList.hasModifierProperty("static")) {
            parameterRegisterCount++;
        }
        return parameterRegisterCount;
    }

    public SmaliParameterStatement[] getParameterStatements() {
        return findChildrenByClass(SmaliParameterStatement.class);
    }

    @Override public boolean isConstructor() {
        return hasModifierProperty("constructor") && !hasModifierProperty("static");
    }

    @Override public boolean isVarArgs() {
        return hasModifierProperty("varargs");
    }

    @NotNull @Override public MethodSignature getSignature(@NotNull PsiSubstitutor substitutor) {
        return MethodSignatureBackedByPsiMethod.create(this, substitutor);
    }

    @NotNull @Override public SmaliMemberName getNameIdentifier() {
        SmaliMemberName memberName = findChildByClass(SmaliMemberName.class);
        assert memberName != null;
        return memberName;
    }

    @NotNull @Override public PsiMethod[] findSuperMethods() {
        return PsiSuperMethodImplUtil.findSuperMethods(this);
    }

    @NotNull @Override public PsiMethod[] findSuperMethods(boolean checkAccess) {
        return PsiSuperMethodImplUtil.findSuperMethods(this, checkAccess);
    }

    @NotNull @Override public PsiMethod[] findSuperMethods(PsiClass parentClass) {
        return PsiSuperMethodImplUtil.findSuperMethods(this, parentClass);
    }

    @NotNull @Override
    public List<MethodSignatureBackedByPsiMethod> findSuperMethodSignaturesIncludingStatic(boolean checkAccess) {
        return PsiSuperMethodImplUtil.findSuperMethodSignaturesIncludingStatic(this, checkAccess);
    }

    @Nullable @Override public PsiMethod findDeepestSuperMethod() {
        return PsiSuperMethodImplUtil.findDeepestSuperMethod(this);
    }

    @NotNull @Override public PsiMethod[] findDeepestSuperMethods() {
        return PsiSuperMethodImplUtil.findDeepestSuperMethods(this);
    }

    @NotNull @Override public SmaliModifierList getModifierList() {
        return getRequiredStubOrPsiChild(SmaliElementTypes.MODIFIER_LIST);
    }

    @Override public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
        // TODO: implement this
        throw new IncorrectOperationException();
    }

    @NotNull @Override public HierarchicalMethodSignature getHierarchicalMethodSignature() {
        return PsiSuperMethodImplUtil.getHierarchicalMethodSignature(this);
    }

    @Nullable @Override public PsiDocComment getDocComment() {
        // not applicable
        return null;
    }

    @Override public boolean isDeprecated() {
        return PsiImplUtil.isDeprecatedByAnnotation(this);
    }

    @Nullable @Override public PsiTypeParameterList getTypeParameterList() {
        // TODO: (generics) implement this
        return null;
    }

    @NotNull @Override public PsiTypeParameter[] getTypeParameters() {
        // TODO: (generics) implement this
        return new PsiTypeParameter[0];
    }

    @Nullable @Override public PsiClass getContainingClass() {
        return (SmaliClass)getStubOrPsiParent();
    }

    @Override public boolean hasModifierProperty(@ModifierConstant @NonNls @NotNull String name) {
        return getModifierList().hasModifierProperty(name);
    }

    @Nullable @Override public SmaliAccessList getAccessFlagsNode() {
        return findChildByClass(SmaliAccessList.class);
    }

    @NotNull @Override public SmaliAnnotation[] getAnnotations() {
        return getStubOrPsiChildren(SmaliElementTypes.ANNOTATION, new SmaliAnnotation[0]);
    }

    @NotNull @Override public SmaliAnnotation[] getApplicableAnnotations() {
        return getAnnotations();
    }

    @Nullable @Override public SmaliAnnotation findAnnotation(@NotNull @NonNls String qualifiedName) {
        for (SmaliAnnotation annotation: getAnnotations()) {
            if (qualifiedName.equals(annotation.getQualifiedName())) {
                return annotation;
            }
        }
        return null;
    }

    @NotNull @Override public SmaliAnnotation addAnnotation(@NotNull @NonNls String qualifiedName) {
        // TODO: implement this
        return null;
    }
}
