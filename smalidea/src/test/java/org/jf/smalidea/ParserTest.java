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

public class ParserTest extends LightCodeInsightParsingTestCase {
    public ParserTest() {
        super("", "smalidea", SmaliLanguage.INSTANCE);
    }

    @Override
    protected String getTestDataPath() {
        return "testData";
    }

    public void testEmpty() throws Exception { doTest(true); }
    public void testFieldAnnotations() throws Exception { doTest(true); }
    public void testInvalidAnnotation() throws Exception { doTest(true); }
    public void testInvalidClassDirective() throws Exception { doTest(true); }
    public void testInvalidClassDirective2() throws Exception { doTest(true); }
    public void testInvalidClassDirective3() throws Exception { doTest(true); }
    public void testInvalidEnumLiteral() throws Exception { doTest(true); }
    public void testInvalidField() throws Exception { doTest(true); }
    public void testInvalidField2() throws Exception { doTest(true); }
    public void testInvalidField3() throws Exception { doTest(true); }
    public void testInvalidField4() throws Exception { doTest(true); }
    public void testInvalidInstruction() throws Exception { doTest(true); }
    public void testInvalidLocal() throws Exception { doTest(true);}
    public void testParamListInvalidParameter() throws Exception { doTest(true); }
    public void testSuperClassInvalidSyntax() throws Exception { doTest(true); }
    public void testSuperClassInvalidSyntax2() throws Exception { doTest(true); }
    public void testInvalidMethodReference() throws Exception { doTest(true); }
    public void testInvalidParameter() throws Exception { doTest(true); }
    public void testInvalidMethod() throws Exception { doTest(true); }
    public void testInvalidMethod2() throws Exception { doTest(true); }
    public void testInvalidMethod3() throws Exception { doTest(true); }
    public void testInvalidMethod4() throws Exception { doTest(true); }
    public void testMissingDotDot() throws Exception { doTest(true); }
}
