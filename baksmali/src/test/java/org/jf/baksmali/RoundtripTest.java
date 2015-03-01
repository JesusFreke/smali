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

package org.jf.baksmali;

import org.antlr.runtime.RecognitionException;
import org.jf.baksmali.Adaptors.ClassDefinition;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.smali.SmaliTestUtils;
import org.jf.util.IndentingWriter;
import org.jf.util.TextUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

public class RoundtripTest {
    @Test
    public void testParamListMethodName() throws IOException, RecognitionException {
        String text = "" +
                ".class Lblah;\n" +
                ".super Ljava/lang/Object;\n" +
                "# virtual methods\n" +
                ".method public abstract II()V\n" +
                ".end method\n";
        ClassDef classDef = SmaliTestUtils.compileSmali(text);

        baksmaliOptions options = new baksmaliOptions();
        options.useImplicitReferences = true;

        StringWriter stringWriter = new StringWriter();
        IndentingWriter writer = new IndentingWriter(stringWriter);
        ClassDefinition classDefinition = new ClassDefinition(options, classDef);
        classDefinition.writeTo(writer);
        writer.close();

        Assert.assertEquals(TextUtils.normalizeWhitespace(text),
                TextUtils.normalizeWhitespace(stringWriter.toString()));
    }
}
