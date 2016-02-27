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
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jf.smalidea.psi.SmaliElementTypes;
import org.jf.smalidea.psi.iface.SmaliModifierListOwner;
import org.jf.smalidea.psi.stub.SmaliMethodParameterStub;
import org.jf.smalidea.util.NameUtils;

public class SmaliMethodParameter extends SmaliStubBasedPsiElement<SmaliMethodParameterStub>
        implements PsiParameter, SmaliModifierListOwner {
    public SmaliMethodParameter(@NotNull SmaliMethodParameterStub stub) {
        super(stub, SmaliElementTypes.METHOD_PARAMETER);
    }

    public SmaliMethodParameter(@NotNull ASTNode node) {
        super(node);
    }

    @NotNull @Override public SmaliModifierList getModifierList() {
        return getRequiredStubOrPsiChild(SmaliElementTypes.MODIFIER_LIST);
    }

    @NotNull @Override public PsiElement getDeclarationScope() {
        return getParentMethod();
    }

    @Override public boolean isVarArgs() {
        if (getType().getArrayDimensions() == 0 || !getParentMethod().isVarArgs()) {
            return false;
        }

        SmaliMethodParamList paramList = getStubOrPsiParentOfType(SmaliMethodParamList.class);
        if (paramList == null) {
            return false;
        }
        SmaliMethodParameter[] parameters = paramList.getParameters();
        // is this the last parameter?
        return parameters[parameters.length-1] == this;
    }

    @NotNull @Override public SmaliTypeElement getTypeElement() {
        SmaliTypeElement typeElement = findChildByClass(SmaliTypeElement.class);
        assert typeElement != null;
        return typeElement;
    }

    @NotNull @Override public PsiType getType() {
        SmaliMethodParameterStub stub = getStub();
        if (stub != null) {
            return NameUtils.resolveSmaliToPsiType(this, stub.getSmaliTypeName());
        }
        return getTypeElement().getType();
    }

    @Nullable @Override public PsiExpression getInitializer() {
        // not applicable
        return null;
    }

    @Override public boolean hasInitializer() {
        return false;
    }

    @Override public void normalizeDeclaration() throws IncorrectOperationException {
        // not applicable
    }

    @Nullable @Override public Object computeConstantValue() {
        // not applicable
        return null;
    }

    @Nullable @Override public String getName() {
        SmaliMethodParameterStub stub = getStub();
        if (stub != null) {
            return stub.getName();
        }
        SmaliLocalName name = getNameIdentifier();
        if (name == null) {
            return null;
        }
        // TODO: get the actual string value
        return getNameIdentifier().getText();
    }

    @Nullable @Override public SmaliLocalName getNameIdentifier() {
        SmaliParameterStatement parameterStatement = findParameterStatement();
        if (parameterStatement == null) {
            return null;
        }

        return parameterStatement.getNameIdentifier();
    }

    @Override public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
        // TODO: implement this
        throw new UnsupportedOperationException();
    }

    @Override public boolean hasModifierProperty(@ModifierConstant @NonNls @NotNull String name) {
        // not applicable
        return false;
    }

    /**
     * Returns the number of registers required for this parameter. 1 for most types, but 2 for double/long.
     */
    public int getRegisterCount() {
        PsiType type = getType();
        if (type == PsiType.DOUBLE || type == PsiType.LONG) {
            return 2;
        }
        return 1;
    }

    @NotNull public SmaliMethod getParentMethod() {
        SmaliMethod smaliMethod = findStubOrPsiAncestorOfType(SmaliMethod.class);
        assert smaliMethod != null;
        return smaliMethod;
    }

    /**
     * Gets the parameter register number of this parameters. This is the number of a pNN style register reference.
     */
    public int getParameterRegisterNumber() {
        // TODO: it might be a good idea to cache this, or at least do it non-recursively
        PsiElement prevSibling = getPrevSibling();
        if (prevSibling == null) {
            return getParentMethod().isStatic() ? 0 : 1;
        }
        assert prevSibling instanceof SmaliMethodParameter;
        SmaliMethodParameter prevParam = (SmaliMethodParameter)prevSibling;
        return prevParam.getParameterRegisterNumber() + prevParam.getRegisterCount();
    }

    /**
     * Gets the register number of this parameters. This is the number of a rNN style register reference.
     */
    public int getRegisterNumber() {
        SmaliMethod parentMethod = getParentMethod();
        return getParameterRegisterNumber() + parentMethod.getRegisterCount() -
                parentMethod.getParameterRegisterCount();
    }

    @Nullable
    private SmaliParameterStatement findParameterStatement() {
        SmaliMethod parentMethod = getParentMethod();

        for (SmaliParameterStatement parameterStatement: parentMethod.getParameterStatements()) {
            SmaliRegisterReference registerReference = parameterStatement.getParameterRegister();
            if (registerReference != null && registerReference.getRegisterNumber() == getRegisterNumber()) {
                return parameterStatement;
            }
        }
        return null;
    }

    @NotNull @Override public SmaliAnnotation[] getAnnotations() {
        SmaliParameterStatement parameterStatement = findParameterStatement();
        if (parameterStatement == null) {
            return new SmaliAnnotation[0];
        }
        return parameterStatement.getAnnotations();
    }

    @NotNull @Override public SmaliAnnotation[] getApplicableAnnotations() {
        return getAnnotations();
    }

    @Nullable @Override public SmaliAnnotation findAnnotation(@NotNull @NonNls String qualifiedName) {
        SmaliParameterStatement parameterStatement = findParameterStatement();
        if (parameterStatement == null) {
            return null;
        }
        return parameterStatement.findAnnotation(qualifiedName);
    }

    @NotNull @Override public SmaliAnnotation addAnnotation(@NotNull @NonNls String qualifiedName) {
        SmaliParameterStatement parameterStatement = findParameterStatement();
        if (parameterStatement == null) {
            // TODO: add a parameter statement for this parameter if not found
            throw new UnsupportedOperationException();
        }
        return parameterStatement.addAnnotation(qualifiedName);
    }
}
