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

package org.jf.smalidea;

import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.impl.JavaAwareProjectJdkTableImpl;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.testFramework.ResolveTestCase;
import org.jf.smalidea.psi.impl.SmaliMethodReference;
import org.junit.Assert;

public class MethodReferenceTest extends ResolveTestCase {
    /**
     * Test a reference to a java method from a smali class
     */
    public void testJavaReferenceFromSmali() throws Exception {
        String text =
                ".class public Lmy/pkg/blah; .super Ljava/lang/Object;\n" +
                        ".method public blah()V\n" +
                        "    .locals 1\n" +

                        "    invoke-static {}, Ljava/lang/System;->nano<ref>Time()J\n" +
                        "    return-void\n" +
                        ".end method";

        SmaliMethodReference methodReference = (SmaliMethodReference)configureByFileText(text, "blah.smali");

        Assert.assertNotNull(methodReference);
        Assert.assertEquals("nanoTime", methodReference.getName());

        PsiMethod resolvedMethod = (PsiMethod)methodReference.resolve();
        Assert.assertNotNull(resolvedMethod);
        Assert.assertEquals("nanoTime", resolvedMethod.getName());
        Assert.assertNotNull(resolvedMethod.getContainingClass());
        Assert.assertEquals("java.lang.System", resolvedMethod.getContainingClass().getQualifiedName());
        Assert.assertEquals(0, resolvedMethod.getParameterList().getParametersCount());
        Assert.assertNotNull(resolvedMethod.getReturnType());
        Assert.assertEquals("long", resolvedMethod.getReturnType().getCanonicalText());
    }

    /**
     * Test a reference to a smali method from a smali class
     */
    public void testSmaliReferenceFromSmali() throws Exception {
        createFile("blarg.smali", ".class public Lblarg; .super Ljava/lang/Object;" +
                ".method public static blort(ILjava/lang/String;)V\n" +
                "    .locals 0\n" +
                "    return-void\n" +
                ".end method\n");

        String text =
                ".class public Lmy/pkg/blah; .super Ljava/lang/Object;\n" +
                        ".method public blah2()V\n" +
                        "    .locals 0\n" +
                        "    invoke-static {}, Lblarg;->bl<ref>ort(ILjava/lang/String;)V\n" +
                        "    return-void\n" +
                        ".end method";

        SmaliMethodReference methodReference = (SmaliMethodReference)configureByFileText(text, "blah.smali");

        Assert.assertNotNull(methodReference);
        Assert.assertEquals("blort", methodReference.getName());

        PsiMethod resolvedMethod = (PsiMethod)methodReference.resolve();
        Assert.assertNotNull(resolvedMethod);
        Assert.assertEquals("blort", resolvedMethod.getName());
        Assert.assertNotNull(resolvedMethod.getContainingClass());
        Assert.assertEquals("blarg", resolvedMethod.getContainingClass().getQualifiedName());
        Assert.assertEquals(2, resolvedMethod.getParameterList().getParametersCount());
        Assert.assertEquals("int", resolvedMethod.getParameterList().getParameters()[0].getType().getCanonicalText());
        Assert.assertEquals("java.lang.String",
                resolvedMethod.getParameterList().getParameters()[1].getType().getCanonicalText());
        Assert.assertNotNull(resolvedMethod.getReturnType());
        Assert.assertEquals("void", resolvedMethod.getReturnType().getCanonicalText());
    }

    /**
     * Test a reference to a smali method from a java class
     */
    public void testSmaliReferenceFromJava() throws Exception {
        createFile("blarg.smali", ".class public Lblarg; .super Ljava/lang/Object;" +
                ".method public static blort(ILjava/lang/String;)V\n" +
                "    .locals 0\n" +
                "    return-void\n" +
                ".end method\n");


        String text = "public class blah { public static void something() {" +
                        "blarg.bl<ref>ort(10, \"bob\");" +
                        "}}";

        PsiReference methodReference = configureByFileText(text, "blah.java");

        Assert.assertNotNull(methodReference);

        PsiMethod resolvedMethod = (PsiMethod)methodReference.resolve();
        Assert.assertNotNull(resolvedMethod);
        Assert.assertEquals("blort", resolvedMethod.getName());
        Assert.assertNotNull(resolvedMethod.getContainingClass());
        Assert.assertEquals("blarg", resolvedMethod.getContainingClass().getQualifiedName());
        Assert.assertEquals(2, resolvedMethod.getParameterList().getParametersCount());
        Assert.assertEquals("int", resolvedMethod.getParameterList().getParameters()[0].getType().getCanonicalText());
        Assert.assertEquals("java.lang.String",
                resolvedMethod.getParameterList().getParameters()[1].getType().getCanonicalText());
        Assert.assertNotNull(resolvedMethod.getReturnType());
        Assert.assertEquals("void", resolvedMethod.getReturnType().getCanonicalText());
    }

    @Override
    protected Sdk getTestProjectJdk() {
        return JavaAwareProjectJdkTableImpl.getInstanceEx().getInternalJdk();
    }
}
