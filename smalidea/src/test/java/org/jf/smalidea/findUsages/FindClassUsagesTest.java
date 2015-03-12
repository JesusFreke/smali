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

public class FindClassUsagesTest extends FindUsagesTest {
    public void testSmaliUsageInSmaliFile() throws Exception {
        addFile("blah.smali", ".class public Lbl<ref><usage>ah; .super Ljava/lang/Object;");
        addFile("blarg.smali", "" +
                ".class public Lblarg; .super Ljava/lang/Object;\n" +
                ".method public doSomething()V\n" +
                "  .registers 1\n" +
                "  new-instance v0, Lbl<usage>ah;\n" +
                "  invoke-direct {v0}, Lbl<usage>ah;-><init>()V\n" +
                "  return-void\n" +
                ".end method");
        doTest();
    }

    public void testSmaliUsageInJavaFile() throws Exception {
        addFile("blah.smali", ".class public Lbl<ref><usage>ah; .super Ljava/lang/Object;");
        addFile("blarg.java", "" +
                "public class blarg {\n" +
                "    public void blargMethod() {\n" +
                "        new bl<usage>ah();\n" +
                "    }\n" +
                "}");
        doTest();
    }

    public void testJavaUsageInSmaliFile() throws Exception {
        addFile("blah.java", "" +
                "public class blah {\n" +
                "    public bl<usage>ah blahMethod() {\n" +
                "        return new bl<usage><ref>ah();\n" +
                "    }\n" +
                "}");
        addFile("blarg.smali", "" +
                ".class public Lblarg; .super Ljava/lang/Object;\n" +
                ".method public doSomething()V\n" +
                "  .registers 1\n" +
                "  new-instance v0, Lbl<usage>ah;\n" +
                "  invoke-direct {v0}, Lbl<usage>ah;-><init>()V\n" +
                "  return-void\n" +
                ".end method");
        doTest();
    }
}
