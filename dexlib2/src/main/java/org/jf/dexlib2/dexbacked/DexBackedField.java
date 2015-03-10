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

import org.jf.dexlib2.base.reference.BaseFieldReference;
import org.jf.dexlib2.dexbacked.raw.FieldIdItem;
import org.jf.dexlib2.dexbacked.util.AnnotationsDirectory;
import org.jf.dexlib2.dexbacked.util.StaticInitialValueIterator;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.iface.value.EncodedValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

public class DexBackedField extends BaseFieldReference implements Field {
    @Nonnull public final DexBackedDexFile dexFile;
    @Nonnull public final ClassDef classDef;

    public final int accessFlags;
    @Nullable public final EncodedValue initialValue;
    public final int annotationSetOffset;

    public final int fieldIndex;

    private int fieldIdItemOffset;

    public DexBackedField(@Nonnull DexReader reader,
                          @Nonnull DexBackedClassDef classDef,
                          int previousFieldIndex,
                          @Nonnull StaticInitialValueIterator staticInitialValueIterator,
                          @Nonnull AnnotationsDirectory.AnnotationIterator annotationIterator) {
        this.dexFile = reader.dexBuf;
        this.classDef = classDef;

        // large values may be used for the index delta, which cause the cumulative index to overflow upon
        // addition, effectively allowing out of order entries.
        int fieldIndexDiff = reader.readLargeUleb128();
        this.fieldIndex = fieldIndexDiff + previousFieldIndex;
        this.accessFlags = reader.readSmallUleb128();

        this.annotationSetOffset = annotationIterator.seekTo(fieldIndex);
        this.initialValue = staticInitialValueIterator.getNextOrNull();
    }

    public DexBackedField(@Nonnull DexReader reader,
                          @Nonnull DexBackedClassDef classDef,
                          int previousFieldIndex,
                          @Nonnull AnnotationsDirectory.AnnotationIterator annotationIterator) {
        this.dexFile = reader.dexBuf;
        this.classDef = classDef;

        // large values may be used for the index delta, which cause the cumulative index to overflow upon
        // addition, effectively allowing out of order entries.
        int fieldIndexDiff = reader.readLargeUleb128();
        this.fieldIndex = fieldIndexDiff + previousFieldIndex;
        this.accessFlags = reader.readSmallUleb128();

        this.annotationSetOffset = annotationIterator.seekTo(fieldIndex);
        this.initialValue = null;
    }

    @Nonnull
    @Override
    public String getName() {
        return dexFile.getString(dexFile.readSmallUint(getFieldIdItemOffset() + FieldIdItem.NAME_OFFSET));
    }

    @Nonnull
    @Override
    public String getType() {
        return dexFile.getType(dexFile.readUshort(getFieldIdItemOffset() + FieldIdItem.TYPE_OFFSET));
    }

    @Nonnull @Override public String getDefiningClass() { return classDef.getType(); }
    @Override public int getAccessFlags() { return accessFlags; }
    @Nullable @Override public EncodedValue getInitialValue() { return initialValue; }

    @Nonnull
    @Override
    public Set<? extends DexBackedAnnotation> getAnnotations() {
        return AnnotationsDirectory.getAnnotations(dexFile, annotationSetOffset);
    }

    // The ART specification says that fields should be ordered by reference, 64-bit fields, 32-bit,
    // 16-bit and finally 8-bit. The problem is that the current Android version (API 21) doesn't specify
    // the ordering of fields with different type, but that have the same size (float and int for example).
    // This has been fixed in AOSP https://android-review.googlesource.com/#/c/114281/2/runtime/class_linker.cc
    // where it sorts by the field type if the size is equal.
    // Since there is no way of telling how same-sized, same-named fields are sorted,
    // we just follow the AOSP specification below and hope for the best.
    @Override
    public int compareTo(@Nonnull FieldReference o) {
        if(o instanceof DexBackedField && dexFile instanceof DexBackedOatFile) {
            DexBackedOatFile oatFile = (DexBackedOatFile) dexFile;

            char type1 = getType().charAt(0);
            if(type1 == '[') {
                type1 = 'L';
            }
            char type2 = o.getType().charAt(0);
            if(type2 == '[') {
                type2 = 'L';
            }
            boolean isPrimitive1 = isPrimitive(type1);
            boolean isPrimitive2 = isPrimitive(type2);
            int size1 = getFieldSize(type1);
            int size2 = getFieldSize(type2);

            if(type1 != type2) {
                // In OAT versions before 47 the ordering was undefined when two fields had the same name and size.
                if(oatFile.getVersion() < 47) {
                    if(isPrimitive1 && isPrimitive2) {
                        return size2 - size1;
                    } else {
                        return (isPrimitive1)?1:-1;
                    }
                } else {
                    if(!isPrimitive1) {
                        return 1;
                    }
                    if(!isPrimitive2) {
                        return -1;
                    }

                    if(size1 != size2) {
                        return size2 - size1;
                    }

                    return type1 - type2;
                }
            }

            // OAT v48 sorts the fields by the field index. (https://android-review.googlesource.com/#/c/114814/2/runtime/class_linker.cc)
            if(oatFile.getVersion() >= 48) {
                DexBackedField oField = (DexBackedField) o;
                return fieldIndex - oField.fieldIndex;
            } else {
                return getName().compareTo(o.getName());
            }
        }

        return super.compareTo(o);
    }

    /**
     * Skips the reader over the specified number of encoded_field structures
     *
     * @param reader The reader to skip
     * @param count The number of encoded_field structures to skip over
     */
    public static void skipFields(@Nonnull DexReader reader, int count) {
        for (int i=0; i<count; i++) {
            reader.skipUleb128();
            reader.skipUleb128();
        }
    }

    private int getFieldIdItemOffset() {
        if (fieldIdItemOffset == 0) {
            fieldIdItemOffset = dexFile.getFieldIdItemOffset(fieldIndex);
        }
        return fieldIdItemOffset;
    }

    private byte getFieldSize(char fieldType) {
        switch (fieldType) {
            case 'J':
            case 'D':
                return 8;
            case '[':
            case 'L':
                return 4;
            case 'I':
            case 'F':
                return 4;
            case 'C':
            case 'S':
                return 2;
            case 'Z':
            case 'B':
                return 1;
            default:
                return 0;
        }
    }

    private boolean isPrimitive(char fieldType) {
        return fieldType != 'L' && fieldType != '[';
    }
}
