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

package org.jf.util;

import com.google.common.collect.ImmutableSortedSet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;

public abstract class ImmutableSortedSetConverter<ImmutableItem, Item> {

    @Nonnull
    public ImmutableSortedSet<ImmutableItem> convert(@Nonnull Comparator<? super ImmutableItem> comparator,
                                                     @Nullable final Iterable<? extends Item> iterable) {
        if (iterable == null) {
            return ImmutableSortedSet.of();
        }

        boolean needsCopy = false;
        if (iterable instanceof ImmutableSortedSet &&
                ((ImmutableSortedSet)iterable).comparator().equals(comparator)) {
            for (Item element: iterable) {
                if (!isImmutable(element)) {
                    needsCopy = true;
                    break;
                }
            }
        } else {
            needsCopy = true;
        }

        if (!needsCopy) {
            return (ImmutableSortedSet<ImmutableItem>)iterable;
        }

        final Iterator<? extends Item> iter = iterable.iterator();


        return ImmutableSortedSet.copyOf(comparator, new Iterator<ImmutableItem>() {
            @Override public boolean hasNext() { return iter.hasNext(); }
            @Override public ImmutableItem next() { return makeImmutable(iter.next()); }
            @Override public void remove() { iter.remove(); }
        });
    }

    @Nonnull
    public SortedSet<ImmutableItem> convert(@Nonnull Comparator<? super ImmutableItem> comparator,
                                            @Nullable final SortedSet<? extends Item> sortedSet) {
        if (sortedSet == null || sortedSet.size() == 0) {
            return ImmutableSortedSet.of();
        }

        @SuppressWarnings("unchecked")
        ImmutableItem[] newItems = (ImmutableItem[])new Object[sortedSet.size()];
        int index = 0;
        for (Item item: sortedSet) {
            newItems[index++] = makeImmutable(item);
        }

        return ArraySortedSet.of(comparator, newItems);
    }

    protected abstract boolean isImmutable(@Nonnull Item item);
    @Nonnull protected abstract ImmutableItem makeImmutable(@Nonnull Item item);
}
