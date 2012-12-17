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
import org.jf.dexlib2.base.BaseAnnotationElement;
import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.AnnotationElement;
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

public class AnnotationPool {
    @Nonnull private final Map<Annotation, Integer> internedAnnotations = Maps.newHashMap();
    @Nonnull private final DexFile dexFile;
    private int sectionOffset = -1;

    public AnnotationPool(@Nonnull DexFile dexFile) {
        this.dexFile = dexFile;
    }

    public void intern(@Nonnull Annotation annotation) {
        Integer prev = internedAnnotations.put(annotation, 0);
        if (prev == null) {
            dexFile.typePool.intern(annotation.getType());
            for (AnnotationElement element: annotation.getElements()) {
                dexFile.stringPool.intern(element.getName());
                dexFile.internEncodedValue(element.getValue());
            }
        }
    }

    public int getOffset(@Nonnull Annotation annotation) {
        Integer offset = internedAnnotations.get(annotation);
        if (offset == null) {
            throw new ExceptionWithContext("Annotation not found.");
        }
        return offset;
    }

    public int getNumItems() {
        return internedAnnotations.size();
    }

    public int getSectionOffset() {
        if (sectionOffset < 0) {
            throw new ExceptionWithContext("Section offset has not been set yet!");
        }
        return sectionOffset;
    }

    public void write(@Nonnull DexWriter writer) throws IOException {
        List<Annotation> annotations = Lists.newArrayList(internedAnnotations.keySet());
        Collections.sort(annotations);

        sectionOffset = writer.getPosition();
        for (Annotation annotation: annotations) {
            internedAnnotations.put(annotation, writer.getPosition());
            writer.writeUbyte(annotation.getVisibility());
            writer.writeUleb128(dexFile.typePool.getIndex(annotation.getType()));
            writer.writeUleb128(annotation.getElements().size());

            SortedSet<? extends AnnotationElement> sortedElements =
                    ImmutableSortedSet.copyOf(BaseAnnotationElement.BY_NAME, annotation.getElements());
            for (AnnotationElement element: sortedElements) {
                writer.writeUleb128(dexFile.stringPool.getIndex(element.getName()));
                dexFile.writeEncodedValue(writer, element.getValue());
            }
        }
    }
}
