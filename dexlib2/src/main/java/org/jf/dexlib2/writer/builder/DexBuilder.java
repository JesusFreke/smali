/*
 * Copyright 2013, Google Inc.
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

package org.jf.dexlib2.writer.builder;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.ValueType;
import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.AnnotationElement;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.MethodParameter;
import org.jf.dexlib2.iface.reference.*;
import org.jf.dexlib2.iface.value.*;
import org.jf.dexlib2.writer.DexWriter;
import org.jf.dexlib2.writer.builder.BuilderEncodedValues.*;
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class DexBuilder extends DexWriter<BuilderStringReference, BuilderStringReference, BuilderTypeReference,
        BuilderTypeReference, BuilderMethodProtoReference, BuilderFieldReference, BuilderMethodReference,
        BuilderClassDef, BuilderAnnotation, BuilderAnnotationSet, BuilderTypeList, BuilderField, BuilderMethod,
        BuilderEncodedValue, BuilderAnnotationElement, BuilderStringPool, BuilderTypePool, BuilderProtoPool,
        BuilderFieldPool, BuilderMethodPool, BuilderClassPool, BuilderTypeListPool, BuilderAnnotationPool,
        BuilderAnnotationSetPool> {

    public DexBuilder(@Nonnull Opcodes opcodes) {
        super(opcodes);
    }

    @Nonnull @Override protected SectionProvider getSectionProvider() {
        return new DexBuilderSectionProvider();
    }

    @Nonnull public BuilderField internField(@Nonnull String definingClass,
                                             @Nonnull String name,
                                             @Nonnull String type,
                                             int accessFlags,
                                             @Nullable EncodedValue initialValue,
                                             @Nonnull Set<? extends Annotation> annotations) {
        return new BuilderField(fieldSection.internField(definingClass, name, type),
                accessFlags,
                internNullableEncodedValue(initialValue),
                annotationSetSection.internAnnotationSet(annotations));
    }

    @Nonnull public BuilderMethod internMethod(@Nonnull String definingClass,
                                               @Nonnull String name,
                                               @Nullable List<? extends MethodParameter> parameters,
                                               @Nonnull String returnType,
                                               int accessFlags,
                                               @Nonnull Set<? extends Annotation> annotations,
                                               @Nullable MethodImplementation methodImplementation) {
        if (parameters == null) {
            parameters = ImmutableList.of();
        }
        return new BuilderMethod(methodSection.internMethod(definingClass, name, parameters, returnType),
                internMethodParameters(parameters),
                accessFlags,
                annotationSetSection.internAnnotationSet(annotations),
                methodImplementation);
    }

    @Nonnull public BuilderClassDef internClassDef(@Nonnull String type,
                                                   int accessFlags,
                                                   @Nullable String superclass,
                                                   @Nullable List<String> interfaces,
                                                   @Nullable String sourceFile,
                                                   @Nonnull Set<? extends Annotation> annotations,
                                                   @Nullable Iterable<? extends BuilderField> fields,
                                                   @Nullable Iterable<? extends BuilderMethod> methods) {
        if (interfaces == null) {
            interfaces = ImmutableList.of();
        } else {
            Set<String> interfaces_copy = Sets.newHashSet(interfaces);
            Iterator<String> interfaceIterator = interfaces.iterator();
            while (interfaceIterator.hasNext()) {
                String iface = interfaceIterator.next();
                if (!interfaces_copy.contains(iface)) {
                    interfaceIterator.remove();
                } else {
                    interfaces_copy.remove(iface);
                }
            }
        }

        return classSection.internClass(new BuilderClassDef(typeSection.internType(type),
                accessFlags,
                typeSection.internNullableType(superclass),
                typeListSection.internTypeList(interfaces),
                stringSection.internNullableString(sourceFile),
                annotationSetSection.internAnnotationSet(annotations),
                fields,
                methods));
    }

    @Nonnull public BuilderStringReference internStringReference(@Nonnull String string) {
        return stringSection.internString(string);
    }

    @Nullable public BuilderStringReference internNullableStringReference(@Nullable String string) {
        if (string != null) {
            return internStringReference(string);
        }
        return null;
    }

    @Nonnull public BuilderTypeReference internTypeReference(@Nonnull String type) {
        return typeSection.internType(type);
    }

    @Nullable public BuilderTypeReference internNullableTypeReference(@Nullable String type) {
        if (type != null) {
            return internTypeReference(type);
        }
        return null;
    }

    @Nonnull public BuilderFieldReference internFieldReference(@Nonnull FieldReference field) {
        return fieldSection.internField(field);
    }

    @Nonnull public BuilderMethodReference internMethodReference(@Nonnull MethodReference method) {
        return methodSection.internMethod(method);
    }

    @Nonnull public BuilderMethodProtoReference internMethodProtoReference(@Nonnull MethodProtoReference methodProto) {
        return protoSection.internMethodProto(methodProto);
    }

    @Nonnull public BuilderReference internReference(@Nonnull Reference reference) {
        if (reference instanceof StringReference) {
            return internStringReference(((StringReference)reference).getString());
        }
        if (reference instanceof TypeReference) {
            return internTypeReference(((TypeReference)reference).getType());
        }
        if (reference instanceof MethodReference) {
            return internMethodReference((MethodReference)reference);
        }
        if (reference instanceof FieldReference) {
            return internFieldReference((FieldReference)reference);
        }
        if (reference instanceof MethodProtoReference) {
            return internMethodProtoReference((MethodProtoReference) reference);
        }
        throw new IllegalArgumentException("Could not determine type of reference");
    }

    @Nonnull private List<BuilderMethodParameter> internMethodParameters(
            @Nullable List<? extends MethodParameter> methodParameters) {
        if (methodParameters == null) {
            return ImmutableList.of();
        }
        return ImmutableList.copyOf(Iterators.transform(methodParameters.iterator(),
                new Function<MethodParameter, BuilderMethodParameter>() {
                    @Nullable @Override public BuilderMethodParameter apply(MethodParameter input) {
                        return internMethodParameter(input);
                    }
                }));
    }

    @Nonnull private BuilderMethodParameter internMethodParameter(@Nonnull MethodParameter methodParameter) {
        return new BuilderMethodParameter(
                typeSection.internType(methodParameter.getType()),
                stringSection.internNullableString(methodParameter.getName()),
                annotationSetSection.internAnnotationSet(methodParameter.getAnnotations()));
    }

    @Override protected void writeEncodedValue(@Nonnull InternalEncodedValueWriter writer,
                                               @Nonnull BuilderEncodedValue encodedValue) throws IOException {
        switch (encodedValue.getValueType()) {
            case ValueType.ANNOTATION:
                BuilderAnnotationEncodedValue annotationEncodedValue = (BuilderAnnotationEncodedValue)encodedValue;
                writer.writeAnnotation(annotationEncodedValue.typeReference, annotationEncodedValue.elements);
                break;
            case ValueType.ARRAY:
                BuilderArrayEncodedValue arrayEncodedValue = (BuilderArrayEncodedValue)encodedValue;
                writer.writeArray(arrayEncodedValue.elements);
                break;
            case ValueType.BOOLEAN:
                writer.writeBoolean(((BooleanEncodedValue)encodedValue).getValue());
                break;
            case ValueType.BYTE:
                writer.writeByte(((ByteEncodedValue)encodedValue).getValue());
                break;
            case ValueType.CHAR:
                writer.writeChar(((CharEncodedValue)encodedValue).getValue());
                break;
            case ValueType.DOUBLE:
                writer.writeDouble(((DoubleEncodedValue)encodedValue).getValue());
                break;
            case ValueType.ENUM:
                writer.writeEnum(((BuilderEnumEncodedValue)encodedValue).getValue());
                break;
            case ValueType.FIELD:
                writer.writeField(((BuilderFieldEncodedValue)encodedValue).fieldReference);
                break;
            case ValueType.FLOAT:
                writer.writeFloat(((FloatEncodedValue)encodedValue).getValue());
                break;
            case ValueType.INT:
                writer.writeInt(((IntEncodedValue)encodedValue).getValue());
                break;
            case ValueType.LONG:
                writer.writeLong(((LongEncodedValue)encodedValue).getValue());
                break;
            case ValueType.METHOD:
                writer.writeMethod(((BuilderMethodEncodedValue)encodedValue).methodReference);
                break;
            case ValueType.NULL:
                writer.writeNull();
                break;
            case ValueType.SHORT:
                writer.writeShort(((ShortEncodedValue)encodedValue).getValue());
                break;
            case ValueType.STRING:
                writer.writeString(((BuilderStringEncodedValue)encodedValue).stringReference);
                break;
            case ValueType.TYPE:
                writer.writeType(((BuilderTypeEncodedValue)encodedValue).typeReference);
                break;
            default:
                throw new ExceptionWithContext("Unrecognized value type: %d", encodedValue.getValueType());
        }
    }

    @Nonnull Set<? extends BuilderAnnotationElement> internAnnotationElements(
            @Nonnull Set<? extends AnnotationElement> elements) {
        return ImmutableSet.copyOf(
                Iterators.transform(elements.iterator(),
                        new Function<AnnotationElement, BuilderAnnotationElement>() {
                            @Nullable @Override
                            public BuilderAnnotationElement apply(AnnotationElement input) {
                                return internAnnotationElement(input);
                            }
                        }));
    }

    @Nonnull private BuilderAnnotationElement internAnnotationElement(@Nonnull AnnotationElement annotationElement) {
        return new BuilderAnnotationElement(stringSection.internString(annotationElement.getName()),
                internEncodedValue(annotationElement.getValue()));
    }

    @Nullable BuilderEncodedValue internNullableEncodedValue(@Nullable EncodedValue encodedValue) {
        if (encodedValue == null) {
            return null;
        }
        return internEncodedValue(encodedValue);
    }

    @Nonnull private BuilderEncodedValue internEncodedValue(@Nonnull EncodedValue encodedValue) {
        switch (encodedValue.getValueType()) {
            case ValueType.ANNOTATION:
                return internAnnotationEncodedValue((AnnotationEncodedValue)encodedValue);
            case ValueType.ARRAY:
                return internArrayEncodedValue((ArrayEncodedValue)encodedValue);
            case ValueType.BOOLEAN:
                boolean value = ((BooleanEncodedValue)encodedValue).getValue();
                return value?BuilderBooleanEncodedValue.TRUE_VALUE:BuilderBooleanEncodedValue.FALSE_VALUE;
            case ValueType.BYTE:
                return new BuilderByteEncodedValue(((ByteEncodedValue)encodedValue).getValue());
            case ValueType.CHAR:
                return new BuilderCharEncodedValue(((CharEncodedValue)encodedValue).getValue());
            case ValueType.DOUBLE:
                return new BuilderDoubleEncodedValue(((DoubleEncodedValue)encodedValue).getValue());
            case ValueType.ENUM:
                return internEnumEncodedValue((EnumEncodedValue)encodedValue);
            case ValueType.FIELD:
                return internFieldEncodedValue((FieldEncodedValue)encodedValue);
            case ValueType.FLOAT:
                return new BuilderFloatEncodedValue(((FloatEncodedValue)encodedValue).getValue());
            case ValueType.INT:
                return new BuilderIntEncodedValue(((IntEncodedValue)encodedValue).getValue());
            case ValueType.LONG:
                return new BuilderLongEncodedValue(((LongEncodedValue)encodedValue).getValue());
            case ValueType.METHOD:
                return internMethodEncodedValue((MethodEncodedValue)encodedValue);
            case ValueType.NULL:
                return BuilderNullEncodedValue.INSTANCE;
            case ValueType.SHORT:
                return new BuilderShortEncodedValue(((ShortEncodedValue)encodedValue).getValue());
            case ValueType.STRING:
                return internStringEncodedValue((StringEncodedValue)encodedValue);
            case ValueType.TYPE:
                return internTypeEncodedValue((TypeEncodedValue)encodedValue);
            default:
                throw new ExceptionWithContext("Unexpected encoded value type: %d", encodedValue.getValueType());
        }
    }

    @Nonnull private BuilderAnnotationEncodedValue internAnnotationEncodedValue(@Nonnull AnnotationEncodedValue value) {
        return new BuilderAnnotationEncodedValue(
                typeSection.internType(value.getType()),
                internAnnotationElements(value.getElements()));
    }

    @Nonnull private BuilderArrayEncodedValue internArrayEncodedValue(@Nonnull ArrayEncodedValue value) {
        return new BuilderArrayEncodedValue(
                ImmutableList.copyOf(
                        Iterators.transform(value.getValue().iterator(),
                                new Function<EncodedValue, BuilderEncodedValue>() {
                                    @Nullable @Override public BuilderEncodedValue apply(EncodedValue input) {
                                        return internEncodedValue(input);
                                    }
                                })));
    }

    @Nonnull private BuilderEnumEncodedValue internEnumEncodedValue(@Nonnull EnumEncodedValue value) {
        return new BuilderEnumEncodedValue(fieldSection.internField(value.getValue()));
    }

    @Nonnull private BuilderFieldEncodedValue internFieldEncodedValue(@Nonnull FieldEncodedValue value) {
        return new BuilderFieldEncodedValue(fieldSection.internField(value.getValue()));
    }

    @Nonnull private BuilderMethodEncodedValue internMethodEncodedValue(@Nonnull MethodEncodedValue value) {
        return new BuilderMethodEncodedValue(methodSection.internMethod(value.getValue()));
    }

    @Nonnull private BuilderStringEncodedValue internStringEncodedValue(@Nonnull StringEncodedValue string) {
        return new BuilderStringEncodedValue(stringSection.internString(string.getValue()));
    }

    @Nonnull private BuilderTypeEncodedValue internTypeEncodedValue(@Nonnull TypeEncodedValue type) {
        return new BuilderTypeEncodedValue(typeSection.internType(type.getValue()));
    }

    protected class DexBuilderSectionProvider extends SectionProvider {
        @Nonnull @Override public BuilderStringPool getStringSection() {
            return new BuilderStringPool();
        }

        @Nonnull @Override public BuilderTypePool getTypeSection() {
            return new BuilderTypePool(DexBuilder.this);
        }

        @Nonnull @Override public BuilderProtoPool getProtoSection() {
            return new BuilderProtoPool(DexBuilder.this);
        }

        @Nonnull @Override public BuilderFieldPool getFieldSection() {
            return new BuilderFieldPool(DexBuilder.this);
        }

        @Nonnull @Override public BuilderMethodPool getMethodSection() {
            return new BuilderMethodPool(DexBuilder.this);
        }

        @Nonnull @Override public BuilderClassPool getClassSection() {
            return new BuilderClassPool(DexBuilder.this);
        }

        @Nonnull @Override public BuilderTypeListPool getTypeListSection() {
            return new BuilderTypeListPool(DexBuilder.this);
        }

        @Nonnull @Override public BuilderAnnotationPool getAnnotationSection() {
            return new BuilderAnnotationPool(DexBuilder.this);
        }

        @Nonnull @Override public BuilderAnnotationSetPool getAnnotationSetSection() {
            return new BuilderAnnotationSetPool(DexBuilder.this);
        }
    }
}
