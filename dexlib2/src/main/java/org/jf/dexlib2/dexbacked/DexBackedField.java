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

import org.jf.dexlib2.DexFile;
import org.jf.dexlib2.DexFileReader;
import org.jf.dexlib2.dexbacked.util.AnnotationsDirectory;
import org.jf.dexlib2.dexbacked.util.StaticInitialValueIterator;
import org.jf.dexlib2.dexbacked.value.DexBackedEncodedValue;
import org.jf.dexlib2.iface.Field;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class DexBackedField implements Field {
    public final DexFile dexFile;

    @Nonnull public final String name;
    @Nonnull public final String type;
    public final int accessFlags;
    @Nullable public final DexBackedEncodedValue initialValue;
    public final int annotationSetOffset;

    public final int fieldIndex;

    public DexBackedField(@Nonnull DexFileReader dexFileReader,
                          int previousFieldIndex,
                          @Nonnull StaticInitialValueIterator staticInitialValueIterator,
                          @Nonnull AnnotationsDirectory.AnnotationIterator annotationIterator) {
        this.dexFile = dexFileReader.getDexFile();

        int fieldIndexDiff = dexFileReader.readSmallUleb128();
        this.fieldIndex = fieldIndexDiff + previousFieldIndex;
        this.accessFlags = dexFileReader.readSmallUleb128();

        this.annotationSetOffset = annotationIterator.seekTo(fieldIndex);
        this.initialValue = staticInitialValueIterator.getNextOrNull();

        int fieldIdItemOffset = dexFileReader.getFieldIdItemOffset(fieldIndex);
        this.type = dexFileReader.getType(dexFileReader.readUshort(fieldIdItemOffset+2));
        this.name = dexFileReader.getString(dexFileReader.readSmallUint(fieldIdItemOffset+2));
    }


    @Nonnull @Override public String getName() { return name; }
    @Nonnull @Override public String getType() { return type; }
    @Override public int getAccessFlags() { return accessFlags; }
    @Nullable @Override public DexBackedEncodedValue getInitialValue() { return initialValue; }

    @Nonnull
    @Override
    public List<? extends DexBackedAnnotation> getAnnotations() {
        return AnnotationsDirectory.getAnnotations(dexFile, annotationSetOffset);
    }

    /**
     *  This returns the field index
     * @param dexFileReader The reader to skip
     */

    /**
     * Skips the reader over a single encoded_field structure.
     * @param dexFileReader The {@code DexFileReader} to skip
     * @param previousFieldIndex The field index of the previous field, or 0 if this is the first
     * @return The field index of the field that was skipped
     */
    public static int skipEncodedField(@Nonnull DexFileReader dexFileReader, int previousFieldIndex) {
        int idxDiff = dexFileReader.readSmallUleb128();
        dexFileReader.skipUleb128();
        return previousFieldIndex + idxDiff;
    }

    /**
     * Skips the reader over the specified number of encoded_field structures
     *
     * This is intended to be used for
     *
     * @param dexFileReader The reader to skip
     * @param count The number of encoded_field structures to skip over
     */
    public static void skipAllFields(@Nonnull DexFileReader dexFileReader, int count) {
        for (int i=0; i<count; i++) {
            dexFileReader.skipUleb128();
            dexFileReader.skipUleb128();
        }
    }
}
