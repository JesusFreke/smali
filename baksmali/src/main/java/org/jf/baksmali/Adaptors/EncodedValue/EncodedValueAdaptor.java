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
import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;

public abstract class EncodedValueAdaptor {
    public static StringTemplate make(StringTemplateGroup stg, EncodedValue encodedValue) {
        switch (encodedValue.getValueType()) {
            case VALUE_ANNOTATION:
                return AnnotationEncodedValueAdaptor.makeTemplate(stg, (AnnotationEncodedValue)encodedValue);
            case VALUE_ARRAY:
                return ArrayEncodedValueAdaptor.makeTemplate(stg, (ArrayEncodedValue)encodedValue);
            case VALUE_BOOLEAN:
                return SimpleEncodedValueAdaptor.makeTemplate(stg, ((BooleanEncodedValue)encodedValue).value);
            case VALUE_BYTE:
                return SimpleEncodedValueAdaptor.makeTemplate(stg, ((ByteEncodedValue)encodedValue).value);
            case VALUE_CHAR:
                return SimpleEncodedValueAdaptor.makeTemplate(stg, ((CharEncodedValue)encodedValue).value);
            case VALUE_DOUBLE:
                return SimpleEncodedValueAdaptor.makeTemplate(stg, ((DoubleEncodedValue)encodedValue).value);
            case VALUE_ENUM:
                return EnumEncodedValueAdaptor.makeTemplate(stg,
                        FieldReference.makeTemplate(stg, ((EnumEncodedValue)encodedValue).value));
            case VALUE_FIELD:
                return EncodedIndexedItemAdaptor.makeTemplate(stg, FieldReference.makeTemplate(stg,
                        ((FieldEncodedValue)encodedValue).value));
            case VALUE_FLOAT:
                return SimpleEncodedValueAdaptor.makeTemplate(stg, ((FloatEncodedValue)encodedValue).value);
            case VALUE_INT:
                return SimpleEncodedValueAdaptor.makeTemplate(stg, ((IntEncodedValue)encodedValue).value);
            case VALUE_LONG:
                return SimpleEncodedValueAdaptor.makeTemplate(stg, ((LongEncodedValue)encodedValue).value);
            case VALUE_METHOD:
                return EncodedIndexedItemAdaptor.makeTemplate(stg, MethodReference.makeTemplate(stg,
                        ((MethodEncodedValue)encodedValue).value));
            case VALUE_NULL:
                return SimpleEncodedValueAdaptor.makeTemplate(stg, "null");
            case VALUE_SHORT:
                return SimpleEncodedValueAdaptor.makeTemplate(stg, ((ShortEncodedValue)encodedValue).value);
            case VALUE_STRING:
                return EncodedIndexedItemAdaptor.makeTemplate(stg, StringReference.makeTemplate(stg,
                        ((StringEncodedValue)encodedValue).value));
            case VALUE_TYPE:
                return EncodedIndexedItemAdaptor.makeTemplate(stg, TypeReference.makeTemplate(stg,
                        ((TypeEncodedValue)encodedValue).value));
        }
        return null;
    }
}
