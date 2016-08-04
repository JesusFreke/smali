/*
 * Copyright 2016, Google Inc.
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

package org.jf.dexlib2.dexbacked.reference;

import com.google.common.collect.ImmutableList;
import org.jf.dexlib2.base.reference.BaseMethodProtoReference;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.raw.ProtoIdItem;
import org.jf.dexlib2.dexbacked.raw.TypeListItem;
import org.jf.dexlib2.dexbacked.util.FixedSizeList;

import java.util.List;
import javax.annotation.Nonnull;

public class DexBackedMethodProtoReference extends BaseMethodProtoReference {
    @Nonnull public final DexBackedDexFile dexFile;
    private final int protoIdItemOffset;

    public DexBackedMethodProtoReference(@Nonnull DexBackedDexFile dexFile, int protoIndex) {
        this.dexFile = dexFile;
        this.protoIdItemOffset = dexFile.getProtoIdItemOffset(protoIndex);
    }

    @Nonnull
    @Override
    public List<String> getParameterTypes() {
        final int parametersOffset = dexFile.readSmallUint(protoIdItemOffset + ProtoIdItem.PARAMETERS_OFFSET);
        if (parametersOffset > 0) {
            final int parameterCount = dexFile.readSmallUint(parametersOffset + TypeListItem.SIZE_OFFSET);
            final int paramListStart = parametersOffset + TypeListItem.LIST_OFFSET;
            return new FixedSizeList<String>() {
                @Nonnull
                @Override
                public String readItem(final int index) {
                    return dexFile.getType(dexFile.readUshort(paramListStart + 2*index));
                }
                @Override public int size() { return parameterCount; }
            };
        }
        return ImmutableList.of();
    }

    @Nonnull
    @Override
    public String getReturnType() {
        return dexFile.getType(dexFile.readSmallUint(protoIdItemOffset + ProtoIdItem.RETURN_TYPE_OFFSET));
    }
}
