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

import org.jf.dexlib.code.Instruction;
import org.jf.dexlib.code.Opcode;
import org.jf.dexlib.DexFile;
import org.jf.dexlib.IndexedItem;
import org.jf.dexlib.util.Input;

import java.util.List;
import java.util.ArrayList;

public class ArrayDataPseudoInstruction extends Instruction
{
    private int elementWidth;
    private List<byte[]> values;

    public ArrayDataPseudoInstruction(DexFile dexFile, int elementWidth, List<byte[]> values) {
        super(dexFile, Opcode.NOP, (IndexedItem)null);

        this.elementWidth = elementWidth;
        this.values = values;
        
        int byteCount = 0;

        for (byte[] value: values) {
            byteCount += value.length;
        }

        if (byteCount % elementWidth != 0) {
            throw new RuntimeException("There are not a whole number of " + ((Integer)elementWidth).toString() + " byte elements");
        }

        int elementCount = byteCount / elementWidth;

        encodedInstruction = new byte[byteCount+8];
        encodedInstruction[0] = 0x00;
        encodedInstruction[1] = 0x03; //fill-array-data psuedo-opcode

        encodedInstruction[2] = (byte)elementWidth;
        encodedInstruction[3] = (byte)(elementWidth >> 8);

        encodedInstruction[4] = (byte)elementCount;
        encodedInstruction[5] = (byte)(elementCount >> 8);
        encodedInstruction[6] = (byte)(elementCount >> 16);
        encodedInstruction[7] = (byte)(elementCount >> 24);

        int position = 8;

        for (byte[] value: values) {
            for (byte byteValue: value) {
                encodedInstruction[position++] = byteValue;
            }
        }
    }

    private ArrayDataPseudoInstruction() {
    }

    protected void checkFormat(Format format) {
        //no need to check the format
    }
                                 
    public static ArrayDataPseudoInstruction make(DexFile dexFile, Input input) {
        byte opcodeByte = input.readByte();
        if (opcodeByte != 0x00) {
            throw new RuntimeException("Invalid opcode byte for an ArrayData pseudo-instruction");
        }
        byte subopcodeByte = input.readByte();
        if (subopcodeByte != 0x03) {
            throw new RuntimeException("Invalid sub-opcode byte for an ArrayData pseudo-instruction");
        }

        int elementWidth = input.readShort();
        int size = input.readInt();

        List<byte[]> elementsList = new ArrayList<byte[]>();

        for (int i=0; i<size; i++) {
            elementsList.add(input.readBytes(elementWidth));
        }

        return new ArrayDataPseudoInstruction(dexFile, elementWidth, elementsList);
    }

    public Format getFormat() {
        return Format.ArrayData;
    }

    protected Instruction makeClone() {
        return new ArrayDataPseudoInstruction();
    }

    public int getElementWidth() {
        return elementWidth;
    }

    public List<byte[]> getValues() {
        return values;
    }
}
