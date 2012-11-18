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
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;

public class TypeListPool {
    @Nonnull private final Map<Key, Integer> internedTypeListItems = Maps.newHashMap();
    @Nonnull private final DexFile dexFile;

    public TypeListPool(@Nonnull DexFile dexFile) {
        this.dexFile = dexFile;
    }

    public void intern(@Nonnull Collection<? extends CharSequence> types) {
        Key key = new Key(types);
        Integer prev = internedTypeListItems.put(key, 0);
        if (prev == null) {
            for (CharSequence type: types) {
                dexFile.typePool.intern(type);
            }
        }
    }

    public int getOffset(@Nonnull Collection<? extends CharSequence> types) {
        Key key = new Key(types);
        Integer offset = internedTypeListItems.get(key);
        if (offset == null) {
            throw new ExceptionWithContext("Type list not found.: %s", key);
        }
        return offset;
    }

    public void write(@Nonnull DexWriter writer) throws IOException {
        List<Key> typeLists = Lists.newArrayList(internedTypeListItems.keySet());
        Collections.sort(typeLists);

        for (Key typeList: typeLists) {
            writer.align();
            internedTypeListItems.put(typeList, writer.getPosition());
            Collection<? extends CharSequence> types = typeList.getTypes();
            writer.writeInt(types.size());
            for (CharSequence type: types) {
                writer.writeUshort(dexFile.typePool.getIndex(type));
            }
        }
    }

    public static class Key implements Comparable<Key> {
        @Nonnull private Collection<? extends CharSequence> types;

        public Key(@Nonnull Collection<? extends CharSequence> types) {
            this.types = types;
        }

        @Override
        public int hashCode() {
            int hashCode = 1;
            for (CharSequence type: types) {
                hashCode = hashCode*31 + type.toString().hashCode();
            }
            return hashCode;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Key) {
                Key other = (Key)o;
                if (types.size() != other.types.size()) {
                    return false;
                }
                Iterator<? extends CharSequence> otherTypes = other.types.iterator();
                for (CharSequence type: types) {
                    if (!type.toString().equals(otherTypes.next().toString())) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (CharSequence type: types) {
                sb.append(type.toString());
            }
            return sb.toString();
        }

        @Nonnull
        public Collection<? extends CharSequence> getTypes() {
            return types;
        }

        @Override
        public int compareTo(Key o) {
            Iterator<? extends CharSequence> other = o.types.iterator();
            for (CharSequence type: types) {
                if (!other.hasNext()) {
                    return 1;
                }
                int comparison = type.toString().compareTo(other.next().toString());
                if (comparison != 0) {
                    return comparison;
                }
            }
            if (other.hasNext()) {
                return -1;
            }
            return 0;
        }
    }
}
