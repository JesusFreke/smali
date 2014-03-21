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
import org.jf.smalidea.psi.impl.SmaliModifierList;
import org.jf.smalidea.psi.stub.SmaliModifierListStub;
import org.jf.smalidea.psi.stub.SmaliStubElementType;

import java.io.IOException;

public class SmaliModifierListElementType extends SmaliStubElementType<SmaliModifierListStub, SmaliModifierList> {
    public static final SmaliModifierListElementType INSTANCE = new SmaliModifierListElementType();

    private SmaliModifierListElementType() {
        super("MODIFIER_LIST");
    }

    @NotNull @Override public String getExternalId() {
        return "smali.modifier_list";
    }

    @Override public SmaliModifierList createPsi(@NotNull SmaliModifierListStub stub) {
        return new SmaliModifierList(stub);
    }

    @Override public SmaliModifierList createPsi(@NotNull ASTNode node) {
        return new SmaliModifierList(node);
    }

    @Override public SmaliModifierListStub createStub(@NotNull SmaliModifierList psi, StubElement parentStub) {
        return new SmaliModifierListStub(parentStub, psi.getAccessFlags());
    }

    @Override
    public void serialize(@NotNull SmaliModifierListStub stub, @NotNull StubOutputStream dataStream)
            throws IOException {
        dataStream.writeVarInt(stub.getAccessFlags());
    }

    @NotNull @Override
    public SmaliModifierListStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub)
            throws IOException {
        return new SmaliModifierListStub(parentStub, dataStream.readVarInt());
    }

    @Override public void indexStub(@NotNull SmaliModifierListStub stub, @NotNull IndexSink sink) {
    }
}
