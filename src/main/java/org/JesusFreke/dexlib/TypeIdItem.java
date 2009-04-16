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

public class TypeIdItem extends IndexedItem<TypeIdItem> {
    private final Field[] fields;

    private final IndexedItemReference<StringIdItem> type;

    public TypeIdItem(DexFile dexFile, int index) {
        super(index);
        fields = new Field[] {
                type = new IndexedItemReference<StringIdItem>(dexFile.StringIdsSection, new IntegerField())
        };
    }

    public TypeIdItem(DexFile dexFile, StringIdItem stringIdItem) {
        super(-1);
        fields = new Field[] {
                type = new IndexedItemReference<StringIdItem>(dexFile, stringIdItem, new IntegerField())
        };
    }

    public TypeIdItem(DexFile dexFile, String value) {
        super(-1);
        StringDataItem stringDataItem = new StringDataItem(value);
        StringIdItem stringIdItem = new StringIdItem(dexFile, stringDataItem);
        fields = new Field[] {
                type = new IndexedItemReference<StringIdItem>(dexFile, stringIdItem, new IntegerField())
        };
    }

    protected int getAlignment() {
        return 4;
    }

    protected Field[] getFields() {
        return fields;
    }

    public int getWordCount() {
        String type = this.toString();
        /** Only the long and double primitive types are 2 words,
         * everything else is a single word
         */
        if (type.equals("J") || type.equals("D")) {
            return 2;
        } else {
            return 1;
        }
    }

    public ItemType getItemType() {
        return ItemType.TYPE_TYPE_ID_ITEM;
    }

    public String toString() {
        return type.getReference().toString();
    }

    public int compareTo(TypeIdItem o) {
        //sort by the index of the StringIdItem
        return type.compareTo(o.type);
    }

    public String toShorty() {
        String type = toString();
        if (type.length() > 1) {
            return "L";
        } else {
            return type;
        }
    }
}
