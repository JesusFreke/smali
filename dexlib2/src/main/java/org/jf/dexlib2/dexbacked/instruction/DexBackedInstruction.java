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

import org.jf.dexlib2.dexbacked.DexReader;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.formats.*;
import org.jf.dexlib2.immutable.instruction.*;
import org.jf.util.ExceptionWithContext;
import org.jf.util.NibbleUtils;

import javax.annotation.Nonnull;

public abstract class DexBackedInstruction {
    @Nonnull
    public static Instruction readFrom(@Nonnull DexReader reader) {
        int opcodeValue = reader.readUbyte();
        if (opcodeValue == 0) {
            reader.moveRelative(-1);
            opcodeValue = reader.readUshort();
        }

        Opcode opcode = Opcode.getOpcodeByValue(opcodeValue);

        //TODO: handle unexpected/unknown opcodes

        switch (opcode.format) {
            case Format10t:
                return instruction10t(opcode, reader);
            case Format10x:
                return instruction10x(opcode, reader);
            case Format11n:
                return instruction11n(opcode, reader);
            case Format11x:
                return instruction11x(opcode, reader);
            case Format12x:
                return instruction12x(opcode, reader);
            case Format20t:
                return instruction20t(opcode, reader);
            case Format21c:
                return instruction21c(opcode, reader);
            case Format21ih:
                return instruction21ih(opcode, reader);
            case Format21lh:
                return instruction21lh(opcode, reader);
            case Format21s:
                return instruction21s(opcode, reader);
            case Format21t:
                return instruction21t(opcode, reader);
            case Format22b:
                return instruction22b(opcode, reader);
            case Format22c:
                return instruction22c(opcode, reader);
            case Format22s:
                return instruction22s(opcode, reader);
            case Format22t:
                return instruction22t(opcode, reader);
            case Format22x:
                return instruction22x(opcode, reader);
            case Format23x:
                return instruction23x(opcode, reader);
            case Format30t:
                return instruction30t(opcode, reader);
            case Format31c:
                return instruction31c(opcode, reader);
            case Format31i:
                return instruction31i(opcode, reader);
            case Format31t:
                return instruction31t(opcode, reader);
            case Format32x:
                return instruction32x(opcode, reader);
            case Format35c:
                return instruction35c(opcode, reader);
            case Format3rc:
                return instruction3rc(opcode, reader);
            case Format51l:
                return instruction51l(opcode, reader);
            case PackedSwitchPayload:
                return packedSwitchPayload(reader);
            case SparseSwitchPayload:
                return sparseSwitchPayload(reader);
            case ArrayPayload:
                return arrayPayload(reader);
            //TODO: temporary, until we get all instructions implemented
            default:
                throw new ExceptionWithContext("Unexpected opcode format: %s", opcode.format.toString());
        }
    }

    @Nonnull
    private static Instruction10t instruction10t(@Nonnull Opcode opcode, @Nonnull DexReader reader) {
        int offset = reader.readByte();
        return new ImmutableInstruction10t(opcode, offset);
    }

    @Nonnull
    private static Instruction10x instruction10x(@Nonnull Opcode opcode, @Nonnull DexReader reader) {
        reader.skipByte();
        return new ImmutableInstruction10x(opcode);
    }

    @Nonnull
    private static Instruction11n instruction11n(@Nonnull Opcode opcode, @Nonnull DexReader reader) {
        int b = reader.readUbyte();
        int registerA = NibbleUtils.extractLowUnsignedNibble(b);
        int literal = NibbleUtils.extractHighSignedNibble(b);
        return new ImmutableInstruction11n(opcode, registerA, literal);
    }

    @Nonnull
    private static Instruction11x instruction11x(@Nonnull Opcode opcode, @Nonnull DexReader reader) {
        int registerA = reader.readUbyte();
        return new ImmutableInstruction11x(opcode, registerA);
    }

    @Nonnull
    private static Instruction12x instruction12x(@Nonnull Opcode opcode, @Nonnull DexReader reader) {
        int b = reader.readUbyte();
        int registerA = NibbleUtils.extractLowUnsignedNibble(b);
        int registerB = NibbleUtils.extractHighUnsignedNibble(b);
        return new ImmutableInstruction12x(opcode, registerA, registerB);
    }

    @Nonnull
    private static Instruction20t instruction20t(@Nonnull Opcode opcode, @Nonnull DexReader reader) {
        reader.skipByte();
        int offset = reader.readShort();
        return new ImmutableInstruction20t(opcode, offset);
    }

    @Nonnull
    private static Instruction21c instruction21c(@Nonnull Opcode opcode, @Nonnull DexReader reader) {
        int registerA = reader.readUbyte();
        int referenceIndex = reader.readUshort();
        String reference = reader.getReference(opcode.referenceType, referenceIndex);
        return new ImmutableInstruction21c(opcode, registerA, reference);
    }

    @Nonnull
    private static Instruction21ih instruction21ih(@Nonnull Opcode opcode, @Nonnull DexReader reader) {
        int registerA = reader.readUbyte();
        int literalHat = reader.readShort();
        return new ImmutableInstruction21ih(opcode, registerA, literalHat << 16);
    }

    @Nonnull
    private static Instruction21lh instruction21lh(@Nonnull Opcode opcode, @Nonnull DexReader reader) {
        int registerA = reader.readUbyte();
        int literalHat = reader.readShort();
        return new ImmutableInstruction21lh(opcode, registerA, ((long)literalHat) << 48);
    }

    @Nonnull
    private static Instruction21s instruction21s(@Nonnull Opcode opcode, @Nonnull DexReader reader) {
        int registerA = reader.readUbyte();
        int literal = reader.readShort();
        return new ImmutableInstruction21s(opcode, registerA, literal);
    }

    @Nonnull
    private static Instruction21t instruction21t(@Nonnull Opcode opcode, @Nonnull DexReader reader) {
        int registerA = reader.readUbyte();
        int offset = reader.readShort();
        return new ImmutableInstruction21t(opcode, registerA, offset);
    }

    @Nonnull
    private static Instruction22b instruction22b(@Nonnull Opcode opcode, @Nonnull DexReader reader) {
        int registerA = reader.readUbyte();
        int registerB = reader.readUbyte();
        int literal = reader.readByte();
        return new ImmutableInstruction22b(opcode, registerA, registerB, literal);
    }

    @Nonnull
    private static Instruction22c instruction22c(@Nonnull Opcode opcode, @Nonnull DexReader reader) {
        int b = reader.readUbyte();
        int registerA = NibbleUtils.extractLowUnsignedNibble(b);
        int registerB = NibbleUtils.extractHighUnsignedNibble(b);

        int referenceIndex = reader.readUshort();
        String reference = reader.getReference(opcode.referenceType, referenceIndex);
        return new ImmutableInstruction22c(opcode, registerA, registerB, reference);
    }

    @Nonnull
    private static Instruction22s instruction22s(@Nonnull Opcode opcode, @Nonnull DexReader reader) {
        int b = reader.readUbyte();
        int registerA = NibbleUtils.extractLowUnsignedNibble(b);
        int registerB = NibbleUtils.extractHighUnsignedNibble(b);
        int literal = reader.readShort();
        return new ImmutableInstruction22s(opcode, registerA, registerB, literal);
    }

    @Nonnull
    private static Instruction22t instruction22t(@Nonnull Opcode opcode, @Nonnull DexReader reader) {
        int b = reader.readUbyte();
        int registerA = NibbleUtils.extractLowUnsignedNibble(b);
        int registerB = NibbleUtils.extractHighUnsignedNibble(b);
        int offset = reader.readShort();
        return new ImmutableInstruction22t(opcode, registerA, registerB, offset);
    }

    @Nonnull
    private static Instruction22x instruction22x(@Nonnull Opcode opcode, @Nonnull DexReader reader) {
        int registerA = reader.readUbyte();
        int registerB = reader.readUshort();
        return new ImmutableInstruction22x(opcode, registerA, registerB);
    }

    @Nonnull
    private static Instruction23x instruction23x(@Nonnull Opcode opcode, @Nonnull DexReader reader) {
        int registerA = reader.readUbyte();
        int registerB = reader.readUbyte();
        int registerC = reader.readUbyte();
        return new ImmutableInstruction23x(opcode, registerA, registerB, registerC);
    }

    @Nonnull
    private static Instruction30t instruction30t(@Nonnull Opcode opcode, @Nonnull DexReader reader) {
        reader.skipByte();
        int offset = reader.readInt();
        return new ImmutableInstruction30t(opcode, offset);
    }

    @Nonnull
    private static Instruction31c instruction31c(@Nonnull Opcode opcode, @Nonnull DexReader reader) {
        int registerA = reader.readUbyte();
        int referenceIndex = reader.readSmallUint();
        String reference = reader.getReference(opcode.referenceType, referenceIndex);
        return new ImmutableInstruction31c(opcode, registerA, reference);
    }

    @Nonnull
    private static Instruction31i instruction31i(@Nonnull Opcode opcode, @Nonnull DexReader reader) {
        int registerA = reader.readUbyte();
        int literal = reader.readInt();
        return new ImmutableInstruction31i(opcode, registerA, literal);
    }

    @Nonnull
    private static Instruction31t instruction31t(@Nonnull Opcode opcode, @Nonnull DexReader reader) {
        int registerA = reader.readUbyte();
        int offset = reader.readInt();
        return new ImmutableInstruction31t(opcode, registerA, offset);
    }

    @Nonnull
    private static Instruction32x instruction32x(@Nonnull Opcode opcode, @Nonnull DexReader reader) {
        reader.skipByte();
        int registerA = reader.readUshort();
        int registerB = reader.readUshort();
        return new ImmutableInstruction32x(opcode, registerA, registerB);
    }

    @Nonnull
    private static Instruction35c instruction35c(@Nonnull Opcode opcode, @Nonnull DexReader reader) {
        int b = reader.readUbyte();
        int registerCount = NibbleUtils.extractHighUnsignedNibble(b);
        int registerG = NibbleUtils.extractLowUnsignedNibble(b);

        int referenceIndex = reader.readUshort();
        String reference = reader.getReference(opcode.referenceType, referenceIndex);

        b = reader.readUbyte();
        int registerC = NibbleUtils.extractLowUnsignedNibble(b);
        int registerD = NibbleUtils.extractHighUnsignedNibble(b);

        b = reader.readUbyte();
        int registerE = NibbleUtils.extractLowUnsignedNibble(b);
        int registerF = NibbleUtils.extractHighUnsignedNibble(b);

        return new ImmutableInstruction35c(opcode, registerCount, registerC, registerD,
                registerE, registerF, registerG, reference);
    }

    @Nonnull
    private static Instruction3rc instruction3rc(@Nonnull Opcode opcode, @Nonnull DexReader reader) {
        int registerCount = reader.readUbyte();
        int referenceIndex = reader.readUshort();
        String reference = reader.getReference(opcode.referenceType, referenceIndex);
        int startRegister = reader.readUshort();
        return new ImmutableInstruction3rc(opcode, startRegister, registerCount, reference);
    }

    @Nonnull
    private static Instruction51l instruction51l(@Nonnull Opcode opcode, @Nonnull DexReader reader) {
        int registerA = reader.readUbyte();
        long literal = reader.readLong();
        return new ImmutableInstruction51l(opcode, registerA, literal);
    }

    @Nonnull
    private static DexBackedPackedSwitchPayload packedSwitchPayload(@Nonnull DexReader reader) {
        // the reader is currently positioned after the 2-byte "opcode"
        int instructionStartOffset = reader.getOffset() - 2;
        DexBackedPackedSwitchPayload instruction =
                new DexBackedPackedSwitchPayload(reader.getDexBuffer(), instructionStartOffset);
        reader.setOffset(instructionStartOffset + instruction.getCodeUnits() * 2);
        return instruction;
    }

    @Nonnull
    private static DexBackedSparseSwitchPayload sparseSwitchPayload(@Nonnull DexReader reader) {
        // the reader is currently positioned after the 2-byte "opcode"
        int instructionStartOffset = reader.getOffset() - 2;
        DexBackedSparseSwitchPayload instruction =
                new DexBackedSparseSwitchPayload(reader.getDexBuffer(), instructionStartOffset);
        reader.setOffset(instructionStartOffset + instruction.getCodeUnits() * 2);
        return instruction;
    }

    @Nonnull
    private static DexBackedArrayPayload arrayPayload(@Nonnull DexReader reader) {
        // the reader is currently positioned after the 2-byte "opcode"
        int instructionStartOffset = reader.getOffset() - 2;
        DexBackedArrayPayload instruction = new DexBackedArrayPayload(reader.getDexBuffer(), instructionStartOffset);
        reader.setOffset(instructionStartOffset + instruction.getCodeUnits() * 2);
        return instruction;
    }
}
