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

package org.jf.dexlib.Util;

import org.jf.dexlib.StringIdItem;
import org.jf.dexlib.TypeIdItem;
import org.jf.dexlib.DebugInfoItem;
import org.jf.dexlib.Debug.*;

public class DebugInfoDecoder {
    private final DebugInfoItem debugItem;
    private final DebugInfoDelegate delegate;
    private final int registers;
    
    public DebugInfoDecoder(DebugInfoItem debugItem, DebugInfoDelegate delegate, int registers) {
        this.debugItem = debugItem;
        this.delegate = delegate;
        this.registers = registers;
    }

    public void decode() {
        int address = 0;
        int line = debugItem.getLineStart();
        Local[] locals = new Local[registers];

        for (DebugInstruction debugInst: debugItem.getDebugInstructions()) {
            switch (debugInst.getOpcode()) {
                case 0x00:
                    return;
                case 0x01:
                    address += ((AdvancePC)debugInst).getAddressDelta();
                    break;
                case 0x02:
                    line += ((AdvanceLine)debugInst).getLineDelta();
                    break;
                case 0x03:
                {
                    StartLocal inst = (StartLocal)debugInst;
                    Local local = new Local(inst.getRegisterNumber(), inst.getName(), inst.getType(), null);
                    locals[inst.getRegisterNumber()] = local;
                    delegate.startLocal(address, local);
                    break;
                }
                case 0x04:
                {
                    StartLocalExtended inst = (StartLocalExtended)debugInst;
                    Local local = new Local(inst.getRegisterNumber(), inst.getName(), inst.getType(),
                            inst.getSignature());
                    locals[inst.getRegisterNumber()] = local;
                    delegate.startLocal(address, local);
                    break;
                }
                case 0x05:
                {
                    EndLocal inst = (EndLocal)debugInst;
                    Local local = locals[inst.getRegisterNumber()];
                    if (local == null) {
                        local = new Local(inst.getRegisterNumber(), null, null, null);
                    }
                    delegate.endLocal(address, local);
                    break;
                }
                case 0x06:
                {
                    RestartLocal inst = (RestartLocal)debugInst;
                    Local local = locals[inst.getRegisterNumber()];
                    if (local == null) {
                        local = new Local(inst.getRegisterNumber(), null, null, null);
                    }
                    delegate.restartLocal(address, local);
                    break;
                }
                case 0x07:
                    delegate.endPrologue(address);
                    break;
                case 0x08:
                    delegate.startEpilogue(address);
                    break;
                case 0x09:
                    delegate.setFile(address, ((SetFile)debugInst).getFileName());
                    break;
                default:
                {
                    SpecialOpcode inst = (SpecialOpcode)debugInst;
                    address += inst.getAddressDelta();
                    line += inst.getLineDelta();
                    delegate.line(address, line);
                    break;
                }
            }
        }
    }

    public class Local {
        public final int register;
        public final StringIdItem name;
        public final TypeIdItem type;
        public final StringIdItem signature;
        public Local(int register, StringIdItem name, TypeIdItem type, StringIdItem signature) {
            this.register = register;
            this.name = name;
            this.type = type;
            this.signature = signature;
        }
    }

    public static interface DebugInfoDelegate {
        public void endPrologue(int address);
        public void startEpilogue(int address);
        public void startLocal(int address, Local local);
        public void endLocal(int address, Local local);
        public void restartLocal(int address, Local local);
        public void setFile(int address, StringIdItem fileName);
        public void line(int address, int line);
    }
}
