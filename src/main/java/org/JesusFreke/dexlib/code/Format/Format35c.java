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

import org.JesusFreke.dexlib.*;
import org.JesusFreke.dexlib.code.Instruction;
import org.JesusFreke.dexlib.code.Opcode;
import static org.JesusFreke.dexlib.code.Opcode.*;

public class Format35c extends Format
{
    public static final Format35c Format = new Format35c();

    private Format35c() {
    }

    public Instruction make(DexFile dexFile, byte opcode, byte regCount, byte regD, byte regE, byte regF, byte regG, byte regA, IndexedItem item) {
        byte[] bytes = new byte[6];

        Opcode op = Opcode.getOpcodeByValue(opcode);

        checkOpcodeFormat(op);

        if (regCount > 5) {
            throw new RuntimeException("regCount cannot be greater than 5");
        }
        if (regD >= 16 ||
            regE >= 16 ||
            regF >= 16 ||
            regG >= 16 ||
            regA >= 16) {
            throw new RuntimeException("All register args must fit in 4 bits");
        }

        bytes[0] = opcode;
        bytes[1] = (byte)((regCount << 4) | regA);
        bytes[4] = (byte)((regE << 4) | regD);
        bytes[5] = (byte)((regG << 4) | regF);

        //go ahead and make the instruction, to verify that item is the correct type. If it isn't,
        //the construction will throw an exception
        Instruction instruction = new Instruction(dexFile, bytes, item);

        if (opcode == FILLED_NEW_ARRAY.value) {
            //check data for filled-new-array opcode
            String type = ((TypeIdItem)item).toString();
            if (type.charAt(0) != '[') {
                throw new RuntimeException("The type must be an array type");
            }
            if (type.charAt(1) == 'J' || type.charAt(1) == 'D') {
                throw new RuntimeException("The type cannot be an array of longs or doubles");
            }
        } else if (opcode >= INVOKE_VIRTUAL.value && opcode <= INVOKE_INTERFACE.value) {
            //check data for invoke-* opcodes
            MethodIdItem methodIdItem = (MethodIdItem)item;
            if (methodIdItem.getParameterWordCount(opcode == INVOKE_STATIC.value) != regCount) {
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
        return "35c";
    }

    /*@Test
    public void testInvoke() {
        DexFile dexFile = new DexFile();
        ArrayList<TypeIdItem> types = new ArrayList<TypeIdItem>();
        types.add(new TypeIdItem(dexFile, "I"));
        types.add(new TypeIdItem(dexFile, "I"));
        types.add(new TypeIdItem(dexFile, "I"));
        types.add(new TypeIdItem(dexFile, "I"));
        types.add(new TypeIdItem(dexFile, "I"));
        MethodIdItem method = new MethodIdItem(dexFile, new TypeIdItem(dexFile, "test"), "test", new ProtoIdItem(dexFile, new TypeIdItem(dexFile, "V"), types));

        Instruction ins = make(dexFile, (byte)INVOKE_VIRTUAL.value, (byte)5, (byte)0, (byte)1, (byte)2, (byte)3, (byte)4, method);
        assertTrue("Is everything put in the right place?", java.util.Arrays.equals(ins.getBytes(), new byte[] {0x6e, 0x54, 0x00, 0x00, 0x10, 0x32}));
    }*/
}
