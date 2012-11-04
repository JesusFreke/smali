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

package org.jf.dexlib2.dexbacked.value;

import org.jf.dexlib2.ValueType;
import org.jf.dexlib2.dexbacked.DexBuffer;
import org.jf.dexlib2.dexbacked.DexReader;
import org.jf.dexlib2.dexbacked.util.VariableSizeList;
import org.jf.dexlib2.iface.value.ArrayEncodedValue;
import org.jf.dexlib2.iface.value.EncodedValue;

import javax.annotation.Nonnull;
import java.util.List;

public class DexBackedArrayEncodedValue implements ArrayEncodedValue {
    @Nonnull public final DexBuffer dexBuf;
    private final int encodedArrayOffset;

    public DexBackedArrayEncodedValue(@Nonnull DexReader reader) {
        this.dexBuf = reader.getDexBuffer();
        this.encodedArrayOffset = reader.getOffset();
        skipFrom(reader);
    }

    public static void skipFrom(@Nonnull DexReader reader) {
        int elementCount = reader.readSmallUleb128();
        for (int i=0; i<elementCount; i++) {
            DexBackedEncodedValue.skipFrom(reader);
        }
    }

    @Override public int getValueType() { return ValueType.ARRAY; }

    @Nonnull
    @Override
    public List<? extends EncodedValue> getValue() {
        DexReader reader = dexBuf.readerAt(encodedArrayOffset);
        final int size = reader.readSmallUleb128();

        return new VariableSizeList<EncodedValue>(dexBuf, reader.getOffset()) {
            @Nonnull
            @Override
            protected EncodedValue readItem(@Nonnull DexReader dexReader, int index) {
                return DexBackedEncodedValue.readFrom(dexReader);
            }

            @Override
            protected void skipItem(@Nonnull DexReader reader, int index) {
                DexBackedEncodedValue.skipFrom(reader);
            }

            @Override public int size() { return size;}
        };
    }
}
