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
 * INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.dexlib.code;

import org.jf.dexlib.DexFile;
import org.jf.dexlib.Field;
import org.jf.dexlib.IndexedItem;
import org.jf.dexlib.code.Format.ArrayDataPseudoInstruction;
import org.jf.dexlib.code.Format.PackedSwitchDataPseudoInstruction;
import org.jf.dexlib.code.Format.SparseSwitchDataPseudoInstruction;
import org.jf.dexlib.util.AnnotatedOutput;
import org.jf.dexlib.util.Input;

public class InstructionField implements Field<InstructionField> {
    private Instruction instruction;
    private DexFile dexFile;

    public InstructionField(DexFile dexFile) {
        this.dexFile = dexFile;
    }

    public InstructionField(DexFile dexFile, Instruction instruction) {
        this.dexFile = dexFile;
        this.instruction = instruction;
    }

    public void writeTo(AnnotatedOutput out) {
        byte[] bytes = instruction.encodedInstruction;
        IndexedItem reference = instruction.getReferencedItem();
        
        out.annotate(bytes.length, "instruction");
        if (needsAlign()) {
            //the "special instructions" must be 4 byte aligned
            out.alignTo(4);
            out.write(bytes);
        } else if (reference == null) {
            out.write(bytes);
        } else {
            out.write(bytes,0,2);
            //handle const-string/jumbo as a special case
            if (bytes[0] == 0x1b) {
                out.writeInt(reference.getIndex());
            } else {
                int index = reference.getIndex();
                if (index > 0xFFFF) {
                    throw new RuntimeException("String index doesn't fit.");
                }
                out.writeShort(reference.getIndex());
                out.write(bytes, 4, bytes.length - 4);
            }
        }
    }

    public void readFrom(Input in) {
        int startPos = in.getCursor();
        
        byte opByte = in.readByte();

        if (opByte == 0x00) {
            byte secondByte = in.readByte();

            switch (secondByte) {
                case 0x00:
                    //nop
                    instruction = Opcode.NOP.format.Factory.makeInstruction(dexFile, Opcode.NOP, new byte[]{0x00});
                    return;
                case 0x01:
                    //packed switch
                    in.setCursor(startPos);
                    instruction = PackedSwitchDataPseudoInstruction.make(dexFile, in);
                    return;
                case 0x02:
                    //sparse switch
                    in.setCursor(startPos);
                    instruction = SparseSwitchDataPseudoInstruction.make(dexFile, in);
                    return;
                case 0x03:
                    //array data
                    in.setCursor(startPos);
                    instruction = ArrayDataPseudoInstruction.make(dexFile, in);
                    return;
                default:
                    throw new RuntimeException("Invalid 2nd byte for opcode 0x00");
            }
        }

        Opcode opcode = Opcode.getOpcodeByValue(opByte);
        instruction = opcode.format.Factory.makeInstruction(dexFile, opcode, in.readBytes(opcode.format.size - 1));
    }

    public int place(int offset) {
        return offset + getSize(offset);
    }

    public void copyTo(DexFile dexFile, InstructionField copy) {
        copy.instruction = instruction.cloneTo(dexFile);
    }

    public int getSize(int offset) {
        if (this.needsAlign() && (offset % 4) != 0) {
            return instruction.encodedInstruction.length + 2;
        } else {
            return instruction.encodedInstruction.length;
        }
    }

    private boolean needsAlign() {
        //true if the opcode is one of the "special format" opcodes
        return instruction.encodedInstruction[0] == 0 && instruction.encodedInstruction[1] > 0; 
    }

    public Instruction getInstruction() {
        return instruction;
    }
}
