/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver (JesusFreke)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.baksmali.Adaptors;

import org.jf.dexlib.Util.Utf8Utils;
import org.jf.util.IndentingWriter;
import org.jf.dexlib.*;
import org.jf.dexlib.Code.Analysis.ValidationException;
import org.jf.dexlib.Code.Format.Instruction21c;
import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.EncodedValue.EncodedValue;
import org.jf.dexlib.Util.AccessFlags;
import org.jf.dexlib.Util.SparseArray;

import java.io.IOException;
import java.util.List;

public class ClassDefinition {
    private ClassDefItem classDefItem;
    private ClassDataItem classDataItem;

    private SparseArray<AnnotationSetItem> methodAnnotationsMap;
    private SparseArray<AnnotationSetItem> fieldAnnotationsMap;
    private SparseArray<AnnotationSetRefList> parameterAnnotationsMap;

    private SparseArray<FieldIdItem> fieldsSetInStaticConstructor;

    protected boolean validationErrors;

    public ClassDefinition(ClassDefItem classDefItem) {
        this.classDefItem = classDefItem;
        this.classDataItem = classDefItem.getClassData();
        buildAnnotationMaps();
        findFieldsSetInStaticConstructor();
    }

    public boolean hadValidationErrors() {
        return validationErrors;
    }

    private void buildAnnotationMaps() {
        AnnotationDirectoryItem annotationDirectory = classDefItem.getAnnotations();
        if (annotationDirectory == null) {
            methodAnnotationsMap = new SparseArray<AnnotationSetItem>(0);
            fieldAnnotationsMap = new SparseArray<AnnotationSetItem>(0);
            parameterAnnotationsMap = new SparseArray<AnnotationSetRefList>(0);
            return;
        }

        methodAnnotationsMap = new SparseArray<AnnotationSetItem>(annotationDirectory.getMethodAnnotationCount());
        annotationDirectory.iterateMethodAnnotations(new AnnotationDirectoryItem.MethodAnnotationIteratorDelegate() {
            public void processMethodAnnotations(MethodIdItem method, AnnotationSetItem methodAnnotations) {
                methodAnnotationsMap.put(method.getIndex(), methodAnnotations);
            }
        });

        fieldAnnotationsMap = new SparseArray<AnnotationSetItem>(annotationDirectory.getFieldAnnotationCount());
        annotationDirectory.iterateFieldAnnotations(new AnnotationDirectoryItem.FieldAnnotationIteratorDelegate() {
            public void processFieldAnnotations(FieldIdItem field, AnnotationSetItem fieldAnnotations) {
                fieldAnnotationsMap.put(field.getIndex(), fieldAnnotations);
            }
        });

        parameterAnnotationsMap = new SparseArray<AnnotationSetRefList>(
                annotationDirectory.getParameterAnnotationCount());
        annotationDirectory.iterateParameterAnnotations(
          new AnnotationDirectoryItem.ParameterAnnotationIteratorDelegate() {
            public void processParameterAnnotations(MethodIdItem method, AnnotationSetRefList parameterAnnotations) {
                parameterAnnotationsMap.put(method.getIndex(), parameterAnnotations);
            }
        });
    }

    private void findFieldsSetInStaticConstructor() {
        fieldsSetInStaticConstructor = new SparseArray<FieldIdItem>();

        if (classDataItem == null) {
            return;
        }

        for (ClassDataItem.EncodedMethod directMethod: classDataItem.getDirectMethods()) {
            if (directMethod.method.getMethodName().getStringValue().equals("<clinit>") &&
                    directMethod.codeItem != null) {
                for (Instruction instruction: directMethod.codeItem.getInstructions()) {
                    switch (instruction.opcode) {
                        case SPUT:
                        case SPUT_BOOLEAN:
                        case SPUT_BYTE:
                        case SPUT_CHAR:
                        case SPUT_OBJECT:
                        case SPUT_SHORT:
                        case SPUT_WIDE:
                            Instruction21c ins = (Instruction21c)instruction;
                            FieldIdItem fieldIdItem = (FieldIdItem)ins.getReferencedItem();
                            fieldsSetInStaticConstructor.put(fieldIdItem.getIndex(), fieldIdItem);
                    }
                }
            }
        }
    }

    public void writeTo(IndentingWriter writer) throws IOException {
        writeClass(writer);
        writeSuper(writer);
        writeSourceFile(writer);
        writeInterfaces(writer);
        writeAnnotations(writer);
        writeStaticFields(writer);
        writeInstanceFields(writer);
        writeDirectMethods(writer);
        writeVirtualMethods(writer);
        return ;
    }

    private void writeClass(IndentingWriter writer) throws IOException {
        writer.write(".class ");
        writeAccessFlags(writer);
        writer.write(classDefItem.getClassType().getTypeDescriptor());
        writer.write('\n');
    }

    private void writeAccessFlags(IndentingWriter writer) throws IOException {
        for (AccessFlags accessFlag: AccessFlags.getAccessFlagsForClass(classDefItem.getAccessFlags())) {
            writer.write(accessFlag.toString());
            writer.write(' ');
        }
    }

    private void writeSuper(IndentingWriter writer) throws IOException {
        TypeIdItem superClass = classDefItem.getSuperclass();
        if (superClass != null) {
            writer.write(".super ");
            writer.write(superClass.getTypeDescriptor());
            writer.write('\n');
        }
    }

    private void writeSourceFile(IndentingWriter writer) throws IOException {
        StringIdItem sourceFile = classDefItem.getSourceFile();
        if (sourceFile != null) {
            writer.write(".source \"");
            Utf8Utils.writeEscapedString(writer, sourceFile.getStringValue());
            writer.write("\"\n");
        }
    }

    private void writeInterfaces(IndentingWriter writer) throws IOException {
        TypeListItem interfaceList = classDefItem.getInterfaces();
        if (interfaceList == null) {
            return;
        }

        List<TypeIdItem> interfaces = interfaceList.getTypes();
        if (interfaces == null || interfaces.size() == 0) {
            return;
        }

        writer.write('\n');
        writer.write("# interfaces\n");
        for (TypeIdItem typeIdItem: interfaceList.getTypes()) {
            writer.write(".implements ");
            writer.write(typeIdItem.getTypeDescriptor());
            writer.write('\n');
        }
    }

    private void writeAnnotations(IndentingWriter writer) throws IOException {
        AnnotationDirectoryItem annotationDirectory = classDefItem.getAnnotations();
        if (annotationDirectory == null) {
            return;
        }

        AnnotationSetItem annotationSet = annotationDirectory.getClassAnnotations();
        if (annotationSet == null) {
            return;
        }

        writer.write("\n\n");
        writer.write("# annotations\n");
        AnnotationFormatter.writeTo(writer, annotationSet);
    }

    private void writeStaticFields(IndentingWriter writer) throws IOException {
        if (classDataItem == null) {
            return;
        }
        //if classDataItem is not null, then classDefItem won't be null either
        assert(classDefItem != null);

        EncodedArrayItem encodedStaticInitializers = classDefItem.getStaticFieldInitializers();

        EncodedValue[] staticInitializers;
        if (encodedStaticInitializers != null) {
            staticInitializers = encodedStaticInitializers.getEncodedArray().values;
        } else {
            staticInitializers = new EncodedValue[0];
        }

        ClassDataItem.EncodedField[] encodedFields = classDataItem.getStaticFields();
        if (encodedFields == null || encodedFields.length == 0) {
            return;
        }

        writer.write("\n\n");
        writer.write("# static fields\n");

        boolean first = true;
        for (int i=0; i<encodedFields.length; i++) {
            if (!first) {
                writer.write('\n');
            }
            first = false;

            ClassDataItem.EncodedField field = encodedFields[i];
            EncodedValue encodedValue = null;
            if (i < staticInitializers.length) {
                encodedValue = staticInitializers[i];
            }
            AnnotationSetItem annotationSet = fieldAnnotationsMap.get(field.field.getIndex());

            boolean setInStaticConstructor =
                    fieldsSetInStaticConstructor.get(field.field.getIndex()) != null;

            FieldDefinition.writeTo(writer, field, encodedValue, annotationSet, setInStaticConstructor);
        }
    }

    private void writeInstanceFields(IndentingWriter writer) throws IOException {
        if (classDataItem == null) {
            return;
        }

        ClassDataItem.EncodedField[] encodedFields = classDataItem.getInstanceFields();
        if (encodedFields == null || encodedFields.length == 0) {
            return;
        }

        writer.write("\n\n");
        writer.write("# instance fields\n");
        boolean first = true;
        for (ClassDataItem.EncodedField field: classDataItem.getInstanceFields()) {
            if (!first) {
                writer.write('\n');
            }
            first = false;

            AnnotationSetItem annotationSet = fieldAnnotationsMap.get(field.field.getIndex());

            FieldDefinition.writeTo(writer, field, null, annotationSet, false);
        }
    }

    private void writeDirectMethods(IndentingWriter writer) throws IOException {
        if (classDataItem == null) {
            return;
        }

        ClassDataItem.EncodedMethod[] directMethods = classDataItem.getDirectMethods();

        if (directMethods == null || directMethods.length == 0) {
            return;
        }

        writer.write("\n\n");
        writer.write("# direct methods\n");
        writeMethods(writer, directMethods);
    }

    private void writeVirtualMethods(IndentingWriter writer) throws IOException {
        if (classDataItem == null) {
            return;
        }

        ClassDataItem.EncodedMethod[] virtualMethods = classDataItem.getVirtualMethods();

        if (virtualMethods == null || virtualMethods.length == 0) {
            return;
        }

        writer.write("\n\n");
        writer.write("# virtual methods\n");
        writeMethods(writer, virtualMethods);
    }

    private void writeMethods(IndentingWriter writer, ClassDataItem.EncodedMethod[] methods) throws IOException {
        boolean first = true;
        for (ClassDataItem.EncodedMethod method: methods) {
            if (!first) {
                writer.write('\n');
            }
            first = false;

            AnnotationSetItem annotationSet = methodAnnotationsMap.get(method.method.getIndex());
            AnnotationSetRefList parameterAnnotationList = parameterAnnotationsMap.get(method.method.getIndex());

            MethodDefinition methodDefinition = new MethodDefinition(method);
            methodDefinition.writeTo(writer, annotationSet, parameterAnnotationList);

            ValidationException validationException = methodDefinition.getValidationException();
            if (validationException != null) {
                System.err.println(String.format("Error while disassembling method %s. Continuing.",
                        method.method.getMethodString()));
                validationException.printStackTrace(System.err);
                this.validationErrors = true;
            }
        }
    }
}
