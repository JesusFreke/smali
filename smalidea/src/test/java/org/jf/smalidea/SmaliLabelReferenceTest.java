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

import com.intellij.testFramework.ResolveTestCase;
import org.jf.dexlib2.Opcode;
import org.jf.smalidea.psi.impl.SmaliInstruction;
import org.jf.smalidea.psi.impl.SmaliLabel;
import org.jf.smalidea.psi.impl.SmaliLabelReference;
import org.junit.Assert;

public class SmaliLabelReferenceTest extends ResolveTestCase {

    public void testLabelReference() throws Exception {
        String text =
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
                        "    goto :go<ref>to_4\n" +
                        ".end method";;

        SmaliLabelReference labelReference = (SmaliLabelReference)configureByFileText(text, "blah.smali");

        Assert.assertNotNull(labelReference);
        Assert.assertEquals("goto_4", labelReference.getName());

        SmaliLabel resolvedLabel = labelReference.resolve();
        Assert.assertNotNull(resolvedLabel);
        Assert.assertEquals("goto_4", resolvedLabel.getName());

        SmaliInstruction nextInstruction = resolvedLabel.findNextSiblingByClass(SmaliInstruction.class);
        Assert.assertNotNull(nextInstruction);
        Assert.assertEquals(8, nextInstruction.getOffset());
        Assert.assertEquals(Opcode.RETURN, nextInstruction.getOpcode());
    }
}
