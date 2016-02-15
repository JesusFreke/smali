/*
 * Copyright 2016, Google Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 * Neither the name of Google Inc. nor the names of its
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

import com.google.common.collect.Sets;
import com.intellij.codeInsight.completion.CodeCompletionHandlerBase;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.debugger.NoDataException;
import com.intellij.debugger.engine.evaluation.CodeFragmentKind;
import com.intellij.debugger.engine.evaluation.TextWithImportsImpl;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaCodeFragment;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jf.smalidea.debugging.SmaliCodeFragmentFactory;
import org.jf.smalidea.psi.impl.SmaliFile;
import org.junit.Assert;

import java.util.HashSet;
import java.util.List;

public class SmaliCodeFragmentFactoryTest extends LightCodeInsightFixtureTestCase {
    private static final String testClass =
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

    public void testCompletion() throws NoDataException {
        SmaliFile smaliFile = (SmaliFile)myFixture.addFileToProject("my/pkg/blah.smali", testClass);

        PsiElement context = smaliFile.getPsiClass().getMethods()[0].getInstructions().get(0);
        assertCompletionContains("v", context, new String[] {"v2", "v3"}, new String[] {"v0", "v1", "p0", "p1"});
        assertCompletionContains("p", context, new String[] {"p0", "p1"}, new String[] {"v0", "v1", "v2", "v3"});

        context = smaliFile.getPsiClass().getMethods()[0].getInstructions().get(2);
        assertCompletionContains("v", context, new String[] {"v1", "v2", "v3"}, new String[] {"v0", "p0", "p1"});
        assertCompletionContains("p", context, new String[] {"p0", "p1"}, new String[] {"v0", "v1", "v2", "v3"});
    }

    private void assertCompletionContains(String completionText, PsiElement context, String[] expectedItems,
                                          String[] disallowedItems) {
        SmaliCodeFragmentFactory codeFragmentFactory = new SmaliCodeFragmentFactory();
        JavaCodeFragment fragment = codeFragmentFactory.createCodeFragment(
                new TextWithImportsImpl(CodeFragmentKind.EXPRESSION, completionText),
                context, getProject());

        Editor editor = createEditor(fragment.getVirtualFile());
        editor.getCaretModel().moveToOffset(completionText.length());

        new CodeCompletionHandlerBase(CompletionType.BASIC).invokeCompletion(getProject(), editor);
        List<LookupElement> elements = LookupManager.getInstance(getProject()).getActiveLookup().getItems();

        HashSet expectedSet = Sets.newHashSet(expectedItems);
        HashSet disallowedSet = Sets.newHashSet(disallowedItems);

        for (LookupElement element: elements) {
            expectedSet.remove(element.toString());
            Assert.assertFalse(disallowedSet.contains(element.toString()));
        }

        Assert.assertTrue(expectedSet.size() == 0);
    }

    protected Editor createEditor(@NotNull VirtualFile file) {
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        Editor editor = FileEditorManager.getInstance(getProject()).openTextEditor(
                new OpenFileDescriptor(getProject(), file, 0), false);
        DaemonCodeAnalyzer.getInstance(getProject()).restart();

        ((EditorImpl)editor).setCaretActive();
        return editor;
    }
}
