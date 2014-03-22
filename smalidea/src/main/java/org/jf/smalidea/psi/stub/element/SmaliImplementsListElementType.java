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
import org.jf.smalidea.psi.SmaliElementTypes;
import org.jf.smalidea.psi.impl.SmaliImplementsList;
import org.jf.smalidea.psi.stub.SmaliImplementsListStub;
import org.jf.smalidea.psi.stub.SmaliStubElementType;

import java.io.IOException;

public class SmaliImplementsListElementType extends
        SmaliStubElementType<SmaliImplementsListStub, SmaliImplementsList> {
    public static final SmaliImplementsListElementType INSTANCE = new SmaliImplementsListElementType();

    private SmaliImplementsListElementType() {
        super("IMPLEMENTS_LIST");
    }

    @NotNull @Override public String getExternalId() {
        return "smali.implements_list";
    }

    @Override public SmaliImplementsList createPsi(@NotNull ASTNode node) {
        return new SmaliImplementsList(node);
    }

    @Override public SmaliImplementsList createPsi(@NotNull SmaliImplementsListStub stub) {
        return new SmaliImplementsList(stub);
    }

    @Override public SmaliImplementsListStub createStub(@NotNull SmaliImplementsList psi, StubElement parentStub) {
        return new SmaliImplementsListStub(parentStub, SmaliElementTypes.IMPLEMENTS_LIST);
    }

    @Override
    public void serialize(@NotNull SmaliImplementsListStub stub, @NotNull StubOutputStream dataStream)
            throws IOException {
    }

    @NotNull @Override
    public SmaliImplementsListStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub)
            throws IOException {
        return new SmaliImplementsListStub(parentStub, SmaliElementTypes.IMPLEMENTS_LIST);
    }

    @Override public void indexStub(@NotNull SmaliImplementsListStub stub, @NotNull IndexSink sink) {
    }
}
