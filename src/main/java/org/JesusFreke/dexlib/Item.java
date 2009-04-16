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

import org.JesusFreke.dexlib.util.Input;
import org.JesusFreke.dexlib.util.Output;
import org.JesusFreke.dexlib.ItemType;

public abstract class Item<T extends Item> {
    private int offset = -1;

    protected Item() {
    }

    protected Item(int offset) {
        this.offset = offset;
    }

    public boolean isPlaced() {
        return offset > -1;
    }

    public void unplace() {
        offset = -1;
    }

    public int place(int index, int offset) {
        offset = alignOffset(offset);

        //TODO: take out
        System.out.println("Placing item type " + getItemType().getTypeName() + " at offset " + ((Integer)offset).toString());

        this.offset = offset;

        Field[] fields = getFields();

        for (Field field: fields) {
            offset = field.place(offset);
        }
        return offset;
    }

    public void readFrom(Input in) {
        for (Field field: getFields()) {
            field.readFrom(in);
        }
    }

    public void writeTo(Output out) {
        out.alignTo(getAlignment());

        //TODO: take out
        System.out.println("Writing item type " + getItemType().getTypeName() + " at offset " + ((Integer)out.getCursor()).toString());

        if (out.getCursor() != offset) {
            throw new RuntimeException("Item is being written somewhere other than where it was placed"); 
        }
        for (Field field: getFields()) {
            field.writeTo(out);
        }
    }

    protected abstract int getAlignment();
    
    protected int alignOffset(int offset) {
        int mask = getAlignment() - 1;

        return (offset + mask) & ~mask;
    }

    protected int getOffset() {
        return offset;
    }

    protected abstract Field[] getFields();

    public abstract ItemType getItemType();

    public void copyTo(DexFile dexFile, T copy) {
        Field[] fields = getFields();
        Field[] fieldsCopy = copy.getFields();
        for (int i = 0; i < fields.length; i++) {
            fields[i].copyTo(dexFile, fieldsCopy[i]);
        }
    }

    public int hashCode() {
        int h = 1;
        for (Field field: getFields()) {
            h = h*31 + field.hashCode();
        }
        return h;
    }

    public boolean equals(Object o) {
        if (!(o instanceof Item)) {
            return false;
        }

        Item other = (Item)o;
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
