/*
 * [The "BSD licence"]
 * Copyright (c) 2009 Ben Gruver
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.dexlib;

import org.jf.dexlib.Util.Input;

import java.util.Collections;

public abstract class IndexedSection<T extends IndexedItem<T>> extends Section<T> {
    public IndexedSection() {
    }

    public T getByIndex(int index) {
        for (int i = items.size(); i <= index; i++) {
            items.add(null);
        }
        T item = items.get(index);
        if (item == null) {
            item = make(index);
            items.set(index, item);
        }
        return item;
    }

    public void readFrom(int size, Input in) {
        super.setSize(size);
        this.offset = in.getCursor();

        for (int i = 0; i < size(); i++) {
            T item = getByIndex(i);
            item.readFrom(in, i);
            in.alignTo(item.getAlignment());
        }
    }
    
    protected abstract T make(int index);

    public T intern(DexFile dexFile, T item) {
        T itemToReturn = getInternedItem(item);

        if (itemToReturn == null) {
            /**
             * Make a new item at the end of this section, and copy the fields
             * to the new item
             */
            itemToReturn = getByIndex(size());
            item.copyTo(dexFile, itemToReturn);
            uniqueItems.put(itemToReturn, itemToReturn);
        }

        return itemToReturn;
    }

    public int place(int offset) {
        sortSection();

        return super.place(offset);
    }
}
