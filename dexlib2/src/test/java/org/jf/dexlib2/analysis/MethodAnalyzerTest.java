/*
 * Copyright 2016, Google Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 * Neither the name of Google Inc. nor the names of its
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

package org.jf.dexlib2.analysis;

import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.builder.MethodImplementationBuilder;
import org.jf.dexlib2.builder.instruction.BuilderInstruction10x;
import org.jf.dexlib2.builder.instruction.BuilderInstruction12x;
import org.jf.dexlib2.builder.instruction.BuilderInstruction21t;
import org.jf.dexlib2.builder.instruction.BuilderInstruction22c;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.immutable.ImmutableClassDef;
import org.jf.dexlib2.immutable.ImmutableDexFile;
import org.jf.dexlib2.immutable.ImmutableMethod;
import org.jf.dexlib2.immutable.ImmutableMethodParameter;
import org.jf.dexlib2.immutable.reference.ImmutableTypeReference;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class MethodAnalyzerTest {

    @Test
    public void testInstanceOfNarrowingEqz() throws IOException {
        MethodImplementationBuilder builder = new MethodImplementationBuilder(2);

        builder.addInstruction(new BuilderInstruction22c(Opcode.INSTANCE_OF, 0, 1,
                new ImmutableTypeReference("Lmain;")));
        builder.addInstruction(new BuilderInstruction21t(Opcode.IF_EQZ, 0, builder.getLabel("not_instance_of")));
        builder.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));

        builder.addLabel("not_instance_of");
        builder.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));

        MethodImplementation methodImplementation = builder.getMethodImplementation();

        Method method = new ImmutableMethod("Lmain;", "narrowing",
                Collections.singletonList(new ImmutableMethodParameter("Ljava/lang/Object;", null, null)), "V",
                AccessFlags.PUBLIC.getValue(), null, methodImplementation);
        ClassDef classDef = new ImmutableClassDef("Lmain;", AccessFlags.PUBLIC.getValue(), "Ljava/lang/Object;", null,
                null, null, null, Collections.singletonList(method));
        DexFile dexFile = new ImmutableDexFile(Opcodes.getDefault(), Collections.singletonList(classDef));

        ClassPath classPath = new ClassPath(new DexClassProvider(dexFile));
        MethodAnalyzer methodAnalyzer = new MethodAnalyzer(classPath, method, null, false);

        List<AnalyzedInstruction> analyzedInstructions = methodAnalyzer.getAnalyzedInstructions();
        Assert.assertEquals("Lmain;", analyzedInstructions.get(2).getPreInstructionRegisterType(1).type.getType());

        Assert.assertEquals("Ljava/lang/Object;",
                analyzedInstructions.get(3).getPreInstructionRegisterType(1).type.getType());
    }

    @Test
    public void testInstanceOfNarrowingNez() throws IOException {
        MethodImplementationBuilder builder = new MethodImplementationBuilder(2);

        builder.addInstruction(new BuilderInstruction22c(Opcode.INSTANCE_OF, 0, 1,
                new ImmutableTypeReference("Lmain;")));
        builder.addInstruction(new BuilderInstruction21t(Opcode.IF_NEZ, 0, builder.getLabel("instance_of")));
        builder.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));

        builder.addLabel("instance_of");
        builder.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));

        MethodImplementation methodImplementation = builder.getMethodImplementation();

        Method method = new ImmutableMethod("Lmain;", "narrowing",
                Collections.singletonList(new ImmutableMethodParameter("Ljava/lang/Object;", null, null)), "V",
                AccessFlags.PUBLIC.getValue(), null, methodImplementation);
        ClassDef classDef = new ImmutableClassDef("Lmain;", AccessFlags.PUBLIC.getValue(), "Ljava/lang/Object;", null,
                null, null, null, Collections.singletonList(method));
        DexFile dexFile = new ImmutableDexFile(Opcodes.getDefault(), Collections.singletonList(classDef));

        ClassPath classPath = new ClassPath(new DexClassProvider(dexFile));
        MethodAnalyzer methodAnalyzer = new MethodAnalyzer(classPath, method, null, false);

        List<AnalyzedInstruction> analyzedInstructions = methodAnalyzer.getAnalyzedInstructions();
        Assert.assertEquals("Ljava/lang/Object;",
                analyzedInstructions.get(2).getPreInstructionRegisterType(1).type.getType());

        Assert.assertEquals("Lmain;", analyzedInstructions.get(3).getPreInstructionRegisterType(1).type.getType());
    }

    @Test
    public void testInstanceOfNarrowingAfterMove() throws IOException {
        MethodImplementationBuilder builder = new MethodImplementationBuilder(3);

        builder.addInstruction(new BuilderInstruction12x(Opcode.MOVE_OBJECT, 1, 2));
        builder.addInstruction(new BuilderInstruction22c(Opcode.INSTANCE_OF, 0, 1,
                new ImmutableTypeReference("Lmain;")));
        builder.addInstruction(new BuilderInstruction21t(Opcode.IF_EQZ, 0, builder.getLabel("not_instance_of")));
        builder.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));

        builder.addLabel("not_instance_of");
        builder.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));

        MethodImplementation methodImplementation = builder.getMethodImplementation();

        Method method = new ImmutableMethod("Lmain;", "narrowing",
                Collections.singletonList(new ImmutableMethodParameter("Ljava/lang/Object;", null, null)), "V",
                AccessFlags.PUBLIC.getValue(), null, methodImplementation);
        ClassDef classDef = new ImmutableClassDef("Lmain;", AccessFlags.PUBLIC.getValue(), "Ljava/lang/Object;", null,
                null, null, null, Collections.singletonList(method));
        DexFile dexFile = new ImmutableDexFile(Opcodes.getDefault(), Collections.singletonList(classDef));

        ClassPath classPath = new ClassPath(new DexClassProvider(dexFile));
        MethodAnalyzer methodAnalyzer = new MethodAnalyzer(classPath, method, null, false);

        List<AnalyzedInstruction> analyzedInstructions = methodAnalyzer.getAnalyzedInstructions();
        Assert.assertEquals("Lmain;", analyzedInstructions.get(3).getPreInstructionRegisterType(1).type.getType());
        Assert.assertEquals("Lmain;", analyzedInstructions.get(3).getPreInstructionRegisterType(2).type.getType());

        Assert.assertEquals("Ljava/lang/Object;",
                analyzedInstructions.get(4).getPreInstructionRegisterType(1).type.getType());
        Assert.assertEquals("Ljava/lang/Object;",
                analyzedInstructions.get(4).getPreInstructionRegisterType(2).type.getType());
    }
}
