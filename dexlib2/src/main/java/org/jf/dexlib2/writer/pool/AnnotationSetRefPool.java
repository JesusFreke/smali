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
import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;
import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodParameter;
import org.jf.dexlib2.writer.pool.AnnotationSetRefPool.Key;
import org.jf.dexlib2.writer.AnnotationSetRefSection;
import org.jf.util.CollectionUtils;

import javax.annotation.Nonnull;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class AnnotationSetRefPool extends BaseNullableOffsetPool<Key>
        implements AnnotationSetRefSection<Set<? extends Annotation>, Key> {
    @Nonnull private final AnnotationSetPool annotationSetPool;

    public AnnotationSetRefPool(@Nonnull AnnotationSetPool annotationSetPool) {
        this.annotationSetPool = annotationSetPool;
    }

    public void intern(@Nonnull Method method) {
        Key annotationSetRefKey = new Key(method);
        Integer prev = internedItems.put(annotationSetRefKey, 0);
        if (prev == null) {
            for (Set<? extends Annotation> annotationSet: annotationSetRefKey.getAnnotationSets()) {
                annotationSetPool.intern(annotationSet);
            }
        }
    }

    @Nonnull @Override public Collection<Set<? extends Annotation>> getAnnotationSets(@Nonnull Key key) {
        return key.getAnnotationSets();
    }

    public static class Key implements Comparable<Key> {
        @Nonnull private final Method method;
        private final int size;

        public Key(@Nonnull Method method) {
            this.method = method;
            this.size = CollectionUtils.lastIndexOf(method.getParameters(), HAS_ANNOTATIONS) + 1;
        }

        public Collection<Set<? extends Annotation>> getAnnotationSets() {
            return new AbstractCollection<Set<? extends Annotation>>() {
                @Nonnull @Override public Iterator<Set<? extends Annotation>> iterator() {
                    return FluentIterable.from(method.getParameters())
                            .limit(size)
                            .transform(PARAMETER_ANNOTATIONS).iterator();
                }

                @Override public int size() {
                    return size;
                }
            };
        }

        @Override
        public int hashCode() {
            int hashCode = 1;
            for (Set<? extends Annotation> annotationSet: getAnnotationSets()) {
                hashCode = hashCode*31 + annotationSet.hashCode();
            }
            return hashCode;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Key) {
                Key other = (Key)o;
                if (size != other.size) {
                    return false;
                }
                Iterator<Set<? extends Annotation>> otherAnnotationSets = getAnnotationSets().iterator();
                for (Set<? extends Annotation> annotationSet: getAnnotationSets()) {
                    if (!annotationSet.equals(otherAnnotationSets.next())) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }

        private static final Predicate<MethodParameter> HAS_ANNOTATIONS = new Predicate<MethodParameter>() {
            @Override
            public boolean apply(MethodParameter input) {
                return input.getAnnotations().size() > 0;
            }
        };

        private static final Function<MethodParameter, Set<? extends Annotation>> PARAMETER_ANNOTATIONS =
                new Function<MethodParameter, Set<? extends Annotation>>() {
                    @Override
                    public Set<? extends Annotation> apply(MethodParameter input) {
                        return input.getAnnotations();
                    }
                };

        @Override
        public int compareTo(Key o) {
            int res = Ints.compare(size, o.size);
            if (res != 0) return res;
            return CollectionUtils.compareAsIterable(CollectionUtils.setComparator(Ordering.natural()),
                    getAnnotationSets(), o.getAnnotationSets());
        }
    }
}
