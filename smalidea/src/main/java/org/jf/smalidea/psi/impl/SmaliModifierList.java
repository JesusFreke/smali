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
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifier.ModifierConstant;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.StubBasedPsiElement;
import com.intellij.psi.impl.source.tree.Factory;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jf.dexlib2.AccessFlags;
import org.jf.smalidea.SmaliTokens;
import org.jf.smalidea.psi.iface.SmaliModifierListOwner;
import org.jf.smalidea.psi.stub.SmaliModifierListStub;
import org.jf.smalidea.psi.stub.element.SmaliModifierListElementType;

import javax.annotation.Nonnull;

public class SmaliModifierList extends SmaliStubBasedPsiElement<SmaliModifierListStub>
        implements StubBasedPsiElement<SmaliModifierListStub>, PsiModifierList {
    public SmaliModifierList(@NotNull ASTNode node) {
        super(node);
    }

    public SmaliModifierList(@NotNull SmaliModifierListStub stub) {
        super(stub, SmaliModifierListElementType.INSTANCE);
    }

    public int getAccessFlags() {
        SmaliModifierListStub stub = getStub();
        if (stub != null) {
            return stub.getAccessFlags();
        }

        int flags = 0;

        for (PsiElement accessSpec: findChildrenByType(SmaliTokens.ACCESS_SPEC)) {
            AccessFlags flag = AccessFlags.getAccessFlag(accessSpec.getText());
            if (flag != null) {
                flags |= flag.getValue();
            }
        }

        return flags;
    }

    @Override public boolean hasModifierProperty(@ModifierConstant @NotNull @NonNls String name) {
        return hasExplicitModifier(name);
    }

    @Override public boolean hasExplicitModifier(@ModifierConstant @NotNull @NonNls String name) {
        SmaliModifierListStub stub = getStub();
        if (stub != null) {
            AccessFlags flag = AccessFlags.getAccessFlag(name);
            if (flag == null) {
                return false;
            }
            return (stub.getAccessFlags() & flag.getValue()) != 0;
        }

        for (PsiElement accessSpec: findChildrenByType(SmaliTokens.ACCESS_SPEC)) {
            if (accessSpec.getText().equals(name)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void setModifierProperty(@ModifierConstant @NotNull @NonNls String name, boolean addModifier)
            throws IncorrectOperationException {
        if (addModifier) {

            final TreeElement leaf = Factory.createSingleLeafElement(SmaliTokens.ACCESS_SPEC, name, null, getManager());

            new WriteCommandAction.Simple(getProject(), getContainingFile()) {
                @Override protected void run() throws Throwable {
                    addInternal(leaf, leaf, null, null);
                }
            }.execute();
        } else {
            final PsiElement accessSpec = getAccessFlagElement(name);
            if (accessSpec != null) {
                new WriteCommandAction.Simple(getProject(), getContainingFile()) {
                    @Override protected void run() throws Throwable {
                        accessSpec.delete();
                    }
                }.execute();
            }
        }
    }

    @Override
    public void checkSetModifierProperty(@ModifierConstant @NotNull @NonNls String name, boolean addModifier)
            throws IncorrectOperationException {
    }

    @Nonnull
    private SmaliModifierListOwner getParentForAnnotations() {
        SmaliModifierListOwner parent = (SmaliModifierListOwner)getStubOrPsiParentOfType(PsiModifierListOwner.class);
        assert parent != null;
        return parent;
    }

    @NotNull @Override public SmaliAnnotation[] getAnnotations() {
        return getParentForAnnotations().getAnnotations();
    }

    @NotNull @Override public SmaliAnnotation[] getApplicableAnnotations() {
        return getParentForAnnotations().getApplicableAnnotations();
    }

    @Nullable @Override public SmaliAnnotation findAnnotation(@NotNull @NonNls String qualifiedName) {
        return getParentForAnnotations().findAnnotation(qualifiedName);
    }

    @NotNull @Override public SmaliAnnotation addAnnotation(@NotNull @NonNls String qualifiedName) {
        return getParentForAnnotations().addAnnotation(qualifiedName);
    }

    @Nullable public PsiElement getAccessFlagElement(@NotNull String accessFlag) {
        for (PsiElement accessSpec: findChildrenByType(SmaliTokens.ACCESS_SPEC)) {
            if (accessSpec.getText().equals(accessFlag)) {
                return accessSpec;
            }
        }
        return null;
    }
}
