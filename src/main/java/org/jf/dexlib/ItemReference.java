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

public abstract class ItemReference<T extends Item<T>, S extends ItemReference<T,S>> implements Field<S> {
    private T item = null;
    private Section<T> section;
    private final CachedIntegerValueField underlyingField;
    private final String fieldName;

    public ItemReference(Section<T> section, CachedIntegerValueField underlyingField, String fieldName) {
        this.section = section;
        this.underlyingField = underlyingField;
        this.fieldName = fieldName;
    }

    public ItemReference(DexFile dexFile, T item, CachedIntegerValueField underlyingField, String fieldName) {
        if (item != null) {
            section = dexFile.getSectionForItem(item);
            this.item = item;
        }
        this.underlyingField = underlyingField;
        this.fieldName = fieldName;
    }

    public T getReference() {
        return item;
    }

    public void setReference(T item) {
        this.item = item;
    }

    public Section<T> getSection() {
        return section;
    }

    public void copyTo(DexFile dexFile, S copy) {
        T referencedItem = getReference();
        if (referencedItem == null) {
            return;
        }
        Section<T> section = copy.getSection();
        T copiedItem = section.intern(dexFile, referencedItem);
        copy.setReference(copiedItem);
    }

    public void writeTo(AnnotatedOutput out) {
        T item = getReference();

        if (item != null) {
            if (!item.isPlaced()) {
                throw new RuntimeException("Trying to write reference to an item that hasn't been placed.");
            }
        }

        //in some cases, we have to re-cache the value here, because the exact offset wasn't known yet
        //during placement. Luckily, all variable sized reference offsets (Leb128) are known at placement,
        //so this won't change the size of anything
        underlyingField.cacheValue(getReferenceValue());

        String annotation = fieldName + ": 0x" + Integer.toHexString(underlyingField.getCachedValue());
        if (item != null) {
            annotation += " (" + item.getConciseIdentity() + ")";
        }

        out.annotate(annotation);
        underlyingField.writeTo(out);
    }

    public void readFrom(Input in) {
        underlyingField.readFrom(in);
        setReference(getReferencedItem(underlyingField.getCachedValue()));
    }

    public int hashCode() {
        if (item == null) {
            return 0;
        }
        return item.hashCode();
    }

    public boolean equals(Object o) {
        if (!(o instanceof ItemReference)) {
            return false;
        }

        ItemReference other = (ItemReference)o;
        if (item != null) {
            return item.equals(other.item);
        } else {
            return other.item == null;
        }
    }

    public int place(int offset) {
        /**
         * We need to cache the reference value so that the underlying field can calculate its length.
         * Sometimes this value isn't correct, but the order that items are placed is such that
         * the value is correct for any references using a variable sized field (i.e. leb128) to
         * store the reference. For non-variable sized fields, it doesn't matter if the value is
         * correct or not. We'll get the correct value for these in writeTo(), just before it is
         * written
         */
        underlyingField.cacheValue(getReferenceValue());
        return underlyingField.place(offset);
    }

    protected abstract int getReferenceValue();
    protected abstract T getReferencedItem(int referenceValue);
}
