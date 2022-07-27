/*
 * Copyright 2014, Google Inc.
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

package org.jf.dexlib2.rewriter;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.jf.util.ExceptionWithContext;
import org.jf.dexlib2.MethodHandleType;
import org.jf.dexlib2.ValueType;
import org.jf.dexlib2.base.reference.BaseMethodHandleReference;
import org.jf.dexlib2.base.reference.BaseMethodProtoReference;
import org.jf.dexlib2.base.reference.BaseTypeReference;
import org.jf.dexlib2.base.value.*;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.iface.reference.MethodHandleReference;
import org.jf.dexlib2.iface.reference.MethodProtoReference;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.iface.reference.Reference;
import org.jf.dexlib2.iface.reference.TypeReference;
import org.jf.dexlib2.iface.value.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class RewriterUtils {
    @Nullable
    public static <T> T rewriteNullable(@Nonnull Rewriter<T> rewriter, @Nullable T value) {
        return value==null?null:rewriter.rewrite(value);
    }

    public static <T> Set<T> rewriteSet(@Nonnull final Rewriter<T> rewriter,
                                        @Nonnull final Set<? extends T> set) {
        return new AbstractSet<T>() {
            @Nonnull @Override public Iterator<T> iterator() {
                final Iterator<? extends T> iterator = set.iterator();
                return new Iterator<T>() {
                    @Override public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    @Override public T next() {
                        return rewriteNullable(rewriter, iterator.next());
                    }

                    @Override public void remove() {
                        iterator.remove();
                    }
                };
            }

            @Override public int size() {
                return set.size();
            }
        };
    }

    public static <T> List<T> rewriteList(@Nonnull final Rewriter<T> rewriter,
                                        @Nonnull final List<? extends T> list) {
        return new AbstractList<T>() {
            @Override public T get(int i) {
                return rewriteNullable(rewriter, list.get(i));
            }

            @Override public int size() {
                return list.size();
            }
        };
    }

    public static <T> Iterable<T> rewriteIterable(@Nonnull final Rewriter<T> rewriter,
                                                  @Nonnull final Iterable<? extends T> iterable) {
        return new Iterable<T>() {
            @Override public Iterator<T> iterator() {
                final Iterator<? extends T> iterator = iterable.iterator();
                return new Iterator<T>() {
                    @Override public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    @Override public T next() {
                        return rewriteNullable(rewriter, iterator.next());
                    }

                    @Override public void remove() {
                        iterator.remove();
                    }
                };
            }
        };
    }

    public static TypeReference rewriteTypeReference(@Nonnull final Rewriter<String> typeRewriter,
                                                     @Nonnull final TypeReference typeReference) {
        return new BaseTypeReference() {
            @Nonnull @Override public String getType() {
                return typeRewriter.rewrite(typeReference.getType());
            }
        };
    }

    @Nonnull public static MethodHandleReference rewriteMethodHandleReference(
            @Nonnull final Rewriters rewriters,
            @Nonnull final MethodHandleReference methodHandleReference) {
        switch (methodHandleReference.getMethodHandleType()) {
            case MethodHandleType.STATIC_PUT:
            case MethodHandleType.STATIC_GET:
            case MethodHandleType.INSTANCE_PUT:
            case MethodHandleType.INSTANCE_GET:
                return new BaseMethodHandleReference() {
                    @Override public int getMethodHandleType() {
                        return methodHandleReference.getMethodHandleType();
                    }

                    @Nonnull @Override public Reference getMemberReference() {
                        return rewriters.getFieldReferenceRewriter().rewrite((FieldReference)methodHandleReference.getMemberReference());
                    }
                };
            case MethodHandleType.INVOKE_STATIC:
            case MethodHandleType.INVOKE_INSTANCE:
            case MethodHandleType.INVOKE_CONSTRUCTOR:
            case MethodHandleType.INVOKE_DIRECT:
            case MethodHandleType.INVOKE_INTERFACE:
                return new BaseMethodHandleReference() {
                    @Override public int getMethodHandleType() {
                        return methodHandleReference.getMethodHandleType();
                    }

                    @Nonnull @Override public Reference getMemberReference() {
                        return rewriters.getMethodReferenceRewriter().rewrite((MethodReference)methodHandleReference.getMemberReference());
                    }
                };
            default:
                throw new ExceptionWithContext("Invalid method handle type: %d",
                        methodHandleReference.getMethodHandleType());
        }
    }

    @Nonnull public static MethodProtoReference rewriteMethodProtoReference(
            @Nonnull final Rewriter<String> typeRewriter,
            @Nonnull final MethodProtoReference methodProtoReference) {
        return new BaseMethodProtoReference() {
            @Nonnull @Override public List<? extends CharSequence> getParameterTypes() {
                return rewriteList(typeRewriter,
                        Lists.transform(methodProtoReference.getParameterTypes(),
                        new Function<CharSequence, String>() {
                            @Nonnull @Override public String apply(CharSequence input) {
                                return input.toString();
                            }
                        }));
            }

            @Nonnull @Override public String getReturnType() {
                return typeRewriter.rewrite(methodProtoReference.getReturnType());
            }
        };
    }

    @Nonnull public static EncodedValue rewriteValue(
            @Nonnull final Rewriters rewriters,
            @Nonnull final EncodedValue encodedValue) {
        switch (encodedValue.getValueType()) {
            case ValueType.INT:
            case ValueType.FLOAT:
            case ValueType.LONG:
            case ValueType.DOUBLE:
            case ValueType.STRING:
                return encodedValue;

            case ValueType.METHOD_TYPE:
                return new BaseMethodTypeEncodedValue () {
                    @Override @Nonnull public MethodProtoReference getValue() {
                        return rewriteMethodProtoReference(
                            rewriters.getTypeRewriter(),
                            ((MethodTypeEncodedValue) encodedValue).getValue());
                    }
                };

            case ValueType.METHOD_HANDLE:
                return new BaseMethodHandleEncodedValue () {
                    @Override @Nonnull public MethodHandleReference getValue() {
                        return rewriteMethodHandleReference(
                            rewriters,
                            ((MethodHandleEncodedValue) encodedValue).getValue());
                    }
                };

            case ValueType.TYPE:
                return new BaseTypeEncodedValue () {
                    @Override @Nonnull public String getValue() {
                        return rewriters.getTypeRewriter().rewrite(((TypeEncodedValue) encodedValue).getValue());
                    }
                };

            case ValueType.FIELD:
                return new BaseFieldEncodedValue () {
                    @Override @Nonnull public FieldReference getValue() {
                        return rewriters.getFieldReferenceRewriter().rewrite(((FieldEncodedValue) encodedValue).getValue());
                    }
                };

            case ValueType.METHOD:
                return new BaseMethodEncodedValue () {
                    @Override @Nonnull public MethodReference getValue() {
                        return rewriters.getMethodReferenceRewriter().rewrite(((MethodEncodedValue) encodedValue).getValue());
                    }
                };

            default:
                throw new ExceptionWithContext("Unsupported encoded value type: %d",
                        encodedValue.getValueType());
        }
    }
}


