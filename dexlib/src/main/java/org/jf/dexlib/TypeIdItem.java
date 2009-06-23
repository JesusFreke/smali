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

public class TypeIdItem extends IndexedItem<TypeIdItem> {
    private final IndexedItemReference<StringIdItem> typeDescriptorReferenceField;

    public TypeIdItem(DexFile dexFile, int index) {
        super(dexFile, index);
        fields = new Field[] {
                typeDescriptorReferenceField = new IndexedItemReference<StringIdItem>(dexFile.StringIdsSection,
                        new IntegerField(null), "descriptor_idx")
        };
    }

    public TypeIdItem(DexFile dexFile, StringIdItem stringIdItem) {
        this(dexFile, -1);
        typeDescriptorReferenceField.setReference(stringIdItem);
    }

    public TypeIdItem(DexFile dexFile, String value) {
        this(dexFile, new StringIdItem(dexFile, value));
    }

    protected int getAlignment() {
        return 4;
    }

    /**
     * Returns the number of 2-byte registers that an instance of this type requires
     * @return The number of 2-byte registers that an instance of this type requires
     */
    public int getRegisterCount() {
        String type = this.getTypeDescriptor();
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

    public String getConciseIdentity() {
        return "type_id_item: " + getTypeDescriptor();
    }

    public String getTypeDescriptor() {
        return typeDescriptorReferenceField.getReference().getStringValue();
    }

    public int compareTo(TypeIdItem o) {
        //sort by the index of the StringIdItem
        return typeDescriptorReferenceField.compareTo(o.typeDescriptorReferenceField);
    }

    public String toShorty() {
        String type = getTypeDescriptor();
        if (type.length() > 1) {
            return "L";
        } else {
            return type;
        }
    }
}
