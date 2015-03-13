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

import com.intellij.codeInsight.TargetElementUtilBase;
import com.intellij.find.findUsages.PsiElement2UsageTargetAdapter;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.usages.UsageTarget;
import com.intellij.usages.UsageTargetProvider;
import org.jetbrains.annotations.Nullable;
import org.jf.smalidea.SmaliTokens;
import org.jf.smalidea.psi.impl.SmaliMemberName;

/**
 * A usage target provider for smali member names consisting of primitive types.
 *
 * For member names like IIII, the default logic to find the usage target doesn't work, due to the member
 * name being split up into multiple leaf tokens.
 */
public class SmaliUsageTargetProvider implements UsageTargetProvider {
    @Nullable @Override public UsageTarget[] getTargets(Editor editor, PsiFile file) {
        PsiElement element = file.findElementAt(
                TargetElementUtilBase.adjustOffset(file, editor.getDocument(), editor.getCaretModel().getOffset()));
        if (element == null) {
            return null;
        }
        return getTargets(element);
    }

    @Nullable @Override public UsageTarget[] getTargets(PsiElement element) {
        ASTNode node = element.getNode();
        if (node == null) {
            return null;
        }

        if (node.getElementType() == SmaliTokens.PARAM_LIST_OR_ID_PRIMITIVE_TYPE) {
            PsiElement parent = element.getParent();
            if (parent instanceof SmaliMemberName) {
                return new UsageTarget[] { new PsiElement2UsageTargetAdapter(parent.getParent()) };
            }
        }
        return null;
    }
}
