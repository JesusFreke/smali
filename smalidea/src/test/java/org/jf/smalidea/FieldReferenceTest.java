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

import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.impl.JavaAwareProjectJdkTableImpl;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiReference;
import com.intellij.testFramework.ResolveTestCase;
import org.jf.smalidea.psi.impl.SmaliFieldReference;
import org.junit.Assert;

public class FieldReferenceTest extends ResolveTestCase {
    /**
     * Test a reference to a java field from a smali class
     */
    public void testJavaReferenceFromSmali() throws Exception {
        String text =
                ".class public Lmy/pkg/blah; .super Ljava/lang/Object;\n" +
                        ".method public blah()V\n" +
                        "    .locals 1\n" +
                        "    sget-object v0, Ljava/lang/System;->o<ref>ut:Ljava/io/PrintStream;\n" +
                        "    return-void\n" +
                        ".end method";

        SmaliFieldReference fieldReference = (SmaliFieldReference)configureByFileText(text, "blah.smali");

        Assert.assertNotNull(fieldReference);
        Assert.assertEquals("out", fieldReference.getName());
        Assert.assertNotNull(fieldReference.getFieldType());
        Assert.assertEquals("java.io.PrintStream", fieldReference.getFieldType().getType().getCanonicalText());

        PsiField resolvedField = fieldReference.resolve();
        Assert.assertNotNull(resolvedField);
        Assert.assertEquals("out", resolvedField.getName());
        Assert.assertNotNull(resolvedField.getContainingClass());
        Assert.assertEquals("java.lang.System", resolvedField.getContainingClass().getQualifiedName());
        Assert.assertEquals("java.io.PrintStream", resolvedField.getType().getCanonicalText());
    }

    /**
     * Test a reference to a smali field from a smali class
     */
    public void testSmaliReferenceFromSmali() throws Exception {
        createFile("blarg.smali", ".class public Lblarg; .super Ljava/lang/Object;" +
                ".field public static blort:I");

        String text =
                ".class public Lmy/pkg/blah; .super Ljava/lang/Object;\n" +
                        ".method public blah()V\n" +
                        "    .locals 1\n" +
                        "    sget v0, Lblarg;->bl<ref>ort:I\n" +
                        "    return-void\n" +
                        ".end method";

        SmaliFieldReference fieldReference = (SmaliFieldReference)configureByFileText(text, "blah.smali");

        Assert.assertNotNull(fieldReference);
        Assert.assertEquals("blort", fieldReference.getName());
        Assert.assertNotNull(fieldReference.getFieldType());
        Assert.assertEquals("int", fieldReference.getFieldType().getType().getCanonicalText());

        PsiField resolvedField = fieldReference.resolve();
        Assert.assertNotNull(resolvedField);
        Assert.assertEquals("blort", resolvedField.getName());
        Assert.assertNotNull(resolvedField.getContainingClass());
        Assert.assertEquals("blarg", resolvedField.getContainingClass().getQualifiedName());
        Assert.assertEquals("int", resolvedField.getType().getCanonicalText());
    }

    /**
     * Test a reference to a smali field from a java class
     */
    public void testSmaliReferenceFromJava() throws Exception {
        createFile("blarg.smali", ".class public Lblarg; .super Ljava/lang/Object;" +
                ".field public static blort:I");

        String text = "public class blah { public static void something() {" +
                        "blarg.bl<ref>ort = 10;" +
                        "}}";

        PsiReference fieldReference = configureByFileText(text, "blah.java");

        Assert.assertNotNull(fieldReference);

        PsiField resolvedField = (PsiField)fieldReference.resolve();
        Assert.assertNotNull(resolvedField);
        Assert.assertEquals("blort", resolvedField.getName());
        Assert.assertNotNull(resolvedField.getContainingClass());
        Assert.assertEquals("blarg", resolvedField.getContainingClass().getQualifiedName());
        Assert.assertEquals("int", resolvedField.getType().getCanonicalText());
    }

    @Override
    protected Sdk getTestProjectJdk() {
        return JavaAwareProjectJdkTableImpl.getInstanceEx().getInternalJdk();
    }
}
