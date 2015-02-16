/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jf.smalidea;

import com.intellij.lang.Language;
import com.intellij.openapi.fileEditor.impl.LoadTextUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.psi.*;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.psi.impl.PsiFileFactoryImpl;
import com.intellij.psi.impl.source.PsiFileImpl;
import com.intellij.psi.stubs.SerializationManagerImpl;
import com.intellij.psi.stubs.SerializerNotFoundException;
import com.intellij.psi.stubs.StubTree;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.testFramework.TestDataFile;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * A test case for parsing tests.
 *
 * This was originally based on com.intellij.testFramework.ParsingTestCase, but was modified
 * to use the LightCodeInsightFixtureTestCase base class, which provides more functionality
 */
public abstract class LightCodeInsightParsingTestCase extends LightCodeInsightFixtureTestCase {
    protected final String myFilePrefix = "";
    protected final String myFileExt;
    @NonNls protected final String myFullDataPath;
    protected final Language myLanguage;

    protected PsiFile myFile;

    public LightCodeInsightParsingTestCase(@NonNls @NotNull String dataPath, @NotNull String fileExt,
                                           @NotNull Language language) {
        myLanguage = language;
        myFullDataPath = getTestDataPath() + "/" + dataPath;
        myFileExt = fileExt;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        myFile = null;
    }

    protected boolean includeRanges() {
        return false;
    }

    protected boolean skipSpaces() {
        return false;
    }

    protected boolean checkAllPsiRoots() {
        return true;
    }

    protected void doTest(boolean checkResult) {
        String name = getTestName(false);
        try {
            String text = loadFile(name + "." + myFileExt);
            PsiFile f = createPsiFile(name, text);

            if (f instanceof PsiFileImpl) {
                // Also want to test stub serialization/deserialization
                StubTree stubTree = ((PsiFileImpl)f).calcStubTree();

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                SerializationManagerImpl.getInstanceEx().serialize(stubTree.getRoot(), baos);
                ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
                SerializationManagerImpl.getInstanceEx().deserialize(bais);
            }

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

    protected void doTest(String suffix) throws IOException {
        String name = getTestName(false);
        String text = loadFile(name + "." + myFileExt);
        myFile = createPsiFile(name, text);
        ensureParsed(myFile);
        assertEquals(text, myFile.getText());
        checkResult(name + suffix, myFile);
    }

    protected void doCodeTest(String code) throws IOException {
        String name = getTestName(false);
        myFile = createPsiFile("a", code);
        ensureParsed(myFile);
        assertEquals(code, myFile.getText());
        checkResult(myFilePrefix + name, myFile);
    }

    protected PsiFile createPsiFile(String name, String text) {
        return createFile(name + "." + myFileExt, text);
    }

    protected PsiFile createFile(@NonNls String name, String text) {
        LightVirtualFile virtualFile = new LightVirtualFile(name, myLanguage, text);
        virtualFile.setCharset(CharsetToolkit.UTF8_CHARSET);
        return createFile(virtualFile);
    }

    protected PsiFile createFile(LightVirtualFile virtualFile) {
        return ((PsiFileFactoryImpl)PsiFileFactory.getInstance(getProject())).trySetupPsiForFile(
                virtualFile, myLanguage, true, false);
    }

    protected void checkResult(@NonNls @TestDataFile String targetDataName, final PsiFile file) throws IOException {
        doCheckResult(myFullDataPath, file, checkAllPsiRoots(), targetDataName, skipSpaces(), includeRanges());
    }

    public static void doCheckResult(String myFullDataPath,
                                     PsiFile file,
                                     boolean checkAllPsiRoots,
                                     String targetDataName,
                                     boolean skipSpaces,
                                     boolean printRanges) throws IOException {
        FileViewProvider provider = file.getViewProvider();
        Set<Language> languages = provider.getLanguages();

        if (!checkAllPsiRoots || languages.size() == 1) {
            doCheckResult(myFullDataPath, targetDataName + ".txt", toParseTreeText(file, skipSpaces, printRanges).trim());
            return;
        }

        for (Language language : languages) {
            PsiFile root = provider.getPsi(language);
            String expectedName = targetDataName + "." + language.getID() + ".txt";
            doCheckResult(myFullDataPath, expectedName, toParseTreeText(root, skipSpaces, printRanges).trim());
        }
    }

    protected void checkResult(@TestDataFile @NonNls String targetDataName, final String text) throws IOException {
        doCheckResult(myFullDataPath, targetDataName, text);
    }

    public static void doCheckResult(String fullPath, String targetDataName, String text) throws IOException {
        String expectedFileName = fullPath + File.separatorChar + targetDataName;
        UsefulTestCase.assertSameLinesWithFile(expectedFileName, text);
    }

    protected static String toParseTreeText(final PsiElement file,  boolean skipSpaces, boolean printRanges) {
        return DebugUtil.psiToString(file, skipSpaces, printRanges);
    }

    protected String loadFile(@NonNls @TestDataFile String name) throws IOException {
        return doLoadFile(myFullDataPath, name);
    }

    private static String doLoadFile(String myFullDataPath, String name) throws IOException {
        return FileUtil.loadFile(new File(myFullDataPath, name), CharsetToolkit.UTF8, true).trim();
    }

    public static void ensureParsed(PsiFile file) {
        file.accept(new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                element.acceptChildren(this);
            }
        });
    }
}
