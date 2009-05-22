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

public class MethodIdItem extends IndexedItem<MethodIdItem> {
    private final Field[] fields;

    private final IndexedItemReference<TypeIdItem> classType;
    private final IndexedItemReference<ProtoIdItem> prototype;
    private final IndexedItemReference<StringIdItem> methodName;

    public MethodIdItem(DexFile dexFile, int index) {
        super(index);
        fields = new Field[] {
                classType = new IndexedItemReference<TypeIdItem>(dexFile.TypeIdsSection, new ShortIntegerField()),
                prototype = new IndexedItemReference<ProtoIdItem>(dexFile.ProtoIdsSection, new ShortIntegerField()),
                methodName = new IndexedItemReference<StringIdItem>(dexFile.StringIdsSection, new IntegerField())
        };
    }

    public MethodIdItem(DexFile dexFile, TypeIdItem classType, StringIdItem methodName, ProtoIdItem prototype) {
        super(-1);
        fields = new Field[] {
                this.classType = new IndexedItemReference<TypeIdItem>(dexFile, classType, new ShortIntegerField()),
                this.prototype = new IndexedItemReference<ProtoIdItem>(dexFile, prototype, new ShortIntegerField()),
                this.methodName = new IndexedItemReference<StringIdItem>(dexFile, methodName, new IntegerField())
        };
    }

    public MethodIdItem(DexFile dexFile, TypeIdItem classType, String methodName, ProtoIdItem prototype) {
        this(dexFile, classType, new StringIdItem(dexFile, methodName), prototype);        
    }


    protected int getAlignment() {
        return 4;
    }

    protected Field[] getFields() {
        return fields;
    }

    public ItemType getItemType() {
        return ItemType.TYPE_METHOD_ID_ITEM;
    }

    public TypeIdItem getClassType() {
        return classType.getReference();
    }

    public String getMethodName() {
        return methodName.getReference().toString();
    }

    public void setClassType(TypeIdItem newClassType) {
        classType.setReference(newClassType);
    }

    public String toString() {
        return classType.getReference().toString() + " - " + methodName.getReference().toString();
    }

    public int getParameterWordCount(boolean isStatic) {
        return prototype.getReference().getParameterWordCount() + (isStatic?0:1);
    }

    /**
     * Return the number of parameters, not including the "this" parameter, if any
     * @return The number of parameters, not including the "this" parameter, if any
     */
    public int getParameterCount() {
        return prototype.getReference().getParameterCount();
    }

    public int compareTo(MethodIdItem o) {
        int result = classType.compareTo(o.classType);
        if (result != 0) {
            return result;
        }

        result = methodName.compareTo(o.methodName);
        if (result != 0) {
            return result;
        }

        return prototype.compareTo(o.prototype);
    }
}
