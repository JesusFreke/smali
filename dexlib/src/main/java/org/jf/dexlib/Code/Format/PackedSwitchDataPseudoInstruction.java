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

public class PackedSwitchDataPseudoInstruction extends Instruction {
    public static final Instruction.InstructionFactory Factory = new Factory();

    @Override
    public int getSize() {
        return getTargetCount() * 4 + 8;
    }

    public static void emit(Output out, int firstKey, int[] targets) {
        if (targets.length > 0xFFFF) {
            throw new RuntimeException("The packed-switch data contains too many elements. " +
                    "The maximum number of switch elements is 65535");
        }

        //write out padding, if necessary
        if (out.getCursor() % 4 != 0) {
            out.writeShort(0);
        }

        out.writeByte(0x00);
        out.writeByte(0x01);
        out.writeShort(targets.length);
        out.writeInt(firstKey);

        for (int target : targets) {
            out.writeInt(target);
        }
    }

    public PackedSwitchDataPseudoInstruction(byte[] buffer, int bufferIndex) {
        super(Opcode.NOP, buffer, bufferIndex);

        byte opcodeByte = buffer[bufferIndex++];
        if (opcodeByte != 0x00) {
            throw new RuntimeException("Invalid opcode byte for a PackedSwitchData pseudo-instruction");
        }
        byte subopcodeByte = buffer[bufferIndex];
        if (subopcodeByte != 0x01) {
            throw new RuntimeException("Invalid sub-opcode byte for a PackedSwitchData pseudo-instruction");
        }
    }

    public Format getFormat() {
        return Format.PackedSwitchData;
    }

    public int getTargetCount() {
        return NumberUtils.decodeUnsignedShort(buffer, bufferIndex + 2);
    }

    public int getFirstKey() {
        return NumberUtils.decodeInt(buffer, bufferIndex + 4);
    }

    public static class PackedSwitchTarget {
        public int value;
        public int target;
    }

    public Iterator<PackedSwitchTarget> getTargets() {
        return new Iterator<PackedSwitchTarget>() {
            final int targetCount = getTargetCount();
            int i = 0;
            int position = bufferIndex + 8;
            int value = getFirstKey();

            PackedSwitchTarget packedSwitchTarget = new PackedSwitchTarget();

            public boolean hasNext() {
                return i<targetCount;
            }

            public PackedSwitchTarget next() {
                packedSwitchTarget.value = value++;
                packedSwitchTarget.target = NumberUtils.decodeInt(buffer, position);
                position+=4;
                i++;
                return packedSwitchTarget;
            }

            public void remove() {
            }
        };
    }

    public static interface PackedSwitchTargetIteratorDelegate {
        void ProcessPackedSwitchTarget(int value, int target);
    }

    private static class Factory implements Instruction.InstructionFactory {
        public Instruction makeInstruction(DexFile dexFile, Opcode opcode, byte[] buffer, int bufferIndex) {
            if (opcode != Opcode.NOP) {
                throw new RuntimeException("The opcode for a PackedSwitchDataPseudoInstruction must by NOP");
            }
            return new PackedSwitchDataPseudoInstruction(buffer, bufferIndex);
        }
    }
}
