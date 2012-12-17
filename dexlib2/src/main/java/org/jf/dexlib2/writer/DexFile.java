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

import org.jf.dexlib2.ValueType;
import org.jf.dexlib2.iface.AnnotationElement;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.value.*;
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.Set;

public class DexFile {
    // package-private access for these
    @Nonnull final StringPool stringPool = new StringPool();
    @Nonnull final TypePool typePool = new TypePool(this);
    @Nonnull final FieldPool fieldPool = new FieldPool(this);
    @Nonnull final ProtoPool protoPool = new ProtoPool(this);
    @Nonnull final MethodPool methodPool = new MethodPool(this);

    @Nonnull final TypeListPool typeListPool = new TypeListPool(this);
    @Nonnull final EncodedArrayPool encodedArrayPool = new EncodedArrayPool(this);
    @Nonnull final AnnotationPool annotationPool = new AnnotationPool(this);
    @Nonnull final AnnotationSetPool annotationSetPool = new AnnotationSetPool(this);
    @Nonnull final AnnotationSetRefPool annotationSetRefPool = new AnnotationSetRefPool(this);
    @Nonnull final AnnotationDirectoryPool annotationDirectoryPool = new AnnotationDirectoryPool(this);
    @Nonnull final DebugInfoPool debugInfoPool = new DebugInfoPool(this);
    @Nonnull final CodeItemPool codeItemPool = new CodeItemPool(this);
    @Nonnull final ClassDefPool classDefPool = new ClassDefPool(this);
    @Nonnull final MapItem mapItem = new MapItem(this);
    @Nonnull final HeaderItem headerItem = new HeaderItem(this);

    @Nonnull private final Set<? extends ClassDef> classes;

    private DexFile(Set<? extends ClassDef> classes) {
        this.classes = classes;

        for (ClassDef classDef: classes) {
            classDefPool.intern(classDef);
        }
    }

    public void writeEncodedValue(@Nonnull DexWriter writer, @Nonnull EncodedValue encodedValue) throws IOException {
        int valueType = encodedValue.getValueType();
        switch (valueType) {
            case ValueType.ANNOTATION:
                AnnotationEncodedValue annotationEncodedValue = (AnnotationEncodedValue)encodedValue;
                Collection<? extends AnnotationElement> annotationElements = annotationEncodedValue.getElements();
                writer.writeUleb128(typePool.getIndex(annotationEncodedValue.getType()));
                writer.writeUleb128(annotationElements.size());
                for (AnnotationElement element: annotationElements) {
                    writer.writeUleb128(stringPool.getIndex(element.getName()));
                    writeEncodedValue(writer, element.getValue());
                }
                break;
            case ValueType.ARRAY:
                ArrayEncodedValue arrayEncodedValue = (ArrayEncodedValue)encodedValue;
                Collection<? extends EncodedValue> elements = arrayEncodedValue.getValue();
                writer.writeUleb128(elements.size());
                for (EncodedValue element: elements) {
                    writeEncodedValue(writer, element);
                }
                break;
            case ValueType.BOOLEAN:
                writer.writeEncodedValueHeader(valueType, (((BooleanEncodedValue)encodedValue).getValue()?1:0));
                break;
            case ValueType.BYTE:
                writer.writeEncodedInt(valueType, ((ByteEncodedValue)encodedValue).getValue());
                break;
            case ValueType.CHAR:
                writer.writeEncodedInt(valueType, ((CharEncodedValue)encodedValue).getValue());
                break;
            case ValueType.DOUBLE:
                writer.writeEncodedDouble(valueType, ((DoubleEncodedValue)encodedValue).getValue());
                break;
            case ValueType.ENUM:
                writer.writeEncodedUint(valueType, fieldPool.getIndex(((EnumEncodedValue)encodedValue).getValue()));
                break;
            case ValueType.FIELD:
                writer.writeEncodedUint(valueType, fieldPool.getIndex(((FieldEncodedValue)encodedValue).getValue()));
                break;
            case ValueType.FLOAT:
                writer.writeEncodedFloat(valueType, ((FloatEncodedValue)encodedValue).getValue());
                break;
            case ValueType.INT:
                writer.writeEncodedInt(valueType, ((IntEncodedValue)encodedValue).getValue());
                break;
            case ValueType.LONG:
                writer.writeEncodedLong(valueType, ((LongEncodedValue)encodedValue).getValue());
                break;
            case ValueType.METHOD:
                writer.writeEncodedUint(valueType, methodPool.getIndex(((MethodEncodedValue)encodedValue).getValue()));
                break;
            case ValueType.NULL:
                writer.write(valueType);
                break;
            case ValueType.SHORT:
                writer.writeEncodedInt(valueType, ((ShortEncodedValue)encodedValue).getValue());
                break;
            case ValueType.STRING:
                writer.writeEncodedUint(valueType, stringPool.getIndex(((StringEncodedValue)encodedValue).getValue()));
                break;
            case ValueType.TYPE:
                writer.writeEncodedUint(valueType, typePool.getIndex(((TypeEncodedValue)encodedValue).getValue()));
                break;
            default:
                throw new ExceptionWithContext("Unrecognized value type: %d", encodedValue.getValueType());
        }
    }

    public void internEncodedValue(@Nonnull EncodedValue encodedValue) {
        switch (encodedValue.getValueType()) {
            case ValueType.ARRAY:
                ArrayEncodedValue arrayEncodedValue = (ArrayEncodedValue)encodedValue;
                for (EncodedValue value: arrayEncodedValue.getValue()) {
                    internEncodedValue(value);
                }
                return;
            case ValueType.ANNOTATION:
                AnnotationEncodedValue annotationEncodedValue = (AnnotationEncodedValue)encodedValue;
                typePool.intern(annotationEncodedValue.getType());
                for(AnnotationElement annotationElement: annotationEncodedValue.getElements()) {
                    stringPool.intern(annotationElement.getName());
                    internEncodedValue(annotationElement.getValue());
                }
                return;
            case ValueType.STRING:
                StringEncodedValue stringEncodedValue = (StringEncodedValue)encodedValue;
                stringPool.intern(stringEncodedValue.getValue());
                return;
            case ValueType.TYPE:
                TypeEncodedValue typeEncodedValue = (TypeEncodedValue)encodedValue;
                typePool.intern(typeEncodedValue.getValue());
                return;
            case ValueType.ENUM:
                EnumEncodedValue enumEncodedValue = (EnumEncodedValue)encodedValue;
                fieldPool.intern(enumEncodedValue.getValue());
                return;
            case ValueType.FIELD:
                FieldEncodedValue fieldEncodedValue = (FieldEncodedValue)encodedValue;
                fieldPool.intern(fieldEncodedValue.getValue());
                return;
            case ValueType.METHOD:
                MethodEncodedValue methodEncodedValue = (MethodEncodedValue)encodedValue;
                methodPool.intern(methodEncodedValue.getValue());
                return;
            default:
                // nothing to do
                break;
        }
    }

    private int getDataSectionOffset() {
        return HeaderItem.HEADER_ITEM_SIZE +
               stringPool.getIndexedSectionSize() +
               typePool.getIndexedSectionSize() +
               protoPool.getIndexedSectionSize() +
               fieldPool.getIndexedSectionSize() +
               methodPool.getIndexedSectionSize() +
               classDefPool.getIndexedSectionSize();
    }

    private void writeTo(@Nonnull String path) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(path, "rw");
        try {
            int dataSectionOffset = getDataSectionOffset();
            DexWriter headerWriter  = outputAt(raf, 0);
            DexWriter indexWriter   = outputAt(raf, HeaderItem.HEADER_ITEM_SIZE);
            DexWriter offsetWriter  = outputAt(raf, dataSectionOffset);
            try {
                stringPool.write(indexWriter, offsetWriter);
                typePool.write(indexWriter);
                fieldPool.write(indexWriter);
                typeListPool.write(offsetWriter);
                protoPool.write(indexWriter);
                methodPool.write(indexWriter);
                encodedArrayPool.write(offsetWriter);
                annotationPool.write(offsetWriter);
                annotationSetPool.write(offsetWriter);
                annotationSetRefPool.write(offsetWriter);
                annotationDirectoryPool.write(offsetWriter);
                debugInfoPool.write(offsetWriter);
                codeItemPool.write(offsetWriter);
                classDefPool.write(indexWriter, offsetWriter);
                mapItem.write(offsetWriter);
                headerItem.write(headerWriter, dataSectionOffset, offsetWriter.getPosition());
            } finally {
                headerWriter.close();
                indexWriter.close();
                offsetWriter.close();
            }
            FileChannel fileChannel = raf.getChannel();
            headerItem.updateSignature(fileChannel);
            headerItem.updateChecksum(fileChannel);
        } finally {
            raf.close();
        }
    }

    private static DexWriter outputAt(RandomAccessFile raf, int filePosition) throws IOException {
        return new DexWriter(raf.getChannel(), filePosition);
    }

    public static void writeTo(@Nonnull String path, @Nonnull org.jf.dexlib2.iface.DexFile input) throws IOException {
        DexFile dexFile = new DexFile(input.getClasses());
        dexFile.writeTo(path);
    }
}
