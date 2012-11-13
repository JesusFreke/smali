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

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Iterator;

/**
 * This class converts a list of items to an immutable list of immutable items
 *
 * @param <ImmutableItem> The immutable version of the element
 * @param <Item> The normal version of the element
 */
public abstract class ImmutableListConverter<ImmutableItem, Item> {
    /**
     * Converts a {@code List} of {@code Item}s to an {@code ImmutableList} of {@code ImmutableItem}s.
     *
     * If the provided list is already an ImmutableList of ImmutableItems, then the list is not copied and is returned
     * as-is. If the list is null, an empty ImmutableList will be returned
     *
     * @param iterable The iterable of items to convert.
     * @return An ImmutableList of ImmutableItem. If list is null, an empty list will be returned.
     */
    @Nonnull
    public ImmutableList<ImmutableItem> convert(@Nullable final Iterable<? extends Item> iterable) {
        if (iterable == null) {
            return ImmutableList.of();
        }

        boolean needsCopy = false;
        if (iterable instanceof ImmutableList) {
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
            return (ImmutableList<ImmutableItem>)iterable;
        }

        final Iterator<? extends Item> iter = iterable.iterator();

        return ImmutableList.copyOf(new Iterator<ImmutableItem>() {
            @Override public boolean hasNext() { return iter.hasNext(); }
            @Override public ImmutableItem next() { return makeImmutable(iter.next()); }
            @Override public void remove() { iter.remove(); }
        });
    }

    protected abstract boolean isImmutable(@Nonnull Item item);
    @Nonnull protected abstract ImmutableItem makeImmutable(@Nonnull Item item);
}
