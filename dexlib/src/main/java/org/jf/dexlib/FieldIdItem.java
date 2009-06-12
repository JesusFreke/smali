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

public class FieldIdItem extends IndexedItem<FieldIdItem> {
    private final IndexedItemReference<TypeIdItem> classTypeReferenceField;
    private final IndexedItemReference<TypeIdItem> fieldTypeReferenceField;
    private final IndexedItemReference<StringIdItem> fieldNameReferenceField;

    public FieldIdItem(DexFile dexFile, int index) {
        super(index);
        fields = new Field[] {
                classTypeReferenceField = new IndexedItemReference<TypeIdItem>(dexFile.TypeIdsSection,
                        new ShortIntegerField(null), "class_idx"),
                fieldTypeReferenceField = new IndexedItemReference<TypeIdItem>(dexFile.TypeIdsSection,
                        new ShortIntegerField(null), "type_idx"),
                fieldNameReferenceField = new IndexedItemReference<StringIdItem>(dexFile.StringIdsSection,
                        new IntegerField(null), "name_idx")
        };
    }

    public FieldIdItem(DexFile dexFile, TypeIdItem classType, StringIdItem fieldName, TypeIdItem fieldType) {
        this(dexFile, -1);
        classTypeReferenceField.setReference(classType);
        fieldTypeReferenceField.setReference(fieldType);
        fieldNameReferenceField.setReference(fieldName);
    }

    public FieldIdItem(DexFile dexFile, TypeIdItem classType, String fieldName, TypeIdItem fieldType) {
        this(dexFile, classType, new StringIdItem(dexFile, fieldName), fieldType);
    }

    protected int getAlignment() {
        return 4;
    }

    public ItemType getItemType() {
        return ItemType.TYPE_FIELD_ID_ITEM;
    }

    public StringIdItem getFieldName() {
        return fieldNameReferenceField.getReference();
    }

    public String getConciseIdentity() {
        String parentClass = classTypeReferenceField.getReference().getTypeDescriptor();
        //strip off the leading L and trailing ;
        parentClass = parentClass.substring(1, parentClass.length() - 1);

        return parentClass + "/" + fieldNameReferenceField.getReference().getStringValue() +
                ":" + fieldTypeReferenceField.getReference().getTypeDescriptor();
    }

    public int compareTo(FieldIdItem o) {
        int result = classTypeReferenceField.compareTo(o.classTypeReferenceField);
        if (result != 0) {
            return result;
        }

        result = fieldNameReferenceField.compareTo(o.fieldNameReferenceField);
        if (result != 0) {
            return result;
        }

        return fieldTypeReferenceField.compareTo(o.fieldTypeReferenceField);

    }

    public TypeIdItem getFieldType() {
        return fieldTypeReferenceField.getReference();
    }
}
