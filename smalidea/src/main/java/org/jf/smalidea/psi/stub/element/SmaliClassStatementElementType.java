/*
 * Copyright 2015, Google Inc.
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
import org.jf.smalidea.psi.impl.SmaliClassStatement;
import org.jf.smalidea.psi.stub.SmaliClassStatementStub;

import java.io.IOException;

public class SmaliClassStatementElementType extends SmaliStubElementType<SmaliClassStatementStub, SmaliClassStatement> {
    public static final SmaliClassStatementElementType INSTANCE = new SmaliClassStatementElementType();

    public SmaliClassStatementElementType() {
        super("CLASS_STATEMENT");
    }

    @Override public SmaliClassStatement createPsi(@NotNull ASTNode node) {
        return new SmaliClassStatement(node);
    }

    @Override public SmaliClassStatement createPsi(@NotNull SmaliClassStatementStub stub) {
        return new SmaliClassStatement(stub);
    }

    @Override public SmaliClassStatementStub createStub(@NotNull SmaliClassStatement psi, StubElement parentStub) {
        return new SmaliClassStatementStub(parentStub, psi.getQualifiedName());
    }

    @NotNull @Override public String getExternalId() {
        return "smali.class_statement";
    }

    @Override
    public void serialize(@NotNull SmaliClassStatementStub stub, @NotNull StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getQualifiedName());
    }

    @NotNull @Override
    public SmaliClassStatementStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        return new SmaliClassStatementStub(parentStub, deserializeNullableString(dataStream));
    }

    @Override public void indexStub(@NotNull SmaliClassStatementStub stub, @NotNull IndexSink sink) {
    }
}
