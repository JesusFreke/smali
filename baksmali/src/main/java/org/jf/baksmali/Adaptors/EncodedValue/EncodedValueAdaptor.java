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
 * INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.baksmali.Adaptors.EncodedValue;

import org.jf.dexlib.EncodedValue.*;
import org.jf.dexlib.TypeIdItem;
import org.jf.dexlib.StringIdItem;

public abstract class EncodedValueAdaptor {
    public static EncodedValueAdaptor make(EncodedValue encodedValue) {
        switch (encodedValue.getValueType()) {
            case VALUE_ANNOTATION:
                return null;
            case VALUE_ARRAY:
                return null;
            case VALUE_BOOLEAN:
                return new SimpleEncodedValueAdaptor(((BoolEncodedValueSubField)encodedValue.getValue()).getValue());                
            case VALUE_BYTE:
                return new SimpleEncodedValueAdaptor(((ByteEncodedValueSubField)encodedValue.getValue()).getValue());
            case VALUE_CHAR:
                return new SimpleEncodedValueAdaptor(((CharEncodedValueSubField)encodedValue.getValue()).getValue());
            case VALUE_DOUBLE:
                return new SimpleEncodedValueAdaptor(((DoubleEncodedValueSubField)encodedValue.getValue()).getValue());
            case VALUE_ENUM:
                return null;
            case VALUE_FIELD:
                return null;
            case VALUE_FLOAT:
                return new SimpleEncodedValueAdaptor(((FloatEncodedValueSubField)encodedValue.getValue()).getValue());
            case VALUE_INT:
                return new SimpleEncodedValueAdaptor(((IntEncodedValueSubField)encodedValue.getValue()).getValue());
            case VALUE_LONG:
                return new SimpleEncodedValueAdaptor(((LongEncodedValueSubField)encodedValue.getValue()).getValue());
            case VALUE_METHOD:
                return null;
            case VALUE_NULL:
                return null;
            case VALUE_SHORT:
                return new SimpleEncodedValueAdaptor(((ShortEncodedValueSubField)encodedValue.getValue()).getValue());
            case VALUE_STRING:
                return new StringEncodedValueAdaptor(((EncodedIndexedItemReference<StringIdItem>)encodedValue.getValue()).getValue());
            case VALUE_TYPE:
                return new TypeEncodedValueAdaptor(((EncodedIndexedItemReference<TypeIdItem>)encodedValue.getValue()).getValue());
        }
        return null;
    }

    public abstract String getFormat();

    public abstract Object getValue();
}
