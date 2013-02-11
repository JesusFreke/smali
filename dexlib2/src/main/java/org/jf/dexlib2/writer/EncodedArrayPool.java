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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Ints;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.value.EncodedValue;
import org.jf.dexlib2.immutable.value.ImmutableEncodedValueFactory;
import org.jf.dexlib2.util.EncodedValueUtils;
import org.jf.dexlib2.util.FieldUtil;
import org.jf.util.CollectionUtils;
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;

public class EncodedArrayPool {
    @Nonnull private final Map<Key, Integer> internedEncodedArrayItems = Maps.newHashMap();
    @Nonnull private final DexFile dexFile;
    private int sectionOffset = -1;

    public EncodedArrayPool(DexFile dexFile) {
        this.dexFile = dexFile;
    }

    public void intern(@Nonnull ClassDef classDef) {
        Key key = Key.of(classDef);
        if (key != null) {
            Integer prev = internedEncodedArrayItems.put(key, 0);
            if (prev == null) {
                for (EncodedValue encodedValue: key.getElements()) {
                    dexFile.internEncodedValue(encodedValue);
                }
            }
        }
    }

    public int getOffset(@Nonnull ClassDef classDef) {
        Key key = Key.of(classDef);
        if (key != null) {
            Integer offset = internedEncodedArrayItems.get(key);
            if (offset == null) {
                throw new ExceptionWithContext("Encoded array not found.");
            }
            return offset;
        }
        return 0;
    }

    public int getNumItems() {
        return internedEncodedArrayItems.size();
    }

    public int getSectionOffset() {
        if (sectionOffset < 0) {
            throw new ExceptionWithContext("Section offset has not been set yet!");
        }
        return sectionOffset;
    }

    public void write(@Nonnull DexWriter writer) throws IOException {
        List<Key> encodedArrays = Lists.newArrayList(internedEncodedArrayItems.keySet());
        Collections.sort(encodedArrays);

        sectionOffset = writer.getPosition();
        for (Key encodedArray: encodedArrays) {
            internedEncodedArrayItems.put(encodedArray, writer.getPosition());
            writer.writeUleb128(encodedArray.getElementCount());
            for (EncodedValue value: encodedArray.getElements()) {
                dexFile.writeEncodedValue(writer, value);
            }
        }
    }

    public static class Key implements Comparable<Key> {
        private final Set<? extends Field> fields;
        private final int size;
        
        private static class FieldComparator implements Comparator<Field> {
			@Override
			public int compare(Field o1, Field o2) {
				return o1.compareTo(o2);
			}
        }

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

        private Key(@Nonnull Set<? extends Field> fields, int size) {
            this.fields = fields;
            this.size = size;
        }

        @Nullable
        public static Key of(@Nonnull ClassDef classDef) {
        	Set<? extends Field> fields = FluentIterable.from(classDef.getFields()).toImmutableSortedSet(new FieldComparator());

            Iterable<? extends Field> staticFields = FluentIterable.from(fields).filter(IS_STATIC_FIELD);
            int lastIndex = CollectionUtils.lastIndexOf(staticFields, HAS_INITIALIZER);

            if (lastIndex > -1) {
                return new Key(fields, lastIndex+1);
            }
            return null;
        }

        public int getElementCount() {
            return size;
        }

        @Nonnull
        public Iterable<EncodedValue> getElements() {
            return FluentIterable.from(fields)
                    .filter(IS_STATIC_FIELD)
                    .limit(size)
                    .transform(GET_INITIAL_VALUE);
        }

        @Override
        public int hashCode() {
            return CollectionUtils.listHashCode(getElements());
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Key) {
                Key other = (Key)o;
                if (size != other.size) {
                    return false;
                }
                return Iterables.elementsEqual(getElements(), other.getElements());
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

        private static final Predicate<Field> IS_STATIC_FIELD = new Predicate<Field>() {
            @Override
            public boolean apply(Field input) {
                return FieldUtil.isStatic(input);
            }
        };

        @Override
        public int compareTo(Key o) {
            int res = Ints.compare(size, o.size);
            if (res != 0) {
                return res;
            }
            Iterator<EncodedValue> otherElements = o.getElements().iterator();
            for (EncodedValue element: getElements()) {
                res = element.compareTo(otherElements.next());
                if (res != 0) {
                    return res;
                }
            }
            return 0;
        }
    }
}
