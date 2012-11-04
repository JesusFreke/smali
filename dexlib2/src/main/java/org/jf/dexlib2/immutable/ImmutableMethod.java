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

package org.jf.dexlib2.immutable;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import org.jf.dexlib2.iface.*;
import org.jf.util.ImmutableListConverter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ImmutableMethod implements Method {
    @Nonnull public final String containingClass;
    @Nonnull public final String name;
    @Nonnull public final ImmutableList<? extends ImmutableMethodParameter> parameters;
    @Nonnull public final String returnType;
    public final int accessFlags;
    @Nonnull public final ImmutableList<? extends ImmutableAnnotation> annotations;
    @Nullable public final ImmutableMethodImplementation methodImplementation;

    public ImmutableMethod(@Nonnull String containingClass,
                           @Nonnull String name,
                           @Nullable List<? extends MethodParameter> parameters,
                           @Nonnull String returnType,
                           int accessFlags,
                           @Nullable List<? extends Annotation> annotations,
                           @Nullable MethodImplementation methodImplementation) {
        this.containingClass = containingClass;
        this.name = name;
        this.parameters = ImmutableMethodParameter.immutableListOf(parameters);
        this.returnType = returnType;
        this.accessFlags = accessFlags;
        this.annotations = ImmutableAnnotation.immutableListOf(annotations);
        this.methodImplementation = ImmutableMethodImplementation.of(methodImplementation);
    }

    public ImmutableMethod(@Nonnull String containingClass,
                           @Nonnull String name,
                           @Nullable ImmutableList<? extends ImmutableMethodParameter> parameters,
                           @Nonnull String returnType,
                           int accessFlags,
                           @Nullable ImmutableList<? extends ImmutableAnnotation> annotations,
                           @Nullable ImmutableMethodImplementation methodImplementation) {
        this.containingClass = containingClass;
        this.name = name;
        this.parameters = Objects.firstNonNull(parameters, ImmutableList.<ImmutableMethodParameter>of());
        this.returnType = returnType;
        this.accessFlags = accessFlags;
        this.annotations = Objects.firstNonNull(annotations, ImmutableList.<ImmutableAnnotation>of());
        this.methodImplementation = methodImplementation;
    }

    public static ImmutableMethod of(Method method) {
        if (method instanceof ImmutableMethod) {
            return (ImmutableMethod)method;
        }
        return new ImmutableMethod(
                method.getContainingClass(),
                method.getName(),
                method.getParameters(),
                method.getReturnType(),
                method.getAccessFlags(),
                method.getAnnotations(),
                method.getImplementation());
    }

    @Nonnull public String getContainingClass() { return containingClass; }
    @Nonnull public String getName() { return name; }
    @Nonnull public ImmutableList<? extends ImmutableMethodParameter> getParameters() { return parameters; }
    @Nonnull public String getReturnType() { return returnType; }
    public int getAccessFlags() { return accessFlags; }
    @Nonnull public ImmutableList<? extends ImmutableAnnotation> getAnnotations() { return annotations; }
    @Nullable public ImmutableMethodImplementation getImplementation() { return methodImplementation; }

    @Nonnull
    public static ImmutableList<ImmutableMethod> immutableListOf(List<? extends Method> list) {
        return CONVERTER.convert(list);
    }

    private static final ImmutableListConverter<ImmutableMethod, Method> CONVERTER =
            new ImmutableListConverter<ImmutableMethod, Method>() {
                @Override
                protected boolean isImmutable(Method item) {
                    return item instanceof ImmutableMethod;
                }

                @Override
                protected ImmutableMethod makeImmutable(Method item) {
                    return ImmutableMethod.of(item);
                }
            };
}
