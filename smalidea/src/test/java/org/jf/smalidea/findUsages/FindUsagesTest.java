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

import com.google.common.collect.Lists;
import com.intellij.codeInsight.TargetElementUtilBase;
import com.intellij.find.FindManager;
import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.FindUsagesManager;
import com.intellij.find.findUsages.FindUsagesOptions;
import com.intellij.find.impl.FindManagerImpl;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.testFramework.PsiTestCase;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.PsiElementUsageTarget;
import com.intellij.usages.UsageTarget;
import com.intellij.usages.UsageTargetUtil;
import com.intellij.util.CommonProcessors;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;

import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class FindUsagesTest extends PsiTestCase {
    public static final String USAGE_TAG = "<usage>";
    public static final String REF_TAG = "<ref>";

    private class TestFile {
        @NotNull public final String fileName;
        @NotNull public final String fileText;
        @NotNull public final PsiFile psiFile;

        public TestFile(@NotNull String fileName, @NotNull String fileText) throws Exception {
            this.fileName = fileName;
            this.fileText = fileText;
            this.psiFile = createFile(fileName, getText());
        }

        @NotNull
        public String getText() {
            return fileText.replace(REF_TAG, "").replace(USAGE_TAG, "");
        }

        public int getRefIndex() {
            return fileText.replace(USAGE_TAG, "").indexOf(REF_TAG);
        }

        public List<Integer> getUsageIndices() {
            Matcher matcher = Pattern.compile(USAGE_TAG).matcher(fileText.replace(REF_TAG, ""));
            List<Integer> matches = Lists.newArrayList();

            int adjustment = 0;
            while (matcher.find()) {
                matches.add(matcher.start() - adjustment);
                adjustment += USAGE_TAG.length();
            }
            return matches;
        }
    }

    private List<TestFile> testFiles;

    @Override
    public void setUp() throws Exception {
        testFiles = Lists.newArrayList();
        super.setUp();
    }

    protected void addFile(String fileName, String fileText) throws Exception {
        testFiles.add(new TestFile(fileName, fileText));
    }

    protected void doTest() {

        PsiReference reference = null;
        PsiElement targetElement = null;

        for (TestFile testFile: testFiles) {
            int refIndex = testFile.getRefIndex();
            if (refIndex != -1) {
                PsiElement element = testFile.psiFile.findElementAt(refIndex);

                UsageTarget[] targets = UsageTargetUtil.findUsageTargets(element);
                if (targets != null) {
                    for (UsageTarget target : targets) {
                        if (target instanceof PsiElementUsageTarget) {
                            targetElement = ((PsiElementUsageTarget)target).getElement();
                            break;
                        }
                    }
                }

                if (targetElement == null) {
                    reference = testFile.psiFile.findReferenceAt(refIndex);
                    if (reference != null) {
                        targetElement = reference.resolve();
                    } else {
                        targetElement = TargetElementUtilBase.getInstance().getNamedElement(
                                testFile.psiFile.findElementAt(refIndex), 0);
                    }
                }
                break;
            }
        }

        Assert.assertNotNull(targetElement);

        Collection<UsageInfo> usages = findUsages(targetElement);
        for (TestFile testFile: testFiles) {
            assertUsages(testFile, usages);
        }
    }

    private void assertUsages(@NotNull TestFile testFile, @NotNull Collection<UsageInfo> usages) {
        List<UsageInfo> fileUsages = Lists.newArrayList();
        for (UsageInfo usage: usages) {
            if (usage.getFile().getName().equals(testFile.fileName)) {
                fileUsages.add(usage);
            }
        }

        for (Integer usageIndex: testFile.getUsageIndices()) {
            boolean found = false;
            for (UsageInfo usage: fileUsages) {
                int startOffset = usage.getElement().getNode().getStartOffset();
                int length = usage.getElement().getTextLength();

                if (usageIndex >= startOffset && usageIndex < startOffset + length) {
                    fileUsages.remove(usage);
                    found = true;
                    break;
                }
            }
            Assert.assertTrue(found);
        }
        Assert.assertEquals(0, fileUsages.size());
    }

    private Collection<UsageInfo> findUsages(@NotNull PsiElement element) {
        FindUsagesManager findUsagesManager =
                ((FindManagerImpl)FindManager.getInstance(getProject())).getFindUsagesManager();

        FindUsagesHandler findUsagesHandler =
                findUsagesManager.getFindUsagesHandler(element, false);
        Assert.assertNotNull(findUsagesHandler);

        final FindUsagesOptions options = findUsagesHandler.getFindUsagesOptions();
        final CommonProcessors.CollectProcessor<UsageInfo> processor =
                new CommonProcessors.CollectProcessor<UsageInfo>();

        for (PsiElement primaryElement : findUsagesHandler.getPrimaryElements()) {
            findUsagesHandler.processElementUsages(primaryElement, processor, options);
        }

        for (PsiElement secondaryElement: findUsagesHandler.getSecondaryElements()) {
            findUsagesHandler.processElementUsages(secondaryElement, processor, options);
        }

        return processor.getResults();
    }
}
