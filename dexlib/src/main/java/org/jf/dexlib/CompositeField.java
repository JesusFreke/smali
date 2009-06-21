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

import org.jf.dexlib.Util.AnnotatedOutput;
import org.jf.dexlib.Util.Input;


public abstract class CompositeField<T extends CompositeField<T>> implements Field<T> {
    protected Field[] fields;
    private final String fieldName;

    /**
     * Every instance of a specific subclass have a field array with the same structure.
     * In other words have the same size, and the same type of field at each position.
     */
    public CompositeField(String fieldName){
        this.fieldName = fieldName;
    }

    public void writeTo(AnnotatedOutput out) {
        out.annotate(0, fieldName + ":");
        out.indent();
        for (Field field: fields) {
            field.writeTo(out);
        }
        out.deindent();
    }

    public void readFrom(Input in) {
        for (Field field: fields) {
            field.readFrom(in);
        }
    }

    public int place(int offset) {
        for (Field field: fields) {
            offset = field.place(offset);
        }
        return offset;
    }

    public void copyTo(DexFile dexFile, T copy) {
        for (int i = 0; i < fields.length; i++) {
            /**
             * This assumes that the fields will be the same for every instance
             * of a specific concrete subclass. By making this assumption, every subclass is
             * prevented from having to implement copyTo
             */
            fields[i].copyTo(dexFile, copy.fields[i]);
        }
    }

    public int hashCode() {
        int h = 1;
        for (Field field: fields) {
            h = h * 31 + field.hashCode();
        }
        return h;
    }

    public boolean equals(Object o) {
        if (!(o instanceof CompositeField)) {
            return false;
        }

        CompositeField other = (CompositeField)o;
        if (fields.length != other.fields.length) {
            return false;
        }
        for (int i = 0; i < fields.length; i++) {
            if (!fields[i].equals(other.fields[i])) {
                return false;
            }
        }

        return true;
    }
}
