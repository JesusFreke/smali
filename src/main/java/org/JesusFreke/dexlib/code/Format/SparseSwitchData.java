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

package org.JesusFreke.dexlib.code.Format;

import org.JesusFreke.dexlib.code.Instruction;
import org.JesusFreke.dexlib.code.Opcode;
import org.JesusFreke.dexlib.DexFile;

import java.util.List;

public class SparseSwitchData
{
    //TODO: change from 2 List<Integer> to int[,]
    public static Instruction make(DexFile dexFile, List<Integer> keys, List<Integer> targets) {
        byte[] bytes;

        if (keys.size() != targets.size()) {
            throw new RuntimeException("The number of keys and offsets don't match");
        }


        if (targets.size() > 0xFFFF) {
            throw new RuntimeException("The sparse-switch data contains too many elements. " +
                    "The maximum number of switch elements is 65535");
        }

        //TODO: need to sort the switch elements based on key, in ascending order

        bytes = new byte[targets.size() * 8 + 4];
        int position = 4;


        for (int key: keys) {
            bytes[position++] = (byte)key;
            bytes[position++] = (byte)(key >> 8);
            bytes[position++] = (byte)(key >> 16);
            bytes[position++] = (byte)(key >> 24);
        }

        for (int target: targets) {
            bytes[position++] = (byte)target;
            bytes[position++] = (byte)(target >> 8);
            bytes[position++] = (byte)(target >> 16);
            bytes[position++] = (byte)(target >> 24);
        }

        //sparse-switch psuedo-opcode
        bytes[0] = 0x00;
        bytes[1] = 0x02;

        bytes[2] = (byte)targets.size();
        bytes[3] = (byte)(targets.size() >> 8);

        return new Instruction(dexFile, bytes, null);
    }
}
