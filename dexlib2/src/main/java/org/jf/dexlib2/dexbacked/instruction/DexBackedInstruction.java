/*
 * Copyright 2012, Google Inc.
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

package org.jf.dexlib2.dexbacked.instruction;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.dexbacked.DexBuffer;
import org.jf.dexlib2.dexbacked.DexReader;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;

public abstract class DexBackedInstruction implements Instruction {
    @Nonnull public final DexBuffer dexBuf;
    @Nonnull public final Opcode opcode;
    public final int instructionStart;

    public DexBackedInstruction(@Nonnull DexBuffer dexBuf,
                                @Nonnull Opcode opcode,
                                int instructionStart) {
        this.dexBuf = dexBuf;
        this.opcode = opcode;
        this.instructionStart = instructionStart;
    }

    @Nonnull public Opcode getOpcode() { return opcode; }
    @Override public int getCodeUnits() { return opcode.format.size / 2; }

    @Nonnull
    public static Instruction readFrom(@Nonnull DexReader reader) {
        int opcodeValue = reader.peekUbyte();

        if (opcodeValue == 0) {
            opcodeValue = reader.peekUshort();
        }

        Opcode opcode = Opcode.getOpcodeByValue(opcodeValue);

        //TODO: handle unexpected/unknown opcodes
        Instruction instruction = buildInstruction(reader.dexBuf, opcode, reader.getOffset());
        reader.moveRelative(instruction.getCodeUnits()*2);
        return instruction;
    }
    
    private static DexBackedInstruction buildInstruction(@Nonnull DexBuffer dexBuf, Opcode opcode,
                                                         int instructionStartOffset) {
        switch (opcode.format) {
            case Format10t:
                return new DexBackedInstruction10t(dexBuf, opcode, instructionStartOffset);
            case Format10x:
                return new DexBackedInstruction10x(dexBuf, opcode, instructionStartOffset);
            case Format11n:
                return new DexBackedInstruction11n(dexBuf, opcode, instructionStartOffset);
            case Format11x:
                return new DexBackedInstruction11x(dexBuf, opcode, instructionStartOffset);
            case Format12x:
                return new DexBackedInstruction12x(dexBuf, opcode, instructionStartOffset);
            case Format20t:
                return new DexBackedInstruction20t(dexBuf, opcode, instructionStartOffset);
            case Format21c:
                return new DexBackedInstruction21c(dexBuf, opcode, instructionStartOffset);
            case Format21ih:
                return new DexBackedInstruction21ih(dexBuf, opcode, instructionStartOffset);
            case Format21lh:
                return new DexBackedInstruction21lh(dexBuf, opcode, instructionStartOffset);
            case Format21s:
                return new DexBackedInstruction21s(dexBuf, opcode, instructionStartOffset);
            case Format21t:
                return new DexBackedInstruction21t(dexBuf, opcode, instructionStartOffset);
            case Format22b:
                return new DexBackedInstruction22b(dexBuf, opcode, instructionStartOffset);
            case Format22c:
                return new DexBackedInstruction22c(dexBuf, opcode, instructionStartOffset);
            case Format22s:
                return new DexBackedInstruction22s(dexBuf, opcode, instructionStartOffset);
            case Format22t:
                return new DexBackedInstruction22t(dexBuf, opcode, instructionStartOffset);
            case Format22x:
                return new DexBackedInstruction22x(dexBuf, opcode, instructionStartOffset);
            case Format23x:
                return new DexBackedInstruction23x(dexBuf, opcode, instructionStartOffset);
            case Format30t:
                return new DexBackedInstruction30t(dexBuf, opcode, instructionStartOffset);
            case Format31c:
                return new DexBackedInstruction31c(dexBuf, opcode, instructionStartOffset);
            case Format31i:
                return new DexBackedInstruction31i(dexBuf, opcode, instructionStartOffset);
            case Format31t:
                return new DexBackedInstruction31t(dexBuf, opcode, instructionStartOffset);
            case Format32x:
                return new DexBackedInstruction32x(dexBuf, opcode, instructionStartOffset);
            case Format35c:
                return new DexBackedInstruction35c(dexBuf, opcode, instructionStartOffset);
            case Format3rc:
                return new DexBackedInstruction3rc(dexBuf, opcode, instructionStartOffset);
            case Format51l:
                return new DexBackedInstruction51l(dexBuf, opcode, instructionStartOffset);
            case PackedSwitchPayload:
                return new DexBackedPackedSwitchPayload(dexBuf, instructionStartOffset);
            case SparseSwitchPayload:
                return new DexBackedSparseSwitchPayload(dexBuf, instructionStartOffset);
            case ArrayPayload:
                return new DexBackedArrayPayload(dexBuf, instructionStartOffset);
                //TODO: temporary, until we get all instructions implemented
            default:
                throw new ExceptionWithContext("Unexpected opcode format: %s", opcode.format.toString());
        }
    }
}
