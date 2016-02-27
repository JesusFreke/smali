/*
 * Copyright 2014, Google Inc.
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

package org.jf.smalidea.dexlib;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.base.reference.BaseMethodReference;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.debug.DebugItem;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.smalidea.dexlib.instruction.SmalideaInstruction;
import org.jf.smalidea.psi.impl.SmaliCatchStatement;
import org.jf.smalidea.psi.impl.SmaliInstruction;
import org.jf.smalidea.psi.impl.SmaliMethod;
import org.jf.smalidea.util.NameUtils;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class SmalideaMethod extends BaseMethodReference implements Method {
    private final PsiMethod psiMethod;

    public SmalideaMethod(@NotNull PsiMethod psiMethod) {
        this.psiMethod = psiMethod;
    }

    @Nonnull @Override public String getDefiningClass() {
        PsiClass cls = psiMethod.getContainingClass();
        assert cls != null;
        return NameUtils.javaToSmaliType(cls);
    }

    @Nonnull @Override public List<? extends MethodParameter> getParameters() {
        PsiParameter[] parameters = psiMethod.getParameterList().getParameters();

        return Lists.transform(Arrays.asList(parameters), new Function<PsiParameter, MethodParameter>() {
            @Nullable @Override
            public MethodParameter apply(@Nullable PsiParameter psiParameter) {
                if (psiParameter == null) {
                    return null;
                }
                return new SmalideaMethodParameter(psiParameter);
            }
        });
    }

    @Override public int getAccessFlags() {
        if (psiMethod instanceof SmaliMethod) {
            return ((SmaliMethod)psiMethod).getModifierList().getAccessFlags();
        } else {
            int flags = 0;
            PsiModifierList modifierList = psiMethod.getModifierList();
            if (modifierList.hasModifierProperty("public")) {
                flags |= AccessFlags.PUBLIC.getValue();
            } else if (modifierList.hasModifierProperty("protected")) {
                flags |= AccessFlags.PROTECTED.getValue();
            } else if (modifierList.hasModifierProperty("private")) {
                flags |= AccessFlags.PRIVATE.getValue();
            }

            if (modifierList.hasModifierProperty("static")) {
                flags |= AccessFlags.STATIC.getValue();
            }

            if (modifierList.hasModifierProperty("final")) {
                flags |= AccessFlags.FINAL.getValue();
            }

            boolean isNative = false;
            if (modifierList.hasModifierProperty("native")) {
                flags |= AccessFlags.NATIVE.getValue();
                isNative = true;
            }

            if (modifierList.hasModifierProperty("synchronized")) {
                if (isNative) {
                    flags |= AccessFlags.SYNCHRONIZED.getValue();
                } else {
                    flags |= AccessFlags.DECLARED_SYNCHRONIZED.getValue();
                }
            }

            if (psiMethod.isVarArgs()) {
                flags |= AccessFlags.VARARGS.getValue();
            }

            if (modifierList.hasModifierProperty("abstract")) {
                flags |= AccessFlags.ABSTRACT.getValue();
            }

            if (modifierList.hasModifierProperty("strictfp")) {
                flags |= AccessFlags.STRICTFP.getValue();
            }

            if (psiMethod.isConstructor()) {
                flags |= AccessFlags.CONSTRUCTOR.getValue();
            }
            return flags;
        }
    }

    @Nonnull @Override public Set<? extends Annotation> getAnnotations() {
        // TODO: implement this
        return ImmutableSet.of();
    }

    @Nullable @Override public MethodImplementation getImplementation() {
        if (psiMethod instanceof SmaliMethod) {
            final SmaliMethod smaliMethod = (SmaliMethod)this.psiMethod;

            List<SmaliInstruction> instructions = smaliMethod.getInstructions();
            if (instructions.size() == 0) {
                return null;
            }

            // TODO: cache this?
            return new MethodImplementation() {
                @Override public int getRegisterCount() {
                    return smaliMethod.getRegisterCount();
                }

                @Nonnull @Override public Iterable<? extends Instruction> getInstructions() {
                    return Lists.transform(smaliMethod.getInstructions(),
                            new Function<SmaliInstruction, Instruction>() {
                                @Override
                                public Instruction apply(SmaliInstruction smaliInstruction) {
                                    return SmalideaInstruction.of(smaliInstruction);
                                }
                            });
                }

                @Nonnull @Override public List<? extends TryBlock<? extends ExceptionHandler>> getTryBlocks() {
                    return Lists.transform(smaliMethod.getCatchStatements(),
                            new Function<SmaliCatchStatement, TryBlock<? extends ExceptionHandler>>() {
                                @Override
                                public TryBlock<? extends ExceptionHandler> apply(
                                        SmaliCatchStatement smaliCatchStatement) {
                                    assert smaliCatchStatement != null;
                                    return new SmalideaTryBlock(smaliCatchStatement);
                                }
                            });
                }

                @Nonnull @Override public Iterable<? extends DebugItem> getDebugItems() {
                    // TODO: implement this
                    return ImmutableList.of();
                }
            };
        }
        return null;
    }

    @Nonnull @Override public String getName() {
        return psiMethod.getName();
    }

    @Nonnull @Override public List<? extends CharSequence> getParameterTypes() {
        PsiParameter[] parameters = psiMethod.getParameterList().getParameters();

        return Lists.transform(Arrays.asList(parameters), new Function<PsiParameter, CharSequence>() {
            @Nullable @Override
            public CharSequence apply(@Nullable PsiParameter psiParameter) {
                if (psiParameter == null) {
                    return null;
                }
                return psiParameter.getText();
            }
        });
    }

    @Nonnull @Override public String getReturnType() {
        return psiMethod.getReturnTypeElement().getText();
    }
}
