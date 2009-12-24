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

import org.jf.dexlib.Code.*;
import org.jf.dexlib.Code.Format.Instruction20t;
import org.jf.dexlib.Code.Format.Instruction30t;
import org.jf.dexlib.Code.Format.Instruction21c;
import org.jf.dexlib.Code.Format.Instruction31c;
import org.jf.dexlib.Util.*;
import org.jf.dexlib.Debug.DebugInstructionIterator;

import java.util.List;
import java.util.ArrayList;

public class CodeItem extends Item<CodeItem> {
    private int registerCount;
    private int inWords;
    private int outWords;
    private DebugInfoItem debugInfo;
    private Instruction[] instructions;
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
     * @param instructions the instructions for this code item
     * @param tries an array of the tries defined for this code/method
     * @param encodedCatchHandlers an array of the exception handlers defined for this code/method
     */
    private CodeItem(DexFile dexFile,
                    int registerCount,
                    int inWords,
                    int outWords,
                    DebugInfoItem debugInfo,
                    Instruction[] instructions,
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

        this.instructions = instructions;
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
     * @param instructions the instructions for this code item
     * @param tries a list of the tries defined for this code/method or null if none
     * @param encodedCatchHandlers a list of the exception handlers defined for this code/method or null if none
     * @return a new <code>CodeItem</code> with the given values.
     */
    public static CodeItem getInternedCodeItem(DexFile dexFile,
                    int registerCount,
                    int inWords,
                    int outWords,
                    DebugInfoItem debugInfo,
                    List<Instruction> instructions,
                    List<TryItem> tries,
                    List<EncodedCatchHandler> encodedCatchHandlers) {
        TryItem[] triesArray = null;
        EncodedCatchHandler[] encodedCatchHandlersArray = null;
        Instruction[] instructionsArray = null;

        if (tries != null && tries.size() > 0) {
            triesArray = new TryItem[tries.size()];
            tries.toArray(triesArray);
        }

        if (encodedCatchHandlers != null && encodedCatchHandlers.size() > 0) {
            encodedCatchHandlersArray = new EncodedCatchHandler[encodedCatchHandlers.size()];
            encodedCatchHandlers.toArray(encodedCatchHandlersArray);
        }

        if (instructions != null && instructions.size() > 0) {
            instructionsArray = new Instruction[instructions.size()];
            instructions.toArray(instructionsArray);
        }

        CodeItem codeItem = new CodeItem(dexFile, registerCount, inWords, outWords, debugInfo, instructionsArray,
                triesArray, encodedCatchHandlersArray);
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

        final ArrayList<Instruction> instructionList = new ArrayList<Instruction>();

        byte[] encodedInstructions = in.readBytes(instructionCount * 2);
        InstructionIterator.IterateInstructions(dexFile, encodedInstructions,
                new InstructionIterator.ProcessInstructionDelegate() {
                    public void ProcessInstruction(int index, Instruction instruction) {
                        instructionList.add(instruction);
                    }
                });

        this.instructions = new Instruction[instructionList.size()];
        instructionList.toArray(instructions);

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
        offset += 16 + getInstructionsLength();

        if (tries != null && tries.length > 0) {
            if (offset % 4 != 0) {
                offset+=2;
            }

            offset += tries.length * 8;
            int encodedCatchHandlerBaseOffset = offset;
            offset += Leb128Utils.unsignedLeb128Size(encodedCatchHandlers.length);
            for (EncodedCatchHandler encodedCatchHandler: encodedCatchHandlers) {
                offset = encodedCatchHandler.place(offset, encodedCatchHandlerBaseOffset);
            }
        }
        return offset;
    }

    /** {@inheritDoc} */
    protected void writeItem(final AnnotatedOutput out) {
        int instructionsLength = getInstructionsLength()/2;

        if (out.annotates()) {
            out.annotate(0, parent.method.getMethodString());
            out.annotate(2, "registers_size: 0x" + Integer.toHexString(registerCount) + " (" + registerCount + ")");
            out.annotate(2, "ins_size: 0x" + Integer.toHexString(inWords) + " (" + inWords + ")");
            out.annotate(2, "outs_size: 0x" + Integer.toHexString(outWords) + " (" + outWords + ")");
            int triesLength = tries==null?0:tries.length;
            out.annotate(2, "tries_size: 0x" + Integer.toHexString(triesLength) + " (" + triesLength + ")");
            if (debugInfo == null) {
                out.annotate(4, "debug_info_off:");
            } else {
                out.annotate(4, "debug_info_off: 0x" + debugInfo.getOffset());
            }
            out.annotate(4, "insns_size: 0x" + Integer.toHexString(instructionsLength) + " (" +
                    (instructionsLength) + ")");
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
            out.writeInt(debugInfo.getOffset());
        }

        int currentCodeOffset = 0;
        for (Instruction instruction: instructions) {
            currentCodeOffset += instruction.getSize(currentCodeOffset);
        }

        out.writeInt(instructionsLength);

        currentCodeOffset = 0;
        for (Instruction instruction: instructions) {
            currentCodeOffset = instruction.write(out, currentCodeOffset);
        }

        if (tries != null && tries.length > 0) {
            if (out.annotates()) {
                if ((currentCodeOffset % 4) != 0) {
                    out.annotate("padding");
                    out.writeShort(0);
                }

                int index = 0;
                for (TryItem tryItem: tries) {
                    out.annotate(0, "[0x" + Integer.toHexString(index++) + "] try_item");
                    out.indent();
                    tryItem.writeTo(out);
                    out.deindent();
                }

                out.annotate("handler_count: 0x" + Integer.toHexString(encodedCatchHandlers.length) + "(" +
                        encodedCatchHandlers.length + ")");
                out.writeUnsignedLeb128(encodedCatchHandlers.length);

                index = 0;
                for (EncodedCatchHandler encodedCatchHandler: encodedCatchHandlers) {
                    out.annotate(0, "[" + Integer.toHexString(index++) + "] encoded_catch_handler");
                    out.indent();
                    encodedCatchHandler.writeTo(out);
                    out.deindent();
                }
            } else {
                if ((currentCodeOffset % 4) != 0) {
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
    }

    /** {@inheritDoc} */
    public ItemType getItemType() {
        return ItemType.TYPE_CODE_ITEM;
    }

    /** {@inheritDoc} */
    public String getConciseIdentity() {
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
     * @return an array of the instructions in this code item
     */
    public Instruction[] getInstructions() {
        return instructions;
    }

    /**
     * @return an array of the <code>TryItem</code> objects in this <code>CodeItem</code>
     */
    public TryItem[] getTries() {
        return tries;
    }

    /**
     * @return an array of the <code>EncodedCatchHandler</code> objects in this <code>CodeItem</code>
     */
    public EncodedCatchHandler[] getHandlers() {
        return encodedCatchHandlers;
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

    /**
     * Used by OdexUtil to update this <code>CodeItem</code> with a deodexed version of the instructions
     * @param newInstructions the new instructions to use for this code item
     */
    public void updateCode(Instruction[] newInstructions) {
        this.instructions = newInstructions;
    }

    private int getInstructionsLength() {
        int offset = 0;
        for (Instruction instruction: instructions) {
            offset += instruction.getSize(offset);
        }
        return offset;
    }

    /**
     * Go through the instructions and perform any of the following fixes that are applicable
     * - Replace const-string instruction with const-string/jumbo, when the string index is too big
     * - Replace goto and goto/16 with a larger version of goto, when the target is too far away
     * TODO: we should be able to replace if-* instructions with targets that are too far away with a negated if followed by a goto/32 to the original target
     * TODO: remove multiple nops that occur before a switch/array data pseudo instruction. In some cases, multiple smali-baksmali cycles with changes in between could cause nops to start piling up
     *
     * The above fixes are applied iteratively, until no more fixes have been performed
     */
    public void fixInstructions(boolean fixStringConst, boolean fixGoto) {
        boolean didSomething = false;

        do
        {
            didSomething = false;

            int currentCodeOffset = 0;
            for (int i=0; i<instructions.length; i++) {
                Instruction instruction = instructions[i];

                if (fixGoto && instruction.opcode == Opcode.GOTO) {
                    int offset = ((OffsetInstruction)instruction).getOffset();

                    if (((byte)offset) != offset) {
                        //the offset doesn't fit within a byte, we need to upgrade to a goto/16 or goto/32

                        if ((short)offset == offset) {
                            //the offset fits in a short, so upgrade to a goto/16            h
                            replaceInstructionAtOffset(currentCodeOffset, new Instruction20t(Opcode.GOTO_16, offset));
                        }
                        else {
                            //The offset won't fit into a short, we have to upgrade to a goto/32
                            replaceInstructionAtOffset(currentCodeOffset, new Instruction30t(Opcode.GOTO_32, offset));
                        }
                        didSomething = true;
                        break;
                    }
                } else if (fixGoto && instruction.opcode == Opcode.GOTO_16) {
                    int offset = ((OffsetInstruction)instruction).getOffset();

                    if (((short)offset) != offset) {
                        //the offset doesn't fit within a short, we need to upgrade to a goto/32
                        replaceInstructionAtOffset(currentCodeOffset, new Instruction30t(Opcode.GOTO_32, offset));
                        didSomething = true;
                        break;
                    }
                } else if (fixStringConst && instruction.opcode == Opcode.CONST_STRING) {
                    Instruction21c constStringInstruction = (Instruction21c)instruction;
                    if (constStringInstruction.getReferencedItem().getIndex() > 0xFFFF) {
                        replaceInstructionAtOffset(currentCodeOffset, new Instruction31c(Opcode.CONST_STRING_JUMBO,
                                (short)constStringInstruction.getRegisterA(),
                                constStringInstruction.getReferencedItem()));
                        didSomething = true;
                        break;
                    }
                }

                currentCodeOffset += instruction.getSize(currentCodeOffset);
            }
        }while(didSomething);
    }

    private void replaceInstructionAtOffset(int offset, Instruction replacementInstruction) {
        Instruction originalInstruction = null;

        int[] originalInstructionOffsets = new int[instructions.length+1];
        SparseIntArray originalSwitchOffsetByOriginalSwitchDataOffset = new SparseIntArray();

        int currentCodeOffset = 0;
        int instructionIndex = 0;
        int i;
        for (i=0; i<instructions.length; i++) {
            Instruction instruction = instructions[i];

            if (currentCodeOffset == offset) {
                originalInstruction = instruction;
                instructionIndex = i;
            }

            if (instruction.opcode == Opcode.PACKED_SWITCH || instruction.opcode == Opcode.SPARSE_SWITCH) {
                OffsetInstruction offsetInstruction = (OffsetInstruction)instruction;

                int switchDataOffset = currentCodeOffset + offsetInstruction.getOffset() * 2;
                if (originalSwitchOffsetByOriginalSwitchDataOffset.indexOfKey(switchDataOffset) < 0) {
                    originalSwitchOffsetByOriginalSwitchDataOffset.put(switchDataOffset, currentCodeOffset);
                }
            }

            originalInstructionOffsets[i] = currentCodeOffset;
            currentCodeOffset += instruction.getSize(currentCodeOffset);
        }
        //add the offset just past the end of the last instruction, to help when fixing up try blocks that end
        //at the end of the method
        originalInstructionOffsets[i] = currentCodeOffset;

        if (originalInstruction == null) {
            throw new RuntimeException("There is no instruction at offset " + offset);
        }

        instructions[instructionIndex] = replacementInstruction;

        //if we're replacing the instruction with one of the same size, we don't have to worry about fixing
        //up any offsets
        if (originalInstruction.getSize(offset) == replacementInstruction.getSize(offset)) {
            return;
        }

        //TODO: replace these with a callable delegate
        final SparseIntArray originalOffsetsByNewOffset = new SparseIntArray();
        final SparseIntArray newOffsetsByOriginalOffset = new SparseIntArray();

        currentCodeOffset = 0;
        for (i=0; i<instructions.length; i++) {
            Instruction instruction = instructions[i];

            int originalOffset = originalInstructionOffsets[i];
            originalOffsetsByNewOffset.append(currentCodeOffset, originalOffset);
            newOffsetsByOriginalOffset.append(originalOffset, currentCodeOffset);

            currentCodeOffset += instruction.getSize(currentCodeOffset);
        }

        //add the offset just past the end of the last instruction, to help when fixing up try blocks that end
        //at the end of the method
        originalOffsetsByNewOffset.append(currentCodeOffset, originalInstructionOffsets[i]);
        newOffsetsByOriginalOffset.append(originalInstructionOffsets[i], currentCodeOffset);

        //update any "offset" instructions, or switch data instructions
        currentCodeOffset = 0;
        for (i=0; i<instructions.length; i++) {
            Instruction instruction = instructions[i];

            if (instruction instanceof OffsetInstruction) {
                OffsetInstruction offsetInstruction = (OffsetInstruction)instruction;

                assert originalOffsetsByNewOffset.indexOfKey(currentCodeOffset) >= 0;
                int originalOffset = originalOffsetsByNewOffset.get(currentCodeOffset);

                int originalInstructionTarget = originalOffset + offsetInstruction.getOffset() * 2;

                assert newOffsetsByOriginalOffset.indexOfKey(originalInstructionTarget) >= 0;
                int newInstructionTarget = newOffsetsByOriginalOffset.get(originalInstructionTarget);

                int newOffset = (newInstructionTarget - currentCodeOffset) / 2;

                if (newOffset != offsetInstruction.getOffset()) {
                    offsetInstruction.updateOffset(newOffset);
                }
            } else if (instruction instanceof MultiOffsetInstruction) {
                MultiOffsetInstruction multiOffsetInstruction = (MultiOffsetInstruction)instruction;

                assert originalOffsetsByNewOffset.indexOfKey(currentCodeOffset) >= 0;
                int originalDataOffset = originalOffsetsByNewOffset.get(currentCodeOffset);

                int originalSwitchOffset = originalSwitchOffsetByOriginalSwitchDataOffset.get(originalDataOffset);
                if (originalSwitchOffset == 0) {
                    throw new RuntimeException("This method contains an unreferenced switch data block, and can't be automatically fixed.");
                }

                assert newOffsetsByOriginalOffset.indexOfKey(originalSwitchOffset) >= 0;
                int newSwitchOffset = newOffsetsByOriginalOffset.get(originalSwitchOffset);

                int[] targets = multiOffsetInstruction.getTargets();
                for (int t=0; t<targets.length; t++) {
                    int originalTargetOffset = originalSwitchOffset + targets[t]*2;
                    assert newOffsetsByOriginalOffset.indexOfKey(originalTargetOffset) >= 0;
                    int newTargetOffset = newOffsetsByOriginalOffset.get(originalTargetOffset);
                    int newOffset = (newTargetOffset - newSwitchOffset)/2;
                    if (newOffset != targets[t]) {
                        multiOffsetInstruction.updateTarget(t, newOffset);
                    }
                }
            }
            currentCodeOffset += instruction.getSize(currentCodeOffset);
        }

        if (debugInfo != null) {
            final byte[] encodedDebugInfo = debugInfo.getEncodedDebugInfo();

            ByteArrayInput debugInput = new ByteArrayInput(encodedDebugInfo);

            DebugInstructionFixer debugInstructionFixer = new DebugInstructionFixer(encodedDebugInfo,
                newOffsetsByOriginalOffset, originalOffsetsByNewOffset);
            DebugInstructionIterator.IterateInstructions(debugInput, debugInstructionFixer);

            if (debugInstructionFixer.result != null) {
                debugInfo.setEncodedDebugInfo(debugInstructionFixer.result);
            }
        }

        if (encodedCatchHandlers != null) {
            for (EncodedCatchHandler encodedCatchHandler: encodedCatchHandlers) {
                if (encodedCatchHandler.catchAllHandlerAddress != -1) {
                    assert newOffsetsByOriginalOffset.indexOfKey(encodedCatchHandler.catchAllHandlerAddress*2) >= 0;
                    encodedCatchHandler.catchAllHandlerAddress =
                            newOffsetsByOriginalOffset.get(encodedCatchHandler.catchAllHandlerAddress*2)/2;
                }

                for (EncodedTypeAddrPair handler: encodedCatchHandler.handlers) {
                    assert newOffsetsByOriginalOffset.indexOfKey(handler.handlerAddress*2) >= 0;
                    handler.handlerAddress = newOffsetsByOriginalOffset.get(handler.handlerAddress*2)/2;
                }
            }
        }

        if (this.tries != null) {
            for (TryItem tryItem: tries) {
                int startAddress = tryItem.startAddress;
                int endAddress = tryItem.startAddress + tryItem.instructionCount;

                assert newOffsetsByOriginalOffset.indexOfKey(startAddress * 2) >= 0;
                tryItem.startAddress = newOffsetsByOriginalOffset.get(startAddress * 2)/2;

                assert newOffsetsByOriginalOffset.indexOfKey(endAddress * 2) >= 0;
                tryItem.instructionCount = newOffsetsByOriginalOffset.get(endAddress * 2)/2 - tryItem.startAddress;
            }
        }
    }

    private class DebugInstructionFixer extends DebugInstructionIterator.ProcessRawDebugInstructionDelegate {
        private int address = 0;
        private SparseIntArray newOffsetsByOriginalOffset;
        private SparseIntArray originalOffsetsByNewOffset;
        private final byte[] originalEncodedDebugInfo;
        public byte[] result = null;

        public DebugInstructionFixer(byte[] originalEncodedDebugInfo, SparseIntArray newOffsetsByOriginalOffset,
                                     SparseIntArray originalOffsetsByNewOffset) {
            this.newOffsetsByOriginalOffset = newOffsetsByOriginalOffset;
            this.originalOffsetsByNewOffset = originalOffsetsByNewOffset;
            this.originalEncodedDebugInfo = originalEncodedDebugInfo;
        }


        @Override
        public void ProcessAdvancePC(int startOffset, int length, int addressDelta) {
            address += addressDelta;

            if (result != null) {
                return;
            }

            int newOffset = newOffsetsByOriginalOffset.get(address*2, -1);

            //The address might not point to an actual instruction in some cases, for example, if an AdvancePC
            //instruction was inserted just before a "special" instruction, to fix up the offsets for a previous
            //instruction replacement.
            //In this case, it should be safe to skip, because there will be another AdvancePC/SpecialOpcode that will
            //bump up the address to point to a valid instruction before anything (line/local/etc.) is emitted
            if (newOffset == -1) {
                return;
            }

            assert newOffset != -1;
            newOffset = newOffset / 2;

            if (newOffset != address) {
                int newAddressDelta = newOffset - (address - addressDelta);
                assert newAddressDelta > 0;
                int addressDiffSize = Leb128Utils.unsignedLeb128Size(newAddressDelta);

                result = new byte[originalEncodedDebugInfo.length + addressDiffSize - (length - 1)];

                System.arraycopy(originalEncodedDebugInfo, 0, result, 0, startOffset);

                result[startOffset] = 0x01; //DBG_ADVANCE_PC debug opcode
                Leb128Utils.writeUnsignedLeb128(newAddressDelta, result, startOffset+1);

                System.arraycopy(originalEncodedDebugInfo, startOffset+length, result,
                        startOffset + addressDiffSize + 1,
                        originalEncodedDebugInfo.length - (startOffset + addressDiffSize + 1));
            }
        }

        @Override
        public void ProcessSpecialOpcode(int startOffset, int debugOpcode, int lineDelta,
                                         int addressDelta) {
            address += addressDelta;
            if (result != null) {
                return;
            }

            int newOffset = newOffsetsByOriginalOffset.get(address*2, -1);
            assert newOffset != -1;
            newOffset = newOffset / 2;

            if (newOffset != address) {
                int newAddressDelta = newOffset - (address - addressDelta);
                assert newAddressDelta > 0;

                //if the new address delta won't fit in the special opcode, we need to insert
                //an additional DBG_ADVANCE_PC opcode
                if (lineDelta < 2 && newAddressDelta > 16 || lineDelta > 1 && newAddressDelta > 15) {
                    int additionalAddressDelta = newOffset - address;
                    int additionalAddressDeltaSize = Leb128Utils.signedLeb128Size(additionalAddressDelta);

                    result = new byte[originalEncodedDebugInfo.length + additionalAddressDeltaSize + 1];

                    System.arraycopy(originalEncodedDebugInfo, 0, result, 0, startOffset);
                    result[startOffset] = 0x01; //DBG_ADVANCE_PC
                    Leb128Utils.writeUnsignedLeb128(additionalAddressDelta, result, startOffset+1);
                    System.arraycopy(originalEncodedDebugInfo, startOffset, result,
                            startOffset+additionalAddressDeltaSize+1,
                            result.length - (startOffset+additionalAddressDeltaSize+1));
                } else {
                    result = new byte[originalEncodedDebugInfo.length];
                    System.arraycopy(originalEncodedDebugInfo, 0, result, 0, result.length);
                    result[startOffset] = DebugInfoBuilder.calculateSpecialOpcode(lineDelta,
                            newAddressDelta);
                }
            }
        }
    }

    public static class TryItem {
        /**
         * The address (in 2-byte words) within the code where the try block starts
         */
        private int startAddress;

        /**
         * The number of 2-byte words that the try block covers
         */
        private int instructionCount;

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
                out.annotate(4, "start_addr: 0x" + Integer.toHexString(startAddress));
                out.annotate(2, "insn_count: 0x" + Integer.toHexString(instructionCount) + " (" + instructionCount +
                        ")");
                out.annotate(2, "handler_off: 0x" + Integer.toHexString(encodedCatchHandler.getOffsetInList()));
            }

            out.writeInt(startAddress);
            out.writeShort(instructionCount);
            out.writeShort(encodedCatchHandler.getOffsetInList());
        }

        /**
         * @return The address (in 2-byte words) within the code where the try block starts
         */
        public int getStartAddress() {
            return startAddress;
        }

        /**
         * @return The number of 2-byte words that the try block covers
         */
        public int getInstructionCount() {
            return instructionCount;
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
        private int catchAllHandlerAddress;

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
         * Returns the "Catch All" handler address for this <code>EncodedCatchHandler</code>
         * @return
         */
        public int getCatchAllHandlerAddress() {
            return catchAllHandlerAddress;
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
                out.annotate("size: 0x" + Integer.toHexString(handlers.length) + " (" + handlers.length + ")");

                int size = handlers.length;
                if (catchAllHandlerAddress > -1) {
                    size = size * -1;
                }
                out.writeSignedLeb128(size);

                int index = 0;
                for (EncodedTypeAddrPair handler: handlers) {
                    out.annotate(0, "[" + index++ + "] encoded_type_addr_pair");
                    out.indent();
                    handler.writeTo(out);
                    out.deindent();
                }

                if (catchAllHandlerAddress > -1) {
                    out.annotate("catch_all_addr: 0x" + Integer.toHexString(catchAllHandlerAddress));
                    out.writeUnsignedLeb128(catchAllHandlerAddress);
                }
            } else {
                int size = handlers.length;
                if (catchAllHandlerAddress > -1) {
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

        @Override
        public int hashCode() {
            int hash = 0;
            for (EncodedTypeAddrPair handler: handlers) {
                hash = hash * 31 + handler.hashCode();
            }
            hash = hash * 31 + catchAllHandlerAddress;
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            if (this==o) {
                return true;
            }
            if (o==null || !this.getClass().equals(o.getClass())) {
                return false;
            }

            EncodedCatchHandler other = (EncodedCatchHandler)o;
            if (handlers.length != other.handlers.length || catchAllHandlerAddress != other.catchAllHandlerAddress) {
                return false;
            }

            for (int i=0; i<handlers.length; i++) {
                if (!handlers[i].equals(other.handlers[i])) {
                    return false;
                }
            }

            return true;
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
        private int handlerAddress;

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
                out.annotate("exception_type: " + exceptionType.getTypeDescriptor());
                out.writeUnsignedLeb128(exceptionType.getIndex());

                out.annotate("handler_addr: 0x" + Integer.toHexString(handlerAddress));
                out.writeUnsignedLeb128(handlerAddress);
            } else {
                out.writeUnsignedLeb128(exceptionType.getIndex());
                out.writeUnsignedLeb128(handlerAddress);
            }
        }

        public int getHandlerAddress() {
            return handlerAddress;
        }

        @Override
        public int hashCode() {
            return exceptionType.hashCode() * 31 + handlerAddress;
        }

        @Override
        public boolean equals(Object o) {
            if (this==o) {
                return true;
            }
            if (o==null || !this.getClass().equals(o.getClass())) {
                return false;
            }

            EncodedTypeAddrPair other = (EncodedTypeAddrPair)o;
            return exceptionType == other.exceptionType && handlerAddress == other.handlerAddress;
        }
    }
}
