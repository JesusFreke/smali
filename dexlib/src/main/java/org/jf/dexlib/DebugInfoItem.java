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

package org.jf.dexlib;

import org.jf.dexlib.ItemType;
import org.jf.dexlib.debug.DebugInstructionFactory;
import org.jf.dexlib.debug.EndSequence;
import org.jf.dexlib.debug.DebugInstruction;
import org.jf.dexlib.util.Input;
import org.jf.dexlib.util.AnnotatedOutput;

import java.util.ArrayList;
import java.util.List;

public class DebugInfoItem extends OffsettedItem<DebugInfoItem> {
    private final ArrayList<IndexedItemReference<StringIdItem>> parameterNames =
            new ArrayList<IndexedItemReference<StringIdItem>>();

    private ArrayList<DebugInstruction> instructionFields = new ArrayList<DebugInstruction>();

    private final Leb128Field lineStartField;
    private final ListSizeField parameterNamesSizeField;
    private final FieldListField<IndexedItemReference<StringIdItem>> parameterNamesField;
    private final DebugInstructionList debugInstructionListField;

    public DebugInfoItem(final DexFile dexFile, int offset) {
        super(offset);

        fields = new Field[] {
                lineStartField = new Leb128Field("line_start"),
                parameterNamesSizeField = new ListSizeField(parameterNames, new Leb128Field("parameters_size")),
                parameterNamesField = new FieldListField<IndexedItemReference<StringIdItem>>(
                        parameterNames, "parameter_names") {
                    protected IndexedItemReference<StringIdItem> make() {
                        return new IndexedItemReference<StringIdItem>(dexFile.StringIdsSection,
                                new Leb128p1Field(null), "parameter_name");
                    }
                },
                debugInstructionListField = new DebugInstructionList(dexFile)
        };
    }

    public DebugInfoItem(final DexFile dexFile,
                         int lineStart,
                         List<StringIdItem> parameterNames,
                         List<DebugInstruction> debugInstructions) {
        this(dexFile, 0);

        this.lineStartField.cacheValue(lineStart);

        for (StringIdItem parameterName: parameterNames) {
            IndexedItemReference<StringIdItem> parameterReference = parameterNamesField.make();
            parameterReference.setReference(parameterName);
            this.parameterNames.add(parameterReference);
        }

        this.instructionFields.addAll(debugInstructions);
    }

    public ItemType getItemType() {
        return ItemType.TYPE_DEBUG_INFO_ITEM;
    }

    public String getConciseIdentity() {
        return "debug_info_item @0x" + Integer.toHexString(getOffset());
    }

    private class DebugInstructionList implements Field<DebugInstructionList> {
        private final DexFile dexFile;
        private final ArrayList<DebugInstruction> list;

        public DebugInstructionList(DexFile dexFile) {
            this.dexFile = dexFile;
            list = instructionFields;
        }

        public void writeTo(AnnotatedOutput out) {
            for (DebugInstruction debugInstruction: list) {
                debugInstruction.writeTo(out);
            }
        }

        public void readFrom(Input in) {
            DebugInstruction debugInstruction;
            do {
                debugInstruction = DebugInstructionFactory.readDebugInstruction(dexFile, in);
                list.add(debugInstruction);
            } while (!(debugInstruction instanceof EndSequence));
        }

        public int place(int offset) {
            for (Field field: list) {
                offset = field.place(offset);
            }
            return offset;
        }

        public void copyTo(DexFile dexFile, DebugInstructionList copy) {
            copy.list.clear();
            copy.list.ensureCapacity(list.size());
            for (int i = 0; i < list.size(); i++) {
                DebugInstruction debugInstruction = list.get(i);
                DebugInstruction debugInstructionCopy = DebugInstructionFactory.makeDebugInstruction(dexFile, debugInstruction.getOpcode());
                debugInstruction.copyTo(dexFile, debugInstructionCopy);
                copy.list.add(debugInstructionCopy);
            }
        }
    }
}
