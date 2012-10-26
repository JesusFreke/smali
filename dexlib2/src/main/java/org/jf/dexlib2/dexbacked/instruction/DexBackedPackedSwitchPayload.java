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

package org.jf.dexlib2.dexbacked.instruction;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.dexbacked.DexBuffer;
import org.jf.dexlib2.dexbacked.util.FixedSizeList;
import org.jf.dexlib2.iface.instruction.SwitchElement;
import org.jf.dexlib2.iface.instruction.formats.PackedSwitchPayload;

import javax.annotation.Nonnull;
import java.util.List;

public class DexBackedPackedSwitchPayload implements PackedSwitchPayload {
    public static final Opcode OPCODE = Opcode.PACKED_SWITCH_PAYLOAD;

    @Nonnull public final DexBuffer dexBuf;
    private final int instructionOffset;

    public final int elementCount;

    private static final int ELEMENT_COUNT_OFFSET = 2;
    private static final int FIRST_KEY_OFFSET = 4;
    private static final int TARGETS_OFFSET = 8;

    public DexBackedPackedSwitchPayload(@Nonnull DexBuffer dexBuf,
                                        int instructionOffset) {
        this.dexBuf = dexBuf;
        this.instructionOffset = instructionOffset;

        this.elementCount = dexBuf.readByte(instructionOffset + ELEMENT_COUNT_OFFSET);
    }

    @Nonnull
    @Override
    public List<? extends SwitchElement> getSwitchElements() {
        final int firstKey = dexBuf.readInt(instructionOffset + FIRST_KEY_OFFSET);
        return new FixedSizeList<SwitchElement>() {
            @Nonnull
            @Override
            public SwitchElement readItem(final int index) {
                return new SwitchElement() {
                    @Override
                    public int getKey() {
                        return firstKey + index;
                    }

                    @Override
                    public int getOffset() {
                        return dexBuf.readInt(instructionOffset + TARGETS_OFFSET + index*4);
                    }
                };
            }

            @Override public int size() { return elementCount; }
        };
    }

    @Override public int getCodeUnits() { return 4 + elementCount*2; }
    @Override public Opcode getOpcode() { return OPCODE; }
}
