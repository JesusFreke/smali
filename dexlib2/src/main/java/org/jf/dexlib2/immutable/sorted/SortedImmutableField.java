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

package org.jf.dexlib2.immutable.sorted;

import com.google.common.collect.ImmutableSortedMultiset;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Multiset;
import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.base.reference.BaseFieldReference;
import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.iface.sorted.SortedField;
import org.jf.dexlib2.iface.value.EncodedValue;
import org.jf.dexlib2.immutable.sorted.value.SortedImmutableEncodedValue;
import org.jf.dexlib2.immutable.sorted.value.SortedImmutableEncodedValueFactory;
import org.jf.util.ImmutableSortedSetConverter;
import org.jf.util.ImmutableUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Comparator;

public class SortedImmutableField extends BaseFieldReference implements SortedField {
    @Nonnull public final String containingClass;
    @Nonnull public final String name;
    @Nonnull public final String type;
    public final int accessFlags;
    @Nullable public final SortedImmutableEncodedValue initialValue;
    @Nonnull public final ImmutableSortedSet<? extends SortedImmutableAnnotation> annotations;

    public SortedImmutableField(@Nonnull String containingClass,
                                @Nonnull String name,
                                @Nonnull String type,
                                int accessFlags,
                                @Nullable EncodedValue initialValue,
                                @Nullable Collection<? extends Annotation> annotations) {
        this.containingClass = containingClass;
        this.name = name;
        this.type = type;
        this.accessFlags = accessFlags;
        this.initialValue = SortedImmutableEncodedValueFactory.ofNullable(initialValue);
        this.annotations = SortedImmutableAnnotation.immutableSortedSetOf(annotations);
    }

    public SortedImmutableField(@Nonnull String containingClass,
                                @Nonnull String name,
                                @Nonnull String type,
                                int accessFlags,
                                @Nullable SortedImmutableEncodedValue initialValue,
                                @Nullable ImmutableSortedSet<? extends SortedImmutableAnnotation> annotations) {
        this.containingClass = containingClass;
        this.name = name;
        this.type = type;
        this.accessFlags = accessFlags;
        this.initialValue = initialValue;
        this.annotations = ImmutableUtils.nullToEmptySortedSet(annotations);
    }

    public static SortedImmutableField of(Field field) {
        if (field instanceof SortedImmutableField) {
            return (SortedImmutableField)field;
        }
        return new SortedImmutableField(
                field.getContainingClass(),
                field.getName(),
                field.getType(),
                field.getAccessFlags(),
                field.getInitialValue(),
                field.getAnnotations());
    }

    @Nonnull @Override public String getContainingClass() { return containingClass; }
    @Nonnull @Override public String getName() { return name; }
    @Nonnull @Override public String getType() { return type; }
    @Override public int getAccessFlags() { return accessFlags; }
    @Nullable @Override public SortedImmutableEncodedValue getInitialValue() { return initialValue;}
    @Nonnull @Override public ImmutableSortedSet<? extends SortedImmutableAnnotation> getAnnotations() {
        return annotations;
    }

    public static final Comparator<FieldReference> COMPARE_BY_SIGNATURE = new Comparator<FieldReference>() {
        @Override
        public int compare(FieldReference field1, FieldReference field2) {
            int res = field1.getContainingClass().compareTo(field2.getContainingClass());
            if (res != 0) {
                return res;
            }
            res = field1.getName().compareTo(field2.getName());
            if (res != 0) {
                return res;
            }
            return field1.getType().compareTo(field2.getType());
        }
    };

    @Nonnull
    public static ImmutableSortedSet<SortedImmutableField> immutableSortedSetOf(
            @Nullable Collection<? extends Field> list) {
        ImmutableSortedSet<SortedImmutableField> set = CONVERTER.convert(COMPARE_BY_SIGNATURE, list);
        if (list != null && set.size() < list.size()) {
            // There were duplicate fields. Let's find them and print a warning.
            ImmutableSortedMultiset<Field> multiset = ImmutableSortedMultiset.copyOf(COMPARE_BY_SIGNATURE, list);
            for (Multiset.Entry<Field> entry: multiset.entrySet()) {
                Field field = entry.getElement();
                String fieldType = AccessFlags.STATIC.isSet(field.getAccessFlags())?"static":"instance";
                // TODO: need to provide better context
                System.err.println(String.format("Ignoring duplicate %s field definition for field: %s:%s", fieldType,
                        field.getName(), field.getType()));
            }
        }
        return set;
    }

    private static final ImmutableSortedSetConverter<SortedImmutableField, Field> CONVERTER =
            new ImmutableSortedSetConverter<SortedImmutableField, Field>() {
                @Override
                protected boolean isImmutable(@Nonnull Field item) {
                    return item instanceof SortedImmutableField;
                }

                @Nonnull
                @Override
                protected SortedImmutableField makeImmutable(@Nonnull Field item) {
                    return SortedImmutableField.of(item);
                }
            };
}
