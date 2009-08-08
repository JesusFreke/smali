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

package org.jf.dexlib;

import org.jf.dexlib.Code.InstructionReader;
import org.jf.dexlib.Code.InstructionIterator;
import org.jf.dexlib.Code.Opcode;
import org.jf.dexlib.Code.InstructionWriter;
import org.jf.dexlib.Util.AnnotatedOutput;
import org.jf.dexlib.Util.Input;
import org.jf.dexlib.Util.SparseArray;
import org.jf.dexlib.Util.Leb128Utils;

public class CodeItem extends Item<CodeItem> {
    private int registerCount;
    private int inWords;
    private int outWords;
    private DebugInfoItem debugInfo;
    private byte[] encodedInstructions;
    private Item[] referencedItems;
    private TryItem[] tries;
    private EncodedCatchHandler[] encodedCatchHandlers;

    private ClassDataItem.EncodedMethod parent;
    
    /**
     * Creates a new uninitialized <code>CodeItem</code>
     * @param dexFile The <code>DexFile</code> that this item belongs to
     */
    public CodeItem(DexFile dexFile) {
        super(dexFile);
    }

    /**
     * Creates a new <code>CodeItem</code> with the given values.
     * @param dexFile The <code>DexFile</code> that this item belongs to
     * @param registerCount the number of registers that the method containing this code uses
     * @param inWords the number of 2-byte words that the parameters to the method containing this code take
     * @param outWords the maximum number of 2-byte words for the arguments of any method call in this code
     * @param debugInfo the debug information for this code/method
     * @param encodedInstructions the instructions, encoded as a byte array
     * @param referencedItems an array of the items referenced by instructions, in order of occurance in the code
     * @param tries an array of the tries defined for this code/method
     * @param encodedCatchHandlers an array of the exception handlers defined for this code/method
     */
    private CodeItem(DexFile dexFile,
                    int registerCount,
                    int inWords,
                    int outWords,
                    DebugInfoItem debugInfo,
                    byte[] encodedInstructions,
                    Item[] referencedItems,
                    TryItem[] tries,
                    EncodedCatchHandler[] encodedCatchHandlers) {
        super(dexFile);

        this.registerCount = registerCount;
        this.inWords = inWords;
        this.outWords = outWords;
        this.debugInfo = debugInfo;
        if (debugInfo != null) {
            debugInfo.setParent(this);
        }
        this.encodedInstructions = encodedInstructions;
        this.referencedItems = referencedItems;
        this.tries = tries;
        this.encodedCatchHandlers = encodedCatchHandlers;
    }

    /**
     * Returns a new <code>CodeItem</code> with the given values.
     * @param dexFile The <code>DexFile</code> that this item belongs to
     * @param registerCount the number of registers that the method containing this code uses
     * @param inWords the number of 2-byte words that the parameters to the method containing this code take
     * @param outWords the maximum number of 2-byte words for the arguments of any method call in this code
     * @param debugInfo the debug information for this code/method
     * @param encodedInstructions the instructions, encoded as a byte array
     * @param referencedItems an array of the items referenced by instructions, in order of occurance in the code
     * @param tries an array of the tries defined for this code/method
     * @param encodedCatchHandlers an array of the exception handlers defined for this code/method
     * @return a new <code>CodeItem</code> with the given values.
     */
    public static CodeItem getInternedCodeItem(DexFile dexFile,
                    int registerCount,
                    int inWords,
                    int outWords,
                    DebugInfoItem debugInfo,
                    byte[] encodedInstructions,
                    Item[] referencedItems,
                    TryItem[] tries,
                    EncodedCatchHandler[] encodedCatchHandlers) {
        CodeItem codeItem = new CodeItem(dexFile, registerCount, inWords, outWords, debugInfo, encodedInstructions,
                referencedItems, tries, encodedCatchHandlers);
        return dexFile.CodeItemsSection.intern(codeItem);
    }

    /** {@inheritDoc} */
    protected void readItem(Input in, ReadContext readContext) {
        this.registerCount = in.readShort();
        this.inWords = in.readShort();
        this.outWords = in.readShort();
        int triesCount = in.readShort();
        this.debugInfo = (DebugInfoItem)readContext.getOffsettedItemByOffset(ItemType.TYPE_DEBUG_INFO_ITEM,
                in.readInt());
        if (this.debugInfo != null) {
            this.debugInfo.setParent(this);
        }                                          
        int instructionCount = in.readInt();
        this.encodedInstructions = in.readBytes(instructionCount * 2);
        this.referencedItems = InstructionReader.getReferencedItems(encodedInstructions, dexFile);
        if (triesCount > 0) {
            in.alignTo(4);

            //we need to read in the catch handlers first, so save the offset to the try items for future reference
            int triesOffset = in.getCursor();
            in.setCursor(triesOffset + 8 * triesCount);

            //read in the encoded catch handlers
            int encodedHandlerStart = in.getCursor();
            int handlerCount = in.readUnsignedLeb128();
            SparseArray<EncodedCatchHandler> handlerMap = new SparseArray<EncodedCatchHandler>(handlerCount);
            encodedCatchHandlers = new EncodedCatchHandler[handlerCount];
            for (int i=0; i<handlerCount; i++) {
                int position = in.getCursor() - encodedHandlerStart;
                encodedCatchHandlers[i] = new EncodedCatchHandler(dexFile, in);
                handlerMap.append(position, encodedCatchHandlers[i]);
            }
            int codeItemEnd = in.getCursor();

            //now go back and read the tries
            in.setCursor(triesOffset);
            tries = new TryItem[triesCount];
            for (int i=0; i<triesCount; i++) {
                tries[i] = new TryItem(in, handlerMap);
            }

            //and now back to the end of the code item
            in.setCursor(codeItemEnd);
        }
    }

    /** {@inheritDoc} */
    protected int placeItem(int offset) {
        offset += 16 + encodedInstructions.length;
        if (tries != null && tries.length > 0) {
            if (encodedInstructions.length % 2 == 1) {
                offset++;
            }

            offset += tries.length * 8;
            int encodedCatchHandlerBaseOffset = offset;
            for (EncodedCatchHandler encodedCatchHandler: encodedCatchHandlers) {
                offset += encodedCatchHandler.place(offset, encodedCatchHandlerBaseOffset);
            }
        }
        return offset;
    }

    /** {@inheritDoc} */
    protected void writeItem(final AnnotatedOutput out) {
        if (out.annotates()) {
            out.annotate(2, "registers_size");
            out.annotate(2, "ins_size");
            out.annotate(2, "outs_size");
            out.annotate(2, "tries_size");
            out.annotate(4, "debug_info_off");
            out.annotate(4, "insns_size");
            InstructionIterator.IterateInstructions(encodedInstructions,
                    new InstructionIterator.ProcessRawInstructionDelegate() {

                        public void ProcessNormalInstruction(Opcode opcode, int index) {
                            out.annotate(opcode.format.size, opcode.name + " instruction");
                        }

                        public void ProcessReferenceInstruction(Opcode opcode, int index) {
                            out.annotate(opcode.format.size, opcode.name + " instruction");
                        }

                        public void ProcessPackedSwitchInstruction(int index, int targetCount, int instructionLength) {
                            out.annotate(instructionLength, "packed_switch instruction");
                        }

                        public void ProcessSparseSwitchInstruction(int index, int targetCount, int instructionLength) {
                            out.annotate(instructionLength, "sparse_switch instruction");
                        }

                        public void ProcessFillArrayDataInstruction(int index, int elementWidth, int elementCount, int instructionLength) {
                            out.annotate(instructionLength, "fill_array_data instruction");
                        }
                    });
            if (tries != null && (tries.length % 2 == 1)) {
                out.annotate(2, "padding");
            }
        }

        out.writeShort(registerCount);
        out.writeShort(inWords);
        out.writeShort(outWords);
        if (tries == null) {
            out.writeShort(0);
        } else {
            out.writeShort(tries.length);
        }
        if (debugInfo == null) {
            out.writeInt(0);
        } else {
            out.writeInt(debugInfo.getIndex());
        }
        out.writeInt(encodedInstructions.length / 2);
        InstructionWriter.writeInstructions(encodedInstructions, referencedItems, out);

        if (tries != null && tries.length > 0) {
            if ((tries.length % 2) == 1) {
                out.writeShort(0);
            }

            for (TryItem tryItem: tries) {
                tryItem.writeTo(out);
            }

            out.writeUnsignedLeb128(encodedCatchHandlers.length);

            for (EncodedCatchHandler encodedCatchHandler: encodedCatchHandlers) {
                encodedCatchHandler.writeTo(out);
            }
        }
    }

    /** {@inheritDoc} */
    public ItemType getItemType() {
        return ItemType.TYPE_CODE_ITEM;
    }

    /** {@inheritDoc} */
    public String getConciseIdentity() {
        //TODO: should mention the method name here
        return "code_item @0x" + Integer.toHexString(getOffset());
    }

    /** {@inheritDoc} */
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
        return parent.method.compareTo(other.parent.method);
    }

    /**
     * @return the register count
     */
    public int getRegisterCount() {
        return registerCount;
    }

    /**
     * @return a byte array containing the encoded instructions
     */
    public byte[] getEncodedInstructions() {
        return encodedInstructions;
    }

    /**
     * @return an array of the <code>TryItem</code> objects in this <code>CodeItem</code>
     */
    public TryItem[] getTries() {
        return tries;
    }

    /**
     * @return the <code>DebugInfoItem</code> associated with this <code>CodeItem</code>
     */
    public DebugInfoItem getDebugInfo() {
        return debugInfo;
    }

    /**
     * Sets the <code>MethodIdItem</code> of the method that this <code>CodeItem</code> is associated with
     * @param encodedMethod the <code>EncodedMethod</code> of the method that this <code>CodeItem</code> is associated
     * with
     */
    protected void setParent(ClassDataItem.EncodedMethod encodedMethod) {
        this.parent = encodedMethod;
    }

    /**
     * @return the MethodIdItem of the method that this CodeItem belongs to
     */
    public ClassDataItem.EncodedMethod getParent() {
        return parent;
    }

    public static class TryItem {
        /**
         * The address (in 2-byte words) within the code where the try block starts
         */
        public final int startAddress;

        /**
         * The number of 2-byte words that the try block covers
         */
        public final int instructionCount;

        /**
         * The associated exception handler 
         */
        public final EncodedCatchHandler encodedCatchHandler;

        /**
         * Construct a new <code>TryItem</code> with the given values
         * @param startAddress the address (in 2-byte words) within the code where the try block starts
         * @param instructionCount the number of 2-byte words that the try block covers
         * @param encodedCatchHandler the associated exception handler 
         */
        public TryItem(int startAddress, int instructionCount, EncodedCatchHandler encodedCatchHandler) {
            this.startAddress = startAddress;
            this.instructionCount = instructionCount;
            this.encodedCatchHandler = encodedCatchHandler;
        }

        /**
         * This is used internally to construct a new <code>TryItem</code> while reading in a <code>DexFile</code>
         * @param in the Input object to read the <code>TryItem</code> from
         * @param encodedCatchHandlers a SparseArray of the EncodedCatchHandlers for this <code>CodeItem</code>. The
         * key should be the offset of the EncodedCatchHandler from the beginning of the encoded_catch_handler_list
         * structure.  
         */
        private TryItem(Input in, SparseArray<EncodedCatchHandler> encodedCatchHandlers) {
            startAddress = in.readInt();
            instructionCount = in.readShort();

            encodedCatchHandler = encodedCatchHandlers.get(in.readShort());
            if (encodedCatchHandler == null) {
                throw new RuntimeException("Could not find the EncodedCatchHandler referenced by this TryItem");
            }
        }

        /**
         * Writes the <code>TryItem</code> to the given <code>AnnotatedOutput</code> object
         * @param out the <code>AnnotatedOutput</code> object to write to
         */
        private void writeTo(AnnotatedOutput out) {
            if (out.annotates()) {
                out.annotate(4, "start_addr");
                out.annotate(2, "insn_count");
                out.annotate(2, "handler_off");
            }

            out.writeInt(startAddress);
            out.writeShort(instructionCount);
            out.writeShort(encodedCatchHandler.getOffsetInList());
        }
    }

    public static class EncodedCatchHandler {
        /**
         * An array of the individual exception handlers
         */
        public final EncodedTypeAddrPair[] handlers;

        /**
         * The address within the code (in 2-byte words) for the catch all handler, or -1 if there is no catch all
         * handler
         */
        public final int catchAllHandlerAddress;

        //TODO: would it be possible to get away without having these? and generate/create these values while writing?
        private int baseOffset;
        private int offset;

        /**
         * Constructs a new <code>EncodedCatchHandler</code> with the given values
         * @param handlers an array of the individual exception handlers
         * @param catchAllHandlerAddress The address within the code (in 2-byte words) for the catch all handler, or -1
         * if there is no catch all handler
         */
        public EncodedCatchHandler(EncodedTypeAddrPair[] handlers, int catchAllHandlerAddress) {
            this.handlers = handlers;
            this.catchAllHandlerAddress = catchAllHandlerAddress;
        }

        /**
         * This is used internally to construct a new <code>EncodedCatchHandler</code> while reading in a
         * <code>DexFile</code>
         * @param dexFile the <code>DexFile</code> that is being read in
         * @param in the Input object to read the <code>EncodedCatchHandler</code> from
         */
        private EncodedCatchHandler(DexFile dexFile, Input in) {
            int handlerCount = in.readSignedLeb128();

            if (handlerCount < 0) {
                handlers = new EncodedTypeAddrPair[-1 * handlerCount];
            } else {
                handlers = new EncodedTypeAddrPair[handlerCount];
            }

            for (int i=0; i<handlers.length; i++) {
                handlers[i] = new EncodedTypeAddrPair(dexFile, in);
            }

            if (handlerCount <= 0) {
                catchAllHandlerAddress = in.readUnsignedLeb128();
            } else {
                catchAllHandlerAddress = -1;
            }
        }

        /**
         * @return the offset of this <code>EncodedCatchHandler</code> from the beginning of the
         * encoded_catch_handler_list structure
         */
        private int getOffsetInList() {
            return offset-baseOffset;
        }

        /**
         * Places the <code>EncodedCatchHandler</code>, storing the offset and baseOffset, and returning the offset
         * immediately following this <code>EncodedCatchHandler</code>
         * @param offset the offset of this <code>EncodedCatchHandler</code> in the <code>DexFile</code>
         * @param baseOffset the offset of the beginning of the encoded_catch_handler_list structure in the
         * <code>DexFile</code>
         * @return the offset immediately following this <code>EncodedCatchHandler</code>
         */
        private int place(int offset, int baseOffset) {
            this.offset = offset;
            this.baseOffset = baseOffset;

            int size = handlers.length;
            if (catchAllHandlerAddress > -1) {
                size *= -1;
                offset += Leb128Utils.unsignedLeb128Size(catchAllHandlerAddress);
            }
            offset += Leb128Utils.signedLeb128Size(size);

            for (EncodedTypeAddrPair handler: handlers) {
                offset += handler.getSize();
            }
            return offset;
        }

        /**
         * Writes the <code>EncodedCatchHandler</code> to the given <code>AnnotatedOutput</code> object
         * @param out the <code>AnnotatedOutput</code> object to write to
         */
        private void writeTo(AnnotatedOutput out) {
            if (out.annotates()) {
                out.annotate("size");

                int size = handlers.length;
                if (catchAllHandlerAddress < 0) {
                    size = size * -1;
                }
                out.writeSignedLeb128(size);

                for (EncodedTypeAddrPair handler: handlers) {
                    handler.writeTo(out);
                }

                if (catchAllHandlerAddress > -1) {
                    out.annotate("catch_all_addr");
                    out.writeUnsignedLeb128(catchAllHandlerAddress);
                }
            } else {
                int size = handlers.length;
                if (catchAllHandlerAddress < 0) {
                    size = size * -1;
                }
                out.writeSignedLeb128(size);

                for (EncodedTypeAddrPair handler: handlers) {
                    handler.writeTo(out);
                }

                if (catchAllHandlerAddress > -1) {
                    out.writeUnsignedLeb128(catchAllHandlerAddress);
                }
            }
        }
    }

    public static class EncodedTypeAddrPair {
        /**
         * The type of the <code>Exception</code> that this handler handles
         */
        public final TypeIdItem exceptionType;

        /**
         * The address (in 2-byte words) in the code of the handler
         */
        public final int handlerAddress;

        /**
         * Constructs a new <code>EncodedTypeAddrPair</code> with the given values
         * @param exceptionType the type of the <code>Exception</code> that this handler handles
         * @param handlerAddress the address (in 2-byte words) in the code of the handler  
         */
        public EncodedTypeAddrPair(TypeIdItem exceptionType, int handlerAddress) {
            this.exceptionType = exceptionType;
            this.handlerAddress = handlerAddress;
        }

        /**
         * This is used internally to construct a new <code>EncodedTypeAddrPair</code> while reading in a
         * <code>DexFile</code>
         * @param dexFile the <code>DexFile</code> that is being read in
         * @param in the Input object to read the <code>EncodedCatchHandler</code> from
         */
        private EncodedTypeAddrPair(DexFile dexFile, Input in) {
            exceptionType = dexFile.TypeIdsSection.getItemByIndex(in.readUnsignedLeb128());
            handlerAddress = in.readUnsignedLeb128();
        }

        /**
         * @return the size of this <code>EncodedTypeAddrPair</code>
         */
        private int getSize() {
            return Leb128Utils.unsignedLeb128Size(exceptionType.getIndex()) +
                   Leb128Utils.unsignedLeb128Size(handlerAddress);
        }

        /**
         * Writes the <code>EncodedTypeAddrPair</code> to the given <code>AnnotatedOutput</code> object
         * @param out the <code>AnnotatedOutput</code> object to write to
         */
        private void writeTo(AnnotatedOutput out) {
            if (out.annotates()) {
                out.annotate("type_idx");
                out.writeUnsignedLeb128(exceptionType.getIndex());

                out.annotate("addr");
                out.writeUnsignedLeb128(handlerAddress);
            } else {
                out.writeUnsignedLeb128(exceptionType.getIndex());
                out.writeUnsignedLeb128(handlerAddress);
            }
        }
    }
}
