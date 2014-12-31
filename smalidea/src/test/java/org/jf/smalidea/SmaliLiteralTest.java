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
import org.jf.smalidea.psi.impl.SmaliFile;
import org.jf.smalidea.psi.impl.SmaliLiteral;
import org.junit.Assert;

public class SmaliLiteralTest extends LightCodeInsightFixtureTestCase {
    private void doTest(long expectedValue, String literalValue) {
        String text =
                ".class public Lmy/pkg/blah; .super Ljava/lang/Object;\n" +
                ".method blah()V\n" +
                "    .registers <ref>" + literalValue + "\n" +
                "    return-void\n" +
                ".end method";

        SmaliFile file = (SmaliFile)myFixture.addFileToProject("my/pkg/blah.smali",
                text.replace("<ref>", ""));

        PsiElement leafElement = file.findElementAt(text.indexOf("<ref>"));
        Assert.assertNotNull(leafElement);
        SmaliLiteral literalElement = (SmaliLiteral)leafElement.getParent();
        Assert.assertNotNull(literalElement);

        Assert.assertEquals(expectedValue, literalElement.getIntegralValue());
    }

    public void testIntegerValue() {
        doTest(123, "123");
    }

    public void testLongValue() {
        doTest(100, "100L");
    }

    public void testShortValue() {
        doTest(99, "99s");
    }

    public void testByteValue() {
        doTest(127, "127t");
    }

    // TODO: test char
    // TODO: test bool
}
