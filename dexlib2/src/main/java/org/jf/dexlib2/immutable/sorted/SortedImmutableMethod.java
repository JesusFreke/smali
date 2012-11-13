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

package org.jf.dexlib2.immutable.sorted;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMultiset;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Multiset;
import org.jf.dexlib2.base.reference.BaseMethodReference;
import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.MethodParameter;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.iface.reference.TypeReference;
import org.jf.dexlib2.iface.sorted.SortedMethod;
import org.jf.dexlib2.iface.sorted.SortedMethodParameter;
import org.jf.dexlib2.immutable.ImmutableMethodImplementation;
import org.jf.dexlib2.util.MethodUtil;
import org.jf.dexlib2.util.ReferenceUtil;
import org.jf.util.ImmutableSortedSetConverter;
import org.jf.util.ImmutableUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;

public class SortedImmutableMethod extends BaseMethodReference implements SortedMethod {
    @Nonnull public final String containingClass;
    @Nonnull public final String name;
    @Nonnull public final ImmutableList<? extends SortedImmutableMethodParameter> parameters;
    @Nonnull public final String returnType;
    public final int accessFlags;
    @Nonnull public final ImmutableSortedSet<? extends SortedImmutableAnnotation> annotations;
    @Nullable public final ImmutableMethodImplementation methodImplementation;

    public SortedImmutableMethod(@Nonnull String containingClass,
                                 @Nonnull String name,
                                 @Nullable Collection<? extends MethodParameter> parameters,
                                 @Nonnull String returnType,
                                 int accessFlags,
                                 @Nullable Collection<? extends Annotation> annotations,
                                 @Nullable MethodImplementation methodImplementation) {
        this.containingClass = containingClass;
        this.name = name;
        this.parameters = SortedImmutableMethodParameter.immutableListOf(parameters);
        this.returnType = returnType;
        this.accessFlags = accessFlags;
        this.annotations = SortedImmutableAnnotation.immutableSortedSetOf(annotations);
        this.methodImplementation = ImmutableMethodImplementation.of(methodImplementation);
    }

    public SortedImmutableMethod(@Nonnull String containingClass,
                                 @Nonnull String name,
                                 @Nullable ImmutableList<? extends SortedImmutableMethodParameter> parameters,
                                 @Nonnull String returnType,
                                 int accessFlags,
                                 @Nullable ImmutableSortedSet<? extends SortedImmutableAnnotation> annotations,
                                 @Nullable ImmutableMethodImplementation methodImplementation) {
        this.containingClass = containingClass;
        this.name = name;
        this.parameters = ImmutableUtils.nullToEmptyList(parameters);
        this.returnType = returnType;
        this.accessFlags = accessFlags;
        this.annotations = ImmutableUtils.nullToEmptySortedSet(annotations);
        this.methodImplementation = methodImplementation;
    }

    public static SortedImmutableMethod of(Method method) {
        if (method instanceof SortedImmutableMethod) {
            return (SortedImmutableMethod)method;
        }
        return new SortedImmutableMethod(
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
    @Nonnull public ImmutableList<? extends SortedMethodParameter> getParameters() { return parameters; }
    @Nonnull public String getReturnType() { return returnType; }
    public int getAccessFlags() { return accessFlags; }
    @Nonnull public ImmutableSortedSet<? extends SortedImmutableAnnotation> getAnnotations() { return annotations; }
    @Nullable public ImmutableMethodImplementation getImplementation() { return methodImplementation; }

    public static final Comparator<MethodReference> COMPARE_BY_SIGNATURE = new Comparator<MethodReference>() {
        @Override
        public int compare(MethodReference method1, MethodReference method2) {
            int res = method1.getContainingClass().compareTo(method2.getContainingClass());
            if (res != 0) {
                return res;
            }
            res = method1.getName().compareTo(method2.getName());
            if (res != 0) {
                return res;
            }
            res = method1.getReturnType().compareTo(method2.getReturnType());
            if (res != 0) {
                return res;
            }
            Collection<? extends TypeReference> params1 = method1.getParameters();
            Collection<? extends TypeReference> params2 = method2.getParameters();
            int params1Size = params1.size();
            int params2Size = params2.size();

            int minSize = Math.min(params1Size, params2Size);

            Iterator<? extends TypeReference> paramIter1 = params1.iterator();
            Iterator<? extends TypeReference> paramIter2 = params2.iterator();

            for (int i=0; i<minSize; i++) {
                res = paramIter1.next().getType().compareTo(paramIter2.next().getType());
                if (res != 0) {
                    return res;
                }
            }

            if (params1Size < params2Size) {
                return -1;
            } else if (params1Size > params2Size) {
                return 1;
            } else {
                return 0;
            }
        }
    };

    @Nonnull
    public static ImmutableSortedSet<SortedImmutableMethod> immutableSortedSetOf(
            @Nullable Collection<? extends Method> list) {
        ImmutableSortedSet<SortedImmutableMethod> set = CONVERTER.convert(COMPARE_BY_SIGNATURE, list);
        if (list != null && set.size() < list.size()) {
            // There were duplicate methods. Let's find them and print a warning.
            ImmutableSortedMultiset<Method> multiset = ImmutableSortedMultiset.copyOf(COMPARE_BY_SIGNATURE, list);
            for (Multiset.Entry<Method> entry: multiset.entrySet()) {
                Method method = entry.getElement();
                String methodType = MethodUtil.isDirect(method)?"direct":"virtual";
                // TODO: need to provide better context
                System.err.println(String.format("Ignoring duplicate %s method definition for method: %s", methodType,
                        ReferenceUtil.getMethodDescriptor(method)));
            }
        }
        return set;
    }

    private static final ImmutableSortedSetConverter<SortedImmutableMethod, Method> CONVERTER =
            new ImmutableSortedSetConverter<SortedImmutableMethod, Method>() {
                @Override
                protected boolean isImmutable(@Nonnull Method item) {
                    return item instanceof SortedImmutableMethod;
                }

                @Nonnull
                @Override
                protected SortedImmutableMethod makeImmutable(@Nonnull Method item) {
                    return SortedImmutableMethod.of(item);
                }
            };
}
