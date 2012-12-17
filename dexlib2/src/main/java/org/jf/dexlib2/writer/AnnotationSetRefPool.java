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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;
import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodParameter;
import org.jf.util.CollectionUtils;
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;

public class AnnotationSetRefPool {
    @Nonnull private final Map<Key, Integer> internedAnnotationSetRefItems = Maps.newHashMap();
    @Nonnull private final DexFile dexFile;
    private int sectionOffset = -1;

    public AnnotationSetRefPool(@Nonnull DexFile dexFile) {
        this.dexFile = dexFile;
    }

    public void intern(@Nonnull Method method) {
        Key annotationSetRefKey = new Key(method);
        Integer prev = internedAnnotationSetRefItems.put(annotationSetRefKey, 0);
        if (prev == null) {
            for (Set<? extends Annotation> annotationSet: annotationSetRefKey.getAnnotationSets()) {
                dexFile.annotationSetPool.intern(annotationSet);
            }
        }
    }

    public int getOffset(@Nonnull Method method) {
        Key annotationSetRefKey = new Key(method);
        Integer offset = internedAnnotationSetRefItems.put(annotationSetRefKey, 0);
        if (offset == null) {
            throw new ExceptionWithContext("Annotation set ref not found.");
        }
        return offset;
    }

    public int getNumItems() {
        return internedAnnotationSetRefItems.size();
    }

    public int getSectionOffset() {
        if (sectionOffset < 0) {
            throw new ExceptionWithContext("Section offset has not been set yet!");
        }
        return sectionOffset;
    }

    public void write(@Nonnull DexWriter writer) throws IOException {
        List<Key> annotationSetRefs =
                Lists.newArrayList(internedAnnotationSetRefItems.keySet());
        Collections.sort(annotationSetRefs);

        writer.align();
        sectionOffset = writer.getPosition();
        for (Key key: annotationSetRefs) {
            writer.align();
            internedAnnotationSetRefItems.put(key, writer.getPosition());
            writer.writeInt(key.getAnnotationSetCount());
            for (Set<? extends Annotation> annotationSet: key.getAnnotationSets()) {
                writer.writeInt(dexFile.annotationSetPool.getOffset(annotationSet));
            }
        }
    }

    private static class Key implements Comparable<Key> {
        @Nonnull private final Method method;
        private final int size;

        public Key(@Nonnull Method method) {
            this.method = method;
            this.size = CollectionUtils.lastIndexOf(method.getParameters(), HAS_ANNOTATIONS) + 1;
        }

        public int getAnnotationSetCount() {
            return size;
        }

        public Iterable<Set<? extends Annotation>> getAnnotationSets() {
            return FluentIterable.from(method.getParameters())
                    .limit(size)
                    .transform(PARAMETER_ANNOTATIONS);
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
