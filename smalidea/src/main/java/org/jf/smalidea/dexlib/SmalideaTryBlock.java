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

package org.jf.smalidea.dexlib;

import org.jf.dexlib2.base.BaseTryBlock;
import org.jf.smalidea.psi.impl.SmaliCatchStatement;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;

public class SmalideaTryBlock extends BaseTryBlock<SmalideaExceptionHandler> {
    @Nonnull private final SmaliCatchStatement catchStatement;

    public SmalideaTryBlock(@Nonnull SmaliCatchStatement catchStatement) {
        this.catchStatement = catchStatement;
    }

    @Override public int getCodeUnitCount() {
        int endOffset = catchStatement.getEndLabel().resolve().getOffset() / 2;
        return endOffset - getStartCodeAddress();
    }

    @Override public int getStartCodeAddress() {
        // TODO: how to handle references to non-existent labels?
        return catchStatement.getStartLabel().resolve().getOffset() / 2;
    }

    @Nonnull @Override public List<? extends SmalideaExceptionHandler> getExceptionHandlers() {
        return Arrays.asList(new SmalideaExceptionHandler(catchStatement));
    }
}
