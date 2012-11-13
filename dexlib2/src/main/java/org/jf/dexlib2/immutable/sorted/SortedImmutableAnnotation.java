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
import org.jf.dexlib2.base.BaseAnnotation;
import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.AnnotationElement;
import org.jf.dexlib2.iface.sorted.SortedAnnotation;
import org.jf.util.ImmutableSortedSetConverter;
import org.jf.util.ImmutableUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Comparator;

public class SortedImmutableAnnotation extends BaseAnnotation implements SortedAnnotation {
    public final int visibility;
    @Nonnull public final String type;
    @Nonnull public final ImmutableSortedSet<? extends SortedImmutableAnnotationElement> elements;

    public SortedImmutableAnnotation(int visibility,
                                     @Nonnull String type,
                                     @Nullable Collection<? extends AnnotationElement> elements) {
        this.visibility = visibility;
        this.type = type;
        this.elements = SortedImmutableAnnotationElement.immutableSortedSetOf(elements);
    }

    public SortedImmutableAnnotation(
            int visibility,
            @Nonnull String type,
            @Nullable ImmutableSortedSet<? extends SortedImmutableAnnotationElement> elements) {
        this.visibility = visibility;
        this.type = type;
        this.elements = ImmutableUtils.nullToEmptySortedSet(elements);
    }

    public static SortedImmutableAnnotation of(Annotation annotation) {
        if (annotation instanceof SortedImmutableAnnotation) {
            return (SortedImmutableAnnotation)annotation;
        }
        return new SortedImmutableAnnotation(
                annotation.getVisibility(),
                annotation.getType(),
                annotation.getElements());
    }

    @Override public int getVisibility() { return visibility; }
    @Nonnull @Override public String getType() { return type; }
    @Nonnull @Override public ImmutableSortedSet<? extends SortedImmutableAnnotationElement> getElements() {
        return elements;
    }

    public static final Comparator<Annotation> COMPARE_BY_TYPE = new Comparator<Annotation>() {
        @Override
        public int compare(Annotation annotation1, Annotation annotation2) {
            return annotation1.getType().compareTo(annotation2.getType());
        }
    };

    @Nonnull
    public static ImmutableSortedSet<SortedImmutableAnnotation> immutableSortedSetOf(
            @Nullable Collection<? extends Annotation> list) {
        ImmutableSortedSet<SortedImmutableAnnotation> set = CONVERTER.convert(COMPARE_BY_TYPE, list);
        if (list != null && set.size() < list.size()) {
            // There were duplicate annotations. Let's find them and print a warning.
            ImmutableSortedMultiset<Annotation> multiset = ImmutableSortedMultiset.copyOf(COMPARE_BY_TYPE, list);
            for (Multiset.Entry<Annotation> entry: multiset.entrySet()) {
                Annotation annotation = entry.getElement();
                // TODO: need to provide better context
                System.err.println(String.format("Ignoring duplicate annotation definition for annotation type: %s",
                        annotation.getType()));
            }
        }
        return set;
    }

    private static final ImmutableSortedSetConverter<SortedImmutableAnnotation, Annotation> CONVERTER =
            new ImmutableSortedSetConverter<SortedImmutableAnnotation, Annotation>() {
                @Override
                protected boolean isImmutable(@Nonnull Annotation item) {
                    return item instanceof SortedImmutableAnnotation;
                }

                @Nonnull
                @Override
                protected SortedImmutableAnnotation makeImmutable(@Nonnull Annotation item) {
                    return SortedImmutableAnnotation.of(item);
                }
            };
}
