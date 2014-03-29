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

import com.intellij.psi.PsiElement;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import junit.framework.Assert;
import org.jf.smalidea.psi.impl.*;

public class SmaliMethodTest extends LightCodeInsightFixtureTestCase {
    public void testMethodRegisters() {
        String text =
                ".class public Lmy/pkg/blah; .super Ljava/lang/Object;\n" +
                ".me<ref>thod blah()V\n" +
                "    .registers 123\n" +
                "    return-void\n" +
                ".end method";

        SmaliFile file = (SmaliFile)myFixture.addFileToProject("my/pkg/blah.smali",
                text.replace("<ref>", ""));

        PsiElement leafElement = file.findElementAt(text.indexOf("<ref>"));
        Assert.assertNotNull(leafElement);
        SmaliMethod methodElement = (SmaliMethod)leafElement.getParent();
        Assert.assertNotNull(methodElement);

        Assert.assertEquals(123, methodElement.getRegisterCount());
        Assert.assertEquals(1, methodElement.getParameterRegisterCount());
    }

    public void testMethodRegisters2() {
        String text =
                ".class public Lmy/pkg/blah; .super Ljava/lang/Object;\n" +
                ".me<ref>thod blah(IJLjava/lang/String;)V\n" +
                "    .locals 123\n" +
                "    return-void\n" +
                ".end method";

        SmaliFile file = (SmaliFile)myFixture.addFileToProject("my/pkg/blah.smali",
                text.replace("<ref>", ""));

        PsiElement leafElement = file.findElementAt(text.indexOf("<ref>"));
        Assert.assertNotNull(leafElement);
        SmaliMethod methodElement = (SmaliMethod)leafElement.getParent();
        Assert.assertNotNull(methodElement);

        Assert.assertEquals(128, methodElement.getRegisterCount());
        Assert.assertEquals(5, methodElement.getParameterRegisterCount());
    }

    public void testStaticRegisterCount() {
        String text =
                ".class public Lmy/pkg/blah; .super Ljava/lang/Object;\n" +
                ".method static blah(IJLjava/lang/String;)V\n" +
                "    .locals 123\n" +
                "    return-void\n" +
                ".end method";

        SmaliFile file = (SmaliFile)myFixture.addFileToProject("my/pkg/blah.smali", text);
        SmaliClass smaliClass = file.getPsiClass();
        SmaliMethod smaliMethod = smaliClass.getMethods()[0];

        Assert.assertEquals(127, smaliMethod.getRegisterCount());
        Assert.assertEquals(4, smaliMethod.getParameterRegisterCount());

        Assert.assertEquals(0, smaliMethod.getParameterList().getParameters()[0].getParameterRegisterNumber());
        Assert.assertEquals(123, smaliMethod.getParameterList().getParameters()[0].getRegisterNumber());
    }

    public void testMethodParams() {
        myFixture.addFileToProject("my/TestAnnotation.smali",
                ".class public interface abstract annotation Lmy/TestAnnotation;\n" +
                ".super Ljava/lang/Object;\n" +
                ".implements Ljava/lang/annotation/Annotation;\n" +
                "\n" +
                ".method public abstract testBooleanValue()Z\n" +
                ".end method\n" +
                "\n" +
                ".method public abstract testStringArrayValue()[Ljava/lang/String;\n" +
                ".end method\n" +
                "\n" +
                ".method public abstract testStringValue()Ljava/lang/String;\n" +
                ".end method");

        String text =
                ".class public Lmy/pkg/blah; .super Ljava/lang/Object;\n" +
                ".method blah(IJLjava/lang/String;)V\n" +
                "    .locals 123\n" +
                "    .param p1, \"anInt\"\n" +
                "    .param p2\n" +
                "        .annotation runtime Lmy/TestAnnotation;\n" +
                "            testStringValue = \"myValue\"\n" +
                "        .end annotation\n" +
                "    .end param\n" +
                "    return-void\n" +
                ".end method";

        SmaliFile file = (SmaliFile)myFixture.addFileToProject("my/pkg/blah.smali", text);

        SmaliClass smaliClass = file.getPsiClass();
        SmaliMethod smaliMethod = smaliClass.getMethods()[0];

        SmaliMethodParamList paramList = smaliMethod.getParameterList();
        SmaliMethodParameter[] parameters = paramList.getParameters();
        Assert.assertEquals(3, parameters.length);

        Assert.assertEquals("int", parameters[0].getType().getCanonicalText());
        Assert.assertEquals("\"anInt\"", parameters[0].getName());
        Assert.assertEquals(1, parameters[0].getRegisterCount());
        Assert.assertEquals(124, parameters[0].getRegisterNumber());
        Assert.assertEquals(1, parameters[0].getParameterRegisterNumber());
        Assert.assertEquals(0, parameters[0].getAnnotations().length);

        Assert.assertEquals("long", parameters[1].getType().getCanonicalText());
        Assert.assertNull(parameters[1].getName());
        Assert.assertEquals(2, parameters[1].getRegisterCount());
        Assert.assertEquals(125, parameters[1].getRegisterNumber());
        Assert.assertEquals(2, parameters[1].getParameterRegisterNumber());
        Assert.assertEquals(1, parameters[1].getAnnotations().length);
        Assert.assertEquals("my.TestAnnotation", parameters[1].getAnnotations()[0].getQualifiedName());

        Assert.assertEquals("java.lang.String", parameters[2].getType().getCanonicalText());
        Assert.assertNull(parameters[2].getName());
        Assert.assertEquals(1, parameters[2].getRegisterCount());
        Assert.assertEquals(127, parameters[2].getRegisterNumber());
        Assert.assertEquals(4, parameters[2].getParameterRegisterNumber());
        Assert.assertEquals(0, parameters[2].getAnnotations().length);
    }

    public void testVarArgsMethod() {
        String text =
                ".class public Lmy/pkg/blah; .super Ljava/lang/Object;\n" +
                ".method varargs static blah(IJ[Ljava/lang/String;)V\n" +
                "    .locals 123\n" +
                "    return-void\n" +
                ".end method\n" +
                ".method varargs static blah2(IJLjava/lang/String;)V\n" +
                "    .locals 123\n" +
                "    return-void\n" +
                ".end method";

        SmaliFile file = (SmaliFile)myFixture.addFileToProject("my/pkg/blah.smali", text);
        SmaliClass smaliClass = file.getPsiClass();
        SmaliMethod smaliMethod = smaliClass.getMethods()[0];

        Assert.assertTrue(smaliMethod.isVarArgs());
        Assert.assertFalse(smaliMethod.getParameterList().getParameters()[0].isVarArgs());
        Assert.assertFalse(smaliMethod.getParameterList().getParameters()[1].isVarArgs());
        Assert.assertTrue(smaliMethod.getParameterList().getParameters()[2].isVarArgs());

        smaliMethod = smaliClass.getMethods()[1];
        Assert.assertTrue(smaliMethod.isVarArgs());
        Assert.assertFalse(smaliMethod.getParameterList().getParameters()[0].isVarArgs());
        Assert.assertFalse(smaliMethod.getParameterList().getParameters()[1].isVarArgs());
        Assert.assertFalse(smaliMethod.getParameterList().getParameters()[2].isVarArgs());
    }
}
