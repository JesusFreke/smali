/*
 * Copyright 2013, Google Inc.
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

package org.jf.dexlib2.writer.pool;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.value.EncodedValue;
import org.jf.dexlib2.immutable.value.ImmutableEncodedValueFactory;
import org.jf.dexlib2.util.EncodedValueUtils;
import org.jf.dexlib2.writer.pool.EncodedArrayPool.Key;
import org.jf.dexlib2.writer.EncodedArraySection;
import org.jf.util.CollectionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class EncodedArrayPool extends BaseNullableOffsetPool<Key>
        implements EncodedArraySection<Key, EncodedValue> {
    @Nonnull private final StringPool stringPool;
    @Nonnull private final TypePool typePool;
    @Nonnull private final FieldPool fieldPool;
    @Nonnull private final MethodPool methodPool;

    public EncodedArrayPool(@Nonnull StringPool stringPool,
                            @Nonnull TypePool typePool,
                            @Nonnull FieldPool fieldPool,
                            @Nonnull MethodPool methodPool) {
        this.stringPool = stringPool;
        this.typePool = typePool;
        this.fieldPool = fieldPool;
        this.methodPool = methodPool;
    }

    public void intern(@Nonnull ClassDef classDef) {
        Key key = Key.of(classDef);
        if (key != null) {
            Integer prev = internedItems.put(key, 0);
            if (prev == null) {
                for (EncodedValue encodedValue: key) {
                    DexPool.internEncodedValue(encodedValue, stringPool, typePool, fieldPool, methodPool);
                }
            }
        }
    }

    @Nonnull @Override public Collection<EncodedValue> getElements(@Nonnull Key key) {
        return key;
    }

    public static class Key extends AbstractCollection<EncodedValue> implements Comparable<Key> {
        private final List<? extends Field> fields;
        private final int size;

        private static final Function<Field, EncodedValue> GET_INITIAL_VALUE =
                new Function<Field, EncodedValue>() {
                    @Override
                    public EncodedValue apply(Field input) {
                        EncodedValue initialValue = input.getInitialValue();
                        if (initialValue == null) {
                            return ImmutableEncodedValueFactory.defaultValueForType(input.getType());
                        }
                        return initialValue;
                    }
                };

        private Key(@Nonnull List<? extends Field> fields, int size) {
            this.fields = fields;
            this.size = size;
        }

        @Nullable
        public static Key of(@Nonnull ClassDef classDef) {
            List<? extends Field> staticFieldsSorted = Ordering.natural().immutableSortedCopy(
                    classDef.getStaticFields());

            int lastIndex = CollectionUtils.lastIndexOf(staticFieldsSorted, HAS_INITIALIZER);
            if (lastIndex > -1) {
                return new Key(staticFieldsSorted, lastIndex+1);
            }
            return null;
        }

        @Override
        public int hashCode() {
            return CollectionUtils.listHashCode(this);
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Key) {
                Key other = (Key)o;
                if (size != other.size) {
                    return false;
                }
                return Iterables.elementsEqual(this, other);
            }
            return false;
        }

        private static final Predicate<Field> HAS_INITIALIZER = new Predicate<Field>() {
            @Override
            public boolean apply(Field input) {
                EncodedValue encodedValue = input.getInitialValue();
                return encodedValue != null && !EncodedValueUtils.isDefaultValue(encodedValue);
            }
        };

        @Override
        public int compareTo(Key o) {
            int res = Ints.compare(size, o.size);
            if (res != 0) {
                return res;
            }
            Iterator<EncodedValue> otherElements = o.iterator();
            for (EncodedValue element: this) {
                res = element.compareTo(otherElements.next());
                if (res != 0) {
                    return res;
                }
            }
            return 0;
        }

        @Nonnull @Override public Iterator<EncodedValue> iterator() {
            return FluentIterable.from(fields)
                    .limit(size)
                    .transform(GET_INITIAL_VALUE).iterator();
        }

        @Override public int size() {
            return size;
        }
    }
}
