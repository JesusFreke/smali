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

import org.jf.dexlib.Code.FiveRegisterInstruction;
import org.jf.dexlib.Code.Opcode;
import org.jf.dexlib.Code.InstructionWithReference;
import org.jf.dexlib.MethodIdItem;
import org.jf.dexlib.Util.AnnotatedOutput;

public class Instruction35msf extends InstructionWithReference implements FiveRegisterInstruction {
    private final Instruction35ms unfixedInstruction;

    public Instruction35msf(Opcode opcode, Instruction35ms unfixedInstruction, MethodIdItem method) {
        //the opcode should be the "fixed" opcode. i.e. iget-object, etc. (NOT the "quick" version)
        super(opcode, method);
        this.unfixedInstruction = unfixedInstruction;
    }

    protected void writeInstruction(AnnotatedOutput out, int currentCodeAddress) {
        byte regA = getRegisterA();
        byte regCount = getRegCount();
        byte regD = getRegisterD();
        byte regE = getRegisterE();
        byte regF = getRegisterF();
        byte regG = getRegisterG();

        out.writeByte(opcode.value);
        out.writeByte((regCount << 4) | regA);
        out.writeShort(this.getReferencedItem().getIndex());
        out.writeByte((regE << 4) | regD);
        out.writeByte((regG << 4) | regF);
    }

    public Format getFormat() {
        return Format.Format35msf;
    }

    public byte getRegCount() {
        return unfixedInstruction.getRegCount();
    }

    public byte getRegisterA() {
        return unfixedInstruction.getRegisterA();
    }

    public byte getRegisterD() {
        return unfixedInstruction.getRegisterD();
    }

    public byte getRegisterE() {
        return unfixedInstruction.getRegisterE();
    }

    public byte getRegisterF() {
        return unfixedInstruction.getRegisterF();
    }

    public byte getRegisterG() {
        return unfixedInstruction.getRegisterG();
    }
}
