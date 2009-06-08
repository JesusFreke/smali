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

package org.jf.dexlib.EncodedValue;

import org.jf.dexlib.*;


public abstract class EncodedValueSubFieldFactory
{
    public static EncodedValueSubField makeEncodedValueField(DexFile dexFile, ValueType valueType) {
        switch (valueType) {
            case VALUE_NULL:
                return new NullEncodedValueSubField();
            case VALUE_BOOLEAN:
                return new BoolEncodedValueSubField();
            case VALUE_BYTE:
                return new ByteEncodedValueSubField();
            case VALUE_CHAR:
                return new CharEncodedValueSubField();
            case VALUE_SHORT:
                return new ShortEncodedValueSubField();
            case VALUE_INT:
                return new IntEncodedValueSubField();
            case VALUE_LONG:
                return new LongEncodedValueSubField();
            case VALUE_FLOAT:
                return new FloatEncodedValueSubField();
            case VALUE_DOUBLE:
                return new DoubleEncodedValueSubField();
            case VALUE_STRING:
                return new EncodedIndexedItemReference<StringIdItem>(dexFile.StringIdsSection, valueType);
            case VALUE_TYPE:
                return new EncodedIndexedItemReference<TypeIdItem>(dexFile.TypeIdsSection, valueType);
            case VALUE_FIELD:
                return new EncodedIndexedItemReference<FieldIdItem>(dexFile.FieldIdsSection, valueType);
            case VALUE_ENUM:
                return new EncodedIndexedItemReference<FieldIdItem>(dexFile.FieldIdsSection, valueType);
            case VALUE_METHOD:
                return new EncodedIndexedItemReference<MethodIdItem>(dexFile.MethodIdsSection, valueType);
            case VALUE_ARRAY:
                return new ArrayEncodedValueSubField(dexFile);
            case VALUE_ANNOTATION:
                return new AnnotationEncodedValueSubField(dexFile);
            default:
                throw new RuntimeException("Invalid ValueType");
        }
    }
}
