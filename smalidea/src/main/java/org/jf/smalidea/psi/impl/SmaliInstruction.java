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

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.Opcodes;
import org.jf.smalidea.SmaliTokens;
import org.jf.smalidea.psi.SmaliCompositeElementFactory;
import org.jf.smalidea.psi.SmaliElementTypes;

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

    @NotNull public Opcode getOpcode() {
        if (opcode == null) {
            ASTNode instructionNode = findChildByType(SmaliTokens.INSTRUCTION_TOKENS);
            // this should be impossible, based on the parser definition
            assert instructionNode != null;

            // TODO: put a project level Opcodes instance with the appropriate api level somewhere
            opcode = new Opcodes(15).getOpcodeByName(instructionNode.getText());
            assert opcode != null;
        }
        return opcode;
    }

    public int getOffset() {
        if (offset == NO_OFFSET) {
            SmaliInstruction previousInstruction = findPrevSiblingByClass(SmaliInstruction.class);
            if (previousInstruction == null) {
                offset = 0;
            } else {
                // TODO: handle variable size instructions
                offset = previousInstruction.getOffset() + previousInstruction.getOpcode().format.size;
            }
        }
        return offset;
    }
}
