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
import org.jf.dexlib2.dexbacked.util.AnnotationsDirectory;
import org.jf.dexlib2.dexbacked.util.FixedSizeList;
import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodParameter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class DexBackedMethod implements Method {
    @Nonnull public final DexBuffer dexBuf;
    @Nonnull public final DexBackedClassDef classDef;

    public final int accessFlags;

    private final int codeOffset;
    private final int parameterAnnotationSetListOffset;
    private final int methodAnnotationSetOffset;

    public final int methodIndex;

    private int methodIdItemOffset;
    private int protoIdItemOffset;
    private int parametersOffset = -1;

    // method_id_item offsets
    private static final int PROTO_OFFSET = 2;
    private static final int NAME_OFFSET = 4;

    // proto_id_item offsets
    private static final int RETURN_TYPE_OFFSET = 4;
    private static final int PARAMETERS_OFFSET = 8;

    public DexBackedMethod(@Nonnull DexReader reader,
                           @Nonnull DexBackedClassDef classDef,
                           int previousMethodIndex) {
        this.dexBuf = reader.getDexBuffer();
        this.classDef = classDef;

        int methodIndexDiff = reader.readSmallUleb128();
        this.methodIndex = methodIndexDiff + previousMethodIndex;
        this.accessFlags = reader.readSmallUleb128();
        this.codeOffset = reader.readSmallUleb128();

        this.methodAnnotationSetOffset = 0;
        this.parameterAnnotationSetListOffset = 0;
    }

    public DexBackedMethod(@Nonnull DexReader reader,
                           @Nonnull DexBackedClassDef classDef,
                           int previousMethodIndex,
                           @Nonnull AnnotationsDirectory.AnnotationIterator methodAnnotationIterator,
                           @Nonnull AnnotationsDirectory.AnnotationIterator paramaterAnnotationIterator) {
        this.dexBuf = reader.getDexBuffer();
        this.classDef = classDef;

        int methodIndexDiff = reader.readSmallUleb128();
        this.methodIndex = methodIndexDiff + previousMethodIndex;
        this.accessFlags = reader.readSmallUleb128();
        this.codeOffset = reader.readSmallUleb128();

        this.methodAnnotationSetOffset = methodAnnotationIterator.seekTo(methodIndex);
        this.parameterAnnotationSetListOffset = paramaterAnnotationIterator.seekTo(methodIndex);
    }

    @Nonnull @Override public String getContainingClass() { return classDef.getType(); }
    @Override public int getAccessFlags() { return accessFlags; }

    @Nonnull
    @Override
    public String getName() {
        return dexBuf.getString(dexBuf.readSmallUint(getMethodIdItemOffset() + NAME_OFFSET));
    }

    @Nonnull
    @Override
    public String getReturnType() {
        return dexBuf.getType(dexBuf.readSmallUint(getProtoIdItemOffset() + RETURN_TYPE_OFFSET));
    }

    @Nonnull
    @Override
    public List<? extends MethodParameter> getParameters() {
        if (getParametersOffset() > 0) {
            DexBackedMethodImplementation methodImpl = getImplementation();
            if (methodImpl != null) {
                return methodImpl.getParametersWithNames();
            }
            return getParametersWithoutNames();
        }
        return ImmutableList.of();
    }

    @Nonnull
    public List<? extends MethodParameter> getParametersWithoutNames() {
        final int parametersOffset = getParametersOffset();
        if (parametersOffset > 0) {
            final int size = dexBuf.readSmallUint(parametersOffset);
            final List<List<? extends DexBackedAnnotation>> parameterAnnotations =
                    AnnotationsDirectory.getParameterAnnotations(dexBuf, parameterAnnotationSetListOffset);

            return new FixedSizeList<MethodParameter>() {
                @Nonnull
                @Override
                public MethodParameter readItem(final int index) {
                    return new MethodParameter() {
                        @Nonnull
                        @Override
                        public String getType() {
                            int typeIndex = dexBuf.readUshort(parametersOffset + 4 + (index * 2));
                            return dexBuf.getType(typeIndex);
                        }

                        @Nonnull
                        @Override
                        public List<? extends Annotation> getAnnotations() {
                            if (index < parameterAnnotations.size()) {
                                return parameterAnnotations.get(index);
                            }
                            return ImmutableList.of();
                        }

                        @Nullable @Override public String getName() { return null; }
                        //TODO: iterate over the annotations to get the signature
                        @Nullable @Override public String getSignature() { return null; }
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
        return AnnotationsDirectory.getAnnotations(dexBuf, methodAnnotationSetOffset);
    }

    @Nullable
    @Override
    public DexBackedMethodImplementation getImplementation() {
        if (codeOffset > 0) {
            return new DexBackedMethodImplementation(dexBuf, this, codeOffset);
        }
        return null;
    }

    private int getMethodIdItemOffset() {
        if (methodIdItemOffset == 0) {
            methodIdItemOffset = dexBuf.getMethodIdItemOffset(methodIndex);
        }
        return methodIdItemOffset;
    }

    private int getProtoIdItemOffset() {
        if (protoIdItemOffset == 0) {
            int protoIndex = dexBuf.readUshort(getMethodIdItemOffset() + PROTO_OFFSET);
            protoIdItemOffset = dexBuf.getProtoIdItemOffset(protoIndex);
        }
        return protoIdItemOffset;
    }

    private int getParametersOffset() {
        if (parametersOffset == -1) {
            parametersOffset = dexBuf.readSmallUint(getProtoIdItemOffset() + PARAMETERS_OFFSET);
        }
        return parametersOffset;
    }

    /**
     * Skips the reader over a single encoded_method structure.
     * @param reader The {@code DexFileReader} to skip
     * @param previousMethodIndex The method index of the previous field, or 0 if this is the first
     * @return The method index of the field that was skipped
     */
    public static int skipEncodedMethod(@Nonnull DexReader reader, int previousMethodIndex) {
        int idxDiff = reader.readSmallUleb128();
        reader.skipUleb128();
        reader.skipUleb128();
        return previousMethodIndex + idxDiff;
    }
}
