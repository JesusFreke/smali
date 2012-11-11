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

package org.jf.dexlib2.immutable;

import com.google.common.collect.ImmutableList;
import org.jf.dexlib2.base.reference.BaseFieldReference;
import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.value.EncodedValue;
import org.jf.dexlib2.immutable.value.ImmutableEncodedValue;
import org.jf.dexlib2.immutable.value.ImmutableEncodedValueFactory;
import org.jf.util.ImmutableListConverter;
import org.jf.util.ImmutableListUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ImmutableField extends BaseFieldReference implements Field {
    @Nonnull public final String containingClass;
    @Nonnull public final String name;
    @Nonnull public final String type;
    public final int accessFlags;
    @Nullable public final ImmutableEncodedValue initialValue;
    @Nonnull public final ImmutableList<? extends ImmutableAnnotation> annotations;

    public ImmutableField(@Nonnull String containingClass,
                          @Nonnull String name,
                          @Nonnull String type,
                          int accessFlags,
                          @Nullable EncodedValue initialValue,
                          @Nullable List<? extends Annotation> annotations) {
        this.containingClass = containingClass;
        this.name = name;
        this.type = type;
        this.accessFlags = accessFlags;
        this.initialValue = ImmutableEncodedValueFactory.of(initialValue);
        this.annotations = ImmutableAnnotation.immutableListOf(annotations);
    }

    public ImmutableField(@Nonnull String containingClass,
                          @Nonnull String name,
                          @Nonnull String type,
                          int accessFlags,
                          @Nullable ImmutableEncodedValue initialValue,
                          @Nullable ImmutableList<? extends ImmutableAnnotation> annotations) {
        this.containingClass = containingClass;
        this.name = name;
        this.type = type;
        this.accessFlags = accessFlags;
        this.initialValue = initialValue;
        this.annotations = ImmutableListUtils.nullToEmptyList(annotations);
    }

    public static ImmutableField of(Field field) {
        if (field instanceof  ImmutableField) {
            return (ImmutableField)field;
        }
        return new ImmutableField(
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
    @Override public EncodedValue getInitialValue() { return initialValue;}
    @Nonnull @Override public ImmutableList<? extends ImmutableAnnotation> getAnnotations() { return annotations; }

    @Nonnull
    public static ImmutableList<ImmutableField> immutableListOf(@Nullable List<? extends Field> list) {
        return CONVERTER.convert(list);
    }

    private static final ImmutableListConverter<ImmutableField, Field> CONVERTER =
            new ImmutableListConverter<ImmutableField, Field>() {
                @Override
                protected boolean isImmutable(Field item) {
                    return item instanceof ImmutableField;
                }

                @Override
                protected ImmutableField makeImmutable(Field item) {
                    return ImmutableField.of(item);
                }
            };
}
