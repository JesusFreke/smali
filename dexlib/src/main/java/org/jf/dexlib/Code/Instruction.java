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

package org.jf.dexlib.Code;

import org.jf.dexlib.*;
import org.jf.dexlib.Code.Format.Format;

public abstract class Instruction {
    public final Opcode opcode;
    protected final byte[] buffer;
    protected final int bufferIndex;

    public int getSize() {
        return opcode.format.size;
    }

    protected Instruction(Opcode opcode) {
        this.opcode = opcode;

        this.bufferIndex = 0;
        this.buffer = new byte[opcode.format.size];
    }

    protected Instruction(Opcode opcode, int bufferSize) {
        this.opcode = opcode;

        this.bufferIndex = 0;
        this.buffer = new byte[bufferSize];
    }

    protected Instruction(Opcode opcode, byte[] buffer, int bufferIndex) {
        this.opcode = opcode;

        this.buffer = buffer;
        this.bufferIndex = bufferIndex;

        if (buffer[bufferIndex] != opcode.value) {
            throw new RuntimeException("The given opcode doesn't match the opcode byte");
        }
    }

    public abstract Format getFormat();

    public static interface InstructionFactory {
        public Instruction makeInstruction(DexFile dexFile, Opcode opcode, byte[] buffer, int bufferIndex);
    }
}
