/*
 * Copyright 2018, Google Inc.
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import org.jf.dexlib2.*;
import org.jf.dexlib2.builder.MethodImplementationBuilder;
import org.jf.dexlib2.builder.instruction.BuilderInstruction35c;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.formats.Instruction35c;
import org.jf.dexlib2.iface.reference.CallSiteReference;
import org.jf.dexlib2.immutable.ImmutableClassDef;
import org.jf.dexlib2.immutable.ImmutableDexFile;
import org.jf.dexlib2.immutable.ImmutableMethod;
import org.jf.dexlib2.immutable.ImmutableMethodImplementation;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction35c;
import org.jf.dexlib2.immutable.reference.ImmutableCallSiteReference;
import org.jf.dexlib2.immutable.reference.ImmutableMethodHandleReference;
import org.jf.dexlib2.immutable.reference.ImmutableMethodProtoReference;
import org.jf.dexlib2.immutable.reference.ImmutableMethodReference;
import org.jf.dexlib2.writer.builder.BuilderCallSiteReference;
import org.jf.dexlib2.writer.builder.BuilderMethod;
import org.jf.dexlib2.writer.builder.DexBuilder;
import org.jf.dexlib2.writer.io.FileDataStore;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class CallSiteTest {
    @Test
    public void testPoolCallSite() throws IOException {
        ClassDef class1 = new ImmutableClassDef("Lcls1;", AccessFlags.PUBLIC.getValue(), "Ljava/lang/Object;", null, null,
                null, null,
                Lists.<Method>newArrayList(
                        new ImmutableMethod("Lcls1", "method1",
                                ImmutableList.of(), "V", AccessFlags.PUBLIC.getValue(), null,
                                new ImmutableMethodImplementation(10, ImmutableList.of(
                                        new ImmutableInstruction35c(Opcode.INVOKE_CUSTOM, 0, 0, 0, 0, 0, 0,
                                                new ImmutableCallSiteReference("call_site_1",
                                                        new ImmutableMethodHandleReference(MethodHandleType.INVOKE_STATIC,
                                                                new ImmutableMethodReference("Lcls1", "loader",
                                                                        ImmutableList.of("Ljava/lang/invoke/Lookup;",
                                                                                "Ljava/lang/String;",
                                                                                "Ljava/lang/invoke/MethodType;"),
                                                                        "Ljava/lang/invoke/CallSite;")),
                                                        "someMethod", new ImmutableMethodProtoReference(ImmutableList.of(), "V"), ImmutableList.of()))
                                ), null, null))));

        File tempFile = File.createTempFile("dex", ".dex");
        DexFileFactory.writeDexFile(tempFile.getPath(),
                new ImmutableDexFile(Opcodes.forArtVersion(111), ImmutableList.of(class1)));

        verifyDexFile(DexFileFactory.loadDexFile(tempFile, Opcodes.forArtVersion(111)));
    }

    @Test
    public void testBuilderCallSite() throws IOException {
        DexBuilder dexBuilder = new DexBuilder(Opcodes.forArtVersion(111));

        BuilderCallSiteReference callSite = dexBuilder.internCallSite(new ImmutableCallSiteReference("call_site_1",
                new ImmutableMethodHandleReference(
                        MethodHandleType.INVOKE_STATIC,
                        new ImmutableMethodReference("Lcls1", "loader", ImmutableList.of("Ljava/lang/invoke/Lookup;",
                                "Ljava/lang/String;",
                                "Ljava/lang/invoke/MethodType;"),
                                "Ljava/lang/invoke/CallSite;")),
                "someMethod",
                new ImmutableMethodProtoReference(ImmutableList.of(), "V"), ImmutableList.of()));

        MethodImplementationBuilder methodImplementationBuilder = new MethodImplementationBuilder(10);
        methodImplementationBuilder.addInstruction(new BuilderInstruction35c(Opcode.INVOKE_CUSTOM, 0, 0, 0, 0, 0, 0,
                callSite));

        BuilderMethod method = dexBuilder.internMethod("Lcls1", "method1", null, "V", 0, ImmutableSet.of(),
                methodImplementationBuilder.getMethodImplementation());
        dexBuilder.internClassDef("Lcls1;", AccessFlags.PUBLIC.getValue(), "Ljava/lang/Object;", null, null,
                ImmutableSet.of(), null,
                ImmutableList.of(method));

        File tempFile = File.createTempFile("dex", ".dex");
        dexBuilder.writeTo(new FileDataStore(tempFile));

        verifyDexFile(DexFileFactory.loadDexFile(tempFile, Opcodes.forArtVersion(111)));
    }

    private void verifyDexFile(DexFile dexFile) {
        Assert.assertEquals(1, dexFile.getClasses().size());
        ClassDef cls = Lists.newArrayList(dexFile.getClasses()).get(0);
        Assert.assertEquals("Lcls1;", cls.getType());
        Assert.assertEquals(1, Lists.newArrayList(cls.getMethods()).size());
        Method method = Iterators.getNext(cls.getMethods().iterator(), null);
        Assert.assertEquals("method1", method.getName());
        Assert.assertEquals(1, Lists.newArrayList(method.getImplementation().getInstructions()).size());
        Instruction instruction = Lists.newArrayList(method.getImplementation().getInstructions().iterator()).get(0);
        Assert.assertEquals(Opcode.INVOKE_CUSTOM, instruction.getOpcode());
        Assert.assertTrue(((Instruction35c) instruction).getReference() instanceof CallSiteReference);
    }
}
