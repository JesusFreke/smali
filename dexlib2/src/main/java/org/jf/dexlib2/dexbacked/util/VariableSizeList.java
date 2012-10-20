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

import org.jf.dexlib2.dexbacked.DexFile;
import org.jf.dexlib2.dexbacked.DexFileReader;
import org.jf.util.AbstractListIterator;

import javax.annotation.Nonnull;
import java.util.AbstractSequentialList;
import java.util.NoSuchElementException;

/**
 * Provides a base class for a list that is backed by variable size items in a dex file
 * @param <T> The type of the item that this list contains
 */
public abstract class VariableSizeList<T> extends AbstractSequentialList<T> {
    @Nonnull private final DexFile dexFile;
    private final int offset;

    public VariableSizeList(@Nonnull DexFile dexFile, int offset) {
        this.dexFile = dexFile;
        this.offset = offset;
    }

    @Nonnull
    protected abstract T readItem(DexFileReader dexFileReader, int index);

    protected void skipItem(DexFileReader dexFileReader, int index) {
        readItem(dexFileReader, index);
    }

    @Nonnull
    @Override
    public Iterator listIterator(int startIndex) {
        Iterator iterator = listIterator();
        if (startIndex < 0 || startIndex >= size()) {
            throw new IndexOutOfBoundsException();
        }
        for (int i=0; i<startIndex; i++) {
            iterator.skip();
        }
        return iterator;
    }

    @Nonnull
    @Override
    public Iterator listIterator() {
        return new Iterator();
    }

    public class Iterator extends AbstractListIterator<T> {
        private int index = 0;
        @Nonnull private final DexFileReader dexFileReader = dexFile.readerAt(offset);

        @Override public boolean hasNext() { return index < size(); }
        @Override public int nextIndex() { return index; }

        @Nonnull
        @Override
        public T next() {
            if (index >= size()) {
                throw new NoSuchElementException();
            }
            return readItem(dexFileReader, index++);
        }

        public void skip() {
            if (index >= size()) {
                throw new NoSuchElementException();
            }
            skipItem(dexFileReader, index++);
        }
    }
}
