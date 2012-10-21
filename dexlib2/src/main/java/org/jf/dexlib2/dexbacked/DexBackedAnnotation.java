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

package org.jf.dexlib2.dexbacked;

import org.jf.dexlib2.dexbacked.util.VariableSizeList;
import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.AnnotationElement;

import javax.annotation.Nonnull;
import java.util.List;

public class DexBackedAnnotation implements Annotation {
    @Nonnull public final DexBuffer dexBuf;

    public final int visibility;
    @Nonnull public final String type;
    private final int elementsOffset;

    public DexBackedAnnotation(@Nonnull DexBuffer dexBuf,
                               int annotationOffset) {
        this.dexBuf = dexBuf;

        DexReader reader = dexBuf.readerAt(annotationOffset);
        this.visibility = reader.readUbyte();
        this.type = reader.getType(reader.readSmallUleb128());
        this.elementsOffset = reader.getOffset();
    }

    @Override public int getVisibility() { return visibility; }
    @Nonnull @Override public String getType() { return type; }

    @Nonnull
    @Override
    public List<? extends AnnotationElement> getElements() {
        DexReader reader = dexBuf.readerAt(elementsOffset);
        final int size = reader.readSmallUleb128();

        return new VariableSizeList<AnnotationElement>(dexBuf, reader.getOffset()) {
            @Nonnull
            @Override
            protected AnnotationElement readItem(DexReader reader, int index) {
                return new DexBackedAnnotationElement(reader);
            }

            @Override public int size() { return size;}
        };
    }
}
