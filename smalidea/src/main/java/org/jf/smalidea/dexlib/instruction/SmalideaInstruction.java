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

package org.jf.smalidea.dexlib.instruction;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.intellij.psi.PsiType;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.ReferenceType;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.reference.Reference;
import org.jf.dexlib2.immutable.reference.ImmutableFieldReference;
import org.jf.dexlib2.immutable.reference.ImmutableMethodReference;
import org.jf.dexlib2.immutable.reference.ImmutableStringReference;
import org.jf.dexlib2.immutable.reference.ImmutableTypeReference;
import org.jf.smalidea.psi.impl.*;
import org.jf.smalidea.util.NameUtils;
import org.jf.smalidea.util.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public abstract class SmalideaInstruction implements Instruction {
    @Nonnull protected final SmaliInstruction psiInstruction;

    protected SmalideaInstruction(@Nonnull SmaliInstruction instruction) {
        this.psiInstruction = instruction;
    }

    @Nonnull
    public static SmalideaInstruction of(SmaliInstruction instruction) {
        switch (instruction.getOpcode().format) {
            case Format10t:
                return new SmalideaInstruction10t(instruction);
            case Format10x:
                return new SmalideaInstruction10x(instruction);
            case Format11n:
                return new SmalideaInstruction11n(instruction);
            case Format11x:
                return new SmalideaInstruction11x(instruction);
            case Format12x:
                return new SmalideaInstruction12x(instruction);
            case Format20t:
                return new SmalideaInstruction20t(instruction);
            case Format21c:
                return new SmalideaInstruction21c(instruction);
            case Format21ih:
                return new SmalideaInstruction21ih(instruction);
            case Format21lh:
                return new SmalideaInstruction21lh(instruction);
            case Format21s:
                return new SmalideaInstruction21s(instruction);
            case Format21t:
                return new SmalideaInstruction21t(instruction);
            case Format22b:
                return new SmalideaInstruction22b(instruction);
            case Format22c:
                return new SmalideaInstruction22c(instruction);
            case Format22s:
                return new SmalideaInstruction22s(instruction);
            case Format22t:
                return new SmalideaInstruction22t(instruction);
            case Format22x:
                return new SmalideaInstruction22x(instruction);
            case Format23x:
                return new SmalideaInstruction23x(instruction);
            case Format30t:
                return new SmalideaInstruction30t(instruction);
            case Format31c:
                return new SmalideaInstruction31c(instruction);
            case Format31i:
                return new SmalideaInstruction31i(instruction);
            case Format31t:
                return new SmalideaInstruction31t(instruction);
            case Format32x:
                return new SmalideaInstruction32x(instruction);
            case Format35c:
                return new SmalideaInstruction35c(instruction);
            case Format3rc:
                return new SmalideaInstruction3rc(instruction);
            case Format51l:
                return new SmalideaInstruction51l(instruction);
            case PackedSwitchPayload:
                return new SmalideaPackedSwitchPayload(instruction);
            case SparseSwitchPayload:
                return new SmalideaSparseSwitchPayload(instruction);
            case ArrayPayload:
                return new SmalideaArrayPayload(instruction);
            default:
                throw new RuntimeException("Unexpected instruction type");
        }
    }

    @Nonnull public Opcode getOpcode() {
        return psiInstruction.getOpcode();
    }

    public int getCodeUnits() {
        return getOpcode().format.size / 2;
    }

    public int getCodeOffset() {
        SmaliLabelReference labelReference = psiInstruction.getTarget();
        if (labelReference == null) {
            return -1;
        }

        SmaliLabel label = labelReference.resolve();
        if (label == null) {
            return -1;
        }
        return (label.getOffset() - psiInstruction.getOffset())/2;
    }

    public int getRegisterCount() {
        return psiInstruction.getRegisterCount();
    }

    public int getRegisterA() {
        return psiInstruction.getRegister(0);
    }

    public int getRegisterB() {
        return psiInstruction.getRegister(1);
    }

    public int getRegisterC() {
        return psiInstruction.getRegister(2);
    }

    public int getNarrowLiteral() {
        SmaliLiteral literal = psiInstruction.getLiteral();
        if (literal == null) {
            return 0;
        }
        return (int)literal.getIntegralValue();
    }

    public long getWideLiteral() {
        SmaliLiteral literal = psiInstruction.getLiteral();
        if (literal == null) {
            return 0;
        }
        return literal.getIntegralValue();
    }

    @Nonnull public Reference getReference() {
        switch (getReferenceType()) {
            case ReferenceType.STRING:
                return new ImmutableStringReference(StringUtils.parseQuotedString(
                        psiInstruction.getLiteral().getText()));
            case ReferenceType.TYPE:
                SmaliTypeElement typeReference = psiInstruction.getTypeReference();
                assert typeReference != null;
                return new ImmutableTypeReference(typeReference.getText());
            case ReferenceType.METHOD:
                SmaliMethodReference methodReference = psiInstruction.getMethodReference();
                assert methodReference != null;
                String containingClass = methodReference.getContainingType().getText();
                List<String> paramTypes =
                        Lists.transform(methodReference.getParameterTypes(), new Function<PsiType, String>() {
                            @Nullable @Override public String apply(@Nullable PsiType psiType) {
                                if (psiType == null) {
                                    return null;
                                }
                                return NameUtils.javaToSmaliType(psiType);
                            }
                        });

                return new ImmutableMethodReference(containingClass,
                        methodReference.getName(),
                        paramTypes,
                        methodReference.getReturnType().getText());
            case ReferenceType.FIELD:
                SmaliFieldReference fieldReference = psiInstruction.getFieldReference();
                assert fieldReference != null;
                containingClass = fieldReference.getContainingType().getText();
                return new ImmutableFieldReference(containingClass,
                        fieldReference.getName(),
                        fieldReference.getFieldType().getText());
        }
        assert false;
        return null;
    }

    public int getReferenceType() {
        return psiInstruction.getOpcode().referenceType;
    }

}