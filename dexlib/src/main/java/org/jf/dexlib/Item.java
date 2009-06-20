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

public abstract class Item<T extends Item> {
    protected int offset = -1;
    protected int index = -1;
    
    protected Field[] fields;

    protected Item() {
    }

    public boolean isPlaced() {
        return offset > -1;
    }

    public void unplace() {
        offset = -1;
    }

    public int place(int index, int offset) {
        offset = alignOffset(offset);

        this.index = index;
        this.offset = offset;

        for (Field field: fields) {
            offset = field.place(offset);
        }
        return offset;
    }

    public void readFrom(Input in, int index) {
        this.offset = in.getCursor();
        this.index = index;
        for (Field field: fields) {
            field.readFrom(in);
        }
    }

    public void writeTo(AnnotatedOutput out) {
        out.alignTo(getAlignment());

        out.annotate(0, "[0x" + Integer.toHexString(this.getIndex()) + "] " + this.getItemType().getTypeName() ); 
        out.indent();

        if (out.getCursor() != offset) {
            throw new RuntimeException("Item is being written somewhere other than where it was placed"); 
        }
        for (Field field: fields) {
            field.writeTo(out);
        }

        out.deindent();
    }

    protected int getAlignment() {
        return 1;
    }
    
    protected int alignOffset(int offset) {
        int mask = getAlignment() - 1;

        return (offset + mask) & ~mask;
    }

    public int getOffset() {
        return offset;
    }

    public int getIndex() {
        return index; 
    }

    public abstract ItemType getItemType();

    public void copyTo(DexFile dexFile, T copy) {
        for (int i = 0; i < fields.length; i++) {
            fields[i].copyTo(dexFile, copy.fields[i]);
        }
    }

    public int hashCode() {
        int h = 1;
        for (Field field: fields) {
            h = h*31 + field.hashCode();
        }
        return h;
    }

    public boolean equals(Object o) {
        if (!this.getClass().isInstance(o)) {
            return false;
        }

        Item other = (Item)o;

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

    /**
     * Returns a concise string value that conveys the identity of this item
     * @return A concise string value that conveys the identity of this item
     */
    public abstract String getConciseIdentity();

    public String toString() {
        return getConciseIdentity();
    }
}
