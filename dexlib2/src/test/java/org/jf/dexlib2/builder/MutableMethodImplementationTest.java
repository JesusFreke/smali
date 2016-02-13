/*
 * Copyright 2015, Google Inc.
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

package org.jf.dexlib2.builder;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.instruction.BuilderInstruction10x;
import org.jf.dexlib2.builder.instruction.BuilderInstruction32x;
import org.jf.dexlib2.iface.MethodImplementation;
import org.junit.Assert;
import org.junit.Test;

public class MutableMethodImplementationTest {

    @Test
    public void testTryEndAtEndOfMethod() {
        MethodImplementationBuilder builder = new MethodImplementationBuilder(10);

        Label startLabel = builder.addLabel("start");
        builder.addInstruction(new BuilderInstruction10x(Opcode.NOP));
        builder.addInstruction(new BuilderInstruction10x(Opcode.NOP));
        builder.addInstruction(new BuilderInstruction10x(Opcode.NOP));
        builder.addInstruction(new BuilderInstruction10x(Opcode.NOP));
        builder.addInstruction(new BuilderInstruction10x(Opcode.NOP));
        builder.addInstruction(new BuilderInstruction32x(Opcode.MOVE_16, 0, 0));
        Label endLabel = builder.addLabel("end");

        builder.addCatch(startLabel, endLabel, startLabel);

        MethodImplementation methodImplementation = builder.getMethodImplementation();

        Assert.assertEquals(0, methodImplementation.getTryBlocks().get(0).getStartCodeAddress());
        Assert.assertEquals(8, methodImplementation.getTryBlocks().get(0).getCodeUnitCount());

        methodImplementation = new MutableMethodImplementation(methodImplementation);

        Assert.assertEquals(0, methodImplementation.getTryBlocks().get(0).getStartCodeAddress());
        Assert.assertEquals(8, methodImplementation.getTryBlocks().get(0).getCodeUnitCount());
    }

    @Test
    public void testNewLabelByAddress() {
        MethodImplementationBuilder builder = new MethodImplementationBuilder(10);

        builder.addInstruction(new BuilderInstruction10x(Opcode.NOP));
        builder.addInstruction(new BuilderInstruction10x(Opcode.NOP));
        builder.addInstruction(new BuilderInstruction10x(Opcode.NOP));
        builder.addInstruction(new BuilderInstruction10x(Opcode.NOP));
        builder.addInstruction(new BuilderInstruction10x(Opcode.NOP));
        builder.addInstruction(new BuilderInstruction32x(Opcode.MOVE_16, 0, 0));

        MutableMethodImplementation mutableMethodImplementation =
                new MutableMethodImplementation(builder.getMethodImplementation());

        mutableMethodImplementation.addCatch(
                mutableMethodImplementation.newLabelForAddress(0),
                mutableMethodImplementation.newLabelForAddress(8),
                mutableMethodImplementation.newLabelForAddress(1));

        Assert.assertEquals(0, mutableMethodImplementation.getTryBlocks().get(0).getStartCodeAddress());
        Assert.assertEquals(8, mutableMethodImplementation.getTryBlocks().get(0).getCodeUnitCount());
        Assert.assertEquals(1, mutableMethodImplementation.getTryBlocks().get(0).getExceptionHandlers().get(0)
                .getHandlerCodeAddress());
    }

    @Test
    public void testNewLabelByIndex() {
        MethodImplementationBuilder builder = new MethodImplementationBuilder(10);

        builder.addInstruction(new BuilderInstruction10x(Opcode.NOP));
        builder.addInstruction(new BuilderInstruction10x(Opcode.NOP));
        builder.addInstruction(new BuilderInstruction10x(Opcode.NOP));
        builder.addInstruction(new BuilderInstruction10x(Opcode.NOP));
        builder.addInstruction(new BuilderInstruction10x(Opcode.NOP));
        builder.addInstruction(new BuilderInstruction32x(Opcode.MOVE_16, 0, 0));

        MutableMethodImplementation mutableMethodImplementation =
                new MutableMethodImplementation(builder.getMethodImplementation());

        mutableMethodImplementation.addCatch(
                mutableMethodImplementation.newLabelForIndex(0),
                mutableMethodImplementation.newLabelForIndex(6),
                mutableMethodImplementation.newLabelForIndex(1));

        Assert.assertEquals(0, mutableMethodImplementation.getTryBlocks().get(0).getStartCodeAddress());
        Assert.assertEquals(8, mutableMethodImplementation.getTryBlocks().get(0).getCodeUnitCount());
        Assert.assertEquals(1, mutableMethodImplementation.getTryBlocks().get(0).getExceptionHandlers().get(0)
                .getHandlerCodeAddress());
    }
}
