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
import org.jf.baksmali.Adaptors.Reference.*;

public abstract class EncodedValueAdaptor {
    public static EncodedValueAdaptor make(EncodedValue encodedValue) {
        switch (encodedValue.getValueType()) {
            case VALUE_ANNOTATION:
                return new AnnotationEncodedValueAdaptor((AnnotationEncodedValue)encodedValue);
            case VALUE_ARRAY:
                return new ArrayEncodedValueAdaptor((ArrayEncodedValue)encodedValue);
            case VALUE_BOOLEAN:
                return new SimpleEncodedValueAdaptor(((BooleanEncodedValue)encodedValue).value);
            case VALUE_BYTE:
                return new SimpleEncodedValueAdaptor(((ByteEncodedValue)encodedValue).value);
            case VALUE_CHAR:
                return new SimpleEncodedValueAdaptor(((CharEncodedValue)encodedValue).value);
            case VALUE_DOUBLE:
                return new SimpleEncodedValueAdaptor(((DoubleEncodedValue)encodedValue).value);
            case VALUE_ENUM:
                return new EnumEncodedValueAdaptor(new FieldReference(((EnumEncodedValue)encodedValue).value));
            case VALUE_FIELD:
                return new EncodedIndexedItemAdaptor(new FieldReference(((FieldEncodedValue)encodedValue).value));
            case VALUE_FLOAT:
                return new SimpleEncodedValueAdaptor(((FloatEncodedValue)encodedValue).value);
            case VALUE_INT:
                return new SimpleEncodedValueAdaptor(((IntEncodedValue)encodedValue).value);
            case VALUE_LONG:                                                                                
                return new SimpleEncodedValueAdaptor(((LongEncodedValue)encodedValue).value);
            case VALUE_METHOD:
                return new EncodedIndexedItemAdaptor(new MethodReference(((MethodEncodedValue)encodedValue).value));
            case VALUE_NULL:
                return new SimpleEncodedValueAdaptor("null");
            case VALUE_SHORT:
                return new SimpleEncodedValueAdaptor(((ShortEncodedValue)encodedValue).value);
            case VALUE_STRING:
                return new EncodedIndexedItemAdaptor(new StringReference(((StringEncodedValue)encodedValue).value));
            case VALUE_TYPE:
                return new EncodedIndexedItemAdaptor(new TypeReference(((TypeEncodedValue)encodedValue).value));
        }
        return null;
    }

    public abstract String getFormat();

    public abstract Object getValue();
}
