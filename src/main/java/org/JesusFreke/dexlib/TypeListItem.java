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

import java.util.ArrayList;
import java.util.List;

public class TypeListItem extends OffsettedItem<TypeListItem> implements Comparable<TypeListItem> {
    private final Field[] fields;
    private final ArrayList<IndexedItemReference<TypeIdItem>> typeList = new ArrayList<IndexedItemReference<TypeIdItem>>();

    public TypeListItem(final DexFile dexFile, int offset) {
        super(offset);

        fields = new Field[] {
                new ListSizeField(typeList, new IntegerField()),
                new FieldListField<IndexedItemReference<TypeIdItem>>(typeList) {
                    protected IndexedItemReference<TypeIdItem> make() {
                        return new IndexedItemReference<TypeIdItem>(dexFile.TypeIdsSection, new ShortIntegerField());
                    }
                }                
        };
    }

    public TypeListItem(final DexFile dexFile, List<TypeIdItem> types) {
        this(dexFile, 0);

        for (TypeIdItem typeIdItem: types) {
            typeList.add(new IndexedItemReference<TypeIdItem>(dexFile, typeIdItem, new ShortIntegerField()));
        }
    }

    public List<TypeIdItem> getTypes() {
        ArrayList<TypeIdItem> list = new ArrayList<TypeIdItem>(typeList.size());

        for (IndexedItemReference<TypeIdItem> typeIdItemReference: typeList) {
            list.add(typeIdItemReference.getReference());
        }

        return list;
    }

    public Field[] getFields() {
        return fields;
    }

    public int getWordCount() {
        int wordCount = 0;
        for (IndexedItemReference<TypeIdItem> typeRef: typeList) {
            TypeIdItem item = typeRef.getReference();
            wordCount += item.getWordCount();
        }
        return wordCount;
    }

    public int getCount() {
        return typeList.size();
    }

    public int getAlignment() {
        return 4;
    }

    public ItemType getItemType() {
        return ItemType.TYPE_TYPE_LIST;
    }

    public int compareTo(TypeListItem o) {
        if (o == null) {
            return 1;
        }

        int thisSize = typeList.size();
        int otherSize = o.typeList.size();
        int size = Math.min(thisSize, otherSize);

        for (int i = 0; i < size; i++) {
            int result = typeList.get(i).compareTo(o.typeList.get(i));
            if (result != 0) {
                return result;
            }
        }

        if (thisSize < otherSize) {
            return -1;
        } else if (thisSize > otherSize) {
            return 1;
        } else {
            return 0;
        }
    }
}
