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

import org.JesusFreke.dexlib.ItemType;

public class FieldIdItem extends IndexedItem<FieldIdItem> {
    private final Field[] fields;

    private final IndexedItemReference<TypeIdItem> classType;
    private final IndexedItemReference<TypeIdItem> fieldType;
    private final IndexedItemReference<StringIdItem> fieldName;

    public FieldIdItem(DexFile dexFile, int index) {
        super(index);
        fields = new Field[] {
                classType = new IndexedItemReference<TypeIdItem>(dexFile.TypeIdsSection, new ShortIntegerField()),
                fieldType = new IndexedItemReference<TypeIdItem>(dexFile.TypeIdsSection, new ShortIntegerField()),
                fieldName = new IndexedItemReference<StringIdItem>(dexFile.StringIdsSection, new IntegerField())
        };
    }

    public FieldIdItem(DexFile dexFile, TypeIdItem classType, StringIdItem fieldName, TypeIdItem fieldType) {
        super(-1);
        fields = new Field[] {
                this.classType = new IndexedItemReference<TypeIdItem>(dexFile, classType, new ShortIntegerField()),
                this.fieldType = new IndexedItemReference<TypeIdItem>(dexFile, fieldType, new ShortIntegerField()),
                this.fieldName = new IndexedItemReference<StringIdItem>(dexFile, fieldName, new IntegerField())
        };
    }

    public FieldIdItem(DexFile dexFile, TypeIdItem classType, String fieldName, TypeIdItem fieldType) {
        this(dexFile, classType, new StringIdItem(dexFile, fieldName), fieldType);
    }

    protected int getAlignment() {
        return 4;
    }

    protected Field[] getFields() {
        return fields;
    }

    public ItemType getItemType() {
        return ItemType.TYPE_FIELD_ID_ITEM;
    }

    public String toString() {
        return classType.toString() + " - " + fieldName.toString();
    }

    public int compareTo(FieldIdItem o) {
        int result = classType.compareTo(o.classType);
        if (result != 0) {
            return result;
        }

        result = fieldName.compareTo(o.fieldName);
        if (result != 0) {
            return result;
        }

        return fieldType.compareTo(o.fieldType);

    }

    public TypeIdItem getFieldType() {
        return fieldType.getReference();
    }
}
