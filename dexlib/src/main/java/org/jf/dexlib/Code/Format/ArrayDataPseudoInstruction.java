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

import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.Opcode;
import org.jf.dexlib.Util.NumberUtils;
import org.jf.dexlib.Util.Output;
import org.jf.dexlib.DexFile;

import java.util.Iterator;

public class ArrayDataPseudoInstruction extends Instruction {
    public static final Instruction.InstructionFactory Factory = new Factory();

    @Override
    public int getSize() {
        int size = getElementWidth() * getElementCount();
        return size + (size & 0x01) + 8;
    }

    public static void emit(Output out, int elementWidth, byte[] encodedValues) {
        if (encodedValues.length % elementWidth != 0) {
            throw new RuntimeException("There are not a whole number of " + elementWidth + " byte elements");
        }

        int elementCount = encodedValues.length / elementWidth;

        out.writeByte(0x00);
        out.writeShort(0x03);
        out.writeShort(elementWidth);
        out.writeInt(elementCount);
        out.write(encodedValues);
    }

    public ArrayDataPseudoInstruction(byte[] buffer, int bufferIndex) {
        super(Opcode.NOP, buffer, bufferIndex);

        byte opcodeByte = buffer[bufferIndex++];
        if (opcodeByte != 0x00) {
            throw new RuntimeException("Invalid opcode byte for an ArrayData pseudo-instruction");
        }

        byte subopcodeByte = buffer[bufferIndex];
        if (subopcodeByte != 0x03) {
            throw new RuntimeException("Invalid sub-opcode byte for an ArrayData pseudo-instruction");
        }
    }

    public Format getFormat() {
        return Format.ArrayData;
    }

    public int getElementWidth() {
        return NumberUtils.decodeUnsignedShort(buffer[bufferIndex+2], buffer[bufferIndex+3]);
    }

    public int getElementCount() {
        return NumberUtils.decodeInt(buffer, bufferIndex+4);
    }

    public static class ArrayElement {
        public final byte[] buffer;
        public int bufferIndex;
        public final int elementWidth;
        public ArrayElement(byte[] buffer, int elementWidth) {
            this.buffer = buffer;
            this.elementWidth = elementWidth;
        }
    }

    public Iterator<ArrayElement> getElements() {
        return new Iterator<ArrayElement>() {
            final int elementCount = getElementCount();
            int i=0;
            int position = bufferIndex + 8;
            final ArrayElement arrayElement = new ArrayElement(buffer, getElementWidth());

            public boolean hasNext() {
                return i<elementCount;
            }

            public ArrayElement next() {
                arrayElement.bufferIndex = position;
                position += arrayElement.elementWidth;
                i++;
                return arrayElement;
            }

            public void remove() {
            }
        };
    }

    private static class Factory implements Instruction.InstructionFactory {
        public Instruction makeInstruction(DexFile dexFile, Opcode opcode, byte[] buffer, int bufferIndex) {
            if (opcode != Opcode.NOP) {
                throw new RuntimeException("The opcode for an ArrayDataPseudoInstruction must by NOP");
            }
            return new ArrayDataPseudoInstruction(buffer, bufferIndex);
        }
    }
}
