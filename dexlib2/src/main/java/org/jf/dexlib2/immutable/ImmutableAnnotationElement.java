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
import org.jf.dexlib2.immutable.value.ImmutableEncodedValue;
import org.jf.dexlib2.iface.AnnotationElement;
import org.jf.dexlib2.iface.value.EncodedValue;
import org.jf.util.ImmutableListConverter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ImmutableAnnotationElement implements AnnotationElement {
    @Nonnull public final String name;
    @Nonnull public final ImmutableEncodedValue value;

    public ImmutableAnnotationElement(@Nonnull String name,
                                      @Nonnull EncodedValue value) {
        this.name = name;
        this.value = ImmutableEncodedValue.of(value);
    }

    public ImmutableAnnotationElement(@Nonnull String name,
                                      @Nonnull ImmutableEncodedValue value) {
        this.name = name;
        this.value = value;
    }

    public static ImmutableAnnotationElement of(AnnotationElement annotationElement) {
        if (annotationElement instanceof ImmutableAnnotationElement) {
            return (ImmutableAnnotationElement)annotationElement;
        }
        return new ImmutableAnnotationElement(
                annotationElement.getName(),
                annotationElement.getValue());
    }

    @Nonnull @Override public String getName() { return name; }
    @Nonnull @Override public EncodedValue getValue() { return value; }

    @Nonnull
    public static ImmutableList<ImmutableAnnotationElement> immutableListOf(
            @Nullable List<? extends AnnotationElement> list) {
        return CONVERTER.convert(list);
    }

    private static final ImmutableListConverter<ImmutableAnnotationElement, AnnotationElement> CONVERTER =
            new ImmutableListConverter<ImmutableAnnotationElement, AnnotationElement>() {
                @Override
                protected boolean isImmutable(AnnotationElement item) {
                    return item instanceof ImmutableAnnotationElement;
                }

                @Override
                protected ImmutableAnnotationElement makeImmutable(AnnotationElement item) {
                    return ImmutableAnnotationElement.of(item);
                }
            };
}
