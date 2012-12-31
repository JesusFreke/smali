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

package org.jf.dexlib2.writer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.util.CollectionUtils;
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ProtoPool {
    public final static int PROTO_ID_ITEM_SIZE = 0x0C;

    @Nonnull private final Map<Key, Integer> internedProtoIdItems = Maps.newHashMap();
    @Nonnull private final DexFile dexFile;
    private int sectionOffset = -1;

    public ProtoPool(@Nonnull DexFile dexFile) {
        this.dexFile = dexFile;
    }

    public void intern(@Nonnull MethodReference method) {
        // We can't use method directly, because it is likely a full MethodReference. We use a wrapper that computes
        // hashCode and equals based only on the prototype fields
        Key key = new Key(method);
        Integer prev = internedProtoIdItems.put(key, 0);
        if (prev == null) {
            dexFile.stringPool.intern(key.getShorty());
            dexFile.typePool.intern(method.getReturnType());
            dexFile.typeListPool.intern(method.getParameterTypes());
        }
    }

    public int getIndex(@Nonnull MethodReference method) {
        Key key = new Key(method);
        Integer index = internedProtoIdItems.get(key);
        if (index == null) {
            throw new ExceptionWithContext("Prototype not found.: %s", key);
        }
        return index;
    }

    public int getIndexedSectionSize() {
        return internedProtoIdItems.size() * PROTO_ID_ITEM_SIZE;
    }

    public int getNumItems() {
        return internedProtoIdItems.size();
    }

    public int getSectionOffset() {
        if (sectionOffset < 0) {
            throw new ExceptionWithContext("Section offset has not been set yet!");
        }
        return sectionOffset;
    }

    public void write(@Nonnull DexWriter writer) throws IOException {
        List<Key> prototypes = Lists.newArrayList(internedProtoIdItems.keySet());
        Collections.sort(prototypes);

        sectionOffset = writer.getPosition();
        int index = 0;
        for (Key proto: prototypes) {
            internedProtoIdItems.put(proto, index++);

            writer.writeInt(dexFile.stringPool.getIndex(proto.getShorty()));
            writer.writeInt(dexFile.typePool.getIndex(proto.getReturnType()));
            writer.writeInt(dexFile.typeListPool.getOffset(proto.getParameters()));
        }
    }

    private static class Key implements Comparable<Key> {
        @Nonnull private final MethodReference method;

        public Key(@Nonnull MethodReference method) {
            this.method = method;
        }

        @Nonnull public String getReturnType() { return method.getReturnType(); }
        @Nonnull public Collection<? extends CharSequence> getParameters() {
            return method.getParameterTypes();
        }

        public String getShorty() {
            Collection<? extends CharSequence> params = getParameters();
            StringBuilder sb = new StringBuilder(params.size() + 1);
            sb.append(getShortyType(method.getReturnType()));
            for (CharSequence typeRef: params) {
                sb.append(getShortyType(typeRef));
            }
            return sb.toString();
        }

        private static char getShortyType(CharSequence type) {
            if (type.length() > 1) {
                return 'L';
            }
            return type.charAt(0);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append('(');
            for (CharSequence paramType: getParameters()) {
                sb.append(paramType);
            }
            sb.append(')');
            sb.append(getReturnType());
            return sb.toString();
        }

        @Override
        public int hashCode() {
            int hashCode = getReturnType().hashCode();
            return hashCode*31 + getParameters().hashCode();
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (o instanceof Key) {
                Key other = (Key)o;
                return getReturnType().equals(other.getReturnType()) &&
                       getParameters().equals(other.getParameters());
            }
            return false;
        }

        @Override
        public int compareTo(@Nonnull Key o) {
            int res = getReturnType().compareTo(o.getReturnType());
            if (res != 0) return res;
            return CollectionUtils.compareAsIterable(Ordering.usingToString(), getParameters(), o.getParameters());
        }
    }
}
