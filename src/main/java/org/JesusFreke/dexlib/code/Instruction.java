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

package org.JesusFreke.dexlib.code;

import org.JesusFreke.dexlib.*;
import org.JesusFreke.dexlib.util.Input;
import org.JesusFreke.dexlib.util.AnnotatedOutput;

public final class Instruction implements Field<Instruction> {
    private DexFile dexFile;
    private byte[] bytes;
    private Opcode opcode;

    private IndexedItem reference;

    public byte[] getBytes() {
        return bytes;
    }

    public Opcode getOpcode() {
        return opcode;
    }

    public IndexedItem getReference() {
        return reference;
    }

    public Instruction(DexFile dexFile) {
        this.dexFile = dexFile; 
    }

    public Instruction(DexFile dexFile, byte[] bytes, IndexedItem item) {
        this.dexFile = dexFile;
        this.bytes = bytes;
        this.reference = item;
        this.opcode = Opcode.getOpcodeByValue(bytes[0]);
        if (!this.opcode.referenceType.checkItem(item)) {
            throw new RuntimeException("item is not the correct type for this opcode (got " + item.getClass().toString() + ", expecting " + opcode.referenceType.toString() + ")");
        }
    }

    public void readFrom(Input in) {
        int startPos = in.getCursor();
        
        byte opByte = in.readByte();

        if (opByte == 0x00) {
            reference = null;
            byte secondByte = in.readByte();

            int count;


            switch (secondByte) {
                case 0x00:
                    /** nop */
                    bytes = new byte[] { 0x00, 0x00 };
                    return;
                case 0x01:
                    /** packed switch */
                    count = in.readShort();
                    in.setCursor(startPos);
                    bytes = in.readBytes((count * 4) + 8);
                    return;
                case 0x02:
                    /** sparse switch */
                    count = in.readShort();
                    in.setCursor(startPos);
                    bytes = in.readBytes((count * 8) + 4);
                    return;
                case 0x03:
                    /** fill array data */
                    int elementWidth = in.readShort();
                    count = in.readInt();
                    in.setCursor(startPos);
                    bytes = in.readBytes(((elementWidth * count + 1)/2 + 4) * 2);
                    return;
                default:
                    throw new RuntimeException("Invalid 2nd byte for opcode 0x00");
            }
        }

        this.opcode = Opcode.getOpcodeByValue(opByte);

        if (opcode.referenceType != ReferenceType.none) {
            in.skipBytes(1);
            int referenceIndex = in.readShort();

            //handle const-string/jumbo as a special case
            if (opByte == 0x1b) {
                int hiWord = in.readShort();
                if (hiWord != 0) {
                    //TODO: test this..
                    referenceIndex += (hiWord<<16);
                }
            }

            switch (opcode.referenceType) {
                case string:
                    reference = dexFile.StringIdsSection.getByIndex(referenceIndex);
                    break;
                case type:
                    reference = dexFile.TypeIdsSection.getByIndex(referenceIndex);
                    break;
                case field:
                    reference = dexFile.FieldIdsSection.getByIndex(referenceIndex);
                    break;
                case method:
                    reference = dexFile.MethodIdsSection.getByIndex(referenceIndex);
                    break;
            }
        } else {
            reference = null;
        }

        in.setCursor(startPos);
        bytes = in.readBytes(opcode.numBytes);
    }

    public void writeTo(AnnotatedOutput out) {
        out.annotate(bytes.length, "instruction");
        if (needsAlign()) {
            //the "special instructions" must be 4 byte aligned
            out.alignTo(4);
            out.write(bytes);
        } else if (reference == null) {
            out.write(bytes);
        } else {
            out.write(bytes,0,2);
            //handle const-string/jumbo as a special case
            if (bytes[0] == 0x1b) {
                out.writeInt(reference.getIndex());
            } else {
                int index = reference.getIndex();
                if (index > 0xFFFF) {
                    throw new RuntimeException("String index doesn't fit.");
                }
                out.writeShort(reference.getIndex());
                out.write(bytes, 4, bytes.length - 4);
            }
        }
    }

    public void copyTo(DexFile dexFile, Instruction copy) {
        copy.bytes = bytes;
        copy.opcode = opcode;

        switch (opcode.referenceType) {
                case string:
                    copy.reference = dexFile.StringIdsSection.intern(dexFile, (StringIdItem)reference);
                    break;
                case type:
                    copy.reference = dexFile.TypeIdsSection.intern(dexFile, (TypeIdItem)reference);
                    break;
                case field:
                    copy.reference = dexFile.FieldIdsSection.intern(dexFile, (FieldIdItem)reference);
                    break;
                case method:
                    copy.reference = dexFile.MethodIdsSection.intern(dexFile, (MethodIdItem)reference);
                    break;
                case none:
                    break;
            }
    }

    public int place(int offset) {
        return offset + getSize(offset);
    }

    public int getSize(int offset) {
        if (this.needsAlign() && (offset % 4) != 0) {
            return bytes.length + 2;
        } else {
            return bytes.length;
        }
    }

    private boolean needsAlign() {
        //true if the opcode is one of the "special format" opcodes
        return bytes[0] == 0 && bytes[1] > 0;
    }
}
