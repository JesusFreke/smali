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

import com.intellij.debugger.SourcePosition;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jf.dexlib2.Opcode;
import org.jf.smalidea.psi.impl.*;
import org.junit.Assert;

import java.util.List;

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

    private static final String instructionsTestClass =
            ".class public Lmy/pkg/blah; .super Ljava/lang/Object;\n" +
                    ".method public getRandomParentType(I)I\n" +
                    "    .registers 4\n" +
                    "    .param p1, \"edge\"    # I\n" +
                    "\n" +
                    "    .prologue\n" +
                    "    const/4 v1, 0x2\n" +
                    "\n" +
                    "    .line 179\n" +
                    "    if-nez p1, :cond_5\n" +
                    "\n" +
                    "    move v0, v1\n" +
                    "\n" +
                    "    .line 185\n" +
                    "    :goto_4\n" +
                    "    return v0\n" +
                    "\n" +
                    "    .line 182\n" +
                    "    :cond_5\n" +
                    "    if-ne p1, v1, :cond_f\n" +
                    "\n" +
                    "    .line 183\n" +
                    "    sget-object v0, Lorg/jf/Penroser/PenroserApp;->random:Ljava/util/Random;\n" +
                    "\n" +
                    "    const/4 v1, 0x3\n" +
                    "\n" +
                    "    invoke-virtual {v0, v1}, Ljava/util/Random;->nextInt(I)I\n" +
                    "\n" +
                    "    move-result v0\n" +
                    "\n" +
                    "    goto :goto_4\n" +
                    "\n" +
                    "    .line 185\n" +
                    "    :cond_f\n" +
                    "    sget-object v0, Lorg/jf/Penroser/PenroserApp;->random:Ljava/util/Random;\n" +
                    "\n" +
                    "    invoke-virtual {v0, v1}, Ljava/util/Random;->nextInt(I)I\n" +
                    "\n" +
                    "    move-result v0\n" +
                    "\n" +
                    "    goto :goto_4\n" +
                    ".end method";

    public void testGetInstructions() {
        String text = instructionsTestClass;

        SmaliFile file = (SmaliFile)myFixture.addFileToProject("my/pkg/blah.smali", text);
        SmaliClass smaliClass = file.getPsiClass();
        SmaliMethod smaliMethod = smaliClass.getMethods()[0];

        List<SmaliInstruction> instructions = smaliMethod.getInstructions();
        Assert.assertEquals(14, instructions.size());
    }

    private void checkSourcePosition(SmaliMethod smaliMethod, int codeOffset, Opcode opcode) {
        SourcePosition sourcePosition = smaliMethod.getSourcePositionForCodeOffset(codeOffset);
        Assert.assertNotNull(sourcePosition);

        SmaliInstruction instruction = (SmaliInstruction)sourcePosition.getElementAt();
        Assert.assertEquals(opcode, instruction.getOpcode());
        Assert.assertEquals(codeOffset, instruction.getOffset());
    }

    public void testGetSourcePositionForCodeOffset() {
        String text = instructionsTestClass;

        SmaliFile file = (SmaliFile)myFixture.addFileToProject("my/pkg/blah.smali", text);
        SmaliClass smaliClass = file.getPsiClass();
        SmaliMethod smaliMethod = smaliClass.getMethods()[0];

        checkSourcePosition(smaliMethod, 0, Opcode.CONST_4);
        checkSourcePosition(smaliMethod, 2, Opcode.IF_NEZ);
        checkSourcePosition(smaliMethod, 6, Opcode.MOVE);
        checkSourcePosition(smaliMethod, 8, Opcode.RETURN);
        checkSourcePosition(smaliMethod, 10, Opcode.IF_NE);
        checkSourcePosition(smaliMethod, 14, Opcode.SGET_OBJECT);
        checkSourcePosition(smaliMethod, 18, Opcode.CONST_4);
        checkSourcePosition(smaliMethod, 20, Opcode.INVOKE_VIRTUAL);
        checkSourcePosition(smaliMethod, 26, Opcode.MOVE_RESULT);
        checkSourcePosition(smaliMethod, 28, Opcode.GOTO);
        checkSourcePosition(smaliMethod, 30, Opcode.SGET_OBJECT);
        checkSourcePosition(smaliMethod, 34, Opcode.INVOKE_VIRTUAL);
        checkSourcePosition(smaliMethod, 40, Opcode.MOVE_RESULT);
        checkSourcePosition(smaliMethod, 42, Opcode.GOTO);
    }

    public void testThrowsList() {
        String text = instructionsTestClass;

        SmaliFile file = (SmaliFile)myFixture.addFileToProject("my/pkg/blah.smali", text);
        SmaliClass smaliClass = file.getPsiClass();
        SmaliMethod smaliMethod = smaliClass.getMethods()[0];

        SmaliThrowsList throwsList = smaliMethod.getThrowsList();
        Assert.assertNotNull(throwsList);
        Assert.assertEquals(0, throwsList.getReferencedTypes().length);
        Assert.assertEquals(0, throwsList.getReferenceElements().length);
    }

    public void testPrimitiveReturnType() {
        String text = "" +
                ".class public Lmy/pkg/blah; .super Ljava/lang/Object;\n" +
                ".method blah()I\n" +
                "    .registers 123\n" +
                "    return-void\n" +
                ".end method";

        SmaliFile file = (SmaliFile)myFixture.addFileToProject("my/pkg/blah.smali", text);
        SmaliClass smaliClass = file.getPsiClass();
        Assert.assertNotNull(smaliClass);
        SmaliMethod smaliMethod = smaliClass.getMethods()[0];

        Assert.assertNotNull(smaliMethod.getReturnType());
        Assert.assertTrue(smaliMethod.getReturnType().isConvertibleFrom(PsiPrimitiveType.INT));
        Assert.assertTrue(smaliMethod.getReturnType().isAssignableFrom(PsiPrimitiveType.INT));
    }
}
