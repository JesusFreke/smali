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

import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.util.FieldUtil;
import org.jf.dexlib2.util.MethodUtil;
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ClassDefPool {
    public final static int CLASS_DEF_ITEM_SIZE = 0x20;

    @Nonnull private final Map<ClassDef, Boolean> internedClassDefItems = Maps.newHashMap();
    @Nonnull private final Map<String, ClassDef> nameToClassDef = Maps.newHashMap();
    @Nonnull private final DexFile dexFile;

    private int indexSectionOffset = -1;
    private int dataSectionOffset = -1;
    private int classDataCount = 0;

    public ClassDefPool(@Nonnull DexFile dexFile) {
        this.dexFile = dexFile;
    }

    public void intern(@Nonnull ClassDef classDef) {
        Boolean prev = internedClassDefItems.put(classDef, false);
        if (prev != null) {
            // item is already interned, no need to do it again
            // TODO: add additional handling for the case of interning a modified class
            return;
        }

        dexFile.typePool.intern(classDef.getType());
        dexFile.typePool.internNullable(classDef.getSuperclass());
        dexFile.typeListPool.intern(ImmutableSortedSet.copyOf(classDef.getInterfaces()));
        dexFile.stringPool.internNullable(classDef.getSourceFile());
        dexFile.encodedArrayPool.intern(classDef);
        dexFile.annotationDirectoryPool.intern(classDef);
        boolean hasClassData = false;
        for (Field field: classDef.getFields()) {
            hasClassData = true;
            dexFile.fieldPool.intern(field);
        }
        for (Method method: classDef.getMethods()) {
            hasClassData = true;
            dexFile.methodPool.intern(method);
            dexFile.codeItemPool.intern(method);
        }
        if (hasClassData) {
            classDataCount++;
        }

        nameToClassDef.put(classDef.getType(), classDef);
    }

    public int getIndexedSectionSize() {
        return internedClassDefItems.size() * CLASS_DEF_ITEM_SIZE;
    }

    public int getNumClassDefItems() {
        return internedClassDefItems.size();
    }

    public int getNumClassDataItems() {
        return classDataCount;
    }

    public int getIndexSectionOffset() {
        if (indexSectionOffset < 0) {
            throw new ExceptionWithContext("Section offset has not been set yet!");
        }
        return indexSectionOffset;
    }

    public int getDataSectionOffset() {
        if (dataSectionOffset < 0) {
            throw new ExceptionWithContext("Section offset has not been set yet!");
        }
        return dataSectionOffset;
    }

    public void write(@Nonnull DexWriter indexWriter, @Nonnull DexWriter offsetWriter) throws IOException {
        List<ClassDef> classDefs = Lists.newArrayList(internedClassDefItems.keySet());

        indexSectionOffset = indexWriter.getPosition();
        dataSectionOffset = offsetWriter.getPosition();

        for (ClassDef classDef: classDefs) {
            writeClass(indexWriter, offsetWriter, classDef);
        }
    }

    private void writeClass(DexWriter indexWriter, DexWriter offsetWriter, ClassDef classDef) throws IOException {
        if (classDef == null) {
            // class does not exist in this dex file, cannot write it
            return;
        }

        Boolean alreadyWritten = internedClassDefItems.put(classDef, true);
        if (alreadyWritten != null && alreadyWritten) {
            // class has already been written, no need to write it
            return;
        }

        // first, try to write a superclass
        ClassDef superClassDef = nameToClassDef.get(classDef.getSuperclass());
        writeClass(indexWriter, offsetWriter, superClassDef);

        // then, try to write interfaces
        for (String iface: classDef.getInterfaces()) {
            ClassDef interfaceClassDef = nameToClassDef.get(iface);
            writeClass(indexWriter, offsetWriter, interfaceClassDef);
        }

        // and finally, write the class itself
        writeSelf(indexWriter, offsetWriter, classDef);
    }

    private void writeSelf(DexWriter indexWriter, DexWriter offsetWriter, ClassDef classDef) throws IOException {
        indexWriter.writeInt(dexFile.typePool.getIndex(classDef));
        indexWriter.writeInt(classDef.getAccessFlags());
        indexWriter.writeInt(dexFile.typePool.getIndexNullable(classDef.getSuperclass()));

        indexWriter.writeInt(dexFile.typeListPool.getOffset(ImmutableSortedSet.copyOf(classDef.getInterfaces())));

        if (classDef.getSourceFile() != null) {
            indexWriter.writeInt(dexFile.stringPool.getIndexNullable(classDef.getSourceFile()));
        } else {
            indexWriter.writeInt(-1); // TODO: this should be replaced by NO_INDEX
        }

        indexWriter.writeInt(dexFile.annotationDirectoryPool.getOffset(classDef));

        ClassDataItem classDataItem = new ClassDataItem(classDef);

        if (classDataItem.hasData()) {
            indexWriter.writeInt(offsetWriter.getPosition());
            classDataItem.write(offsetWriter);
        } else {
            indexWriter.writeInt(0);
        }

        if (classDataItem.hasStaticFields()) {
            indexWriter.writeInt(dexFile.encodedArrayPool.getOffset(classDef));
        } else {
            indexWriter.writeInt(0);
        }
    }

    private class ClassDataItem {
        ClassDef classDef;
        List<Field> fields;
        List<Method> methods;

        int numStaticFields = 0;
        int numInstanceFields = 0;
        int numDirectMethods = 0;
        int numVirtualMethods = 0;

        private ClassDataItem(ClassDef classDef) {
            this.classDef = classDef;

            fields = Lists.newArrayList(classDef.getFields());
            Collections.sort(fields);

            methods = Lists.newArrayList(classDef.getMethods());
            Collections.sort(methods);

            for (Field field: fields) {
                if (FieldUtil.isStatic(field)) {
                    numStaticFields++;
                } else {
                    numInstanceFields++;
                }
            }
            for (Method method: methods) {
                if (MethodUtil.isDirect(method)) {
                    numDirectMethods++;
                } else {
                    numVirtualMethods++;
                }
            }
        }

        private boolean hasData() {
            return (numStaticFields > 0 || numInstanceFields > 0 || numDirectMethods > 0 || numVirtualMethods > 0);
        }

        private boolean hasStaticFields() {
            return numStaticFields > 0;
        }

        private void writeStaticFields(DexWriter writer) throws IOException {
            int lastIdx = 0;
            for (Field field: fields) {
                if (FieldUtil.isStatic(field)) {
                    int idx = dexFile.fieldPool.getIndex(field);
                    writer.writeUleb128(idx - lastIdx);
                    lastIdx = idx;

                    writer.writeUleb128(field.getAccessFlags());
                }
            }
        }

        private void writeInstanceFields(DexWriter writer) throws IOException {
            int lastIdx = 0;
            for (Field field: fields) {
                if (!FieldUtil.isStatic(field)) {
                    int idx = dexFile.fieldPool.getIndex(field);
                    writer.writeUleb128(idx - lastIdx);
                    lastIdx = idx;

                    writer.writeUleb128(field.getAccessFlags());
                }
            }
        }

        private void writeDirectMethods(DexWriter writer) throws IOException {
            int lastIdx = 0;
            for (Method method: methods) {
                if (MethodUtil.isDirect(method)) {
                    int idx = dexFile.methodPool.getIndex(method);
                    writer.writeUleb128(idx - lastIdx);
                    lastIdx = idx;

                    writer.writeUleb128(method.getAccessFlags());
                    writer.writeUleb128(dexFile.codeItemPool.getOffset(method));
                }
            }
        }

        private void writeVirtualMethods(DexWriter writer) throws IOException {
            int lastIdx = 0;
            for (Method method: methods) {
                if (!MethodUtil.isDirect(method)) {
                    int idx = dexFile.methodPool.getIndex(method);
                    writer.writeUleb128(idx - lastIdx);
                    lastIdx = idx;

                    writer.writeUleb128(method.getAccessFlags());
                    writer.writeUleb128(dexFile.codeItemPool.getOffset(method));
                }
            }
        }

        private void write(DexWriter writer) throws IOException {
            writer.writeUleb128(numStaticFields);
            writer.writeUleb128(numInstanceFields);
            writer.writeUleb128(numDirectMethods);
            writer.writeUleb128(numVirtualMethods);

            writeStaticFields(writer);
            writeInstanceFields(writer);
            writeDirectMethods(writer);
            writeVirtualMethods(writer);
        }
    }

}
