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

import org.jf.dexlib.util.Input;
import org.jf.dexlib.util.AnnotatedOutput;
import org.jf.dexlib.*;

public class EncodedValue extends CompositeField<EncodedValue> {
    private class ValueTypeArgField implements Field<ValueTypeArgField> {
        private ValueType valueType;
        private byte valueArg;

        public ValueTypeArgField() {
        }

        public ValueTypeArgField(ValueType valueType) {
            this.valueType = valueType;
        }

        public void writeTo(AnnotatedOutput out) {
            out.annotate(1, "valuetype=" + Integer.toString(valueType.getMapValue()) + " valueArg=" + Integer.toString(valueArg));
            byte value = (byte)(valueType.getMapValue() | (valueArg << 5));
            out.writeByte(value);
        }

        public void readFrom(Input in) {
            byte value = in.readByte();
            valueType = ValueType.fromByte((byte)(value & 0x1F));
            valueArg = (byte)((value & 0xFF) >>> 5);
        }

        public int place(int offset) {
            return offset + 1;
        }

        public ValueType getValueType() {
            return valueType;
        }

        public byte getValueArg() {
            return valueArg;
        }

        public void copyTo(DexFile dexFile, ValueTypeArgField copy) {
            copy.valueType = valueType;
            copy.valueArg = valueArg;
        }

        public int hashCode() {
            return valueType.hashCode() * 31 + ((Byte)valueArg).hashCode(); 
        }

        public boolean equals(Object o) {
            if (!(o instanceof ValueTypeArgField)) {
                return false;
            }

            ValueTypeArgField other = (ValueTypeArgField)o;
            return valueType.equals(other.valueType) && (valueArg == other.valueArg); 
        }
    }

    private class EncodedValueSubFieldWrapper implements Field<EncodedValueSubFieldWrapper> {
        private final DexFile dexFile;
        private EncodedValueSubField subField;

        public EncodedValueSubFieldWrapper(DexFile dexFile) {
            this.dexFile = dexFile;
        }

        public EncodedValueSubFieldWrapper(DexFile dexFile, EncodedValueSubField subField) {
            this.dexFile = dexFile;
            this.subField = subField;
        }

        public void writeTo(AnnotatedOutput out) {
            subField.writeTo(out);
        }

        public void readFrom(Input in) {
            subField = EncodedValueSubFieldFactory.makeEncodedValueField(dexFile, getValueType());
            subField.setInitialValueArg(getValueArg());
            subField.readFrom(in);
        }

        public int place(int offset) {
            return subField.place(offset);
        }

        public EncodedValueSubField getEncodedValueSubField() {
            return subField;
        }

        public void copyTo(DexFile dexFile, EncodedValueSubFieldWrapper copy) {
            EncodedValueSubField fieldCopy = EncodedValueSubFieldFactory.makeEncodedValueField(dexFile, getValueType());
            copy.subField = fieldCopy;

            //both fields should be the same type because they were both made with the a call to
            //EncodedValueSubFieldFactory.makeEncodedValueField using the same value type.
            subField.copyTo(dexFile, fieldCopy);
        }

        public int hashCode() {
            return subField.hashCode();
        }

        public boolean equals(Object o) {
            if (!(o instanceof EncodedValueSubFieldWrapper)) {
                return false;
            }

            EncodedValueSubFieldWrapper other = (EncodedValueSubFieldWrapper)o;
            return subField.equals(other.subField);
        }
    }

    private final ValueTypeArgField valueTypeArg;
    private final EncodedValueSubFieldWrapper encodedValue;

    public EncodedValue(final DexFile dexFile) {
        super("encoded_value");
        fields = new Field[] {
                valueTypeArg = new ValueTypeArgField(),
                encodedValue = new EncodedValueSubFieldWrapper(dexFile)
        };
    }

    public EncodedValue(final DexFile dexFile, EncodedValueSubField subField) {
        super("encoded_value");
        fields = new Field[] {
                valueTypeArg = new ValueTypeArgField(subField.getValueType()),
                encodedValue = new EncodedValueSubFieldWrapper(dexFile, subField)
        };
    }

    public int place(int offset) {
        offset = valueTypeArg.place(offset);
        int ret = encodedValue.place(offset);

        valueTypeArg.valueArg = encodedValue.getEncodedValueSubField().getValueArg();
        return ret;
    }

    public ValueType getValueType() {
        return valueTypeArg.getValueType();
    }

    public byte getValueArg() {
        return valueTypeArg.getValueArg();
    }
}
