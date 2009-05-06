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

public class ArrayData
{
    public static Instruction make(DexFile dexFile, int elementWidth, List<byte[]> values) {
        byte[] bytes;

        int byteCount = 0;

        for (byte[] value: values) {
            byteCount += value.length;
        }

        if (byteCount % elementWidth != 0) {
            throw new RuntimeException("There are not a whole number of " + ((Integer)elementWidth).toString() + " byte elements");
        }

        bytes = new byte[byteCount+8];
        int position = 8;

        for (byte[] value: values) {
            for (byte byteValue: value) {
                bytes[position++] = byteValue;
            }
        }

        //fill-array-data psuedo-opcode
        bytes[0] = 0x00;
        bytes[1] = 0x03;

        bytes[2] = (byte)elementWidth;
        bytes[3] = (byte)(elementWidth >> 8);

        int elementCount = byteCount / elementWidth;

        bytes[4] = (byte)elementCount;
        bytes[5] = (byte)(elementCount >> 8);
        bytes[6] = (byte)(elementCount >> 16);
        bytes[7] = (byte)(elementCount >> 24);

        return new Instruction(dexFile, bytes, null);
    }
}
