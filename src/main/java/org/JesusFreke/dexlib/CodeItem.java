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

package org.JesusFreke.dexlib;

import org.JesusFreke.dexlib.code.Instruction;
import org.JesusFreke.dexlib.code.Opcode;
import org.JesusFreke.dexlib.ItemType;
import org.JesusFreke.dexlib.util.Input;
import org.JesusFreke.dexlib.util.Output;

import java.util.ArrayList;

public class CodeItem extends OffsettedItem<CodeItem> {
    private final Field[] fields;
    private final ArrayList<Instruction> instructionList;
    private final ArrayList<TryItem> tryItems = new ArrayList<TryItem>();
    private final ArrayList<EncodedCatchHandler> catchHandlerList = new ArrayList<EncodedCatchHandler>();

    private final ShortIntegerField registersCount;
    private final ShortIntegerField inArgumentCount;
    private final ShortIntegerField outArgumentCount;
    private final ListSizeField triesCount;
    private final OffsettedItemReference<DebugInfoItem> debugInfo;
    private final IntegerField instructionsSize;
    private final InstructionListField instructionListField;
    private final PaddingField padding;
    private final FieldListField<TryItem> tries;
    private final EncodedCatchHandlerList catchHandlers;

    public CodeItem(final DexFile dexFile, int offset) {
        super(offset);

        instructionList = new ArrayList<Instruction>();

        fields = new Field[] {
                registersCount = new ShortIntegerField(),
                inArgumentCount = new ShortIntegerField(),
                outArgumentCount = new ShortIntegerField(),
                triesCount = new ListSizeField(tryItems, new ShortIntegerField()),
                debugInfo = new OffsettedItemReference<DebugInfoItem>(dexFile.DebugInfoItemsSection, new IntegerField()),
                instructionsSize = new IntegerField(),
                instructionListField = new InstructionListField(dexFile),
                padding = new PaddingField(),
                tries = new FieldListField<TryItem>(tryItems) {
                    protected TryItem make() {
                        return new TryItem();
                    }
                },

                catchHandlers = new EncodedCatchHandlerList(dexFile)
        };
    }



    public CodeItem(final DexFile dexFile, int registersCount, int inArguments, ArrayList<Instruction> instructions) {
        super(-1);

        this.instructionList = new ArrayList<Instruction>(instructions);
        this.instructionListField = new InstructionListField(dexFile);

        fields = new Field[] {
                this.registersCount = new ShortIntegerField(registersCount),
                this.inArgumentCount = new ShortIntegerField(inArguments),
                this.outArgumentCount = new ShortIntegerField(instructionListField.getOutArguments()),
                this.triesCount = new ListSizeField(tryItems, new ShortIntegerField(0)),
                this.debugInfo = new OffsettedItemReference<DebugInfoItem>(dexFile, null, new IntegerField()),
                this.instructionsSize = new IntegerField(instructionListField.getInstructionWordCount()),
                instructionListField,
                this.padding = new PaddingField(),
                this.tries = new FieldListField<TryItem>(tryItems) {
                    protected TryItem make() {
                        return new TryItem();
                    }
                },
                this.catchHandlers = new EncodedCatchHandlerList(dexFile)
        };
    }

    //TODO: take out
    public void writeTo(Output out) {
        super.writeTo(out);
    }

    protected int getAlignment() {
        return 4;
    }

    public ItemType getItemType() {
        return ItemType.TYPE_CODE_ITEM;
    }

    public Field[] getFields() {
        return fields;
    }

    public class TryItem extends CompositeField<TryItem> {
        private final Field[] fields;

        public TryItem() {
            fields = new Field[] {
                    new IntegerField(),
                    new ShortIntegerField(),
                    new ShortIntegerField()
            };
        }


        protected Field[] getFields() {
            return fields;
        }
    }

    class EncodedCatchHandlerList extends CompositeField<EncodedCatchHandlerList> {
        private boolean fieldPresent = false;

        private final DexFile dexFile;

        public EncodedCatchHandlerList(DexFile dexFile) {
            this.dexFile = dexFile;
        }

        private final Field[] fields = new Field[] {
                new ListSizeField(catchHandlerList, new Leb128Field()),
                new FieldListField<EncodedCatchHandler>(catchHandlerList) {
                    protected EncodedCatchHandler make() {
                        return new EncodedCatchHandler(dexFile);
                    }
                }
        };

        public void readFrom(Input in) {
            if (tryItems.size() > 0) {
                fieldPresent = true;
                super.readFrom(in);
            }
        }

        public void writeTo(Output out) {
            if (fieldPresent) {
                super.writeTo(out);
            }
        }

        public int place(int offset) {
            if (tryItems.size() > 0) {
                fieldPresent = true;
                return super.place(offset);
            } else {
                return offset;
            }
        }

        protected Field[] getFields() {
            return fields;
        }

        public void copyTo(DexFile dexFile, EncodedCatchHandlerList copy) {
            super.copyTo(dexFile, copy);
            copy.fieldPresent = fieldPresent;
        }
    }

    public class EncodedCatchHandler extends CompositeField<EncodedCatchHandler> {
        public final Field[] fields;
        private ArrayList<EncodedTypeAddrPair> list = new ArrayList<EncodedTypeAddrPair>();
        private boolean hasCatchAll = false;                     

        public EncodedCatchHandler(final DexFile dexFile) {
            fields = new Field[] {
                    new ListSizeField(list, new SignedLeb128Field() {
                        public void readFrom(Input in) {
                            super.readFrom(in);
                            hasCatchAll = (getCachedValue() <= 0);
                        }

                        public void cacheValue(int value) {
                            super.cacheValue(value * (hasCatchAll?-1:1));
                        }})
                    ,
                    new FieldListField<EncodedTypeAddrPair>(list) {
                        protected EncodedTypeAddrPair make() {
                            return new EncodedTypeAddrPair(dexFile);
                        }
                    },
                    new Leb128Field() {
                        public void readFrom(Input in) {
                            if (hasCatchAll) {
                                super.readFrom(in);
                            }
                        }

                        public void writeTo(Output out) {
                            if (hasCatchAll) {
                                super.writeTo(out);
                            }
                        }

                        public int place(int offset) {
                            if (hasCatchAll) {
                                return super.place(offset);
                            }
                            return offset;
                        }
                    }
            };
        }

        protected Field[] getFields() {
            return fields;
        }

        public void copyTo(DexFile dexFile, EncodedCatchHandler copy) {
            super.copyTo(dexFile, copy);
            copy.hasCatchAll = hasCatchAll;
        }
    }

    public class EncodedTypeAddrPair extends CompositeField<EncodedTypeAddrPair> {
        public final Field[] fields;

        public EncodedTypeAddrPair(DexFile dexFile) {
            fields = new Field[] {
                    new IndexedItemReference<TypeIdItem>(dexFile.TypeIdsSection, new Leb128Field()),
                    new Leb128Field()
            };
        }

        protected Field[] getFields() {
            return fields;
        }
    }

    private class InstructionListField implements Field<InstructionListField> {
        private final DexFile dexFile;

        public InstructionListField(DexFile dexFile) {
            this.dexFile = dexFile;
        }

        public void writeTo(Output out) {
            int startPosition = out.getCursor();
            for (Instruction instruction: instructionList) {
                instruction.writeTo(out);
            }
            if ((out.getCursor() - startPosition) != (instructionsSize.getCachedValue() * 2)) {
                throw new RuntimeException("Did not write the expected amount of bytes");
            }
        }

        public void readFrom(Input in) {
            int numBytes = instructionsSize.getCachedValue() * 2;
            int startPosition = in.getCursor();

            do {
                Instruction instruction = new Instruction(dexFile);
                instruction.readFrom(in);
                instructionList.add(instruction);
            } while (in.getCursor() - startPosition < numBytes);

            if (in.getCursor() - startPosition != numBytes) {
                throw new RuntimeException("Read past the end of the code section");
            }
        }

        public int place(int offset) {
            return offset + (instructionsSize.getCachedValue() * 2);
        }

        public void copyTo(DexFile dexFile, InstructionListField copy) {
            ArrayList<Instruction> copyInstructionList = copy.getInstructionList();
            copyInstructionList.clear();
            for (Instruction instruction: instructionList) {
                Instruction instructionCopy = new Instruction(dexFile);
                instruction.copyTo(dexFile, instructionCopy);
                copyInstructionList.add(instructionCopy);
            }
        }

        private ArrayList<Instruction> getInstructionList() {
            return instructionList;
        }

        //return the word size of the instruction list
        public int getInstructionWordCount() {
            int bytes = 0;
            //TODO: what about option padding before the special opcodes?
            for (Instruction instruction: instructionList) {
                bytes += instruction.getBytes().length;
            }
            return bytes/2;
        }

        //return the highest parameter word count of any method invokation
        public int getOutArguments() {
            int maxParamWordCount = 0;
            for (Instruction instruction: instructionList) {
                IndexedItem item = instruction.getReference();
                if (item instanceof MethodIdItem) {
                    MethodIdItem methodIdItem = (MethodIdItem)item;
                    Opcode opcode = instruction.getOpcode();

                    boolean isStatic = false;
                    if (opcode == Opcode.INVOKE_STATIC || opcode == Opcode.INVOKE_STATIC_RANGE) {
                        isStatic = true;
                    }
                    int paramWordCount = methodIdItem.getParameterWordCount(isStatic);

                    if (maxParamWordCount < paramWordCount) {
                        maxParamWordCount = paramWordCount;
                    }
                }
            }
            return maxParamWordCount;
        }
    }

    private class PaddingField implements Field {

        public PaddingField() {
        }

        private boolean needsAlign() {
            return (triesCount.getCachedValue() > 0) && (instructionsSize.getCachedValue() % 2 == 1);
        }

        public void writeTo(Output out) {
            if (needsAlign()) {
                out.writeShort(0);
            }
        }

        public void readFrom(Input in) {
            if (needsAlign()) {
                in.skipBytes(2);
            }
        }

        public int place(int offset) {
            if (needsAlign()) {
                return offset + 2;
            } else {
                return offset;
            }
        }

        public int hashCode() {
            return 0;
        }

        public boolean equals(Object o) {
            return getClass() == o.getClass();
        }

        public void copyTo(DexFile dexFile, Field field) {
        }
    }
}
