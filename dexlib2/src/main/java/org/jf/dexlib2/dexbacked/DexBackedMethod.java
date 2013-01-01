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
import com.google.common.collect.Iterators;
import org.jf.dexlib2.base.reference.BaseMethodReference;
import org.jf.dexlib2.dexbacked.util.AnnotationsDirectory;
import org.jf.dexlib2.dexbacked.util.FixedSizeList;
import org.jf.dexlib2.dexbacked.util.ParameterIterator;
import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodParameter;
import org.jf.util.AbstractForwardSequentialList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class DexBackedMethod extends BaseMethodReference implements Method {
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
        this.dexBuf = reader.dexBuf;
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
        this.dexBuf = reader.dexBuf;
        this.classDef = classDef;

        int methodIndexDiff = reader.readSmallUleb128();
        this.methodIndex = methodIndexDiff + previousMethodIndex;
        this.accessFlags = reader.readSmallUleb128();
        this.codeOffset = reader.readSmallUleb128();

        this.methodAnnotationSetOffset = methodAnnotationIterator.seekTo(methodIndex);
        this.parameterAnnotationSetListOffset = paramaterAnnotationIterator.seekTo(methodIndex);
    }

    public int getMethodIndex() { return methodIndex; }
    @Nonnull @Override public String getDefiningClass() { return classDef.getType(); }
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
        int parametersOffset = getParametersOffset();
        if (parametersOffset > 0) {
            final List<String> parameterTypes = getParameterTypes();

            return new AbstractForwardSequentialList<MethodParameter>() {
                @Nonnull @Override public Iterator<MethodParameter> iterator() {
                    return new ParameterIterator(parameterTypes,
                            getParameterAnnotations(),
                            getParameterNames());
                }

                @Override public int size() {
                    return parameterTypes.size();
                }
            };
        }
        return ImmutableList.of();
    }

    @Nonnull
    public List<? extends Set<? extends DexBackedAnnotation>> getParameterAnnotations() {
        return AnnotationsDirectory.getParameterAnnotations(dexBuf, parameterAnnotationSetListOffset);
    }

    @Nonnull
    public Iterator<String> getParameterNames() {
        DexBackedMethodImplementation methodImpl = getImplementation();
        if (methodImpl != null) {
            return methodImpl.getParameterNames(null);
        }
        return Iterators.emptyIterator();
    }

    @Nonnull
    @Override
    public List<String> getParameterTypes() {
        final int parametersOffset = getParametersOffset();
        if (parametersOffset > 0) {
            final int parameterCount = dexBuf.readSmallUint(parametersOffset + DexBuffer.TYPE_LIST_SIZE_OFFSET);
            final int paramListStart = parametersOffset + DexBuffer.TYPE_LIST_LIST_OFFSET;
            return new FixedSizeList<String>() {
                @Nonnull
                @Override
                public String readItem(final int index) {
                    return dexBuf.getType(dexBuf.readUshort(paramListStart + 2*index));
                }
                @Override public int size() { return parameterCount; }
            };
        }
        return ImmutableList.of();
    }

    @Nonnull
    @Override
    public Set<? extends Annotation> getAnnotations() {
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
}
