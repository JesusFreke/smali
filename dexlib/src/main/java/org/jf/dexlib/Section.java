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

import org.jf.dexlib.util.AnnotatedOutput;
import org.jf.dexlib.util.Input;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class Section<T extends Item> {
    protected ArrayList<T> items;
    protected HashMap<T, T> uniqueItems = null;

    /**
     * When offset > -1, this section is "placed" at the specified offset. All
     * items should have sequential indexes, and be placed appropriately.
     *
     * To unplace the section, set offset to -1, and set the offset of all
     * items to -1 
     */
    protected int offset = -1;

    public Section() {
        items = new ArrayList<T>();
    }

    public int place(int offset) {
        this.offset = offset;
        for (int i=0; i < items.size(); i++) {
            T item = items.get(i);
            if (item == null) {
                throw new RuntimeException("This section contains a null item");
            }
            offset = item.place(i, offset);
            if (i == 0) {
                /**
                 * if this item type has an alignment requirement,
                 * then item.getOffset() may be different than the
                 * offset that was passed in to this method, so we have
                 * to initialize the section offset to the actual
                 * (post-alignment) offset of the first item
                 */
                this.offset = item.getOffset();
            }
        }

        return offset;
    }

    public void unplace() {
        for (Item item: items) {
            item.unplace();
        }
    }

    public void writeTo(AnnotatedOutput out) {
        for (int i = 0; i < size(); i++) {
            T item = items.get(i);
            if (item == null) {
                throw new RuntimeException("Cannot write section because all items haven't been initialized");
            }
            item.writeTo(out);
            out.annotate(0, " ");
        }
        out.annotate(0, " ");
    }

    public abstract void readFrom(int size, Input in);

    protected void setSize(int size) {
        if (items.size() > size) {
            throw new RuntimeException("There are references elsewhere to items in this section, that are " +
                    "beyond the end of the section");
        }

        items.ensureCapacity(size);
        for (int i = items.size(); i < size; i++) {
            items.add(null);
        }
    }
    
    public int size() {
        return items.size();
    }

    public boolean isPlaced() {
        return offset > -1;
    }

    public int getOffset() {
        return offset;
    }

    protected T getInternedItem(T item) {
        if (uniqueItems == null) {
            buildInternedItemMap();
        }
        return uniqueItems.get(item);
    }

    protected void buildInternedItemMap() {
        uniqueItems = new HashMap<T,T>();
        for (T item: items) {
            uniqueItems.put(item, item);
        }
    }

    public abstract T intern(DexFile dexFile, T item);
}
