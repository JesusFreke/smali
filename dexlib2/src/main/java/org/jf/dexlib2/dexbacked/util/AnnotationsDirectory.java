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

package org.jf.dexlib2.dexbacked.util;

import com.google.common.collect.ImmutableList;
import org.jf.dexlib2.DexFile;
import org.jf.dexlib2.dexbacked.DexBackedAnnotation;

import javax.annotation.Nonnull;
import java.util.List;

public abstract class AnnotationsDirectory {
    public static final AnnotationsDirectory EMPTY = new AnnotationsDirectory() {
        @Override public int getFieldAnnotationCount() { return 0; }
        @Nonnull @Override public List<? extends DexBackedAnnotation> getClassAnnotations() {return ImmutableList.of();}
        @Nonnull @Override public AnnotationIterator getFieldAnnotationIterator() { return AnnotationIterator.EMPTY; }
    };

    public abstract int getFieldAnnotationCount();
    @Nonnull public abstract List<? extends DexBackedAnnotation> getClassAnnotations();
    @Nonnull public abstract AnnotationIterator getFieldAnnotationIterator();

    public static AnnotationsDirectory newOrEmpty(@Nonnull DexFile dexFile,
                                                  int directoryAnnotationsOffset) {
        if (directoryAnnotationsOffset == 0) {
            return EMPTY;
        }
        return new AnnotationsDirectoryImpl(dexFile, directoryAnnotationsOffset);
    }


    public interface AnnotationIterator {
        public static final AnnotationIterator EMPTY = new AnnotationIterator() {
            @Override public int seekTo(int fieldIndex) { return 0; }
        };

        public int seekTo(int fieldIndex);
    }

    @Nonnull
    public static List<? extends DexBackedAnnotation> getAnnotations(@Nonnull final DexFile dexFile,
                                                                     final int annotationSetOffset) {
        if (annotationSetOffset != 0) {
            final int size = dexFile.readSmallUint(annotationSetOffset);
            return new FixedSizeList<DexBackedAnnotation>() {
                @Override
                public DexBackedAnnotation readItem(int index) {
                    int annotationOffset = dexFile.readSmallUint(annotationSetOffset + 4 + (4*index));
                    return new DexBackedAnnotation(dexFile, annotationOffset);
                }

                @Override public int size() { return size; }
            };
        }

        return ImmutableList.of();
    }


    private static class AnnotationsDirectoryImpl extends AnnotationsDirectory {
        @Nonnull public final DexFile dexFile;
        private final int directoryOffset;

        private static final int FIELD_COUNT_OFFSET = 4;
        private static final int ANNOTATIONS_START_OFFSET = 16;

        public AnnotationsDirectoryImpl(@Nonnull DexFile dexFile,
                                        int directoryOffset) {
            this.dexFile = dexFile;
            this.directoryOffset = directoryOffset;
        }

        public int getFieldAnnotationCount() {
            return dexFile.readSmallUint(directoryOffset + FIELD_COUNT_OFFSET);
        }

        @Nonnull
        public List<? extends DexBackedAnnotation> getClassAnnotations() {
            return getAnnotations(dexFile, dexFile.readSmallUint(directoryOffset));
        }

        @Nonnull
        public AnnotationIterator getFieldAnnotationIterator() {
            return new AnnotationIteratorImpl(directoryOffset + ANNOTATIONS_START_OFFSET, getFieldAnnotationCount());
        }

        private class AnnotationIteratorImpl implements AnnotationIterator {
            private final int startOffset;
            private final int size;
            private int currentIndex;
            private int currentFieldIndex;

            public AnnotationIteratorImpl(int startOffset, int size) {
                this.startOffset = startOffset;
                this.size = size;
                if (size > 0) {
                    currentFieldIndex = dexFile.readSmallUint(startOffset);
                    this.currentIndex = 0;
                } else {
                    currentFieldIndex = -1;
                    this.currentIndex = -1;
                }
            }

            public int seekTo(int fieldIndex) {
                while (currentFieldIndex < fieldIndex && (currentIndex+1) < size) {
                    currentIndex++;
                    currentFieldIndex = dexFile.readSmallUint(startOffset + (currentIndex*8));
                }

                if (currentFieldIndex == fieldIndex) {
                    return dexFile.readSmallUint(startOffset + (currentIndex*8)+4);
                }
                return 0;
            }
        }
    }
}
