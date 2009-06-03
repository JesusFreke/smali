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

import org.JesusFreke.dexlib.util.EncodedValueUtils;
import org.JesusFreke.dexlib.util.Input;
import org.JesusFreke.dexlib.util.AnnotatedOutput;

public class FloatEncodedValueSubField
        extends SimpleEncodedValueSubField<Float, FloatEncodedValueSubField>
{
    public FloatEncodedValueSubField() {
    }

    public FloatEncodedValueSubField(float value) {
        this.value = value;
    }
    public void writeTo(AnnotatedOutput out) {
        byte[] bytes = EncodedValueUtils.encodeRightZeroExtendedValue(((long)Float.floatToIntBits(value)) << 32);
        out.annotate(bytes.length, "FloatEncodedValueSubField");
        out.write(bytes);
    }

    public void readFrom(Input in) {
        long longValue = EncodedValueUtils.decodeRightZeroExtendedValue(in.readBytes(valueArg + 1));
        value = Float.intBitsToFloat((int)((longValue >> 32) & 0xFFFFFFFF));
    }

    public int place(int offset) {
        return offset + EncodedValueUtils.getRequiredBytesForRightZeroExtendedValue(
                ((long)Float.floatToRawIntBits(value)) << 32);
    }

    public byte getValueArg() {
        return (byte)(EncodedValueUtils.getRequiredBytesForRightZeroExtendedValue(
                ((long)Float.floatToRawIntBits(value)) << 32) - 1);
    }

    public ValueType getValueType() {
        return ValueType.VALUE_FLOAT;
    }
}
