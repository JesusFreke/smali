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

package org.JesusFreke.dexlib.debug;

import org.JesusFreke.dexlib.DexFile;
import org.JesusFreke.dexlib.util.Input;

public abstract class DebugInstructionFactory {
    public static DebugInstruction readDebugInstruction(DexFile dexFile, Input in) {
        int startCursor = in.getCursor();
        byte opcode = in.readByte();
        in.setCursor(startCursor);

        DebugInstruction debugInstruction = makeDebugInstruction(dexFile, opcode);
        debugInstruction.readFrom(in);
        return debugInstruction;
    }

    public static DebugInstruction makeDebugInstruction(DexFile dexFile, byte opcode) {
        switch (opcode) {
            case 0x00:
                return new EndSequence();
            case 0x01:
                return new AdvancePC();
            case 0x02:
                return new AdvanceLine();
            case 0x03:
                return new StartLocal(dexFile);
            case 0x04:
                return new StartLocalExtended(dexFile);
            case 0x05:
                return new EndLocal();
            case 0x06:
                return new RestartLocal();
            case 0x07:
                return new SetPrologueEnd();
            case 0x08:
                return new SetEpilogueBegin();
            case 0x09:
                return new SetFile(dexFile);
            default:
                return new SpecialOpcode(opcode);
        }

    }
}
