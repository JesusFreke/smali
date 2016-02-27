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
import com.intellij.psi.PsiReferenceList;
import com.intellij.psi.StubBasedPsiElement;
import com.intellij.psi.stubs.IStubElementType;
import org.jetbrains.annotations.NotNull;
import org.jf.smalidea.psi.stub.SmaliBaseReferenceListStub;
import org.jf.smalidea.util.NameUtils;

public abstract class SmaliBaseReferenceList<StubT extends SmaliBaseReferenceListStub>
        extends SmaliStubBasedPsiElement<StubT> implements StubBasedPsiElement<StubT>, PsiReferenceList {
    protected SmaliBaseReferenceList(@NotNull StubT stub, @NotNull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    protected SmaliBaseReferenceList(@NotNull ASTNode node) {
        super(node);
    }

    @NotNull @Override public SmaliClassType[] getReferencedTypes() {
        StubT stub = getStub();
        if (stub != null) {
            return stub.getReferencedTypes();
        }

        SmaliClassTypeElement[] references = getReferenceElements();

        SmaliClassType[] referenceTypes = new SmaliClassType[references.length];

        for (int i=0; i<references.length; i++) {
            referenceTypes[i] = references[i].getType();
        }
        return referenceTypes;
    }

    @NotNull public String[] getReferenceNames() {
        SmaliBaseReferenceListStub stub = getStub();

        if (stub != null) {
            String[] smaliNames = stub.getSmaliTypeNames();
            String[] referenceNames = new String[smaliNames.length];

            for (int i=0; i<smaliNames.length; i++) {
                referenceNames[i] = NameUtils.resolveSmaliToJavaType(this, smaliNames[i]);
            }

            return referenceNames;
        }

        SmaliClassTypeElement[] references = getReferenceElements();

        String[] referenceNames = new String[references.length];

        for (int i=0; i<references.length; i++) {
            referenceNames[i] = references[i].getCanonicalText();
        }
        return referenceNames;
    }

    @NotNull public String[] getSmaliNames() {
        SmaliBaseReferenceListStub stub = getStub();

        if (stub != null) {
            return stub.getSmaliTypeNames();
        }

        SmaliClassTypeElement[] references = getReferenceElements();

        String[] smaliNames = new String[references.length];

        for (int i=0; i<references.length; i++) {
            smaliNames[i] = references[i].getSmaliName();
        }
        return smaliNames;
    }

    @Override public boolean isWritable() {
        return false;
    }

    @NotNull @Override public abstract SmaliClassTypeElement[] getReferenceElements();
}
