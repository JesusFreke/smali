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
import org.jf.dexlib.util.AnnotatedOutput;

import java.util.ArrayList;

public abstract class FieldListField<T extends Field> implements Field<FieldListField<T>> {
    final ArrayList<T> list;
    private final String fieldName;

    public FieldListField(ArrayList<T> list, String fieldName) {
        this.list = list;
        this.fieldName = fieldName;
    }

    public void writeTo(AnnotatedOutput out) {
        if (list.size() > 0) {
            int i=0;
            for (Field field: list) {
                out.annotate(0, fieldName + "[" + Integer.toString(i) + "]");
                out.indent();
                field.writeTo(out);
                out.deindent();
                i++;
            }
        }
    }

    public void readFrom(Input in) {
        for (int i = 0; i < list.size(); i++) {
            T field = list.get(i);

            if (field == null) {
                field = make();
                list.set(i, field);
            }
            field.readFrom(in);
        }
    }

    protected abstract T make();    

    public int place(int offset) {
        for (Field field: list) {
            offset = field.place(offset);
        }
        return offset;
    }

    public void copyTo(DexFile dexFile, FieldListField<T> copy) {
        copy.list.clear();
        copy.list.ensureCapacity(list.size());
        for (int i = 0; i < list.size(); i++) {
            T fieldCopy = copy.make();
            list.get(i).copyTo(dexFile, fieldCopy);
            copy.list.add(fieldCopy);
        }
    }

    public int hashCode() {
        int h = 1;
        for (int i = 0; i < list.size(); i++) {
            h = h * 31 + list.get(i).hashCode();
        }
        return h;
    }

    public boolean equals(Object o) {
        if (!(o instanceof FieldListField)) {
            return false;
        }

        FieldListField other = (FieldListField)o;
        if (list.size() != other.list.size()) {
            return false;
        }

        for (int i = 0; i < list.size(); i++) {
            if (!list.get(i).equals(other.list.get(i))) {
                return false;
            }
        }
        return true;
    }
}
