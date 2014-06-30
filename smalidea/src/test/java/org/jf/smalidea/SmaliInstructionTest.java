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
import org.jf.dexlib2.Opcode;
import org.jf.smalidea.psi.impl.SmaliFile;
import org.jf.smalidea.psi.impl.SmaliInstruction;
import org.junit.Assert;

public class SmaliInstructionTest extends LightCodeInsightFixtureTestCase {
    public void testSingleInstruction() {
        String text =
                ".class public Lmy/pkg/blah; .super Ljava/lang/Object;\n" +
                        ".method blah(IJLjava/lang/String;)V\n" +
                        "    .locals 0\n" +
                        "    r<ref>eturn-void\n" +
                        ".end method";

        SmaliFile file = (SmaliFile)myFixture.addFileToProject("my/pkg/blah.smali",
                text.replace("<ref>", ""));

        PsiElement leafElement = file.findElementAt(text.indexOf("<ref>"));
        Assert.assertNotNull(leafElement);
        SmaliInstruction instructionElement = (SmaliInstruction)leafElement.getParent();
        Assert.assertNotNull(instructionElement);

        Assert.assertEquals(Opcode.RETURN_VOID, instructionElement.getOpcode());
        Assert.assertEquals(0, instructionElement.getOffset());
    }

    public void testMultipleInstructions() {
        String text =
                ".class public Lmy/pkg/blah; .super Ljava/lang/Object;\n" +
                        ".method blah(IJLjava/lang/String;)I\n" +
                        "    .locals 1\n" +
                        "    const v0, 1234\n" +
                        "    r<ref>eturn v0\n" +
                        ".end method";

        SmaliFile file = (SmaliFile)myFixture.addFileToProject("my/pkg/blah.smali",
                text.replace("<ref>", ""));

        PsiElement leafElement = file.findElementAt(text.indexOf("<ref>"));
        Assert.assertNotNull(leafElement);
        SmaliInstruction instructionElement = (SmaliInstruction)leafElement.getParent();
        Assert.assertNotNull(instructionElement);

        Assert.assertEquals(Opcode.RETURN, instructionElement.getOpcode());
        Assert.assertEquals(6, instructionElement.getOffset());
    }
}
