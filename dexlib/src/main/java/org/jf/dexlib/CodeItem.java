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

import org.jf.dexlib.Code.InstructionField;
import org.jf.dexlib.Code.Opcode;
import org.jf.dexlib.Util.AnnotatedOutput;
import org.jf.dexlib.Util.Input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Collections;

public class CodeItem extends OffsettedItem<CodeItem> {
    private final ArrayList<InstructionField> instructionList;
    private final ArrayList<TryItem> tryItems = new ArrayList<TryItem>();
    private final ArrayList<EncodedCatchHandler> catchHandlerList = new ArrayList<EncodedCatchHandler>();

    private final ShortIntegerField registersCountField;
    private final ShortIntegerField inArgumentCountField;
    private final ShortIntegerField outArgumentCountField;
    private final ListSizeField triesCountField;
    private final OffsettedItemReference<DebugInfoItem> debugInfoReferenceField;
    private final IntegerField instructionsSizeField;
    private final InstructionListField instructionListField;
    private final PaddingField paddingField;
    private final FieldListField<TryItem> triesListField;
    private final EncodedCatchHandlerList catchHandlersListField;

    private MethodIdItem parent = null;

    public CodeItem(final DexFile dexFile, int offset) {
        super(dexFile, offset);

        instructionList = new ArrayList<InstructionField>();

        fields = new Field[] {
                registersCountField = new ShortIntegerField("registers_size"),
                inArgumentCountField = new ShortIntegerField("ins_size"),
                outArgumentCountField = new ShortIntegerField("outs_size"),
                triesCountField = new ListSizeField(tryItems, new ShortIntegerField("tries_size")),
                debugInfoReferenceField = new OffsettedItemReference<DebugInfoItem>(dexFile.DebugInfoItemsSection,
                        new IntegerField(null), "debug_off"),
                instructionsSizeField = new IntegerField("insns_size"),
                instructionListField = new InstructionListField(dexFile),
                paddingField = new PaddingField(),
                triesListField = new FieldListField<TryItem>(tryItems, "try_item") {
                    protected TryItem make() {
                        return new TryItem(catchHandlersListField);
                    }
                },

                catchHandlersListField = new EncodedCatchHandlerList(dexFile)
        };
    }

    public CodeItem(final DexFile dexFile,
                    int registersCount,
                    int inArguments,
                    List<InstructionField> instructions,
                    DebugInfoItem debugInfo,
                    List<TryItem> tries,
                    List<EncodedCatchHandler> handlers) {
        this(dexFile, 0);

        instructionList.addAll(instructions);
        instructionsSizeField.cacheValue(instructionListField.getInstructionWordCount());

        if (tries != null) {
            tryItems.addAll(tries);
            if (handlers == null) {
                throw new RuntimeException("The handlers parameter cannot be null if tries parameter is not null");
            }
            catchHandlerList.addAll(handlers);
        } else if (handlers != null) {
            throw new RuntimeException("The handlers parameter must be null if the tries parameter is null");
        }

        registersCountField.cacheValue(registersCount);
        inArgumentCountField.cacheValue(inArguments);
        outArgumentCountField.cacheValue(instructionListField.getOutArguments());
        debugInfoReferenceField.setReference(debugInfo);

        if (debugInfo != null) {
            debugInfo.setParent(this);
        }
    }

    protected int getAlignment() {
        return 4;
    }

    public ItemType getItemType() {
        return ItemType.TYPE_CODE_ITEM;
    }

    public int getRegisterCount() {
        return registersCountField.getCachedValue();
    }

    public List<InstructionField> getInstructions() {
        return Collections.unmodifiableList(instructionList);
    }

    public List<TryItem> getTries() {
        return Collections.unmodifiableList(tryItems);
    }

    public DebugInfoItem getDebugInfo() {
        return debugInfoReferenceField.getReference();
    }

    protected void setParent(MethodIdItem methodIdItem) {
        this.parent = methodIdItem;
    }

    public void copyTo(DexFile dexFile, CodeItem copy)
    {
        for (int i = 0; i < fields.length-2; i++) {
            fields[i].copyTo(dexFile, copy.fields[i]);
        }
        //we need to do this in reverse order, so when the tries are copied,
        //the catchHandler copies will already exist
        catchHandlersListField.copyTo(dexFile, copy.catchHandlersListField);
        triesListField.copyTo(dexFile, copy.triesListField);

        DebugInfoItem copyDebugInfo = copy.getDebugInfo();
        if (copyDebugInfo != null) {
            copyDebugInfo.setParent(copy);
        }
    }

    public void readFrom(Input in, int index) {
        super.readFrom(in, index);

        DebugInfoItem debugInfoItem = debugInfoReferenceField.getReference();
        if (debugInfoItem != null) {
            debugInfoItem.setParent(this);
        }
    }

    public String getConciseIdentity() {
        //TODO: should mention the method name here
        return "code_item @0x" + Integer.toHexString(getOffset());
    }

    public int compareTo(CodeItem other) {
        if (parent == null) {
            if (other.parent == null) {
                return 0;
            }
            return -1;
        }
        if (other.parent == null) {
            return 1;
        }
        return parent.compareTo(other.parent);
    }

    public static class TryItem extends CompositeField<TryItem> {
        private final IntegerField startAddr;
        private final ShortIntegerField insnCount;
        private final EncodedCatchHandlerReference encodedCatchHandlerReference;

        public TryItem(EncodedCatchHandlerList encodedCatchHandlerList) {
            super("try_item");
            fields = new Field[] {
                    startAddr = new IntegerField("start_addr"),
                    insnCount = new ShortIntegerField("insn_count"),
                    encodedCatchHandlerReference = new EncodedCatchHandlerReference(encodedCatchHandlerList)
            };
        }

        public TryItem(int startAddr, int insnCount, EncodedCatchHandler encodedCatchHandler) {
            super("try_item");
            fields = new Field[] {
                    this.startAddr = new IntegerField(startAddr, "start_addr"),
                    this.insnCount = new ShortIntegerField(insnCount, "insn_count"),
                    this.encodedCatchHandlerReference = new EncodedCatchHandlerReference(encodedCatchHandler)
            };
        }

        public int getStartAddress() {
            return startAddr.getCachedValue();
        }

        public int getEndAddress() {
            return startAddr.getCachedValue() + insnCount.getCachedValue();
        }

        public EncodedCatchHandler getHandler() {
            return encodedCatchHandlerReference.getReference();
        }
    }

    public static class EncodedCatchHandlerReference extends ShortIntegerField {
        private final EncodedCatchHandlerList encodedCatchHandlerList;
        private EncodedCatchHandler encodedCatchHandler;

        public EncodedCatchHandlerReference(EncodedCatchHandlerList encodedCatchHandlerList) {
            super("encoded_catch_handler");
            this.encodedCatchHandlerList = encodedCatchHandlerList;
        }

        public EncodedCatchHandlerReference(EncodedCatchHandler encodedCatchHandler) {
            super("encoded_catch_handler");
            this.encodedCatchHandlerList = null;
            this.encodedCatchHandler = encodedCatchHandler;
        }

        public EncodedCatchHandlerList getEncodedCatchHandlerList() {
            return encodedCatchHandlerList;
        }

        private void setReference(EncodedCatchHandler encodedCatchHandler) {
            this.encodedCatchHandler = encodedCatchHandler;
        }

        public EncodedCatchHandler getReference() {
            return encodedCatchHandler;
        }

        public void copyTo(DexFile dexFile, CachedIntegerValueField _copy) {
            EncodedCatchHandlerReference copy = (EncodedCatchHandlerReference)_copy;
            EncodedCatchHandler copiedItem = copy.getEncodedCatchHandlerList().intern(encodedCatchHandler);
            copy.setReference(copiedItem);
        }

        public void writeTo(AnnotatedOutput out) {
            cacheValue(encodedCatchHandler.getOffsetInList());

            super.writeTo(out);
        }

        public void readFrom(Input in) {
            super.readFrom(in);

            encodedCatchHandler = encodedCatchHandlerList.getByOffset(getCachedValue());
        }

        public int place(int offset) {
            cacheValue(encodedCatchHandler.getOffsetInList());
            return super.place(offset);
        }
    }

    public class EncodedCatchHandlerList extends CompositeField<EncodedCatchHandlerList> {
        private boolean fieldPresent = false;
        //this field is only valid when reading a dex file in
        protected HashMap<Integer, EncodedCatchHandler> itemsByOffset =
                new HashMap<Integer, EncodedCatchHandler>();

        protected HashMap<EncodedCatchHandler, EncodedCatchHandler> uniqueItems = null;

        private final DexFile dexFile;

        public EncodedCatchHandler getByOffset(int offset) {
            EncodedCatchHandler encodedCatchHandler = itemsByOffset.get(offset);
            if (encodedCatchHandler == null) {
                encodedCatchHandler = new EncodedCatchHandler(dexFile, offset);
                itemsByOffset.put(offset, encodedCatchHandler);
            }
            return encodedCatchHandler;
        }

        public EncodedCatchHandler intern(EncodedCatchHandler item) {
            if (uniqueItems == null) {
                buildInternedItemMap();
            }
            EncodedCatchHandler encodedCatchHandler = uniqueItems.get(item);
            if (encodedCatchHandler == null) {
                encodedCatchHandler = new EncodedCatchHandler(dexFile, -1);
                catchHandlerList.add(encodedCatchHandler);
                item.copyTo(dexFile, encodedCatchHandler);
                uniqueItems.put(encodedCatchHandler, encodedCatchHandler);
            }
            return encodedCatchHandler;
        }

        private void buildInternedItemMap() {
            uniqueItems = new HashMap<EncodedCatchHandler, EncodedCatchHandler>();
            for (EncodedCatchHandler item: catchHandlerList) {
                uniqueItems.put(item, item);
            }
        }

        public EncodedCatchHandlerList(final DexFile dexFile) {
            super("encoded_catch_handler_list");
            this.dexFile = dexFile;

            fields = new Field[] {
                sizeField = new ListSizeField(catchHandlerList, new Leb128Field("size")),
                listField = new FieldListField<EncodedCatchHandler>(catchHandlerList, "encoded_catch_handler") {
                    protected EncodedCatchHandler make() {
                        return new EncodedCatchHandler(dexFile, 0);
                    }

                    public void readFrom(Input in) {
                        int currentOffset = sizeField.place(0);

                        for (int i = 0; i < list.size(); i++) {
                                EncodedCatchHandler field = list.get(i);

                                if (field == null) {
                                    field = itemsByOffset.get(currentOffset);
                                    if (field == null) {
                                        field = new EncodedCatchHandler(dexFile, currentOffset);
                                    }
                                    list.set(i, field);
                                }
                                int savedOffset = in.getCursor();
                                field.readFrom(in);
                                currentOffset += in.getCursor() - savedOffset;
                            }
                        }
                   }
             };
        }

        private final ListSizeField sizeField;
        private final FieldListField<EncodedCatchHandler> listField;

        public void readFrom(Input in) {
            if (tryItems.size() > 0) {
                fieldPresent = true;
                super.readFrom(in);
            }
        }

        public void writeTo(AnnotatedOutput out) {
            if (fieldPresent) {
                super.writeTo(out);
            }
        }

        public int place(int offset) {
            for (EncodedCatchHandler encodedCatchHandler: listField.list) {
                encodedCatchHandler.setBaseOffset(offset);
            }
            if (tryItems.size() > 0) {
                fieldPresent = true;
                return super.place(offset);
            } else {
                return offset;
            }
        }

        public void copyTo(DexFile dexFile, EncodedCatchHandlerList copy) {
            super.copyTo(dexFile, copy);
            copy.fieldPresent = fieldPresent;
            copy.itemsByOffset.clear();
            int offset = 0;
            for (EncodedCatchHandler encodedCatchHandler: copy.listField.list) {
                copy.itemsByOffset.put(encodedCatchHandler.offset, encodedCatchHandler);
            }
        }
    }

    public static class EncodedCatchHandler extends CompositeField<EncodedCatchHandler> {
        private ArrayList<EncodedTypeAddrPair> list;
        boolean hasCatchAll = false;
        private int baseOffset = 0;

        private final ListSizeField size;
        private final FieldListField<EncodedTypeAddrPair> handlers;
        private final Leb128Field catchAllAddress;

        private int offset;

        public EncodedCatchHandler(final DexFile dexFile, int offset) {
            super("encoded_catch_handler");
            this.offset = offset;
            
            list = new ArrayList<EncodedTypeAddrPair>();
            fields = new Field[] {
                    size = new ListSizeField(list, new SignedLeb128Field("size") {
                        public void readFrom(Input in) {
                            super.readFrom(in);
                            hasCatchAll = (getCachedValue() <= 0);
                        }

                        public void cacheValue(int value) {
                            super.cacheValue(value * (hasCatchAll?-1:1));
                        }})
                    ,
                    handlers = new FieldListField<EncodedTypeAddrPair>(list, "encoded_type_addr_pair") {
                        protected EncodedTypeAddrPair make() {
                            return new EncodedTypeAddrPair(dexFile);
                        }
                    },
                    catchAllAddress = new Leb128Field("catch_all_addr") {
                        public void readFrom(Input in) {
                            if (hasCatchAll) {
                                super.readFrom(in);
                            }
                        }

                        public void writeTo(AnnotatedOutput out) {
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

        public EncodedCatchHandler(final DexFile dexFile, List<EncodedTypeAddrPair> handlers, int catchAllHandler) {
            this(dexFile, 0);

            list.addAll(handlers);
            if (catchAllHandler >= 0) {
                hasCatchAll = true;
                catchAllAddress.cacheValue(catchAllHandler);
            }
        }

        public int getOffsetInList() {
            return offset-baseOffset;
        }

        public void setBaseOffset(int baseOffset) {
            this.baseOffset = baseOffset; 
        }

        public void copyTo(DexFile dexFile, EncodedCatchHandler copy) {
            super.copyTo(dexFile, copy);
            copy.hasCatchAll = hasCatchAll;
            copy.offset = offset;
        }

        public int place(int offset) {
            this.offset = offset;
            return super.place(offset);
        }

        public int getCatchAllAddress() {
            if (hasCatchAll) {
                return catchAllAddress.getCachedValue();
            } else {
                return -1;
            }
        }

        public List<EncodedTypeAddrPair> getHandlers() {
            return Collections.unmodifiableList(list);
        }
    }

    public static class EncodedTypeAddrPair extends CompositeField<EncodedTypeAddrPair> {
        public final IndexedItemReference<TypeIdItem> typeReferenceField;
        public final Leb128Field handlerAddressField;

        public EncodedTypeAddrPair(DexFile dexFile) {
            super("encoded_type_addr_pair");
            fields = new Field[] {
                    typeReferenceField = new IndexedItemReference<TypeIdItem>(dexFile.TypeIdsSection,
                            new Leb128Field(null), "type_idx"),
                    handlerAddressField = new Leb128Field("addr")
            };
        }

        public EncodedTypeAddrPair(DexFile dexFile, TypeIdItem type, int handlerOffset) {
            this(dexFile);
            typeReferenceField.setReference(type);
            handlerAddressField.cacheValue(handlerOffset);
        }

        public TypeIdItem getTypeReferenceField() {
            return typeReferenceField.getReference();
        }

        public int getHandlerAddress() {
            return handlerAddressField.getCachedValue();
        }
    }

    private class InstructionListField implements Field<InstructionListField> {
        private final DexFile dexFile;

        public InstructionListField(DexFile dexFile) {
            this.dexFile = dexFile;
        }

        public void writeTo(AnnotatedOutput out) {
            int startPosition = out.getCursor();
            for (InstructionField instruction: instructionList) {
                instruction.writeTo(out);
            }
            if ((out.getCursor() - startPosition) != (instructionsSizeField.getCachedValue() * 2)) {
                throw new RuntimeException("Did not write the expected amount of bytes");
            }
        }

        public void readFrom(Input in) {
            int numBytes = instructionsSizeField.getCachedValue() * 2;
            int startPosition = in.getCursor();

            do {
                InstructionField instruction = new InstructionField(dexFile);
                instruction.readFrom(in);
                instructionList.add(instruction);
            } while (in.getCursor() - startPosition < numBytes);

            if (in.getCursor() - startPosition != numBytes) {
                throw new RuntimeException("Read past the end of the code section");
            }
        }

        public int place(int offset) {
            return offset + (instructionsSizeField.getCachedValue() * 2);
        }

        public void copyTo(DexFile dexFile, InstructionListField copy) {
            ArrayList<InstructionField> copyInstructionList = copy.getInstructionList();
            copyInstructionList.clear();
            for (InstructionField instruction: instructionList) {
                InstructionField instructionCopy = new InstructionField(dexFile);
                instruction.copyTo(dexFile, instructionCopy);
                copyInstructionList.add(instructionCopy);
            }
        }

        private ArrayList<InstructionField> getInstructionList() {
            return instructionList;
        }

        //return the word size of the instruction list
        public int getInstructionWordCount() {
            int bytes = 0;
            for (InstructionField instruction: instructionList) {
                bytes += instruction.getSize(bytes);
            }
            return bytes/2;
        }

        //return the highest parameter word count of any method invokation
        public int getOutArguments() {
            int maxParamWordCount = 0;
            for (InstructionField instruction: instructionList) {
                IndexedItem item = instruction.getInstruction().getReferencedItem();
                if (item instanceof MethodIdItem) {
                    MethodIdItem methodIdItem = (MethodIdItem)item;
                    Opcode opcode = instruction.getInstruction().getOpcode();

                    boolean isStatic = false;
                    if (opcode == Opcode.INVOKE_STATIC || opcode == Opcode.INVOKE_STATIC_RANGE) {
                        isStatic = true;
                    }
                    int paramWordCount = methodIdItem.getParameterRegisterCount(isStatic);

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
            return (triesCountField.getCachedValue() > 0) && (instructionsSizeField.getCachedValue() % 2 == 1);
        }

        public void writeTo(AnnotatedOutput out) {
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
