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
import org.jf.dexlib2.dexbacked.util.DebugItemList;
import org.jf.dexlib2.dexbacked.util.FixedSizeList;
import org.jf.dexlib2.dexbacked.util.VariableSizeList;
import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.MethodParameter;
import org.jf.dexlib2.iface.TryBlock;
import org.jf.dexlib2.iface.debug.DebugItem;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.util.AlignmentUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class DexBackedMethodImplementation implements MethodImplementation {
    @Nonnull public final DexBuffer dexBuf;
    @Nonnull public final DexBackedMethod method;
    private final int codeOffset;

    public final int registerCount;
    @Nonnull public final ImmutableList<? extends Instruction> instructions;

    // code_item offsets
    private static final int TRIES_SIZE_OFFSET = 6;
    private static final int DEBUG_OFFSET_OFFSET = 8;
    private static final int INSTRUCTIONS_SIZE_OFFSET = 12;
    private static final int INSTRUCTIONS_START_OFFSET = 16;

    private static final int TRY_ITEM_SIZE = 8;

    public DexBackedMethodImplementation(@Nonnull DexBuffer dexBuf,
                                         @Nonnull DexBackedMethod method,
                                         int codeOffset) {
        this.dexBuf = dexBuf;
        this.method = method;
        this.codeOffset = codeOffset;
        this.registerCount = dexBuf.readUshort(codeOffset);

        instructions = buildInstructionList();
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
                @Nonnull
                @Override
                public TryBlock readItem(int index) {
                    return new DexBackedTryBlock(dexBuf,
                            triesStartOffset + index*TRY_ITEM_SIZE,
                            handlersStartOffset);
                }

                @Override
                public int size() {
                    return triesSize;
                }
            };
        }
        return ImmutableList.of();
    }

    @Nonnull
    @Override
    public Iterable<? extends DebugItem> getDebugItems() {
        final int debugInfoOffset = dexBuf.readSmallUint(codeOffset + DEBUG_OFFSET_OFFSET);
        if (debugInfoOffset > 0) {
            return new DebugItemList(dexBuf, debugInfoOffset, this);
        }
        return ImmutableList.of();
    }

    @Nonnull
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

    //TODO: reading the method params from the debug_info_item should probably be centralized with other debug stuff
    @Nullable
    public VariableSizeList<MethodParameter> getParametersWithNamesOrNull() {
        final int debugInfoOffset = dexBuf.readSmallUint(codeOffset + DEBUG_OFFSET_OFFSET);
        if (debugInfoOffset > 0) {
            DexReader reader = dexBuf.readerAt(debugInfoOffset);
            reader.skipUleb128();
            final int parameterNameCount = reader.readSmallUleb128();
            final List<? extends MethodParameter> methodParametersWithoutNames = method.getParametersWithoutNames();
            //TODO: make sure dalvik doesn't allow more parameter names than we have parameters

            return new VariableSizeList<MethodParameter>(dexBuf, reader.getOffset()) {
                @Nonnull
                @Override
                protected MethodParameter readItem(@Nonnull DexReader reader, int index) {
                    final MethodParameter methodParameter = methodParametersWithoutNames.get(index);
                    String _name = null;
                    if (index < parameterNameCount) {
                        _name = reader.getOptionalString(reader.readSmallUleb128() - 1);
                    }
                    final String name = _name;

                    return new MethodParameter() {
                        @Nonnull @Override public String getType() { return methodParameter.getType(); }
                        @Nullable @Override public String getName() { return name; }
                        @Nullable @Override public String getSignature() { return methodParameter.getSignature();}
                        @Nonnull @Override public List<? extends Annotation> getAnnotations() {
                            return methodParameter.getAnnotations();
                        }
                    };
                }

                @Override public int size() { return methodParametersWithoutNames.size(); }
            };
        }
        return null;
    }

    public List<? extends MethodParameter> getParametersWithNames() {
        List<MethodParameter> parameters = getParametersWithNamesOrNull();
        if (parameters != null) {
            return parameters;
        }
        return method.getParametersWithoutNames();
    }
}
