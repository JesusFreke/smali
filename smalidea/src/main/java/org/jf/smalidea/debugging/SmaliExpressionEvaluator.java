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

package org.jf.smalidea.debugging;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluationContext;
import com.intellij.debugger.engine.evaluation.expression.ExpressionEvaluator;
import com.intellij.debugger.engine.evaluation.expression.Modifier;
import com.intellij.psi.PsiElement;
import com.sun.jdi.Value;
import org.jf.smalidea.debugging.value.LazyValue;

import java.util.List;

public class SmaliExpressionEvaluator implements ExpressionEvaluator {
    private final PsiElement fragment;
    private final ExpressionEvaluator evaluator;

    public SmaliExpressionEvaluator(PsiElement fragment, ExpressionEvaluator evaluator) {
        this.fragment = fragment;
        this.evaluator = evaluator;
    }

    @Override public Value evaluate(EvaluationContext context) throws EvaluateException {
        List<LazyValue> lazyValues = fragment.getUserData(SmaliCodeFragmentFactory.SMALI_LAZY_VALUES_KEY);
        if (lazyValues != null) {
            for (LazyValue lazyValue: lazyValues) {
                lazyValue.setEvaluationContext(context);
            }
        }
        return evaluator.evaluate(context);
    }

    @Override public Value getValue() {
        return evaluator.getValue();
    }

    @Override public Modifier getModifier() {
        return evaluator.getModifier();
    }
}
