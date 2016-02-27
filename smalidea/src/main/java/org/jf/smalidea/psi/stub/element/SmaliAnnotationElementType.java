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
import org.jf.smalidea.psi.impl.SmaliAnnotation;
import org.jf.smalidea.psi.stub.SmaliAnnotationStub;

import java.io.IOException;

public class SmaliAnnotationElementType extends SmaliStubElementType<SmaliAnnotationStub, SmaliAnnotation> {
    public static final SmaliAnnotationElementType INSTANCE = new SmaliAnnotationElementType();

    private SmaliAnnotationElementType() {
        super("ANNOTATION");
    }

    @NotNull @Override public String getExternalId() {
        return "smali.annotation";
    }

    @Override public SmaliAnnotation createPsi(@NotNull SmaliAnnotationStub stub) {
        return new SmaliAnnotation(stub);
    }

    @Override public SmaliAnnotation createPsi(@NotNull ASTNode node) {
        return new SmaliAnnotation(node);
    }

    @Override public SmaliAnnotationStub createStub(@NotNull SmaliAnnotation psi, StubElement parentStub) {
        return new SmaliAnnotationStub(parentStub, psi.getSmaliName());
    }

    @Override
    public void serialize(@NotNull SmaliAnnotationStub stub, @NotNull StubOutputStream dataStream) throws IOException {
        dataStream.writeName(stub.getAnnotationSmaliTypeName());
    }

    @NotNull @Override
    public SmaliAnnotationStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        return new SmaliAnnotationStub(parentStub, deserializeNullableString(dataStream));
    }

    @Override public void indexStub(@NotNull SmaliAnnotationStub stub, @NotNull IndexSink sink) {
    }
}
