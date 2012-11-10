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

package org.jf.dexlib2.dexbacked.reference;

import com.google.common.collect.ImmutableList;
import org.jf.dexlib2.base.reference.BaseMethodReference;
import org.jf.dexlib2.dexbacked.DexBuffer;
import org.jf.dexlib2.dexbacked.util.FixedSizeList;
import org.jf.dexlib2.iface.reference.BasicMethodParameter;

import javax.annotation.Nonnull;
import java.util.List;

public class DexBackedMethodReference extends BaseMethodReference {
    @Nonnull public final DexBuffer dexBuf;
    public final int methodIdItemOffset;
    private int protoIdItemOffset;

    public DexBackedMethodReference(@Nonnull DexBuffer dexBuf, int methodIndex) {
        this.dexBuf = dexBuf;
        this.methodIdItemOffset = dexBuf.getMethodIdItemOffset(methodIndex);
    }

    @Nonnull
    @Override
    public String getContainingClass() {
        return dexBuf.getType(dexBuf.readUshort(methodIdItemOffset + DexBuffer.METHOD_CLASS_IDX_OFFSET));
    }

    @Nonnull
    @Override
    public String getName() {
        return dexBuf.getString(dexBuf.readSmallUint(methodIdItemOffset + DexBuffer.METHOD_NAME_IDX_OFFSET));
    }

    @Nonnull
    @Override
    public List<? extends BasicMethodParameter> getParameters() {
        int protoIdItemOffset = getProtoIdItemOffset();
        final int parametersOffset = dexBuf.readSmallUint(protoIdItemOffset + DexBuffer.PROTO_PARAM_LIST_OFF_OFFSET);
        if (parametersOffset > 0) {
            final int parameterCount = dexBuf.readSmallUint(parametersOffset + DexBuffer.TYPE_LIST_SIZE_OFFSET);
            final int paramListStart = parametersOffset + DexBuffer.TYPE_LIST_LIST_OFFSET;
            return new FixedSizeList<BasicMethodParameter>() {
                @Nonnull
                @Override
                public BasicMethodParameter readItem(final int index) {
                    return new BasicMethodParameter() {
                        @Nonnull
                        @Override
                        public String getType() {
                            return dexBuf.getType(dexBuf.readUshort(paramListStart + 2*index));
                        }
                    };
                }
                @Override public int size() { return parameterCount; }
            };
        }
        return ImmutableList.of();
    }

    @Nonnull
    @Override
    public String getReturnType() {
        int protoIdItemOffset = getProtoIdItemOffset();
        return dexBuf.getType(dexBuf.readSmallUint(protoIdItemOffset + DexBuffer.PROTO_RETURN_TYPE_IDX_OFFSET));
    }

    private int getProtoIdItemOffset() {
        if (protoIdItemOffset == 0) {
            protoIdItemOffset = dexBuf.getProtoIdItemOffset(
                    dexBuf.readUshort(methodIdItemOffset + DexBuffer.METHOD_PROTO_IDX_OFFSET));
        }
        return protoIdItemOffset;
    }
}
