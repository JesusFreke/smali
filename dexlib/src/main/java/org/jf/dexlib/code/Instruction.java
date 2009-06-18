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

package org.jf.dexlib.code;

import org.jf.dexlib.*;
import org.jf.dexlib.code.Format.Format;

public abstract class Instruction {
    private DexFile dexFile;
    private Opcode opcode;
    private IndexedItem referencedItem;
    protected byte[] encodedInstruction;

    public int getSize() {
        return encodedInstruction.length;
    }

    public Opcode getOpcode() {
        return opcode;
    }

    public IndexedItem getReferencedItem() {
        return referencedItem;
    }

    protected void setReferencedItem(IndexedItem referencedItem) {
        checkReferenceType(referencedItem, this.opcode);
        this.referencedItem = referencedItem;
    }

    protected Instruction(DexFile dexFile, Opcode opcode, IndexedItem referencedItem) {
        this.dexFile = dexFile;
        this.opcode = opcode;
        this.referencedItem = referencedItem;

        checkFormat(opcode.format);
        checkReferenceType(referencedItem, this.opcode);
    }

    protected void checkFormat(Format format) {
        if (format != getFormat()) {
            throw new RuntimeException(opcode.name + " does not use " + getFormat().toString());
        }
    }

    protected Instruction(DexFile dexFile, Opcode opcode, byte[] rest) {
        this.dexFile = dexFile;
        this.opcode = opcode;

        if ((rest.length + 1) != opcode.format.size) {
            throw new RuntimeException("Invalid instruction size. This opcode is " +
                    Integer.toString(rest.length + 1) + " bytes, but the opcode should be " +
                    Integer.toString(opcode.format.size) + " bytes.");
        }

        this.encodedInstruction = new byte[rest.length + 1];
        encodedInstruction[0] = opcode.value;
        System.arraycopy(rest, 0, encodedInstruction, 1, rest.length);

        if (opcode.referenceType != ReferenceType.none) {
            int itemIndex = (encodedInstruction[3] << 8) | encodedInstruction[2];
            getReferencedItem(dexFile, opcode, itemIndex);
        }
    }

    protected Instruction() {
        //this should only be used to make a blank clone within cloneTo()
    }

    private void checkReferenceType(IndexedItem referencedItem, Opcode opcode) {
        switch (opcode.referenceType) {
            case field:
                if (!(referencedItem instanceof FieldIdItem)) {
                    throw new RuntimeException(referencedItem.getClass().getSimpleName() +
                            " is the wrong item type for opcode " + opcode.name + ". Expecting FieldIdItem.");
                }
                return;
            case method:
                if (!(referencedItem instanceof MethodIdItem)) {
                    throw new RuntimeException(referencedItem.getClass().getSimpleName() +
                            " is the wrong item type for opcode " + opcode.name + ". Expecting MethodIdItem.");
                }
                return;
            case type:
                if (!(referencedItem instanceof TypeIdItem)) {
                    throw new RuntimeException(referencedItem.getClass().getSimpleName() +
                            " is the wrong item type for opcode " + opcode.name + ". Expecting TypeIdItem.");
                }
                return;
            case string:
                if (!(referencedItem instanceof StringIdItem)) {
                    throw new RuntimeException(referencedItem.getClass().getSimpleName() +
                            " is the wrong item type for opcode " + opcode.name + ". Expecting StringIdItem.");
                }
                return;
            default:
                if (referencedItem != null) {
                    throw new RuntimeException(referencedItem.getClass().getSimpleName() +
                            " is invalid for opcode " + opcode.name + ". This opcode does not reference an item");
                }
                return;
        }
    }

    private void getReferencedItem(DexFile dexFile, Opcode opcode, int itemIndex) {
        switch (opcode.referenceType) {
            case field:
                referencedItem = dexFile.FieldIdsSection.getByIndex(itemIndex);
                return;
            case method:
                referencedItem = dexFile.MethodIdsSection.getByIndex(itemIndex);
                return;
            case type:
                referencedItem = dexFile.TypeIdsSection.getByIndex(itemIndex);
                return;
            case string:
                referencedItem = dexFile.StringIdsSection.getByIndex(itemIndex);
                return;
        }
        return;
    }

    public abstract Format getFormat();

    public static interface InstructionFactory {
        public Instruction makeInstruction(DexFile dexFile, Opcode opcode, byte[] rest);
    }

    public Instruction cloneTo(DexFile dexFile) {
        Instruction clone = makeClone();                     
        clone.encodedInstruction = encodedInstruction.clone();
        clone.dexFile = dexFile;
        clone.opcode = opcode;
        if (referencedItem != null) {
        switch (opcode.referenceType) {
                case string:
                    clone.referencedItem = dexFile.StringIdsSection.intern(dexFile, (StringIdItem)referencedItem);
                    break;
                case type:
                    clone.referencedItem = dexFile.TypeIdsSection.intern(dexFile, (TypeIdItem)referencedItem);
                    break;
                case field:
                    clone.referencedItem = dexFile.FieldIdsSection.intern(dexFile, (FieldIdItem)referencedItem);
                    break;
                case method:
                    clone.referencedItem = dexFile.MethodIdsSection.intern(dexFile, (MethodIdItem)referencedItem);
                    break;
                case none:
                    break;
            }
        }

        return clone;
    }

    protected abstract Instruction makeClone();

//    public void readFrom(Input in) {
//        int startPos = in.getCursor();
//
//        byte opByte = in.readByte();
//
//        if (opByte == 0x00) {
//            reference = null;
//            byte secondByte = in.readByte();
//
//            int count;
//
//
//            switch (secondByte) {
//                case 0x00:
//                    /** nop */
//                    bytes = new byte[] { 0x00, 0x00 };
//                    return;
//                case 0x01:
//                    /** packed switch */
//                    count = in.readShort();
//                    in.setCursor(startPos);
//                    bytes = in.readBytes((count * 4) + 8);
//                    return;
//                case 0x02:
//                    /** sparse switch */
//                    count = in.readShort();
//                    in.setCursor(startPos);
//                    bytes = in.readBytes((count * 8) + 4);
//                    return;
//                case 0x03:
//                    /** fill array data */
//                    int elementWidth = in.readShort();
//                    count = in.readInt();
//                    in.setCursor(startPos);
//                    bytes = in.readBytes(((elementWidth * count + 1)/2 + 4) * 2);
//                    return;
//                default:
//                    throw new RuntimeException("Invalid 2nd byte for opcode 0x00");
//            }
//        }
//
//        this.opcode = Opcode.getOpcodeByValue(opByte);
//
//        if (opcode.referenceType != ReferenceType.none) {
//            in.skipBytes(1);
//            int referenceIndex = in.readShort();
//
//            //handle const-string/jumbo as a special case
//            if (opByte == 0x1b) {
//                int hiWord = in.readShort();
//                if (hiWord != 0) {
//                    referenceIndex += (hiWord<<16);
//                }
//            }
//
//            switch (opcode.referenceType) {
//                case string:
//                    reference = dexFile.StringIdsSection.getByIndex(referenceIndex);
//                    break;
//                case type:
//                    reference = dexFile.TypeIdsSection.getByIndex(referenceIndex);
//                    break;
//                case field:
//                    reference = dexFile.FieldIdsSection.getByIndex(referenceIndex);
//                    break;
//                case method:
//                    reference = dexFile.MethodIdsSection.getByIndex(referenceIndex);
//                    break;
//            }
//        } else {
//            reference = null;
//        }
//
//        in.setCursor(startPos);
//        bytes = in.readBytes(opcode.numBytes);
//    }
//
//    public void writeTo(AnnotatedOutput out) {
//        out.annotate(bytes.length, "instruction");
//        if (needsAlign()) {
//            //the "special instructions" must be 4 byte aligned
//            out.alignTo(4);
//            out.write(bytes);
//        } else if (reference == null) {
//            out.write(bytes);
//        } else {
//            out.write(bytes,0,2);
//            //handle const-string/jumbo as a special case
//            if (bytes[0] == 0x1b) {
//                out.writeInt(reference.getIndex());
//            } else {
//                int index = reference.getIndex();
//                if (index > 0xFFFF) {
//                    throw new RuntimeException("String index doesn't fit.");
//                }
//                out.writeShort(reference.getIndex());
//                out.write(bytes, 4, bytes.length - 4);
//            }
//        }
//    }
//
//    public void copyTo(DexFile dexFile, Instruction copy) {
//        copy.bytes = bytes;
//        copy.opcode = opcode;
//
//        switch (opcode.referenceType) {
//                case string:
//                    copy.reference = dexFile.StringIdsSection.intern(dexFile, (StringIdItem)reference);
//                    break;
//                case type:
//                    copy.reference = dexFile.TypeIdsSection.intern(dexFile, (TypeIdItem)reference);
//                    break;
//                case field:
//                    copy.reference = dexFile.FieldIdsSection.intern(dexFile, (FieldIdItem)reference);
//                    break;
//                case method:
//                    copy.reference = dexFile.MethodIdsSection.intern(dexFile, (MethodIdItem)reference);
//                    break;
//                case none:
//                    break;
//            }
//    }
//
//    public int place(int offset) {
//        return offset + getSize(offset);
//    }
//
//    public int getSize(int offset) {
//        if (this.needsAlign() && (offset % 4) != 0) {
//            return bytes.length + 2;
//        } else {
//            return bytes.length;
//        }
//    }
//
//    private boolean needsAlign() {
//        //true if the opcode is one of the "special format" opcodes
//        return bytes[0] == 0 && bytes[1] > 0;
//    }
}
