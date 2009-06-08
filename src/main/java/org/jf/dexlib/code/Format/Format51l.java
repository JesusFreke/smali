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

public class Format51l extends Format
{
    public static final Format51l Format = new Format51l();

    private Format51l() {
    }

    public Instruction make(DexFile dexFile, byte opcode, short regA, long litB) {
        byte[] bytes = new byte[10];

        Opcode op = Opcode.getOpcodeByValue(opcode);

        checkOpcodeFormat(op);

        if (regA >= 1<<8) {
            throw new RuntimeException("The register number must be less than v256");
        }

        bytes[0] = opcode;
        bytes[1] = (byte)regA;

        bytes[2] = (byte)litB;
        bytes[3] = (byte)(litB >> 8);

        bytes[4] = (byte)(litB >> 16);
        bytes[5] = (byte)(litB >> 24);

        bytes[6] = (byte)(litB >> 32);
        bytes[7] = (byte)(litB >> 40);

        bytes[8] = (byte)(litB >> 48);
        bytes[9] = (byte)(litB >> 56);

        return new Instruction(dexFile, bytes, null);
    }

    public int getByteCount() {
        return 10;
    }

    public String getFormatName() {
        return "51l";
    }
}
