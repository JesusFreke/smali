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

import com.google.common.collect.ImmutableSet;
import org.jf.dexlib2.base.reference.BaseTypeReference;
import org.jf.dexlib2.dexbacked.raw.ClassDefItem;
import org.jf.dexlib2.dexbacked.util.AnnotationsDirectory;
import org.jf.dexlib2.dexbacked.util.FixedSizeSet;
import org.jf.dexlib2.dexbacked.util.StaticInitialValueIterator;
import org.jf.dexlib2.dexbacked.util.VariableSizeIterator;
import org.jf.dexlib2.iface.ClassDef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;

public class DexBackedClassDef extends BaseTypeReference implements ClassDef {
    @Nonnull public final DexBackedDexFile dexFile;
    private final int classDefOffset;

    private int classDataOffset = -1;

    @Nullable private AnnotationsDirectory annotationsDirectory;

    public DexBackedClassDef(@Nonnull DexBackedDexFile dexFile,
                             int classDefOffset) {
        this.dexFile = dexFile;
        this.classDefOffset = classDefOffset;
    }

    @Nonnull
    @Override
    public String getType() {
        return dexFile.getType(dexFile.readSmallUint(classDefOffset + ClassDefItem.CLASS_OFFSET));
    }

    @Nullable
    @Override
    public String getSuperclass() {
        return dexFile.getOptionalType(dexFile.readOptionalUint(classDefOffset + ClassDefItem.SUPERCLASS_OFFSET));
    }

    @Override
    public int getAccessFlags() {
        return dexFile.readSmallUint(classDefOffset + ClassDefItem.ACCESS_FLAGS_OFFSET);
    }

    @Nullable
    @Override
    public String getSourceFile() {
        return dexFile.getOptionalString(dexFile.readOptionalUint(classDefOffset + ClassDefItem.SOURCE_FILE_OFFSET));
    }

    @Nonnull
    @Override
    public Set<String> getInterfaces() {
        final int interfacesOffset = dexFile.readSmallUint(classDefOffset + ClassDefItem.INTERFACES_OFFSET);
        if (interfacesOffset > 0) {
            final int size = dexFile.readSmallUint(interfacesOffset);
            return new FixedSizeSet<String>() {
                @Nonnull
                @Override
                public String readItem(int index) {
                    return dexFile.getType(dexFile.readUshort(interfacesOffset + 4 + (2*index)));
                }

                @Override public int size() { return size; }
            };
        }
        return ImmutableSet.of();
    }

    @Nonnull
    @Override
    public Set<? extends DexBackedAnnotation> getAnnotations() {
        return getAnnotationsDirectory().getClassAnnotations();
    }

    @Nonnull
    @Override
    public Set<? extends DexBackedField> getFields() {
        int classDataOffset = getClassDataOffset();
        if (getClassDataOffset() != 0) {
            DexReader reader = dexFile.readerAt(classDataOffset);
            final int staticFieldCount = reader.readSmallUleb128();
            int instanceFieldCount = reader.readSmallUleb128();
            final int fieldCount = staticFieldCount + instanceFieldCount;
            if (fieldCount > 0) {
                reader.skipUleb128(); //direct_methods_size
                reader.skipUleb128(); //virtual_methods_size

                final AnnotationsDirectory annotationsDirectory = getAnnotationsDirectory();
                final int staticInitialValuesOffset =
                        dexFile.readSmallUint(classDefOffset + ClassDefItem.STATIC_VALUES_OFFSET);
                final int fieldsStartOffset = reader.getOffset();

                return new AbstractSet<DexBackedField>() {
                    @Nonnull
                    @Override
                    public Iterator<DexBackedField> iterator() {
                        return new VariableSizeIterator<DexBackedField>(dexFile, fieldsStartOffset, fieldCount) {
                            private int previousFieldIndex = 0;
                            @Nonnull private final AnnotationsDirectory.AnnotationIterator annotationIterator =
                                    annotationsDirectory.getFieldAnnotationIterator();
                            @Nonnull private final StaticInitialValueIterator staticInitialValueIterator =
                                    StaticInitialValueIterator.newOrEmpty(dexFile, staticInitialValuesOffset);

                            @Nonnull
                            @Override
                            protected DexBackedField readNextItem(@Nonnull DexReader reader, int index) {
                                if (index == staticFieldCount) {
                                    // We reached the end of the static field, restart the numbering for
                                    // instance fields
                                    previousFieldIndex = 0;
                                    annotationIterator.reset();
                                }
                                DexBackedField item = new DexBackedField(reader, DexBackedClassDef.this,
                                        previousFieldIndex, staticInitialValueIterator, annotationIterator);
                                previousFieldIndex = item.fieldIndex;
                                return item;
                            }
                        };
                    }

                    @Override public int size() { return fieldCount; }
                };
            }
        }
        return ImmutableSet.of();
    }

    @Nonnull
    @Override
    public Set<? extends DexBackedMethod> getMethods() {
        int classDataOffset = getClassDataOffset();
        if (classDataOffset > 0) {
            DexReader reader = dexFile.readerAt(classDataOffset);
            int staticFieldCount = reader.readSmallUleb128();
            int instanceFieldCount = reader.readSmallUleb128();
            final int directMethodCount = reader.readSmallUleb128();
            int virtualMethodCount = reader.readSmallUleb128();
            final int methodCount = directMethodCount + virtualMethodCount;
            if (methodCount > 0) {
                DexBackedField.skipAllFields(reader, staticFieldCount + instanceFieldCount);

                final AnnotationsDirectory annotationsDirectory = getAnnotationsDirectory();
                final int methodsStartOffset = reader.getOffset();

                return new AbstractSet<DexBackedMethod>() {
                    @Nonnull
                    @Override
                    public Iterator<DexBackedMethod> iterator() {
                        return new VariableSizeIterator<DexBackedMethod>(dexFile, methodsStartOffset, methodCount) {
                            private int previousMethodIndex = 0;
                            @Nonnull private final AnnotationsDirectory.AnnotationIterator methodAnnotationIterator =
                                    annotationsDirectory.getMethodAnnotationIterator();
                            @Nonnull private final AnnotationsDirectory.AnnotationIterator parameterAnnotationIterator =
                                    annotationsDirectory.getParameterAnnotationIterator();

                            @Nonnull
                            @Override
                            protected DexBackedMethod readNextItem(@Nonnull DexReader reader, int index) {
                                if (index == directMethodCount) {
                                    // We reached the end of the direct methods, restart the numbering for
                                    // virtual methods
                                    previousMethodIndex = 0;
                                    methodAnnotationIterator.reset();
                                    parameterAnnotationIterator.reset();
                                }
                                DexBackedMethod item = new DexBackedMethod(reader, DexBackedClassDef.this,
                                        previousMethodIndex, methodAnnotationIterator, parameterAnnotationIterator);
                                previousMethodIndex = item.methodIndex;
                                return item;
                            }
                        };
                    }

                    @Override public int size() { return methodCount; }
                };
            }
        }
        return ImmutableSet.of();
    }

    private int getClassDataOffset() {
        if (classDataOffset == -1) {
            classDataOffset = dexFile.readSmallUint(classDefOffset + ClassDefItem.CLASS_DATA_OFFSET);
        }
        return classDataOffset;
    }

    private AnnotationsDirectory getAnnotationsDirectory() {
        if (annotationsDirectory == null) {
            int annotationsDirectoryOffset = dexFile.readSmallUint(classDefOffset + ClassDefItem.ANNOTATIONS_OFFSET);
            annotationsDirectory = AnnotationsDirectory.newOrEmpty(dexFile, annotationsDirectoryOffset);
        }
        return annotationsDirectory;
    }
}
