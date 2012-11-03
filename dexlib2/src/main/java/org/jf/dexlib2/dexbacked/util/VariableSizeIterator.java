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
import javax.annotation.Nullable;
import java.util.Iterator;

public abstract class VariableSizeIterator<T> implements Iterator<T> {
    private final DexReader reader;

    private int index = 0;
    private T cachedItem = null;

    protected VariableSizeIterator(DexBuffer dexBuf, int offset) {
        this.reader = dexBuf.readerAt(offset);
        cachedItem = readItem(reader, index++);
    }

    /**
     * Reads the next item from reader. If the end of the list has been reached, it should return null.
     *
     * @param reader The {@code DexReader} to read from
     * @param index The index of the item that is being read
     * @return The item that was read, or null if the end of the list has been reached.
     */
    @Nullable protected abstract T readItem(@Nonnull DexReader reader, int index);

    @Override
    public boolean hasNext() {
        return cachedItem != null;
    }

    @Override
    public T next() {
        T ret = cachedItem;
        cachedItem = readItem(reader, index++);
        return ret;
    }

    @Override public void remove() { throw new UnsupportedOperationException(); }
}
