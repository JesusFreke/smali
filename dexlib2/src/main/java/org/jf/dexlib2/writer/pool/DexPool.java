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

package org.jf.dexlib2.writer.pool;

import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.ValueType;
import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.AnnotationElement;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.reference.*;
import org.jf.dexlib2.iface.value.*;
import org.jf.dexlib2.writer.DexWriter;
import org.jf.dexlib2.writer.io.DexDataStore;
import org.jf.dexlib2.writer.io.FileDataStore;
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

public class DexPool extends DexWriter<CharSequence, StringReference, CharSequence, TypeReference,
        MethodProtoReference, FieldReference, MethodReference, PoolClassDef,
        Annotation, Set<? extends Annotation>,
        TypeListPool.Key<? extends Collection<? extends CharSequence>>, Field, PoolMethod,
        EncodedValue, AnnotationElement> {

    @Deprecated
    @Nonnull public static DexPool makeDexPool() {
        return makeDexPool(Opcodes.forApi(20));
    }

    @Deprecated
    @Nonnull public static DexPool makeDexPool(int api) {
        return makeDexPool(Opcodes.forApi(api));
    }

    @Nonnull public static DexPool makeDexPool(@Nonnull Opcodes opcodes) {
        return new DexPool(opcodes);
    }

    protected DexPool(@Nonnull Opcodes opcodes) {
        this(opcodes, new PoolContext());
    }

    private DexPool(@Nonnull Opcodes opcodes, @Nonnull PoolContext context) {
        super(opcodes, context.stringPool, context.typePool, context.protoPool, context.fieldPool, context.methodPool,
                context.classPool, context.typeListPool, context.annotationPool, context.annotationSetPool);
    }

    public static void writeTo(@Nonnull String path, @Nonnull org.jf.dexlib2.iface.DexFile input) throws IOException {
        writeTo(new FileDataStore(new File(path)), input);
    }

    public static void writeTo(@Nonnull DexDataStore dataStore, @Nonnull org.jf.dexlib2.iface.DexFile input) throws IOException {
        DexPool dexPool = makeDexPool(input.getOpcodes());
        dexPool.internClassDefs(input.getClasses());
        dexPool.writeTo(dataStore);
    }

    public void internClassDef(@Nonnull ClassDef classDef) {
        ((ClassPool) classSection).intern(classDef);
    }

    public void internClassDefs(@Nonnull Iterable<? extends ClassDef> classDefs) {
        ClassPool classPool = (ClassPool) classSection;
        for (ClassDef classDef: classDefs) {
            classPool.intern(classDef);
        }
    }

    @Override protected void writeEncodedValue(@Nonnull InternalEncodedValueWriter writer,
                                               @Nonnull EncodedValue encodedValue) throws IOException {
        switch (encodedValue.getValueType()) {
            case ValueType.ANNOTATION:
                AnnotationEncodedValue annotationEncodedValue = (AnnotationEncodedValue)encodedValue;
                writer.writeAnnotation(annotationEncodedValue.getType(), annotationEncodedValue.getElements());
                break;
            case ValueType.ARRAY:
                ArrayEncodedValue arrayEncodedValue = (ArrayEncodedValue)encodedValue;
                writer.writeArray(arrayEncodedValue.getValue());
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
                writer.writeEnum(((EnumEncodedValue)encodedValue).getValue());
                break;
            case ValueType.FIELD:
                writer.writeField(((FieldEncodedValue)encodedValue).getValue());
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
                writer.writeMethod(((MethodEncodedValue)encodedValue).getValue());
                break;
            case ValueType.NULL:
                writer.writeNull();
                break;
            case ValueType.SHORT:
                writer.writeShort(((ShortEncodedValue)encodedValue).getValue());
                break;
            case ValueType.STRING:
                writer.writeString(((StringEncodedValue)encodedValue).getValue());
                break;
            case ValueType.TYPE:
                writer.writeType(((TypeEncodedValue)encodedValue).getValue());
                break;
            default:
                throw new ExceptionWithContext("Unrecognized value type: %d", encodedValue.getValueType());
        }
    }

    public static void internEncodedValue(@Nonnull EncodedValue encodedValue,
                                          @Nonnull StringPool stringPool,
                                          @Nonnull TypePool typePool,
                                          @Nonnull FieldPool fieldPool,
                                          @Nonnull MethodPool methodPool) {
        switch (encodedValue.getValueType()) {
            case ValueType.ANNOTATION:
                AnnotationEncodedValue annotationEncodedValue = (AnnotationEncodedValue)encodedValue;
                typePool.intern(annotationEncodedValue.getType());
                for (AnnotationElement element: annotationEncodedValue.getElements()) {
                    stringPool.intern(element.getName());
                    internEncodedValue(element.getValue(), stringPool, typePool, fieldPool, methodPool);
                }
                break;
            case ValueType.ARRAY:
                for (EncodedValue element: ((ArrayEncodedValue)encodedValue).getValue()) {
                    internEncodedValue(element, stringPool, typePool, fieldPool, methodPool);
                }
                break;
            case ValueType.STRING:
                stringPool.intern(((StringEncodedValue)encodedValue).getValue());
                break;
            case ValueType.TYPE:
                typePool.intern(((TypeEncodedValue)encodedValue).getValue());
                break;
            case ValueType.ENUM:
                fieldPool.intern(((EnumEncodedValue)encodedValue).getValue());
                break;
            case ValueType.FIELD:
                fieldPool.intern(((FieldEncodedValue)encodedValue).getValue());
                break;
            case ValueType.METHOD:
                methodPool.intern(((MethodEncodedValue)encodedValue).getValue());
                break;
        }
    }
}
