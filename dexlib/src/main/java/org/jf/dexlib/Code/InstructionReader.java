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

package org.jf.dexlib.Code;

import org.jf.dexlib.Item;
import org.jf.dexlib.DexFile;
import org.jf.dexlib.Util.NumberUtils;

import java.util.LinkedList;

public class InstructionReader {
    /**
     * Decodes the instructions in the given byte array, and builds a list of the items referenced by instructions,
     * using the given <code>DexFile</code> to resolve the item references
     * @param insns a byte array containing encoded instructions that have just been read in
     * @param dexFile the <code>DexFile</code> used to resolve item references
     * @return an array of the referenced <code>Item</code> objects, in the same order as their occurance in the
     * byte array 
     */
    public static Item[] getReferencedItems(final byte[] insns, final DexFile dexFile) {
        final LinkedList<Item> referencedItems = new LinkedList<Item>();

        InstructionIterator.IterateInstructions(insns, new InstructionIterator.ProcessRawInstructionDelegate() {
            public void ProcessNormalInstruction(Opcode opcode, int index) {
            }

            public void ProcessReferenceInstruction(Opcode opcode, int index) {
                if (opcode == Opcode.CONST_STRING_JUMBO) {
                    int itemIndex = NumberUtils.decodeInt(insns, index+2);
                    if (itemIndex < 0) {
                        throw new RuntimeException("The string index for this const-string/jumbo instruction is too large");
                    }
                    referencedItems.add(dexFile.StringIdsSection.getItemByIndex(itemIndex));
                } else {
                    int itemIndex = NumberUtils.decodeUnsignedShort(insns, index+2);
                    if (itemIndex > 0xFFFF) {
                        throw new RuntimeException("The item index does not fit in 2 bytes");
                    }

                    switch (opcode.referenceType) {
                        case string:
                            referencedItems.add(dexFile.StringIdsSection.getItemByIndex(itemIndex));
                            break;
                        case type:
                            referencedItems.add(dexFile.TypeIdsSection.getItemByIndex(itemIndex));
                            break;
                        case field:
                            referencedItems.add(dexFile.FieldIdsSection.getItemByIndex(itemIndex));
                            break;
                        case method:
                            referencedItems.add(dexFile.MethodIdsSection.getItemByIndex(itemIndex));
                            break;
                    }
                }
            }

            public void ProcessPackedSwitchInstruction(int index, int targetCount, int instructionLength) {
            }

            public void ProcessSparseSwitchInstruction(int index, int targetCount, int instructionLength) {
            }

            public void ProcessFillArrayDataInstruction(int index, int elementWidth, int elementCount, int instructionLength) {
            }
        });

        Item[] items = new Item[referencedItems.size()];
        return referencedItems.toArray(items);
    }
}
