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

package org.jf.smalidea.psi.impl;

import com.google.common.base.Preconditions;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jf.dexlib2.Format;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.analysis.AnalyzedInstruction;
import org.jf.dexlib2.analysis.MethodAnalyzer;
import org.jf.smalidea.SmaliTokens;
import org.jf.smalidea.psi.SmaliCompositeElementFactory;
import org.jf.smalidea.psi.SmaliElementTypes;

import java.util.Arrays;
import java.util.List;

public class SmaliInstruction extends SmaliCompositeElement {
    private static final int NO_OFFSET = -1;

    @Nullable private Opcode opcode;
    private int offset = NO_OFFSET;

    public static final SmaliCompositeElementFactory FACTORY = new SmaliCompositeElementFactory() {
        @Override public SmaliCompositeElement createElement() {
            return new SmaliInstruction();
        }
    };

    public SmaliInstruction() {
        super(SmaliElementTypes.INSTRUCTION);
    }

    @NotNull public SmaliMethod getParentMethod() {
        SmaliMethod smaliMethod = findAncestorByClass(SmaliMethod.class);
        assert smaliMethod != null;
        return smaliMethod;
    }

    @NotNull public Opcode getOpcode() {
        if (opcode == null) {
            ASTNode instructionNode = findChildByType(SmaliTokens.INSTRUCTION_TOKENS);
            // this should be impossible, based on the parser definition
            assert instructionNode != null;

            // TODO: put a project level Opcodes instance with the appropriate api level somewhere
            opcode = new Opcodes(15, false).getOpcodeByName(instructionNode.getText());
            if (opcode == null) {
                if (instructionNode.getText().equals(".packed-switch")) {
                    return Opcode.PACKED_SWITCH_PAYLOAD;
                }
                if (instructionNode.getText().equals(".sparse-switch")) {
                    return Opcode.SPARSE_SWITCH_PAYLOAD;
                }
                if (instructionNode.getText().equals(".array-data")) {
                    return Opcode.ARRAY_PAYLOAD;
                }
                assert false;
            }
        }
        return opcode;
    }

    public int getOffset() {
        // TODO: don't calculate this recursively. ugh!
        if (offset == NO_OFFSET) {
            SmaliInstruction previousInstruction = findPrevSiblingByClass(SmaliInstruction.class);
            if (previousInstruction == null) {
                offset = 0;
            } else {
                offset = previousInstruction.getOffset() + previousInstruction.getInstructionSize();
            }
        }
        return offset;
    }

    public int getRegister(int registerIndex) {
        Preconditions.checkArgument(registerIndex >= 0);

        List<ASTNode> registers = findChildrenByType(SmaliElementTypes.REGISTER_REFERENCE);
        if (registerIndex >= registers.size()) {
            return -1;
        }

        SmaliRegisterReference registerReference = (SmaliRegisterReference)registers.get(registerIndex);
        return registerReference.getRegisterNumber();
    }

    @Nullable
    public SmaliLabelReference getTarget() {
        return findChildByClass(SmaliLabelReference.class);
    }

    public int getRegisterCount() {
        return findChildrenByType(SmaliElementTypes.REGISTER_REFERENCE).size();
    }

    @Nullable
    public SmaliLiteral getLiteral() {
        return findChildByClass(SmaliLiteral.class);
    }

    @Nullable
    public SmaliTypeElement getTypeReference() {
        return findChildByClass(SmaliTypeElement.class);
    }

    @Nullable
    public SmaliFieldReference getFieldReference() {
        return findChildByClass(SmaliFieldReference.class);
    }

    @Nullable
    public SmaliMethodReference getMethodReference() {
        return findChildByClass(SmaliMethodReference.class);
    }

    @Nullable
    public SmaliLiteral getPackedSwitchStartKey() {
        return findChildByClass(SmaliLiteral.class);
    }

    @NotNull
    public List<SmaliPackedSwitchElement> getPackedSwitchElements() {
        return Arrays.asList(findChildrenByClass(SmaliPackedSwitchElement.class));
    }

    @NotNull
    public List<SmaliSparseSwitchElement> getSparseSwitchElements() {
        return Arrays.asList(findChildrenByClass(SmaliSparseSwitchElement.class));
    }

    @Nullable
    public SmaliLiteral getArrayDataWidth() {
        return findChildByClass(SmaliLiteral.class);
    }

    @NotNull
    public List<SmaliArrayDataElement> getArrayDataElements() {
        return Arrays.asList(findChildrenByClass(SmaliArrayDataElement.class));
    }

    public int getInstructionSize() {
        Opcode opcode = getOpcode();
        if (!opcode.format.isPayloadFormat) {
            return opcode.format.size;
        } else if (opcode.format == Format.ArrayPayload) {
            int elementWidth = (int)getArrayDataWidth().getIntegralValue();
            int elementCount = getArrayDataElements().size();

            return 8 + (elementWidth * elementCount + 1);
        } else if (opcode.format == Format.PackedSwitchPayload) {
            return 8 + getPackedSwitchElements().size() * 4;
        } else if (opcode.format == Format.SparseSwitchPayload) {
            return 2 + getSparseSwitchElements().size() * 4;
        }
        assert false;
        throw new RuntimeException();
    }

    private AnalyzedInstruction analyzedInstruction = null;

    @Nullable
    private AnalyzedInstruction getAnalyzedInstructionFromMethod() {
        SmaliMethod method = getParentMethod();

        MethodAnalyzer analyzer = method.getMethodAnalyzer();
        if (analyzer == null) {
            return null;
        }

        int thisOffset = this.getOffset() / 2;
        int codeOffset = 0;

        for (AnalyzedInstruction instruction: analyzer.getAnalyzedInstructions()) {
            if (codeOffset == thisOffset) {
                return instruction;
            }
            assert codeOffset < thisOffset;

            codeOffset += instruction.getOriginalInstruction().getCodeUnits();
        }
        assert false;
        return null;
    }

    @Nullable
    public AnalyzedInstruction getAnalyzedInstruction() {
        if (analyzedInstruction == null) {
            analyzedInstruction = getAnalyzedInstructionFromMethod();
        }
        return analyzedInstruction;
    }

    @Override public void clearCaches() {
        super.clearCaches();
        analyzedInstruction = null;
    }
}
