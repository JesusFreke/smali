/*
 * Copyright 2012, Google Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.dexlib2.immutable.value;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.jf.dexlib2.ValueType;
import org.jf.dexlib2.iface.value.*;
import org.jf.util.ImmutableListConverter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ImmutableEncodedValue implements EncodedValue {
    public final int type;

    protected ImmutableEncodedValue(int type) {
        this.type = type;
    }

    @Nullable
    public static ImmutableEncodedValue of(@Nullable EncodedValue encodedValue) {
        if (encodedValue == null) {
            return null;
        }
        switch (encodedValue.getValueType()) {
            case ValueType.BYTE:
                return ImmutableByteEncodedValue.of((ByteEncodedValue)encodedValue);
            case ValueType.SHORT:
                return ImmutableShortEncodedValue.of((ShortEncodedValue)encodedValue);
            case ValueType.CHAR:
                return ImmutableCharEncodedValue.of((CharEncodedValue)encodedValue);
            case ValueType.INT:
                return ImmutableIntEncodedValue.of((IntEncodedValue)encodedValue);
            case ValueType.LONG:
                return ImmutableLongEncodedValue.of((LongEncodedValue)encodedValue);
            case ValueType.FLOAT:
                return ImmutableFloatEncodedValue.of((FloatEncodedValue)encodedValue);
            case ValueType.DOUBLE:
                return ImmutableDoubleEncodedValue.of((DoubleEncodedValue)encodedValue);
            case ValueType.STRING:
                return ImmutableStringEncodedValue.of((StringEncodedValue)encodedValue);
            case ValueType.TYPE:
                return ImmutableTypeEncodedValue.of((TypeEncodedValue)encodedValue);
            case ValueType.FIELD:
                return ImmutableFieldEncodedValue.of((FieldEncodedValue)encodedValue);
            case ValueType.METHOD:
                return ImmutableMethodEncodedValue.of((MethodEncodedValue)encodedValue);
            case ValueType.ENUM:
                return ImmutableEnumEncodedValue.of((EnumEncodedValue)encodedValue);
            case ValueType.ARRAY:
                return ImmutableArrayEncodedValue.of((ArrayEncodedValue)encodedValue);
            case ValueType.ANNOTATION:
                return ImmutableAnnotationEncodedValue.of((AnnotationEncodedValue)encodedValue);
            case ValueType.NULL:
                return ImmutableNullEncodedValue.INSTANCE;
            case ValueType.BOOLEAN:
                return ImmutableBooleanEncodedValue.of((BooleanEncodedValue)encodedValue);
            default:
                Preconditions.checkArgument(false);
                return null;
        }
    }

    public int getValueType() { return type; }

    @Nonnull
    public static ImmutableList<ImmutableEncodedValue> immutableListOf(@Nullable List<? extends EncodedValue> list) {
        return CONVERTER.convert(list);
    }

    private static final ImmutableListConverter<ImmutableEncodedValue, EncodedValue> CONVERTER =
            new ImmutableListConverter<ImmutableEncodedValue, EncodedValue>() {
                @Override
                protected boolean isImmutable(EncodedValue item) {
                    return item instanceof ImmutableEncodedValue;
                }

                @Override
                protected ImmutableEncodedValue makeImmutable(EncodedValue item) {
                    return ImmutableEncodedValue.of(item);
                }
            };
}
