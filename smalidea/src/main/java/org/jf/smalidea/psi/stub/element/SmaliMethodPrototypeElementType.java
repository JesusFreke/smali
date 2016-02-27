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

package org.jf.smalidea.psi.stub.element;

import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jf.smalidea.psi.impl.SmaliMethodPrototype;
import org.jf.smalidea.psi.impl.SmaliTypeElement;
import org.jf.smalidea.psi.stub.SmaliMethodPrototypeStub;

import java.io.IOException;

public class SmaliMethodPrototypeElementType
        extends SmaliStubElementType<SmaliMethodPrototypeStub, SmaliMethodPrototype> {
    public static final SmaliMethodPrototypeElementType INSTANCE = new SmaliMethodPrototypeElementType();

    private SmaliMethodPrototypeElementType() {
        super("METHOD_PROTOTYPE");
    }

    @NotNull @Override public String getExternalId() {
        return "smali.method_prototype";
    }

    @Override public SmaliMethodPrototype createPsi(@NotNull ASTNode node) {
        return new SmaliMethodPrototype(node);
    }

    @Override public SmaliMethodPrototype createPsi(@NotNull SmaliMethodPrototypeStub stub) {
        return new SmaliMethodPrototype(stub);
    }

    @Override public SmaliMethodPrototypeStub createStub(@NotNull SmaliMethodPrototype psi, StubElement parentStub) {
        SmaliTypeElement returnType = psi.getReturnTypeElement();
        String returnSmaliTypeName = null;
        if (returnType != null) {
            returnSmaliTypeName = returnType.getSmaliName();
        }

        return new SmaliMethodPrototypeStub(parentStub, returnSmaliTypeName);
    }

    @Override
    public void serialize(@NotNull SmaliMethodPrototypeStub stub, @NotNull StubOutputStream dataStream)
            throws IOException {
        dataStream.writeName(stub.getReturnSmaliTypeName());
    }

    @NotNull @Override
    public SmaliMethodPrototypeStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub)
            throws IOException {
        return new SmaliMethodPrototypeStub(parentStub, deserializeNullableString(dataStream));
    }

    @Override public void indexStub(@NotNull SmaliMethodPrototypeStub stub, @NotNull IndexSink sink) {
    }
}
