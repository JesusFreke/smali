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

import org.jf.dexlib.*;
import org.jf.dexlib.util.NumberUtils;
import org.jf.dexlib.code.Instruction;
import org.jf.dexlib.code.Opcode;
import static org.jf.dexlib.code.Opcode.*;

public class Instruction3rc extends Instruction
{
    public static final Instruction.InstructionFactory Factory = new Factory();

    public Instruction3rc(DexFile dexFile, Opcode opcode, short regCount, int startReg, IndexedItem item) {
        super(dexFile, opcode, item);

        if (regCount >= 1<<8) {
            throw new RuntimeException("regCount must be less than 256");
        }
        if (regCount < 0) {
            throw new RuntimeException("regCount cannot be negative");
        }

        if (startReg >= 1<<16) {
            throw new RuntimeException("The beginning register of the range must be less than 65536");
        }
        if (startReg < 0) {
            throw new RuntimeException("The beginning register of the range cannot be negative");
        }

        encodedInstruction = new byte[6];
        encodedInstruction[0] = opcode.value;
        encodedInstruction[1] = (byte)regCount;
        //the item index will be set later, during placement/writing
        encodedInstruction[4] = (byte)startReg;
        encodedInstruction[5] = (byte)(startReg >> 8);

        checkItem();
    }

    private Instruction3rc(DexFile dexFile, Opcode opcode, byte[] rest) {
        super(dexFile, opcode, rest);

        checkItem();
    }

    private Instruction3rc() {
    }

    public Format getFormat() {
        return Format.Format3rc;
    }

    protected Instruction makeClone() {
        return new Instruction3rc();
    }

    private static class Factory implements Instruction.InstructionFactory {
        public Instruction makeInstruction(DexFile dexFile, Opcode opcode, byte[] rest) {
            return new Instruction3rc(dexFile, opcode, rest);
        }
    }


    public short getRegCount() {
        return NumberUtils.decodeUnsignedByte(encodedInstruction[1]);
    }

    public int getStartRegister() {
        return NumberUtils.decodeUnsignedShort(encodedInstruction[4], encodedInstruction[5]);
    }

    private void checkItem() {
        Opcode opcode = getOpcode();
        IndexedItem item = getReferencedItem();

        if (opcode == FILLED_NEW_ARRAY_RANGE) {
            //check data for filled-new-array/range opcode
            String type = ((TypeIdItem)item).getTypeDescriptor();
            if (type.charAt(0) != '[') {
                throw new RuntimeException("The type must be an array type");
            }
            if (type.charAt(1) == 'J' || type.charAt(1) == 'D') {
                throw new RuntimeException("The type cannot be an array of longs or doubles");
            }
        } else if (opcode.value >= INVOKE_VIRTUAL_RANGE.value && opcode.value <= INVOKE_INTERFACE_RANGE.value) {
            //check data for invoke-*/range opcodes
            MethodIdItem methodIdItem = (MethodIdItem)item;
            if (methodIdItem.getParameterRegisterCount(opcode == INVOKE_STATIC_RANGE) != getRegCount()) {
                throw new RuntimeException("regCount does not match the number of arguments of the method");
            }
        }
    }
}
