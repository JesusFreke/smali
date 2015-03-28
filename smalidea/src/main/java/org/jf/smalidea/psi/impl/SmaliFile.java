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

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.StubElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jf.smalidea.SmaliFileType;
import org.jf.smalidea.SmaliLanguage;
import org.jf.smalidea.psi.SmaliElementTypes;

public class SmaliFile extends PsiFileBase implements PsiClassOwner {
    public SmaliFile(FileViewProvider viewProvider) {
        super(viewProvider, SmaliLanguage.INSTANCE);
    }

    @NotNull @Override public SmaliFileType getFileType() {
        return SmaliFileType.INSTANCE;
    }

    @Nullable
    public SmaliClass getPsiClass() {
        StubElement<? extends PsiElement> stub = (StubElement<? extends PsiElement>)getStub();
        if (stub != null) {
            StubElement<SmaliClass> classElement = stub.findChildStubByType(SmaliElementTypes.CLASS);
            if (classElement != null) {
                return classElement.getPsi();
            } else {
                return null;
            }
        } else {
            return findChildByClass(SmaliClass.class);
        }
    }

    @NotNull @Override public SmaliClass[] getClasses() {
        SmaliClass smaliClass = getPsiClass();
        if (smaliClass == null) {
            return new SmaliClass[] {};
        } else {
            return new SmaliClass[] { smaliClass };
        }
    }

    @NotNull @Override public String getPackageName() {
        SmaliClass smaliClass = getPsiClass();
        if (smaliClass == null) {
            return "";
        }
        return smaliClass.getPackageName();
    }

    @Override public void setPackageName(String packageName) throws IncorrectOperationException {
        SmaliClass smaliClass = getPsiClass();
        if (smaliClass == null) {
            return;
        }
        smaliClass.setPackageName(packageName);
    }
}
