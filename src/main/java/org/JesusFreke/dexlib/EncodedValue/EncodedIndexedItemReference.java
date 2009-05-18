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

package org.JesusFreke.dexlib.EncodedValue;

import org.JesusFreke.dexlib.*;
import org.JesusFreke.dexlib.util.Output;
import org.JesusFreke.dexlib.util.Input;
import org.JesusFreke.dexlib.util.EncodedValueUtils;

/** TODO: it should be possible to somehow use the IndexedItemReference class */
public class EncodedIndexedItemReference<T extends IndexedItem<T>>
    extends ItemReference<T, EncodedIndexedItemReference<T>>
    implements EncodedValueSubField<EncodedIndexedItemReference<T>> {
    private int initialValueArg;
    private ValueType valueType;

    public EncodedIndexedItemReference(IndexedSection<T> section, ValueType valueType) {
        super(section);
        this.valueType = valueType;
    }

    //TODO: implement support for enum values
    public EncodedIndexedItemReference(DexFile dexFile, T item) {
        super(dexFile, item);
        if (item.getClass() == StringIdItem.class) {
            valueType = ValueType.VALUE_STRING;
        } else if (item.getClass() == TypeIdItem.class) {
            valueType = ValueType.VALUE_TYPE;
        } else if (item.getClass() == FieldIdItem.class) {
            valueType = ValueType.VALUE_FIELD;
        } else if (item.getClass() ==  MethodIdItem.class) {
            valueType = ValueType.VALUE_METHOD;
        }
    }

    public void writeTo(Output out) {
        T item = getReference();
        if (!item.isPlaced()) {
            throw new RuntimeException("Trying to write a reference to an item that hasn't been placed.");
        }
        out.write(EncodedValueUtils.encodeUnsignedIntegralValue(item.getIndex()));
    }

    public void readFrom(Input in) {
        setReference(((IndexedSection<T>)getSection()).getByIndex(
                (int)EncodedValueUtils.decodeUnsignedIntegralValue(in.readBytes(initialValueArg + 1))));
    }

    public int place(int offset) {
        T item = getReference();
        if (!item.isPlaced()) {
            throw new RuntimeException("Trying to place a reference to an item that hasn't been placed.");
        }
        return offset + EncodedValueUtils.getRequiredBytesForUnsignedIntegralValue(item.getIndex()); 
    }

    public T getValue() {
        return getReference();
    }

    public void setInitialValueArg(byte valueArg)
    {
        initialValueArg = valueArg;
    }

    public byte getValueArg() {
        return EncodedValueUtils.getRequiredBytesForUnsignedIntegralValue(getReference().getIndex());
    }

    public ValueType getValueType() {
        return valueType;
    }

}
