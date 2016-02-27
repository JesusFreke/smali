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

package org.jf.smalidea.psi.stub;

import com.intellij.psi.PsiManager;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jf.smalidea.psi.impl.LightSmaliClassTypeElement;
import org.jf.smalidea.psi.impl.SmaliBaseReferenceList;
import org.jf.smalidea.psi.impl.SmaliClassType;

public abstract class SmaliBaseReferenceListStub<T extends SmaliBaseReferenceList> extends StubBase<T> {
    @NotNull private final String[] smaliTypeNames;
    @Nullable private SmaliClassType[] classTypes = null;

    protected SmaliBaseReferenceListStub(
            @NotNull StubElement parent, @NotNull IStubElementType elementType, @NotNull String[] smaliTypeNames) {
        super(parent, elementType);
        this.smaliTypeNames = smaliTypeNames;
    }

    @NotNull public String[] getSmaliTypeNames() {
        return smaliTypeNames;
    }

    @NotNull
    public SmaliClassType[] getReferencedTypes() {
        if (classTypes == null) {
            classTypes = new SmaliClassType[smaliTypeNames.length];
            for (int i = 0; i< smaliTypeNames.length; i++) {
                classTypes[i] = new SmaliClassType(
                        new LightSmaliClassTypeElement(PsiManager.getInstance(getProject()), smaliTypeNames[i]));
            }
        }
        return classTypes;
    }
}
