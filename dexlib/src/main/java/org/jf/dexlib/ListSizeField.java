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

public class ListSizeField implements Field<ListSizeField> {
    private final ArrayList<?> list;
    private final CachedIntegerValueField underlyingField;

    public ListSizeField(ArrayList list, CachedIntegerValueField underlyingField) {
        this.list = list;
        this.underlyingField = underlyingField;
    }

    public void writeTo(AnnotatedOutput out) {
        underlyingField.writeTo(out);
    }

    public void readFrom(Input in) {
        underlyingField.readFrom(in);
        /**
         * the absolute value operation is needed for the case when a list size is
         * encoded as the absolute value of a signed integer 
         */
        int listSize = Math.abs(underlyingField.getCachedValue());

        list.clear();
        list.ensureCapacity(listSize);
        for (int i = 0; i < listSize; i++) {
            list.add(null);
        }
    }

    public int place(int offset) {
        underlyingField.cacheValue(list.size());
        return underlyingField.place(offset);
    }

    public void copyTo(DexFile dexFile, ListSizeField copy) {
        //nothing to do, the value is retrieved from the list
    }

    public int getCachedValue() {
        return underlyingField.getCachedValue();
    }

    public void cacheValue(int value) {
        underlyingField.cacheValue(value);
    }

    public int hashCode() {
        //don't affect hash code calculations
        return 0;
    }

    public boolean equals(Object o) {
        if (!(o instanceof ListSizeField)) {
            return false;
        }

        ListSizeField other = (ListSizeField)o;

        return list.size() == other.list.size();
    }
}
