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

package org.jf.smalidea.findUsages;

import com.google.common.collect.Lists;
import com.intellij.codeInsight.TargetElementUtilBase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.testFramework.PsiTestCase;
import org.junit.Assert;

import java.util.List;

public class HighlightLocalClassUsagesTest extends PsiTestCase {
    public void testHighlightLocalClassUsage() throws Exception {
        String fileText = "" +
                ".class public Lbl<ref>arg; .super Ljava/lang/Object;\n" +
                ".method public doSomething()V\n" +
                "  .registers 1\n" +
                "  new-instance v0, Lbl<ref>arg;\n" +
                "  invoke-direct {v0}, Lblah;-><init>()V\n" +
                "  return-void\n" +
                ".end method";

        PsiFile file = createFile("blarg.smali", fileText.replace("<ref>", ""));
        PsiElement target;

        int refIndex = fileText.indexOf("<ref>");
        PsiReference reference = file.findReferenceAt(refIndex);
        if (reference != null) {
            target = reference.resolve();
        } else {
            target = TargetElementUtilBase.getInstance().getNamedElement(
                    file.findElementAt(refIndex), 0);
        }

        final LocalSearchScope scope = new LocalSearchScope(file);

        List<PsiReference> refs = Lists.newArrayList(ReferencesSearch.search(target, scope).findAll());
        Assert.assertEquals(2, refs.size());

        Assert.assertEquals(file.findElementAt(refIndex).getTextOffset(), refs.get(0).getElement().getTextOffset());
        Assert.assertEquals(file.findElementAt(fileText.replaceFirst("<ref>", "").indexOf("<ref>")).getTextOffset(),
                refs.get(1).getElement().getTextOffset());
    }
}
