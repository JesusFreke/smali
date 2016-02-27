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
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jf.smalidea.psi.SmaliElementTypes;
import org.jf.smalidea.psi.iface.SmaliModifierListOwner;
import org.jf.smalidea.psi.leaf.SmaliClassDescriptor;
import org.jf.smalidea.psi.stub.SmaliClassStatementStub;
import org.jf.smalidea.util.NameUtils;

public class SmaliClassStatement extends SmaliStubBasedPsiElement<SmaliClassStatementStub>
        implements SmaliModifierListOwner {
    public SmaliClassStatement(@NotNull SmaliClassStatementStub stub) {
        super(stub, SmaliElementTypes.CLASS_STATEMENT);
    }

    public SmaliClassStatement(@NotNull ASTNode node) {
        super(node);
    }

    @Nullable
    public SmaliClassTypeElement getNameElement() {
        return findChildByClass(SmaliClassTypeElement.class);
    }

    @Nullable
    public SmaliClass getContainingClass() {
        return getStubOrPsiParentOfType(SmaliClass.class);
    }

    @Nullable
    public SmaliClassDescriptor getNameIdentifier() {
        SmaliClassTypeElement classTypeElement = getNameElement();
        if (classTypeElement == null) {
            return null;
        }
        return classTypeElement.getReferenceNameElement();
    }

    /**
     * @return the fully qualified java-style name of the class in this .class statement
     */
    @Nullable
    public String getQualifiedName() {
        SmaliClassStatementStub stub = getStub();
        if (stub != null) {
            return stub.getQualifiedName();
        }

        SmaliClassTypeElement classType = findChildByClass(SmaliClassTypeElement.class);
        if (classType == null) {
            return null;
        }
        // Since this is a class declared in smali, we don't have to worry about handling inner classes,
        // so we can do a pure textual translation of the class name
        return NameUtils.smaliToJavaType(classType.getSmaliName());
    }

    @Nullable
    public SmaliModifierList getModifierList() {
        return getStubOrPsiChild(SmaliElementTypes.MODIFIER_LIST);
    }

    @NotNull
    @Override
    public SmaliAnnotation addAnnotation(@NotNull @NonNls String qualifiedName) {
        SmaliClass containingClass = getContainingClass();
        if (containingClass == null) {
            // TODO: what should we do here?
            return null;
        }
        return containingClass.addAnnotation(qualifiedName);
    }

    @NotNull
    @Override
    public SmaliAnnotation[] getAnnotations() {
        SmaliClass containingClass = getContainingClass();
        if (containingClass == null) {
            return new SmaliAnnotation[0];
        }
        return containingClass.getAnnotations();
    }

    @NotNull
    @Override
    public SmaliAnnotation[] getApplicableAnnotations() {
        SmaliClass containingClass = getContainingClass();
        if (containingClass == null) {
            return new SmaliAnnotation[0];
        }
        return containingClass.getApplicableAnnotations();
    }

    @Nullable
    @Override
    public SmaliAnnotation findAnnotation(@NotNull @NonNls String qualifiedName) {
        SmaliClass containingClass = getContainingClass();
        if (containingClass == null) {
            return null;
        }
        return containingClass.findAnnotation(qualifiedName);
    }

    @Override
    public boolean hasModifierProperty(@NonNls @NotNull String name) {
        SmaliClass containingClass = getContainingClass();
        if (containingClass == null) {
            return false;
        }
        return containingClass.hasModifierProperty(name);
    }
}
