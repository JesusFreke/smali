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

import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.formats.Instruction21c;
import org.jf.util.StringUtils;
import org.jf.util.CommentingIndentingWriter;
import org.jf.util.IndentingWriter;
import org.jf.dexlib.*;
import org.jf.dexlib.Code.Analysis.ValidationException;
import org.jf.dexlib.EncodedValue.EncodedValue;
import org.jf.dexlib.Util.AccessFlags;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;

public class ClassDefinition {
    @Nonnull private final ClassDef classDef;
    @Nonnull private final HashSet<String> fieldsSetInStaticConstructor;

    protected boolean validationErrors;

    public ClassDefinition(ClassDef classDef) {
        this.classDef = classDef;
        fieldsSetInStaticConstructor = findFieldsSetInStaticConstructor();
    }

    public boolean hadValidationErrors() {
        return validationErrors;
    }

    @Nonnull
    private HashSet<String> findFieldsSetInStaticConstructor() {
        HashSet<String> fieldsSetInStaticConstructor = new HashSet<String>();

        for (Method method: classDef.getMethods()) {
            if (method.getName().equals("<clinit>")) {
                MethodImplementation impl = method.getImplementation();
                if (impl != null) {
                    for (Instruction instruction: impl.getInstructions()) {
                        switch (instruction.getOpcode()) {
                            case SPUT:
                            case SPUT_BOOLEAN:
                            case SPUT_BYTE:
                            case SPUT_CHAR:
                            case SPUT_OBJECT:
                            case SPUT_SHORT:
                            case SPUT_WIDE: {
                                Instruction21c ins = (Instruction21c)instruction;
                                String field = ins.getReference();
                                if (field.startsWith(classDef.getName())) {
                                    fieldsSetInStaticConstructor.add(field);
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }
        return fieldsSetInStaticConstructor;
    }

    public void writeTo(IndentingWriter writer) throws IOException {
        writeClass(writer);
        writeSuper(writer);
        writeSourceFile(writer);
        writeInterfaces(writer);
        writeAnnotations(writer);
        writeFields(writer);
        /*writeDirectMethods(writer);
        writeVirtualMethods(writer);*/
    }

    private void writeClass(IndentingWriter writer) throws IOException {
        writer.write(".class ");
        writeAccessFlags(writer);
        writer.write(classDef.getName());
        writer.write('\n');
    }

    private void writeAccessFlags(IndentingWriter writer) throws IOException {
        for (AccessFlags accessFlag: AccessFlags.getAccessFlagsForClass(classDef.getAccessFlags())) {
            writer.write(accessFlag.toString());
            writer.write(' ');
        }
    }

    private void writeSuper(IndentingWriter writer) throws IOException {
        String superClass = classDef.getSuperclass();
        if (superClass != null) {
            writer.write(".super ");
            writer.write(superClass);
            writer.write('\n');
        }
    }

    private void writeSourceFile(IndentingWriter writer) throws IOException {
        String sourceFile = classDef.getSourceFile();
        if (sourceFile != null) {
            writer.write(".source \"");
            StringUtils.writeEscapedString(writer, sourceFile);
            writer.write("\"\n");
        }
    }

    private void writeInterfaces(IndentingWriter writer) throws IOException {
        List<String> interfaces = classDef.getInterfaces();
        if (interfaces.size() != 0) {
            writer.write('\n');
            writer.write("# interfaces\n");
            for (String interfaceName: interfaces) {
                writer.write(".implements ");
                writer.write(interfaceName);
                writer.write('\n');
            }
        }
    }

    private void writeAnnotations(IndentingWriter writer) throws IOException {
        List<? extends Annotation> classAnnotations = classDef.getAnnotations();
        if (classAnnotations.size() != 0) {
            writer.write("\n\n");
            writer.write("# annotations\n");
            AnnotationFormatter.writeTo(writer, classAnnotations);
        }
    }

    private void writeFields(IndentingWriter writer) throws IOException {
        List<? extends Field> fields = classDef.getFields();

        boolean wroteStaticFieldHeader = false;
        boolean wroteInstanceFieldHeader = false;
        for (Field field: fields) {
            //TODO: add an isStatic field to field? or somehow make this better
            boolean isStatic = (field.getAccessFlags() & AccessFlags.STATIC.getValue()) != 0;

            if (isStatic) {
                if (!wroteStaticFieldHeader) {
                    writer.write("\n\n");
                    writer.write("# static fields");
                    wroteStaticFieldHeader = true;
                }
            } else {
                if (!wroteInstanceFieldHeader) {
                    writer.write("\n\n");
                    writer.write("# instance fields");
                    wroteInstanceFieldHeader = true;
                }
            }

            writer.write('\n');

            // TODO: detect duplicate fields.
            // TODO: check if field is set in static constructor

            FieldDefinition.writeTo(writer, field, false);
        }
    }

    //TODO: uncomment
    /*private void writeDirectMethods(IndentingWriter writer) throws IOException {
        if (classDataItem == null) {
            return;
        }

        List<ClassDataItem.EncodedMethod> directMethods = classDataItem.getDirectMethods();
        if (directMethods.size() == 0) {
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

        List<ClassDataItem.EncodedMethod> virtualMethods = classDataItem.getVirtualMethods();

        if (virtualMethods.size() == 0) {
            return;
        }

        writer.write("\n\n");
        writer.write("# virtual methods\n");
        writeMethods(writer, virtualMethods);
    }

    private void writeMethods(IndentingWriter writer, List<ClassDataItem.EncodedMethod> methods) throws IOException {
        for (int i=0; i<methods.size(); i++) {
            ClassDataItem.EncodedMethod method = methods.get(i);
            if (i > 0) {
                writer.write('\n');
            }

            AnnotationSetItem methodAnnotations = null;
            AnnotationSetRefList parameterAnnotations = null;
            AnnotationDirectoryItem annotations = classDefItem.getAnnotations();
            if (annotations != null) {
                methodAnnotations = annotations.getMethodAnnotations(method.method);
                parameterAnnotations = annotations.getParameterAnnotations(method.method);
            }

            IndentingWriter methodWriter = writer;
            // the encoded methods are sorted, so we just have to compare with the previous one to detect duplicates
            if (i > 0 && method.equals(methods.get(i-1))) {
                methodWriter = new CommentingIndentingWriter(writer, "#");
                methodWriter.write("Ignoring method with duplicate signature\n");
                System.err.println(String.format("Warning: class %s has duplicate method %s, Ignoring.",
                        classDefItem.getClassType().getTypeDescriptor(), method.method.getShortMethodString()));
            }

            MethodDefinition methodDefinition = new MethodDefinition(method);
            methodDefinition.writeTo(methodWriter, methodAnnotations, parameterAnnotations);

            ValidationException validationException = methodDefinition.getValidationException();
            if (validationException != null) {
                System.err.println(String.format("Error while disassembling method %s. Continuing.",
                        method.method.getMethodString()));
                validationException.printStackTrace(System.err);
                this.validationErrors = true;
            }
        }
    }*/
}
