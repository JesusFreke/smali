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

import com.intellij.openapi.fileEditor.impl.LoadTextUtil;
import com.intellij.psi.stubs.*;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.testFramework.ParsingTestCase;
import org.jf.smalidea.psi.SmaliElementTypes;
import org.jf.smalidea.psi.impl.SmaliFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ParserTest extends ParsingTestCase {
    public ParserTest() {
        super("", "smalidea", new SmaliParserDefinition());
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        registerApplicationService(SerializationManager.class, new SerializationManagerImpl());

        StubElementTypeHolderEP stubHolder = new StubElementTypeHolderEP();
        stubHolder.holderClass = SmaliElementTypes.class.getCanonicalName();
        registerExtension(StubElementTypeHolderEP.EP_NAME, stubHolder);
    }

    @Override
    protected String getTestDataPath() {
        return "testData";
    }

    @Override protected void doTest(boolean checkResult) {
        String name = getTestName(false);
        try {
            String text = loadFile(name + "." + myFileExt);
            SmaliFile f = (SmaliFile)createPsiFile(name, text);

            StubTree stubTree = f.calcStubTree();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            SerializationManagerImpl.getInstanceEx().serialize(stubTree.getRoot(), baos);
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            SerializationManagerImpl.getInstanceEx().deserialize(bais);

            ensureParsed(f);
            assertEquals("light virtual file text mismatch", text,
                    ((LightVirtualFile)f.getVirtualFile()).getContent().toString());
            assertEquals("virtual file text mismatch", text, LoadTextUtil.loadText(f.getVirtualFile()));
            assertEquals("doc text mismatch", text, f.getViewProvider().getDocument().getText());
            assertEquals("psi text mismatch", text, f.getText());
            if (checkResult){
                checkResult(name, f);
            }
            else{
                toParseTreeText(f, skipSpaces(), includeRanges());
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SerializerNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void testEmpty() throws Exception { doTest(true); }
    public void testInvalidClassDirective() throws Exception { doTest(true); }
    public void testInvalidClassDirective2() throws Exception { doTest(true); }
    public void testInvalidClassDirective3() throws Exception { doTest(true); }
    public void testParamListInvalidParameter() throws Exception { doTest(true); }
    public void testSuperClassInvalidSyntax() throws Exception { doTest(true); }
    public void testSuperClassInvalidSyntax2() throws Exception { doTest(true); }
    public void testInvalidMethod() throws Exception { doTest(true); }
    public void testInvalidMethod2() throws Exception { doTest(true); }
}
