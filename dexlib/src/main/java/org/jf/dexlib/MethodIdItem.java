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

public class MethodIdItem extends IndexedItem<MethodIdItem> {
    private final IndexedItemReference<TypeIdItem> classTypeReferenceField;
    private final IndexedItemReference<ProtoIdItem> prototypeReferenceField;
    private final IndexedItemReference<StringIdItem> methodNameReferenceField;

    public MethodIdItem(DexFile dexFile, int index) {
        super(index);
        fields = new Field[] {
                classTypeReferenceField = new IndexedItemReference<TypeIdItem>(dexFile.TypeIdsSection,
                        new ShortIntegerField(null), "class_idx"),
                prototypeReferenceField = new IndexedItemReference<ProtoIdItem>(dexFile.ProtoIdsSection,
                        new ShortIntegerField(null), "proto_idx"),
                methodNameReferenceField = new IndexedItemReference<StringIdItem>(dexFile.StringIdsSection,
                        new IntegerField(null), "name_idx")
        };
    }

    public MethodIdItem(DexFile dexFile, TypeIdItem classType, StringIdItem methodName, ProtoIdItem prototype) {
        this(dexFile, -1);
        classTypeReferenceField.setReference(classType);
        prototypeReferenceField.setReference(prototype);
        methodNameReferenceField.setReference(methodName);
    }

    protected int getAlignment() {
        return 4;
    }

    public ItemType getItemType() {
        return ItemType.TYPE_METHOD_ID_ITEM;
    }

    public String getConciseIdentity() {
        return "method_id_item: " + getMethodString();
    }

    private String cachedMethodString = null;
    public String getMethodString() {
        if (cachedMethodString == null) {
            String parentClass = classTypeReferenceField.getReference().getTypeDescriptor();
            //strip the leading L and trailing ;
            parentClass = parentClass.substring(1, parentClass.length() - 1);

            cachedMethodString = parentClass + methodNameReferenceField.getReference().getStringValue() +
                    prototypeReferenceField.getReference().getPrototypeString();
        }
        return cachedMethodString;
    }

    public ProtoIdItem getPrototype() {
        return prototypeReferenceField.getReference();
    }

    public String getMethodName() {
        return methodNameReferenceField.getReference().getStringValue();
    }

    public int getParameterRegisterCount(boolean isStatic) {
        return prototypeReferenceField.getReference().getParameterRegisterCount() + (isStatic?0:1);
    }

    public TypeIdItem getContainingClass() {
        return classTypeReferenceField.getReference();
    }

    /**
     * Return the number of parameters, not including the "this" parameter, if any
     * @return The number of parameters, not including the "this" parameter, if any
     */
    public int getParameterCount() {
        return prototypeReferenceField.getReference().getParameterCount();
    }

    public int compareTo(MethodIdItem o) {
        int result = classTypeReferenceField.compareTo(o.classTypeReferenceField);
        if (result != 0) {
            return result;
        }

        result = methodNameReferenceField.compareTo(o.methodNameReferenceField);
        if (result != 0) {
            return result;
        }

        return prototypeReferenceField.compareTo(o.prototypeReferenceField);
    }
}
