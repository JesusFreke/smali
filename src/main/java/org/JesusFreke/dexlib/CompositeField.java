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

package org.JesusFreke.dexlib;

import org.JesusFreke.dexlib.util.Output;
import org.JesusFreke.dexlib.util.Input;


public abstract class CompositeField<T extends CompositeField<T>> implements Field<T> {
    /**
     * Every instance of a specific subclass should return an array with the same structure,
     * in other words have the same size, and the same type of field at each position.
     * @return An array of fields that represents the sub-fields that make up this CompositeField
     */
    protected abstract Field[] getFields();

    public void writeTo(Output out) {
        for (Field field: getFields()) {
            field.writeTo(out);
        }
    }

    public void readFrom(Input in) {
        for (Field field: getFields()) {
            field.readFrom(in);
        }
    }

    public int place(int offset) {
        for (Field field: getFields()) {
            offset = field.place(offset);
        }
        return offset;
    }

    public void copyTo(DexFile dexFile, T copy) {
        Field[] fields = getFields();
        Field[] copyFields = copy.getFields();
        for (int i = 0; i < fields.length; i++) {
            /**
             * This assumes that the fields will be the same for every instance
             * of a specific concrete subclass. By making this assumption, every subclass is
             * prevented from having to implement copyTo
             */
            fields[i].copyTo(dexFile, copyFields[i]);
        }
    }

    public int hashCode() {
        int h = 1;
        for (Field field: getFields()) {
            h = h * 31 + field.hashCode();
        }
        return h;
    }

    public boolean equals(Object o) {
        if (!(o instanceof CompositeField)) {
            return false;
        }

        CompositeField other = (CompositeField)o;
        Field[] fields = getFields();
        Field[] otherFields = other.getFields();
        if (fields.length != otherFields.length) {
            return false;
        }
        for (int i = 0; i < fields.length; i++) {
            if (!fields[i].equals(otherFields[i])) {
                return false;
            }
        }

        return true;
    }
}
