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
import org.JesusFreke.dexlib.IndexedItem;

public class Format22b extends Format
{
    public static final Format22b Format = new Format22b();

    private Format22b() {
    }

    public Instruction make(DexFile dexFile, byte opcode, short regA, short regB, byte litC) {
        byte[] bytes = new byte[4];

        Opcode op = Opcode.getOpcodeByValue(opcode);

        checkOpcodeFormat(op);

        if (regA >= 1<<8) {
            throw new RuntimeException("The register number must be less than v256");
        }

        if (regB >= 1<<8) {
            throw new RuntimeException("The register number must be less than v256");
        }

        bytes[0] = opcode;
        bytes[1] = (byte)regA;
        bytes[2] = (byte)regB;
        bytes[3] = litC;

        return new Instruction(dexFile, bytes, null);
    }

    public int getByteCount() {
        return 4;
    }

    public String getFormatName() {
        return "22b";
    }
}
