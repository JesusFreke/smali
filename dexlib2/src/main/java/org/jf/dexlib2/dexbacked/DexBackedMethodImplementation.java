/*
 * Copyright 2012, Google Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.dexlib2.dexbacked;

import com.google.common.collect.ImmutableList;
import org.jf.dexlib2.dexbacked.instruction.DexBackedInstruction;
import org.jf.dexlib2.dexbacked.util.FixedSizeList;
import org.jf.dexlib2.dexbacked.util.InstructionOffsetMap;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.TryBlock;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.util.AlignmentUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class DexBackedMethodImplementation implements MethodImplementation {
    @Nonnull public final DexBuffer dexBuf;
    private final int codeOffset;

    public final int registerCount;
    @Nonnull public final ImmutableList<? extends Instruction> instructions;
    @Nonnull private final InstructionOffsetMap instructionOffsetMap;

    // code_item offsets
    private static final int TRIES_SIZE_OFFSET = 6;
    private static final int INSTRUCTIONS_SIZE_OFFSET = 12;
    private static final int INSTRUCTIONS_START_OFFSET = 16;

    private static final int TRY_ITEM_SIZE = 8;

    public DexBackedMethodImplementation(@Nonnull DexBuffer dexBuf,
                                         int codeOffset) {
        this.dexBuf = dexBuf;
        this.codeOffset = codeOffset;
        this.registerCount = dexBuf.readUshort(codeOffset);

        instructions = buildInstructionList();
        instructionOffsetMap = buildInstructionOffsetMap();
    }

    @Override public int getRegisterCount() { return registerCount; }
    @Nonnull @Override public ImmutableList<? extends Instruction> getInstructions() { return instructions; }

    @Nonnull
    @Override
    public List<? extends TryBlock> getTryBlocks() {
        final int triesSize = dexBuf.readUshort(codeOffset + TRIES_SIZE_OFFSET);
        if (triesSize > 0) {
            int instructionsSize = dexBuf.readSmallUint(codeOffset + INSTRUCTIONS_SIZE_OFFSET);
            final int triesStartOffset = AlignmentUtils.alignOffset(
                    codeOffset + INSTRUCTIONS_START_OFFSET + (instructionsSize*2), 4);
            final int handlersStartOffset = triesStartOffset + triesSize*TRY_ITEM_SIZE;

            return new FixedSizeList<TryBlock>() {
                @Override
                public TryBlock readItem(int index) {
                    return new DexBackedTryBlock(dexBuf,
                            triesStartOffset + index*TRY_ITEM_SIZE,
                            handlersStartOffset,
                            instructionOffsetMap);
                }

                @Override
                public int size() {
                    return triesSize;
                }
            };
        }
        return ImmutableList.of();
    }

    private ImmutableList<? extends Instruction> buildInstructionList() {
        // instructionsSize is the number of 16-bit code units in the instruction list, not the number of instructions
        int instructionsSize = dexBuf.readSmallUint(codeOffset + INSTRUCTIONS_SIZE_OFFSET);

        // we can use instructionsSize as an upper bound on the number of instructions there will be
        ArrayList<Instruction> instructions = new ArrayList<Instruction>(instructionsSize);
        int instructionsStartOffset = codeOffset + INSTRUCTIONS_START_OFFSET;
        DexReader reader = dexBuf.readerAt(instructionsStartOffset);
        int endOffset = instructionsStartOffset + (instructionsSize*2);

        while (reader.getOffset() < endOffset) {
            instructions.add(DexBackedInstruction.readFrom(reader));
        }

        return ImmutableList.copyOf(instructions);
    }

    /**
     * Builds an InstructionOffsetMap that maps an instruction offset to its index.
     *
     * This must be called after the instructions field has been set
     *
     * @return An InstructionOffsetMap object
     */
    private InstructionOffsetMap buildInstructionOffsetMap() {
        int[] offsets = new int[instructions.size()];
        int currentOffset = 0;
        for (int i=0; i<offsets.length; i++) {
            offsets[i] = currentOffset;
            // TODO: need to handle variable size instructions
            currentOffset += instructions.get(i).getOpcode().format.size;
        }
        return new InstructionOffsetMap(offsets);
    }
}
