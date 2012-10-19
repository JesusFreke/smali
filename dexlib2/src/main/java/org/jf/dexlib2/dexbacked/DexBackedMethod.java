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

import com.google.common.collect.ImmutableList;
import org.jf.dexlib2.DexFile;
import org.jf.dexlib2.DexFileReader;
import org.jf.dexlib2.dexbacked.util.AnnotationsDirectory;
import org.jf.dexlib2.dexbacked.util.FixedSizeList;
import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.MethodParameter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class DexBackedMethod implements Method {
    @Nonnull public final DexFile dexFile;

    @Nonnull public final String name;
    public final int accessFlags;
    @Nonnull public final String returnType;

    private final int codeOffset;
    private final int parametersOffset;
    private final int methodAnnotationSetOffset;
    private final List<List<? extends DexBackedAnnotation>> parameterAnnotations;

    public final int methodIndex;

    // method_id_item offsets
    private static final int PROTO_OFFSET = 2;
    private static final int NAME_OFFSET = 4;

    // proto_id_item offsets
    private static final int RETURN_TYPE_OFFSET = 4;
    private static final int PARAMETERS_OFFSET = 8;

    public DexBackedMethod(@Nonnull DexFileReader dexFileReader,
                          int previousMethodIndex,
                          @Nonnull AnnotationsDirectory.AnnotationIterator methodAnnotationIterator,
                          @Nonnull AnnotationsDirectory.AnnotationIterator paramaterAnnotationIterator) {
        this.dexFile = dexFileReader.getDexFile();

        int methodIndexDiff = dexFileReader.readSmallUleb128();
        this.methodIndex = methodIndexDiff + previousMethodIndex;
        this.accessFlags = dexFileReader.readSmallUleb128();
        this.codeOffset = dexFileReader.readSmallUleb128();

        this.methodAnnotationSetOffset = methodAnnotationIterator.seekTo(methodIndex);
        int parameterAnnotationSetListOffset = paramaterAnnotationIterator.seekTo(methodIndex);
        this.parameterAnnotations =
                AnnotationsDirectory.getParameterAnnotations(dexFile, parameterAnnotationSetListOffset);

        int methodIdItemOffset = dexFileReader.getMethodIdItemOffset(methodIndex);
        int protoIndex = dexFileReader.readUshort(methodIdItemOffset + PROTO_OFFSET);
        int protoIdItemOffset = dexFileReader.getProtoIdItemOffset(protoIndex);

        this.name = dexFileReader.getString(dexFileReader.readSmallUint(methodIdItemOffset + NAME_OFFSET));

        this.returnType = dexFileReader.getString(dexFileReader.readSmallUint(protoIdItemOffset + RETURN_TYPE_OFFSET));
        this.parametersOffset = dexFileReader.readSmallUint(protoIdItemOffset + PARAMETERS_OFFSET);
    }


    @Nonnull @Override public String getName() { return name; }
    @Override public int getAccessFlags() { return accessFlags; }
    @Nonnull @Override public String getReturnType() { return returnType; }

    @Nonnull
    @Override
    public List<? extends MethodParameter> getParameters() {
        if (parametersOffset > 0) {
            final int size = dexFile.readSmallUint(parametersOffset);

            return new FixedSizeList<MethodParameter>() {
                @Override
                public MethodParameter readItem(final int index) {
                    return new MethodParameter() {
                        @Nonnull
                        @Override
                        public String getType() {
                            int typeIndex = dexFile.readUshort(parametersOffset + 4 + (index * 2));
                            return dexFile.getType(typeIndex);
                        }

                        @Nonnull
                        @Override
                        public List<? extends Annotation> getAnnotations() {
                            if (index < parameterAnnotations.size()) {
                                return parameterAnnotations.get(index);
                            }
                            return ImmutableList.of();
                        }
                    };
                }

                @Override public int size() { return size; }
            };
        }

        return ImmutableList.of();
    }

    @Nonnull
    @Override
    public List<? extends Annotation> getAnnotations() {
        return AnnotationsDirectory.getAnnotations(dexFile, methodAnnotationSetOffset);
    }

    @Nullable
    @Override
    public MethodImplementation getImplementation() {
        if (codeOffset > 0) {
            return new DexBackedMethodImplementation(dexFile, codeOffset);
        }
        return null;
    }

    /**
     * Skips the reader over a single encoded_method structure.
     * @param dexFileReader The {@code DexFileReader} to skip
     * @param previousMethodIndex The method index of the previous field, or 0 if this is the first
     * @return The method index of the field that was skipped
     */
    public static int skipEncodedMethod(@Nonnull DexFileReader dexFileReader, int previousMethodIndex) {
        int idxDiff = dexFileReader.readSmallUleb128();
        dexFileReader.skipUleb128();
        dexFileReader.skipUleb128();
        return previousMethodIndex + idxDiff;
    }
}
