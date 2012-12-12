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

package org.jf.dexlib2.writer;

import com.google.common.base.Predicate;
import com.google.common.collect.*;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodParameter;
import org.jf.util.CollectionUtils;
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;

public class AnnotationDirectoryPool {
    @Nonnull private final Map<Key, Integer> internedAnnotationDirectoryItems = Maps.newHashMap();
    @Nonnull private final Map<String, Integer> nonInternedAnnotationDirectoryOffsetMap = Maps.newHashMap();
    @Nonnull private final List<Key> nonInternedAnnotationDirectoryItems = Lists.newArrayList();
    @Nonnull private final DexFile dexFile;

    public AnnotationDirectoryPool(@Nonnull DexFile dexFile) {
        this.dexFile = dexFile;
    }

    public void intern(@Nonnull ClassDef classDef) {
        Key key =  new Key(classDef);

        if (key.hasNonClassAnnotations()) {
            nonInternedAnnotationDirectoryItems.add(key);
        } else if (key.hasClassAnnotations()) {
            Integer prev = internedAnnotationDirectoryItems.put(key, 0);
            if (prev != null) {
                // we don't need to re-intern the contents
                return;
            }
        } else {
            // it's empty. nothing to do.
            return;
        }

        dexFile.annotationSetPool.intern(classDef.getAnnotations());
        for (Field field: key.getFieldsWithAnnotations()) {
            dexFile.annotationSetPool.intern(field.getAnnotations());
        }

        for (Method method: key.getMethodsWithAnnotations()) {
            dexFile.annotationSetPool.intern(method.getAnnotations());
        }

        for (Method method: key.getMethodsWithParameterAnnotations()) {
            dexFile.annotationSetRefPool.intern(method);
        }
    }

    public int getOffset(@Nonnull ClassDef classDef) {
        Integer offset = nonInternedAnnotationDirectoryOffsetMap.get(classDef.getType());
        if (offset == null) {
            Key key = new Key(classDef);
            if (!key.hasNonClassAnnotations()) {
                offset = internedAnnotationDirectoryItems.get(key);
            }
            if (offset == null) {
                if (key.hasClassAnnotations() || key.hasNonClassAnnotations()) {
                    throw new ExceptionWithContext("Annotation directory not found for class %s.", classDef.getType());
                }
                offset = 0;
            }
        }
        return offset;
    }

    public void write(@Nonnull DexWriter writer) throws IOException {
        // we'll write out the interned items first
        List<Key> directoryItems = Lists.newArrayList(internedAnnotationDirectoryItems.keySet());
        Collections.sort(directoryItems);
        for (Key key: directoryItems) {
            writer.align();
            internedAnnotationDirectoryItems.put(key, writer.getPosition());
            writer.writeInt(dexFile.annotationSetPool.getOffset(key.classDef.getAnnotations()));
            writer.writeInt(0);
            writer.writeInt(0);
            writer.writeInt(0);
        }

        // now, write out the non-internable items
        directoryItems = nonInternedAnnotationDirectoryItems;
        Collections.sort(directoryItems);
        for (Key key: directoryItems) {
            writer.align();
            nonInternedAnnotationDirectoryOffsetMap.put(key.classDef.getType(), writer.getPosition());
            writer.writeInt(dexFile.annotationSetPool.getOffset(key.classDef.getAnnotations()));
            writer.writeInt(key.fieldAnnotationCount);
            writer.writeInt(key.methodAnnotationCount);
            writer.writeInt(key.parameterAnnotationCount);

            Iterable<? extends Field> fieldsWithAnnotations = null;
            if (CollectionUtils.isNaturalSortedSet(key.classDef.getFields())) {
                fieldsWithAnnotations = key.getFieldsWithAnnotations();
            } else {
                fieldsWithAnnotations = Lists.newArrayList(key.getFieldsWithAnnotations());
                Collections.sort((List<? extends Field>)fieldsWithAnnotations);
            }
            for (Field field: fieldsWithAnnotations) {
                writer.writeInt(dexFile.fieldPool.getIndex(field));
                writer.writeInt(dexFile.annotationSetPool.getOffset(field.getAnnotations()));
            }

            boolean sortMethods = CollectionUtils.isNaturalSortedSet(key.classDef.getMethods());
            Iterable<? extends Method> methodsWithAnnotations = null;
            if (sortMethods) {
                methodsWithAnnotations = Lists.newArrayList(key.getMethodsWithAnnotations());
                Collections.sort((List<? extends Method>)methodsWithAnnotations);
            } else {
                methodsWithAnnotations = key.getMethodsWithAnnotations();
            }
            for (Method method: methodsWithAnnotations) {
                writer.writeInt(dexFile.methodPool.getIndex(method));
                writer.writeInt(dexFile.annotationSetPool.getOffset(method.getAnnotations()));
            }

            Iterable<? extends Method> methodsWithParameterAnnotations = null;
            if (sortMethods) {
                methodsWithParameterAnnotations = Lists.newArrayList(key.getMethodsWithParameterAnnotations());
                Collections.sort((List<? extends Method>)methodsWithParameterAnnotations);
            } else {
                methodsWithParameterAnnotations = key.getMethodsWithParameterAnnotations();
            }
            for (Method method: methodsWithParameterAnnotations) {
                writer.writeInt(dexFile.methodPool.getIndex(method));
                writer.writeInt(dexFile.annotationSetRefPool.getOffset(method));
            }
        }
    }

    private static final Predicate<Field> FIELD_HAS_ANNOTATION = new Predicate<Field>() {
        @Override
        public boolean apply(Field input) { return input.getAnnotations().size() > 0; }
    };

    private static final Predicate<Method> METHOD_HAS_ANNOTATION = new Predicate<Method>() {
        @Override
        public boolean apply(Method input) { return input.getAnnotations().size() > 0; }
    };

    private static final Predicate<Method> METHOD_HAS_PARAMETER_ANNOTATION = new Predicate<Method>() {
        @Override
        public boolean apply(Method input) {
            for (MethodParameter parameter: input.getParameters()) {
                if (parameter.getAnnotations().size() > 0) {
                    return true;
                }
            }
            return false;
        }
    };

    private static class Key implements Comparable<Key> {
        @Nonnull private final ClassDef classDef;
        private final int fieldAnnotationCount;
        private final int methodAnnotationCount;
        private final int parameterAnnotationCount;

        public Key(@Nonnull ClassDef classDef) {
            this.classDef = classDef;
            this.fieldAnnotationCount = Iterables.size(getFieldsWithAnnotations());
            this.methodAnnotationCount = Iterables.size(getMethodsWithAnnotations());
            this.parameterAnnotationCount = Iterables.size(getMethodsWithParameterAnnotations());
        }

        public int getFieldAnnotationCount() { return fieldAnnotationCount; }
        public int getMethodAnnotationCount() { return methodAnnotationCount; }
        public int getParameterAnnotationCount() { return parameterAnnotationCount; }

        @Nonnull
        public Iterable<? extends Field> getFieldsWithAnnotations() {
            return FluentIterable.from(classDef.getFields()).filter(FIELD_HAS_ANNOTATION);
        }

        @Nonnull
        public Iterable<? extends Method> getMethodsWithAnnotations() {
            return FluentIterable.from(classDef.getMethods()).filter(METHOD_HAS_ANNOTATION);
        }

        @Nonnull
        public Iterable<? extends Method> getMethodsWithParameterAnnotations() {
            return FluentIterable.from(classDef.getMethods()).filter(METHOD_HAS_PARAMETER_ANNOTATION);
        }

        public boolean hasClassAnnotations() {
            return classDef.getAnnotations().size() > 0;
        }

        public boolean hasNonClassAnnotations() {
            return fieldAnnotationCount > 0 ||
                    methodAnnotationCount > 0 ||
                    parameterAnnotationCount > 0;
        }

        @Override
        public int hashCode() {
            // hashCode is only used for internable items - those that only have class annotations.
            return classDef.getAnnotations().hashCode();
        }

        @Override
        public boolean equals(Object o) {
            // equals is only used for internable items - those that only have class annotations
            if (o instanceof Key) {
                Key other = (Key)o;
                if (classDef.getAnnotations().size() != other.classDef.getAnnotations().size()) {
                    return false;
                }
                return Iterables.elementsEqual(classDef.getAnnotations(), other.classDef.getAnnotations());
            }
            return false;
        }

        @Override
        public int compareTo(Key o) {
            // compareTo will only be called on keys of the same internability. An internable key will not be compared
            // with a non-internable one.
            if (hasClassAnnotations()) {
                return classDef.getType().compareTo(o.classDef.getType());
            }
            return CollectionUtils.compareAsSet(classDef.getAnnotations(), o.classDef.getAnnotations());
        }
    }
}
