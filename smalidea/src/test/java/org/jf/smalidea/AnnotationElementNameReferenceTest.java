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

package org.jf.smalidea;

import com.intellij.psi.*;
import com.intellij.testFramework.ResolveTestCase;
import org.junit.Assert;

public class AnnotationElementNameReferenceTest extends ResolveTestCase {
    public void testSmaliReferenceFromSmali() throws Exception {
        createFile("AnnotationWithValues.smali", "" +
                ".class public abstract interface annotation LAnnotationWithValues;\n" +
                ".super Ljava/lang/Object;\n" +
                ".implements Ljava/lang/annotation/Annotation;\n" +
                "\n" +
                ".method public abstract intValue()I\n" +
                ".end method");

        PsiReference reference = configureByFileText("" +
                ".class public Lblah;\n" +
                ".super Ljava/lang/Object;\n" +
                ".annotation runtime LAnnotationWithValues;\n" +
                "  int<ref>Value = 123\n" +
                ".end annotation", "blah.smali");

        PsiElement resolved = reference.resolve();
        Assert.assertNotNull(resolved);
        Assert.assertTrue(resolved instanceof PsiAnnotationMethod);
        Assert.assertEquals("intValue", ((PsiAnnotationMethod)resolved).getName());
        Assert.assertEquals("AnnotationWithValues",
                ((PsiAnnotationMethod)resolved).getContainingClass().getQualifiedName());
    }

    public void testJavaReferenceFromSmali() throws Exception {
        createFile("AnnotationWithValues.java", "" +
                "public @interface AnnotationWithValues {\n" +
                "    int intValue();\n" +
                "}");

        PsiReference reference = configureByFileText("" +
                ".class public Lblah;\n" +
                ".super Ljava/lang/Object;\n" +
                ".annotation runtime LAnnotationWithValues;\n" +
                "  int<ref>Value = 123\n" +
                ".end annotation", "blah.smali");

        PsiElement resolved = reference.resolve();
        Assert.assertNotNull(resolved);
        Assert.assertTrue(resolved instanceof PsiAnnotationMethod);
        Assert.assertEquals("intValue", ((PsiAnnotationMethod)resolved).getName());
        Assert.assertEquals("AnnotationWithValues",
                ((PsiAnnotationMethod)resolved).getContainingClass().getQualifiedName());
    }

    public void testSmaliReferenceFromJava() throws Exception {
        createFile("AnnotationWithValues.smali", "" +
                ".class public abstract interface annotation LAnnotationWithValues;\n" +
                ".super Ljava/lang/Object;\n" +
                ".implements Ljava/lang/annotation/Annotation;\n" +
                "\n" +
                ".method public abstract intValue()I\n" +
                ".end method");

        PsiReference reference = configureByFileText("" +
                "@AnnotationWithValues(int<ref>Value=123)\n" +
                "public class blah {}", "blah.java");

        PsiElement resolved = reference.resolve();
        Assert.assertNotNull(resolved);
        Assert.assertTrue(resolved instanceof PsiAnnotationMethod);
        Assert.assertEquals("intValue", ((PsiAnnotationMethod)resolved).getName());
        Assert.assertEquals("AnnotationWithValues",
                ((PsiAnnotationMethod)resolved).getContainingClass().getQualifiedName());
    }
}
