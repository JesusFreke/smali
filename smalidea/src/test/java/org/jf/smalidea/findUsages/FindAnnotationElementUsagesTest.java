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

public class FindAnnotationElementUsagesTest extends FindUsagesTest {
    public void testSmaliUsageInSmaliFile() throws Exception {
        addFile("AnnotationWithValues.smali", "" +
                ".class public abstract interface annotation LAnnotationWithValues;\n" +
                ".super Ljava/lang/Object;\n" +
                ".implements Ljava/lang/annotation/Annotation;\n" +
                "\n" +
                ".method public abstract intValue()I\n" +
                ".end method\n" +
                ".annotation system Ldalvik/annotation/AnnotationDefault;\n" +
                "    value = .subannotation LAnnotationWithValues;\n" +
                "                int<usage>Value = 4\n" +
                "            .end subannotation\n" +
                ".end annotation");
        addFile("blarg.smali", "" +
                ".class public Lblah;\n" +
                ".super Ljava/lang/Object;\n" +
                ".annotation runtime LAnnotationWithValues;\n" +
                "  int<usage><ref>Value = 123\n" +
                ".end annotation");
        doTest();
    }

    public void testJavaUsageInSmaliFile() throws Exception {
        addFile("AnnotationWithValues.java", "" +
                "\n" +
                "public @interface AnnotationWithValues {\n" +
                "    int int<ref>Value() default 4;\n" +
                "}");
        addFile("blarg.smali", "" +
                ".class public Lblah;\n" +
                ".super Ljava/lang/Object;\n" +
                ".annotation runtime LAnnotationWithValues;\n" +
                "  int<usage>Value = 123\n" +
                ".end annotation");
        doTest();
    }

    public void testSmaliUsageInJavaFile() throws Exception {
        addFile("AnnotationWithValues.smali", "" +
                ".class public abstract interface annotation LAnnotationWithValues;\n" +
                ".super Ljava/lang/Object;\n" +
                ".implements Ljava/lang/annotation/Annotation;\n" +
                "\n" +
                ".method public abstract intValue()I\n" +
                ".end method\n" +
                ".annotation system Ldalvik/annotation/AnnotationDefault;\n" +
                "    value = .subannotation LAnnotationWithValues;\n" +
                "                int<usage><ref>Value = 4\n" +
                "            .end subannotation\n" +
                ".end annotation");
        addFile("blarg.java", "" +
                "\n" +
                "@AnnotationWithValues(int<usage>Value=123)\n" +
                "public class blarg {}");
        doTest();
    }
}
