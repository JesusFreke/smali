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

package org.jf.smalidea.findUsages;

public class FindMethodUsagesTest extends FindUsagesTest {
    public void testSmaliUsageInSmaliFile() throws Exception {
        addFile("blah.smali", "" +
                ".class public Lblah;\n" +
                ".super Ljava/lang/Object;\n" +
                ".method abstract blah<ref>Method()V\n" +
                ".end method");
        addFile("blarg.smali", "" +
                ".class public Lblarg;\n" +
                ".super Ljava/lang/Object;\n" +
                ".method abstract blargMethod()V\n" +
                "  invoke-virtual {v0}, Lblah;->blah<usage>Method()V\n" +
                ".end method");
        doTest();
    }

    public void testSmaliUsageInJavaFile() throws Exception {
        addFile("blah.smali", "" +
                ".class public Lblah;\n" +
                ".super Ljava/lang/Object;\n" +
                ".method abstract blah<ref>Method()V\n" +
                ".end method");
        addFile("blarg.java", "" +
                "public class blarg {\n" +
                "    public void blargMethod() {\n" +
                "        blah b = new blah();\n" +
                "        b.blah<usage>Method();\n" +
                "    }\n" +
                "}");
        doTest();
    }

    public void testJavaUsageInSmaliFile() throws Exception {
        addFile("blah.java", "" +
                "public class blah {\n" +
                "    public void blah<ref>Method() {\n" +
                "    }\n" +
                "}");
        addFile("blarg.smali", "" +
                ".class public Lblarg;\n" +
                ".super Ljava/lang/Object;\n" +
                ".method abstract blargMethod()V\n" +
                "  invoke-virtual {v0}, Lblah;->blah<usage>Method()V\n" +
                ".end method");
        doTest();
    }

    public void testPrimitiveListMethod() throws Exception {
        addFile("blah.smali", "" +
                ".class public Lblah;\n" +
                ".super Ljava/lang/Object;\n" +
                ".method abstract II<ref>II()V\n" +
                ".end method");
        addFile("blarg.smali", "" +
                ".class public Lblarg;\n" +
                ".super Ljava/lang/Object;\n" +
                ".method abstract blargMethod()V\n" +
                "  invoke-virtual {v0}, Lblah;->II<usage>II()V\n" +
                ".end method");
       doTest();
    }
}
