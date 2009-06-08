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

import org.jf.dexlib.*;
import org.jf.dexlib.code.Instruction;
import org.jf.dexlib.code.Opcode;
import static org.jf.dexlib.code.Opcode.*;

public class Format3rc extends Format
{
    public static final Format3rc Format = new Format3rc();

    public Format3rc() {
    }

    public Instruction make(DexFile dexFile, byte opcode, short regCount, int startReg, IndexedItem item) {
        byte[] bytes = new byte[6];

        Opcode op = Opcode.getOpcodeByValue(opcode);

        checkOpcodeFormat(op);

        if (regCount >= 1<<8) {
            throw new RuntimeException("regCount must be less than 256");
        }
        if (regCount < 0) {
            throw new RuntimeException("regCount cannot be negative");
        }

        if (startReg >= 1<<16) {
            throw new RuntimeException("The beginning register of the range must be less than 65536");
        }
        if (startReg < 0) {
            throw new RuntimeException("The beginning register of the range cannot be negative");
        }

        bytes[0] = opcode;
        bytes[1] = (byte)regCount;
        bytes[4] = (byte)startReg;
        bytes[5] = (byte)(startReg >> 8);

        //go ahead and make the instruction now, which will verify that item is the correct type. If it isn't,
        //the construction will throw an exception
        Instruction instruction = new Instruction(dexFile, bytes, item);

        if (opcode == FILLED_NEW_ARRAY_RANGE.value) {
            //check data for filled-new-array/range opcode
            String type = ((TypeIdItem)item).getTypeDescriptor();
            if (type.charAt(0) != '[') {
                throw new RuntimeException("The type must be an array type");
            }
            if (type.charAt(1) == 'J' || type.charAt(1) == 'D') {
                throw new RuntimeException("The type cannot be an array of longs or doubles");
            }
        } else if (opcode >= INVOKE_VIRTUAL_RANGE.value && opcode <= INVOKE_INTERFACE_RANGE.value) {
            //check data for invoke-*/range opcodes
            MethodIdItem methodIdItem = (MethodIdItem)item;
            if (methodIdItem.getParameterRegisterCount(opcode == INVOKE_STATIC_RANGE.value) != regCount) {
                throw new RuntimeException("regCount does not match the number of arguments of the method");
            }
        } else {
            throw new RuntimeException("Opcode " + Integer.toHexString(opcode) + " does not use the 35c format");
        }

        return instruction;
    }

    public int getByteCount() {
        return 6;
    }

    public String getFormatName() {
        return "3rc";
    }

    /*@Test
    public void testInvoke() {
        DexFile dexFile = DexFile.makeBlankDexFile();
        ArrayList<TypeIdItem> types = new ArrayList<TypeIdItem>();
        types.add(new TypeIdItem(dexFile, "I"));
        types.add(new TypeIdItem(dexFile, "I"));
        types.add(new TypeIdItem(dexFile, "I"));
        types.add(new TypeIdItem(dexFile, "I"));
        types.add(new TypeIdItem(dexFile, "I"));
        MethodIdItem method = new MethodIdItem(dexFile, new TypeIdItem(dexFile, "test"), "test", new ProtoIdItem(dexFile, new TypeIdItem(dexFile, "V"), types));

        Instruction ins = Format.make(dexFile, (byte)INVOKE_VIRTUAL_RANGE.value, (short)6, 65500, method);
        byte[] bytes = new byte[] {0x74, 0x06, 0x00, 0x00, (byte)0xDC, (byte)0xFF};
        assertTrue("Is everything put in the right place?", java.util.Arrays.equals(ins.getBytes(), bytes));
    }*/
}
