/*
 * Copyright 2015, Google Inc.
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

package org.jf.smalidea.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jf.dexlib2.Opcode;
import org.jf.smalidea.psi.impl.SmaliInstruction;
import org.jf.smalidea.psi.impl.SmaliLabel;
import org.jf.smalidea.psi.impl.SmaliLabelReference;
import org.jf.smalidea.psi.impl.SmaliMethod;

public class InstructionUtils {

    @Nullable
    public static SmaliInstruction findFirstInstructionWithTarget(
            @NotNull SmaliMethod method, @NotNull Opcode opcode, int targetOffset) {
        for (SmaliInstruction instruction: method.getInstructions()) {
            if (instruction.getOpcode() == opcode) {
                SmaliLabelReference labelReference = instruction.getTarget();
                if (labelReference == null) {
                    continue;
                }

                SmaliLabel label = labelReference.resolve();
                if (label == null) {
                    continue;
                }

                if (label.getOffset() == targetOffset) {
                    return instruction;
                }
            }
        }
        return null;
    }
}
