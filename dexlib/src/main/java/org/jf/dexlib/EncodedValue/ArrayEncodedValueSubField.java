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

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class ArrayEncodedValueSubField extends CompositeField<ArrayEncodedValueSubField>
        implements  EncodedValueSubField<ArrayEncodedValueSubField>, Comparable<EncodedValueSubField>
{

    private final ArrayList<EncodedValue> encodedValues;

    public ArrayEncodedValueSubField(final DexFile dexFile) {
        super("encoded_array");
        
        encodedValues = new ArrayList<EncodedValue>();
        fields = new Field[] {
                new ListSizeField(encodedValues, new Leb128Field("size")),
                new FieldListField<EncodedValue>(encodedValues, "values") {
                    protected EncodedValue make() {
                        return new EncodedValue(dexFile);
                    }
                }
        };
    }

    public ArrayEncodedValueSubField(final DexFile dexFile, ArrayList<EncodedValue> encodedValues) {
        super("encoded_array");
        
        this.encodedValues = encodedValues;

        fields = new Field[] {
                new ListSizeField(this.encodedValues, new Leb128Field("size")),
                new FieldListField<EncodedValue>(encodedValues, "values") {
                    protected EncodedValue make() {
                        return new EncodedValue(dexFile);
                    }
                }
        };
    }

    public void setInitialValueArg(byte valueArg) {
        //valueArg is ignored for arrays
    }

    public byte getValueArg() {
        return 0;
    }

    public ValueType getValueType() {
        return ValueType.VALUE_ARRAY;
    }

    public void add(int index, EncodedValue encodedValue) {
        encodedValues.add(index, encodedValue);
    }

    public List<EncodedValue> getValues() {
        return Collections.unmodifiableList(encodedValues);
    }

    public int compareTo(EncodedValueSubField t) {
        int comp = getValueType().compareTo(t.getValueType());
        if (comp == 0) {
            ArrayEncodedValueSubField other = (ArrayEncodedValueSubField)t;

            comp = ((Integer)encodedValues.size()).compareTo(other.encodedValues.size());
            if (comp == 0) {
                for (int i=0; i<encodedValues.size(); i++) {
                    comp = encodedValues.get(i).compareTo(other.encodedValues.get(i));
                    if (comp != 0) {
                        break;
                    }
                }
            }
        }
        return comp;
    }
}
