/*
 * Copyright 2014, Google Inc.
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

package org.jf.smalidea.dexlib.instruction;

import com.google.common.collect.Lists;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.iface.instruction.SwitchElement;
import org.jf.dexlib2.iface.instruction.formats.PackedSwitchPayload;
import org.jf.smalidea.psi.impl.*;
import org.jf.smalidea.util.InstructionUtils;

import javax.annotation.Nonnull;
import java.util.List;

public class SmalideaPackedSwitchPayload extends SmalideaInstruction implements PackedSwitchPayload {
    public SmalideaPackedSwitchPayload(@Nonnull SmaliInstruction instruction) {
        super(instruction);
    }

    @Nonnull @Override public List<? extends SwitchElement> getSwitchElements() {
        final SmaliLiteral startKey = psiInstruction.getPackedSwitchStartKey();
        assert startKey != null;
        List<SmaliPackedSwitchElement> elements = psiInstruction.getPackedSwitchElements();

        SmaliMethod smaliMethod = psiInstruction.getParentMethod();
        SmaliInstruction packedSwitchInstruction = InstructionUtils.findFirstInstructionWithTarget(
                smaliMethod, Opcode.PACKED_SWITCH, psiInstruction.getOffset());
        final int baseOffset;

        if (packedSwitchInstruction == null) {
            baseOffset = 0;
        } else {
            baseOffset = packedSwitchInstruction.getOffset();
        }

        List<SwitchElement> newElements = Lists.newArrayList();
        // TODO: check for integer wraparound (how does art/dalvik handle that?)
        int initialKey = (int)startKey.getIntegralValue();
        for (int i=0; i<elements.size(); i++) {
            final SmaliPackedSwitchElement element = elements.get(i);

            final int key = initialKey + i;

            newElements.add(new SwitchElement() {
                @Override public int getKey() {
                    return key;
                }

                @Override public int getOffset() {
                    SmaliLabelReference labelReference = element.getTarget();
                    if (labelReference == null) {
                        return 0;
                    }

                    SmaliLabel label = labelReference.resolve();
                    if (label == null) {
                        return 0;
                    }

                    return label.getOffset() - baseOffset;
                }
            });
        }

        return newElements;
    }

    @Override public int getCodeUnits() {
        return psiInstruction.getInstructionSize()/2;
    }
}
