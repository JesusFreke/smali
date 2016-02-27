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

package org.jf.smalidea.findUsages;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.search.LowLevelSearchUtil;
import com.intellij.psi.search.*;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.search.searches.ReferencesSearch.SearchParameters;
import com.intellij.util.Processor;
import com.intellij.util.text.StringSearcher;
import org.jetbrains.annotations.NotNull;
import org.jf.smalidea.util.NameUtils;

public class SmaliClassReferenceSearcher extends QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters> {
    @Override public void processQuery(final SearchParameters queryParameters, final Processor<PsiReference> consumer) {
        final PsiElement element = queryParameters.getElementToSearch();
        if (!(element instanceof PsiClass)) {
            return;
        }

        String smaliType = ApplicationManager.getApplication().runReadAction(
                new Computable<String>() {
                    @Override public String compute() {
                        String qualifiedName = ((PsiClass)element).getQualifiedName();
                        if (qualifiedName != null) {
                            return NameUtils.javaToSmaliType((PsiClass)element);
                        }
                        return null;
                    }
                });
        if (smaliType == null) {
            return;
        }

        final StringSearcher stringSearcher = new StringSearcher(smaliType, true, true, false, false);

        final SingleTargetRequestResultProcessor processor = new SingleTargetRequestResultProcessor(element);

        SearchScope querySearchScope = ApplicationManager.getApplication().runReadAction(
                new Computable<SearchScope>() {
                    @Override public SearchScope compute() {
                        return queryParameters.getEffectiveSearchScope();
                    }
                });

        if (querySearchScope instanceof LocalSearchScope) {
            for (final PsiElement scopeElement : ((LocalSearchScope)querySearchScope).getScope()) {
                ApplicationManager.getApplication().runReadAction(new Runnable() {
                    @Override
                    public void run() {
                        LowLevelSearchUtil.processElementsContainingWordInElement(
                                new TextOccurenceProcessor() {
                                    @Override public boolean execute(
                                            @NotNull PsiElement element, int offsetInElement) {
                                        return processor.processTextOccurrence(element, offsetInElement, consumer);
                                    }
                                },
                                scopeElement, stringSearcher, true, new EmptyProgressIndicator());
                    }
                });
            }
        } else if (querySearchScope instanceof GlobalSearchScope) {
            PsiSearchHelper helper = PsiSearchHelper.SERVICE.getInstance(element.getProject());
            // TODO: limit search scope to only smali files. See, e.g. AnnotatedPackagesSearcher.PackageInfoFilesOnly
            helper.processAllFilesWithWord(smaliType, (GlobalSearchScope)querySearchScope,
                    new Processor<PsiFile>() {
                        @Override
                        public boolean process(PsiFile file) {
                            LowLevelSearchUtil.processElementsContainingWordInElement(
                                    new TextOccurenceProcessor() {
                                        @Override public boolean execute(
                                                @NotNull PsiElement element, int offsetInElement) {
                                            return processor.processTextOccurrence(element, offsetInElement, consumer);
                                        }
                                    },
                                    file, stringSearcher, true, new EmptyProgressIndicator());
                            return true;
                        }
                    }, true);
        } else {
            assert false;
            return;
        }
    }
}
