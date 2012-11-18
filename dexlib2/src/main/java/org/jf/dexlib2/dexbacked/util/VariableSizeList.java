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

package org.jf.dexlib2.dexbacked.util;

import org.jf.dexlib2.dexbacked.DexBuffer;
import org.jf.dexlib2.dexbacked.DexReader;

import javax.annotation.Nonnull;
import java.util.AbstractSequentialList;

public abstract class VariableSizeList<T> extends AbstractSequentialList<T> {
    @Nonnull private final DexBuffer dexBuf;
    private final int offset;
    private final int size;

    public VariableSizeList(@Nonnull DexBuffer dexBuf, int offset, int size) {
        this.dexBuf = dexBuf;
        this.offset = offset;
        this.size = size;
    }

    @Nonnull protected abstract T readNextItem(@Nonnull DexReader reader, int index);

    @Override
    public VariableSizeListIterator<T> listIterator() {
        return listIterator(0);
    }

    @Override public int size() { return size; }

    @Override
    public VariableSizeListIterator<T> listIterator(int index) {
        VariableSizeListIterator<T> iterator = new VariableSizeListIterator<T>(dexBuf, offset, size) {
            @Nonnull
            @Override
            protected T readNextItem(@Nonnull DexReader reader, int index) {
                return VariableSizeList.this.readNextItem(reader, index);
            }
        };
        while (index++ > 0) {
            iterator.next();
        }
        return iterator;
    }
}
