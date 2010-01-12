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
 * INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.dexlib.Code.Format;

import org.jf.dexlib.Code.Opcode;
import org.jf.dexlib.Code.TwoRegisterInstruction;
import org.jf.dexlib.Code.InstructionWithReference;
import org.jf.dexlib.FieldIdItem;
import org.jf.dexlib.Util.AnnotatedOutput;

public class Instruction22csf extends InstructionWithReference implements TwoRegisterInstruction {
    private final Instruction22cs unfixedInstruction;

    public Instruction22csf(Opcode opcode, Instruction22cs unfixedInstruction, FieldIdItem field) {
        //the opcode should be the "fixed" opcode. i.e. iget-object, etc. (NOT the "quick" version)
        super(opcode, field);
        this.unfixedInstruction = unfixedInstruction;
    }

    protected void writeInstruction(AnnotatedOutput out, int currentCodeAddress) {
        byte regA = (byte)getRegisterA();
        byte regB = (byte)getRegisterB();

        out.writeByte(opcode.value);
        out.writeByte((regB << 4) | regA);
        out.writeShort(this.getReferencedItem().getIndex());
    }

    public Format getFormat() {
        return Format.Format22csf;
    }

    public int getRegisterA() {
        return unfixedInstruction.getRegisterA();
    }

    public int getRegisterB() {
        return unfixedInstruction.getRegisterB();
    }
}
