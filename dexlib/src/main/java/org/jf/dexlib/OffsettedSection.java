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

import org.jf.dexlib.util.Input;

import java.util.HashMap;

public abstract class OffsettedSection<T extends OffsettedItem<T>> extends Section<T> {
    protected HashMap<Integer, T> itemsByOffset;    
    
    public OffsettedSection() {
        itemsByOffset = new HashMap<Integer, T>();
    }

    /**
     * Retrieves the item that starts at the given offset, or null if
     * there are none found. This method is intended to only be used
     * while reading in a dex file.
     * @param offset the offset of the item to get
     * @return the item that starts at the given offset, or null if there
     * are none found
     */
    public T getByOffset(int offset) {
        T item = itemsByOffset.get(offset);
        if (item == null) {
            item = make(offset);
            items.add(item);
            itemsByOffset.put(offset, item);
        }
        return item;
    }

    public void readFrom(int size, Input in) {        
        for (int i = 0; i < size; i++) {
            T item = getByOffset(in.getCursor());
            item.readFrom(in);

            //TODO: why are we aligning afterwards?
            in.alignTo(item.getAlignment());
        }
    }

    protected abstract T make(int offset);

    public T intern(DexFile dexFile, T item) {
        T itemToReturn = getInternedItem(item);

        if (itemToReturn == null) {
            itemToReturn = make(-1);
            items.add(itemToReturn);
            item.copyTo(dexFile, itemToReturn);
            uniqueItems.put(itemToReturn, itemToReturn);
        }

        return itemToReturn;
    }
}
