/*
 * [The "BSD licence"]
 * Copyright (c) 2009 Ben Gruver
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.dexlib.code.Format;

import org.jf.dexlib.DexFile;
import org.jf.dexlib.IndexedItem;
import org.jf.dexlib.code.Instruction;
import org.jf.dexlib.code.Opcode;
import org.jf.dexlib.util.NumberUtils;

public class Instruction51l extends Instruction
{
    public static final Instruction.InstructionFactory Factory = new Factory();

    public Instruction51l(DexFile dexFile, Opcode opcode, short regA, long litB) {
        super(dexFile, opcode, (IndexedItem)null);

        if (regA >= 1<<8) {
            throw new RuntimeException("The register number must be less than v256");
        }

        encodedInstruction = new byte[10];
        encodedInstruction[0] = opcode.value;
        encodedInstruction[1] = (byte)regA;
        encodedInstruction[2] = (byte)litB;
        encodedInstruction[3] = (byte)(litB >> 8);
        encodedInstruction[4] = (byte)(litB >> 16);
        encodedInstruction[5] = (byte)(litB >> 24);
        encodedInstruction[6] = (byte)(litB >> 32);
        encodedInstruction[7] = (byte)(litB >> 40);
        encodedInstruction[8] = (byte)(litB >> 48);
        encodedInstruction[9] = (byte)(litB >> 56);
    }

    private Instruction51l(DexFile dexFile, Opcode opcode, byte[] rest) {
        super(dexFile, opcode, rest);
    }

    private Instruction51l() {
    }

    public Format getFormat() {
        return Format.Format51l;
    }

    protected Instruction makeClone() {
        return new Instruction51l();
    }

    private static class Factory implements Instruction.InstructionFactory {
        public Instruction makeInstruction(DexFile dexFile, Opcode opcode, byte[] rest) {
            return new Instruction51l(dexFile, opcode, rest);
        }
    }


    public short getRegister() {
        return NumberUtils.decodeUnsignedByte(encodedInstruction[1]);
    }

    public long getLiteral() {
        return  NumberUtils.decodeLong(encodedInstruction, 2);
    }
}
