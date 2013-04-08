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

import com.google.common.collect.Lists;
import org.jf.baksmali.baksmaliOptions;
import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.formats.Instruction21c;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.util.ReferenceUtil;
import org.jf.util.IndentingWriter;
import org.jf.util.StringUtils;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class ClassDefinition {
    @Nonnull public final baksmaliOptions options;
    @Nonnull public final ClassDef classDef;
    @Nonnull private final HashSet<String> fieldsSetInStaticConstructor;

    protected boolean validationErrors;

    public ClassDefinition(@Nonnull baksmaliOptions options, @Nonnull ClassDef classDef) {
        this.options = options;
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
                                FieldReference fieldRef = (FieldReference)ins.getReference();
                                if (fieldRef.getDefiningClass().equals((classDef.getType()))) {
                                    fieldsSetInStaticConstructor.add(ReferenceUtil.getShortFieldDescriptor(fieldRef));
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
        writeStaticFields(writer);
        writeInstanceFields(writer);
        writeDirectMethods(writer);
        writeVirtualMethods(writer);
    }

    private void writeClass(IndentingWriter writer) throws IOException {
        writer.write(".class ");
        writeAccessFlags(writer);
        writer.write(classDef.getType());
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
        List<String> interfaces = Lists.newArrayList(classDef.getInterfaces());
        Collections.sort(interfaces);

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
        Collection<? extends Annotation> classAnnotations = classDef.getAnnotations();
        if (classAnnotations.size() != 0) {
            writer.write("\n\n");
            writer.write("# annotations\n");
            AnnotationFormatter.writeTo(writer, classAnnotations);
        }
    }

    private void writeStaticFields(IndentingWriter writer) throws IOException {
        boolean wroteHeader = false;
        for (Field field: classDef.getFields()) {
            if (AccessFlags.STATIC.isSet(field.getAccessFlags())) {
                if (!wroteHeader) {
                    writer.write("\n\n");
                    writer.write("# static fields");
                    wroteHeader = true;
                }
                writer.write('\n');
                // TODO: detect duplicate fields.

                boolean setInStaticConstructor =
                        fieldsSetInStaticConstructor.contains(ReferenceUtil.getShortFieldDescriptor(field));

                FieldDefinition.writeTo(writer, field, setInStaticConstructor);
            }
        }
    }

    private void writeInstanceFields(IndentingWriter writer) throws IOException {
        boolean wroteHeader = false;
        for (Field field: classDef.getFields()) {
            if (!AccessFlags.STATIC.isSet(field.getAccessFlags())) {
                if (!wroteHeader) {
                    writer.write("\n\n");
                    writer.write("# instance fields");
                    wroteHeader = true;
                }
                writer.write('\n');
                // TODO: detect duplicate fields.

                FieldDefinition.writeTo(writer, field, false);
            }
        }
    }

    private void writeDirectMethods(IndentingWriter writer) throws IOException {
        boolean wroteHeader = false;
        for (Method method: classDef.getMethods()) {
            int accessFlags = method.getAccessFlags();

            if (AccessFlags.STATIC.isSet(accessFlags) ||
                    AccessFlags.PRIVATE.isSet(accessFlags) ||
                    AccessFlags.CONSTRUCTOR.isSet(accessFlags)) {
                if (!wroteHeader) {
                    writer.write("\n\n");
                    writer.write("# direct methods");
                    wroteHeader = true;
                }
                writer.write('\n');
                // TODO: detect duplicate methods.
                // TODO: check for method validation errors

                MethodImplementation methodImpl = method.getImplementation();
                if (methodImpl == null) {
                    MethodDefinition.writeEmptyMethodTo(writer, method);
                } else {
                    MethodDefinition methodDefinition = new MethodDefinition(this, method, methodImpl);
                    methodDefinition.writeTo(writer);
                }
            }
        }
    }

    private void writeVirtualMethods(IndentingWriter writer) throws IOException {
        boolean wroteHeader = false;
        for (Method method: classDef.getMethods()) {
            int accessFlags = method.getAccessFlags();

            if (!AccessFlags.STATIC.isSet(accessFlags) &&
                    !AccessFlags.PRIVATE.isSet(accessFlags) &&
                    !AccessFlags.CONSTRUCTOR.isSet(accessFlags)) {
                if (!wroteHeader) {
                    writer.write("\n\n");
                    writer.write("# virtual methods");
                    wroteHeader = true;
                }
                writer.write('\n');
                // TODO: detect duplicate methods.
                // TODO: check for method validation errors

                MethodImplementation methodImpl = method.getImplementation();
                if (methodImpl == null) {
                    MethodDefinition.writeEmptyMethodTo(writer, method);
                } else {
                    MethodDefinition methodDefinition = new MethodDefinition(this, method, methodImpl);
                    methodDefinition.writeTo(writer);
                }
            }
        }
    }
}
