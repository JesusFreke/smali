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

package org.jf.dexlib.Code.Format;

import org.jf.dexlib.DexFile;
import org.jf.dexlib.IndexedItem;
import org.jf.dexlib.MethodIdItem;
import org.jf.dexlib.TypeIdItem;
import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.Opcode;
import static org.jf.dexlib.Code.Opcode.*;
import org.jf.dexlib.Util.NumberUtils;

public class Instruction35c extends Instruction
{
    public static final Instruction.InstructionFactory Factory = new Factory();

    public Instruction35c(DexFile dexFile, Opcode opcode, int regCount, byte regD, byte regE, byte regF, byte regG,
                     byte regA, IndexedItem item) {
        super(dexFile, opcode, item);

        if (regCount > 5) {
            throw new RuntimeException("regCount cannot be greater than 5");
        }
        
        if (regD >= 1<<4 ||
            regE >= 1<<4 ||
            regF >= 1<<4 ||
            regG >= 1<<4 ||
            regA >= 1<<4) {
            throw new RuntimeException("All register args must fit in 4 bits");
        }

        encodedInstruction = new byte[6];
        encodedInstruction[0] = opcode.value;
        encodedInstruction[1] = (byte)((regCount << 4) | regA);
        //the item index will be set later, during placement/writing        
        encodedInstruction[4] = (byte)((regE << 4) | regD);
        encodedInstruction[5] = (byte)((regG << 4) | regF);

        checkItem();
    }

    private Instruction35c(DexFile dexFile, Opcode opcode, byte[] rest) {
        super(dexFile, opcode, rest);

        if (getRegCount() > 5) {
            throw new RuntimeException("regCount cannot be greater than 5");
        }

        checkItem();
    }

    private Instruction35c() {
    }

    public Format getFormat() {
        return Format.Format35c;
    }

    protected Instruction makeClone() {
        return new Instruction35c();
    }

    private static class Factory implements Instruction.InstructionFactory {
        public Instruction makeInstruction(DexFile dexFile, Opcode opcode, byte[] rest) {
            return new Instruction35c(dexFile, opcode, rest);
        }
    }


    public byte getRegisterA() {
        return NumberUtils.decodeLowUnsignedNibble(encodedInstruction[1]);
    }

    public byte getRegCount() {
        return NumberUtils.decodeHighUnsignedNibble(encodedInstruction[1]);
    }

    public byte getRegisterD() {
        return NumberUtils.decodeLowUnsignedNibble(encodedInstruction[4]);
    }

    public byte getRegisterE() {
        return NumberUtils.decodeHighUnsignedNibble(encodedInstruction[4]);
    }

    public byte getRegisterF() {
        return NumberUtils.decodeLowUnsignedNibble(encodedInstruction[5]);
    }

    public byte getRegisterG() {
        return NumberUtils.decodeHighUnsignedNibble(encodedInstruction[5]);
    }

    private void checkItem() {
        Opcode opcode = getOpcode();
        IndexedItem item = getReferencedItem();

        if (opcode == FILLED_NEW_ARRAY) {
            //check data for filled-new-array opcode
            String type = ((TypeIdItem)item).getTypeDescriptor();
            if (type.charAt(0) != '[') {
                throw new RuntimeException("The type must be an array type");
            }
            if (type.charAt(1) == 'J' || type.charAt(1) == 'D') {
                throw new RuntimeException("The type cannot be an array of longs or doubles");
            }
        } else if (opcode.value >= INVOKE_VIRTUAL.value && opcode.value <= INVOKE_INTERFACE.value) {
            //check data for invoke-* opcodes
            MethodIdItem methodIdItem = (MethodIdItem)item;
            if (methodIdItem.getParameterRegisterCount(opcode == INVOKE_STATIC) != getRegCount()) {
                throw new RuntimeException("regCount does not match the number of arguments of the method");
            }
        }
    }

}
