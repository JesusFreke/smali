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
import org.jf.dexlib.util.Input;

public class PackedSwitchDataPseudoInstruction extends Instruction
{
    private int firstKey;
    private int[] targets;

    public PackedSwitchDataPseudoInstruction(DexFile dexFile, int firstKey, int[] targets) {
        super(dexFile, Opcode.NOP, (IndexedItem)null);
        this.firstKey = firstKey;
        this.targets = targets;

        if (targets.length > 0xFFFF) {
            throw new RuntimeException("The packed-switch data contains too many elements. " +
                    "The maximum number of switch elements is 65535");
        }

        encodedInstruction = new byte[targets.length * 4 + 8];
        encodedInstruction[0] = 0x00;
        encodedInstruction[1] = 0x01; //packed-switch pseudo-opcode

        encodedInstruction[2] = (byte)targets.length;
        encodedInstruction[3] = (byte)(targets.length >> 8);

        encodedInstruction[4] = (byte)firstKey;
        encodedInstruction[5] = (byte)(firstKey >> 8);
        encodedInstruction[6] = (byte)(firstKey >> 16);
        encodedInstruction[7] = (byte)(firstKey >> 24);

        int position= 8;
        for (int target: targets) {
            encodedInstruction[position++] = (byte)target;
            encodedInstruction[position++] = (byte)(target >> 8);
            encodedInstruction[position++] = (byte)(target >> 16);
            encodedInstruction[position++] = (byte)(target >> 24);
        }
    }

    protected void checkFormat(Format format) {
        //no need to check the format
    }

    private PackedSwitchDataPseudoInstruction() {
    }

    protected Instruction makeClone() {
        return new PackedSwitchDataPseudoInstruction();
    }


    public static PackedSwitchDataPseudoInstruction make(DexFile dexFile, Input input) {
        byte opcodeByte = input.readByte();
        if (opcodeByte != 0x00) {
            throw new RuntimeException("Invalid opcode byte for a PackedSwitchData pseudo-instruction");
        }
        byte subopcodeByte = input.readByte();
        if (subopcodeByte != 0x01) {
            throw new RuntimeException("Invalid sub-opcode byte for a PackedSwitchData pseudo-instruction");
        }

        int targetCount = input.readShort();

        int firstKey = input.readInt();
        int[] targets = new int[targetCount];

        for (int i=0; i<targetCount; i++) {
            targets[i] = input.readInt();
        }

        return new PackedSwitchDataPseudoInstruction(dexFile, firstKey, targets);
    }

    public Format getFormat() {
        return Format.PackedSwitchData;
    }

    public int getFirstKey() {
        return firstKey;
    }

    public int[] getTargets() {
        return targets;
    }
}
