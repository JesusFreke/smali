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

import java.util.ArrayList;

public class ProtoIdItem extends IndexedItem<ProtoIdItem> {
    private final IndexedItemReference<StringIdItem> shortyDescriptorReferenceField;
    private final IndexedItemReference<TypeIdItem> returnTypeReferenceField;
    private final OffsettedItemReference<TypeListItem> parametersReferenceField;

    public ProtoIdItem(DexFile dexFile, int index) {
        super(index);
        fields = new Field[] {
                shortyDescriptorReferenceField = new IndexedItemReference<StringIdItem>(dexFile.StringIdsSection,
                        new IntegerField(null), "shorty_idx"),
                returnTypeReferenceField = new IndexedItemReference<TypeIdItem>(dexFile.TypeIdsSection,
                        new IntegerField(null), "return_type_idx"),
                parametersReferenceField = new OffsettedItemReference<TypeListItem>(dexFile.TypeListsSection,
                        new IntegerField(null), "parameters_off")
        };
    }

    public ProtoIdItem(DexFile dexFile, TypeIdItem returnType, ArrayList<TypeIdItem> parameters)
    {
        this(dexFile, -1);
        shortyDescriptorReferenceField.setReference(
            new StringIdItem(dexFile, createShortyDescriptor(returnType, parameters)));
        returnTypeReferenceField.setReference(returnType);
        if (parameters != null && parameters.size() > 0) {
            parametersReferenceField.setReference(new TypeListItem(dexFile, parameters));
        }
    }

    private String createShortyDescriptor(TypeIdItem returnType, ArrayList<TypeIdItem> parameters) {
        StringBuilder sb = new StringBuilder();
        sb.append(returnType.toShorty());

        if (parameters != null) {
            for (TypeIdItem typeIdItem: parameters) {
                sb.append(typeIdItem.toShorty());
            }
        }
        return sb.toString();
    }

    protected int getAlignment() {
        return 4;
    }

    public int getParameterRegisterCount() {
        TypeListItem typeList = parametersReferenceField.getReference();
        if (typeList == null) {
            return 0;
        } else {
            return typeList.getRegisterCount();
        }
    }

    public int getParameterCount() {
        TypeListItem typeList = parametersReferenceField.getReference();
        if (typeList == null) {
            return 0;
        } else {
            return typeList.getCount();
        }
    }

    public ItemType getItemType() {
        return ItemType.TYPE_PROTO_ID_ITEM;
    }

    public int compareTo(ProtoIdItem o) {
        int result = returnTypeReferenceField.compareTo(o.returnTypeReferenceField);
        if (result != 0) {
            return result;
        }

        TypeListItem thisParameters = parametersReferenceField.getReference();
        if (thisParameters == null) {
            return -1;
        }

        return thisParameters.compareTo(o.parametersReferenceField.getReference());
    }

    public String getConciseIdentity() {
        return "proto_id_item: " + getPrototypeString();
    }

    private String cachedPrototypeString = null;
    public String getPrototypeString() {
        if (cachedPrototypeString == null) {
            StringBuilder sb = new StringBuilder();

            TypeListItem parameterList = this.parametersReferenceField.getReference();

            if (parameterList != null) {
                for (TypeIdItem type: parameterList.getTypes()) {
                    sb.append(type.getTypeDescriptor());
                }
            }

            cachedPrototypeString = "(" + sb.toString() + ")" +
                    this.returnTypeReferenceField.getReference().getTypeDescriptor();
        }
        return cachedPrototypeString;
    }
}
