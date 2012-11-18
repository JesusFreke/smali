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

import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import org.jf.dexlib2.base.BaseAnnotation;
import org.jf.dexlib2.iface.Annotation;
import org.jf.util.CollectionUtils;
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;

public class AnnotationSetPool {
    @Nonnull private final Map<Set<? extends Annotation>, Integer> internedAnnotationSetItems = Maps.newHashMap();
    @Nonnull private final DexFile dexFile;

    public AnnotationSetPool(@Nonnull DexFile dexFile) {
        this.dexFile = dexFile;
    }

    public void intern(@Nonnull Set<? extends Annotation> annotationSet) {
        if (annotationSet.size() > 0) {
            Integer prev = internedAnnotationSetItems.put(annotationSet, 0);
            if (prev == null) {
                for (Annotation annotation: annotationSet) {
                    dexFile.annotationPool.intern(annotation);
                }
            }
        }
    }

    public int getOffset(@Nonnull Set<? extends Annotation> annotationSet) {
        if (annotationSet.size() == 0) {
            return 0;
        }
        Integer offset = internedAnnotationSetItems.get(annotationSet);
        if (offset == null) {
            throw new ExceptionWithContext("Annotation set not found.");
        }
        return offset;
    }

    public void write(@Nonnull DexWriter writer) throws IOException {
        List<Set<? extends Annotation>> annotationSets =
                Lists.newArrayList(internedAnnotationSetItems.keySet());
        Collections.sort(annotationSets, CollectionUtils.listComparator(Ordering.natural()));

        for (Set<? extends Annotation> annotationSet: annotationSets) {
            SortedSet<? extends Annotation> sortedAnnotationSet = ImmutableSortedSet.copyOf(BaseAnnotation.BY_TYPE,
                    annotationSet);
            writer.align();
            internedAnnotationSetItems.put(annotationSet, writer.getPosition());
            writer.writeInt(annotationSet.size());
            for (Annotation annotation: sortedAnnotationSet) {
                writer.writeInt(dexFile.annotationPool.getOffset(annotation));
            }
        }
    }
}
