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
import com.intellij.codeInsight.CodeInsightTestCase;
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
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.impl.JavaAwareProjectJdkTableImpl;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaCodeFragment;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.java.PsiReferenceExpressionImpl;
import org.jetbrains.annotations.NotNull;
import org.jf.smalidea.debugging.SmaliCodeFragmentFactory;
import org.jf.smalidea.psi.impl.SmaliFile;
import org.junit.Assert;

import java.util.HashSet;
import java.util.List;

public class SmaliCodeFragmentFactoryTest extends CodeInsightTestCase {
    private static final String completionTestClass =
            ".class public Lmy/pkg/blah; .super Ljava/lang/Object;\n" +
                    ".method public getRandomParentType(I)I\n" +
                    "    .registers 4\n" +
                    "    .param p1, \"edge\"    # I\n" +
                    "\n" +
                    "    .prologue\n" +
                    "    const/4 v1, 0x2\n" + // 0
                    "\n" +
                    "    .line 179\n" +
                    "    if-nez p1, :cond_5\n" +
                    "\n" +
                    "    move v0, v1\n" + // 2
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
                    "    const/4 v1, 0x3\n" + // 6
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
        SmaliFile smaliFile = (SmaliFile)configureByText(SmaliFileType.INSTANCE, completionTestClass);

        PsiElement context = smaliFile.getPsiClass().getMethods()[0].getInstructions().get(0);
        assertCompletionContains("v", context, new String[] {"v2", "v3"}, new String[] {"v0", "v1", "p0", "p1"});
        assertCompletionContains("p", context, new String[] {"p0", "p1"}, new String[] {"v0", "v1", "v2", "v3"});

        context = smaliFile.getPsiClass().getMethods()[0].getInstructions().get(2);
        assertCompletionContains("v", context, new String[] {"v1", "v2", "v3"}, new String[] {"v0", "p0", "p1"});
        assertCompletionContains("p", context, new String[] {"p0", "p1"}, new String[] {"v0", "v1", "v2", "v3"});

        context = smaliFile.getPsiClass().getMethods()[0].getInstructions().get(6);
        assertCompletionContains("v", context, new String[] {"v0", "v1", "v2", "v3"}, new String[] {"p0", "p1"});
        assertCompletionContains("p", context, new String[] {"p0", "p1"}, new String[] {});
    }

    private static final String registerTypeTestText = "" +
            ".class public LRegisterTypeTest;\n" +
            ".super Ljava/lang/Object;\n" +
            "\n" +
            "# virtual methods\n" +
            ".method public blah()V\n" +
            "    .registers 6\n" +
            "\n" +
            "    .prologue\n" +
            "    const/16 v3, 0xa\n" +
            "\n" +
            "    .line 7\n" +
            "    new-instance v0, Ljava/util/Random;\n" +
            "\n" +
            "    invoke-direct {v0}, Ljava/util/Random;-><init>()V\n" +
            "\n" +
            "    .line 9\n" +
            "    invoke-virtual {v0, v3}, Ljava/util/Random;->nextInt(I)I\n" +
            "\n" +
            "    move-result v1\n" +
            "\n" +
            "    const/4 v2, 0x5\n" +
            "\n" +
            "    if-le v1, v2, :cond_26\n" +
            "\n" +
            "    .line 10\n" +
            "    new-instance v1, Ljava/security/SecureRandom;\n" +
            "\n" +
            "    invoke-direct {v1}, Ljava/security/SecureRandom;-><init>()V\n" +
            "\n" +
            "    .line 14\n" +
            "    :goto_13\n" +
            "    sget-o<ref>bject v2, Ljava/lang/System;->out:Ljava/io/PrintStream;\n" +
            "\n" +
            "    invoke-virtual {v1, v3}, Ljava/util/Random;->nextInt(I)I\n" +
            "\n" +
            "    move-result v1\n" +
            "\n" +
            "    invoke-virtual {v2, v1}, Ljava/io/PrintStream;->println(I)V\n" +
            "\n" +
            "    .line 15\n" +
            "    sget-object v1, Ljava/lang/System;->out:Ljava/io/PrintStream;\n" +
            "\n" +
            "    invoke-virtual {v0}, Ljava/lang/Object;->toString()Ljava/lang/String;\n" +
            "\n" +
            "    move-result-object v0\n" +
            "\n" +
            "    invoke-virtual {v1, v0}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V\n" +
            "\n" +
            "    .line 16\n" +
            "    return-void\n" +
            "\n" +
            "    .line 12\n" +
            "    :cond_26\n" +
            "    invoke-virtual {p0}, LRegisterTypeTest;->getSerializable()Ljava/io/Serializable;\n" +
            "\n" +
            "    move-result-object v1\n" +
            "\n" +
            "    move-object v4, v1\n" +
            "\n" +
            "    move-object v1, v0\n" +
            "\n" +
            "    move-object v0, v4\n" +
            "\n" +
            "    goto :goto_13\n" +
            ".end method\n" +
            "\n" +
            ".method public getSerializable()Ljava/io/Serializable;\n" +
            "    .registers 2\n" +
            "\n" +
            "    .prologue\n" +
            "    .line 19\n" +
            "    new-instance v0, Ljava/util/Random;\n" +
            "\n" +
            "    invoke-direct {v0}, Ljava/util/Random;-><init>()V\n" +
            "\n" +
            "    return-object v0\n" +
            ".end method\n";

    public void testRegisterType() throws NoDataException {
        SmaliFile smaliFile = (SmaliFile)configureByText(SmaliFileType.INSTANCE,
                registerTypeTestText.replace("<ref>", ""));

        int refOffset = registerTypeTestText.indexOf("<ref>");

        PsiElement context = smaliFile.findElementAt(refOffset);
        assertVariableType(context.getParent(), "v1", "java.util.Random");
        assertVariableType(context.getParent(), "v0", "java.io.Serializable");
    }

    public void testUnknownClass() {
        String modifiedText = registerTypeTestText.replace("Random", "Rnd");
        SmaliFile smaliFile = (SmaliFile)configureByText(SmaliFileType.INSTANCE,
                modifiedText.replace("<ref>", ""));

        int refOffset = modifiedText.indexOf("<ref>");

        PsiElement context = smaliFile.findElementAt(refOffset);
        assertVariableType(context.getParent(), "v1", "java.lang.Object");
        assertVariableType(context.getParent(), "v0", "java.lang.Object");
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

    private void assertVariableType(PsiElement context, String variableName, String expectedType) {
        SmaliCodeFragmentFactory codeFragmentFactory = new SmaliCodeFragmentFactory();
        JavaCodeFragment fragment = codeFragmentFactory.createCodeFragment(
                new TextWithImportsImpl(CodeFragmentKind.EXPRESSION, variableName),
                context, getProject());

        Editor editor = createEditor(fragment.getVirtualFile());
        editor.getCaretModel().moveToOffset(1);

        PsiElement element = fragment.findElementAt(0);
        Assert.assertTrue(element.getParent() instanceof PsiReferenceExpressionImpl);
        PsiReferenceExpressionImpl reference = (PsiReferenceExpressionImpl)element.getParent();
        Assert.assertEquals(expectedType, reference.getType().getCanonicalText());
    }

    protected Editor createEditor(@NotNull VirtualFile file) {
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        Editor editor = FileEditorManager.getInstance(getProject()).openTextEditor(
                new OpenFileDescriptor(getProject(), file, 0), false);
        DaemonCodeAnalyzer.getInstance(getProject()).restart();

        ((EditorImpl)editor).setCaretActive();
        return editor;
    }

    @Override
    protected Sdk getTestProjectJdk() {
        return JavaAwareProjectJdkTableImpl.getInstanceEx().getInternalJdk();
    }
}
