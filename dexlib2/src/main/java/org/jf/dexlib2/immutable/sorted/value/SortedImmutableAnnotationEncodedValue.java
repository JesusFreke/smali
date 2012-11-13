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

package org.jf.dexlib2.immutable.sorted.value;

import com.google.common.collect.ImmutableSortedSet;
import org.jf.dexlib2.base.value.BaseAnnotationEncodedValue;
import org.jf.dexlib2.iface.AnnotationElement;
import org.jf.dexlib2.iface.sorted.SortedAnnotationElement;
import org.jf.dexlib2.iface.sorted.value.SortedAnnotationEncodedValue;
import org.jf.dexlib2.iface.value.AnnotationEncodedValue;
import org.jf.dexlib2.immutable.sorted.SortedImmutableAnnotationElement;
import org.jf.util.ImmutableUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

public class SortedImmutableAnnotationEncodedValue extends BaseAnnotationEncodedValue
        implements SortedImmutableEncodedValue, SortedAnnotationEncodedValue {
    @Nonnull public final String type;
    @Nonnull public final ImmutableSortedSet<? extends SortedImmutableAnnotationElement> elements;

    public SortedImmutableAnnotationEncodedValue(@Nonnull String type,
                                                 @Nullable Collection<? extends AnnotationElement> elements) {
        this.type = type;
        this.elements = SortedImmutableAnnotationElement.immutableSortedSetOf(elements);
    }

    public SortedImmutableAnnotationEncodedValue(
            @Nonnull String type,
            @Nullable ImmutableSortedSet<? extends SortedImmutableAnnotationElement> elements) {
        this.type = type;
        this.elements = ImmutableUtils.nullToEmptySortedSet(elements);
    }

    public static SortedImmutableAnnotationEncodedValue of(AnnotationEncodedValue annotationEncodedValue) {
        if (annotationEncodedValue instanceof SortedImmutableAnnotationEncodedValue) {
            return (SortedImmutableAnnotationEncodedValue)annotationEncodedValue;
        }
        return new SortedImmutableAnnotationEncodedValue(
                annotationEncodedValue.getType(),
                annotationEncodedValue.getElements());
    }

    @Nonnull @Override public String getType() { return type; }
    @Nonnull @Override public ImmutableSortedSet<? extends SortedAnnotationElement> getElements() {
        return elements;
    }
}
