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

import com.google.common.collect.Lists;
import junit.framework.Assert;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.formats.*;
import org.jf.dexlib2.immutable.ImmutableMethodImplementation;
import org.jf.dexlib2.immutable.instruction.*;
import org.jf.dexlib2.immutable.reference.ImmutableStringReference;
import org.jf.dexlib2.writer.util.InstructionWriteUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

public class JumboStringConversionTest {
    private static final int MIN_NUM_JUMBO_STRINGS = 256;

    private MockStringPool mStringPool;
    ArrayList<String> mJumboStrings;

    @Before
    public void setup() {
        mStringPool = new MockStringPool();
        StringBuilder stringBuilder = new StringBuilder("a");
        mJumboStrings = Lists.newArrayList();
        int index = 0;

        // populate StringPool, make sure there are more than 64k+MIN_NUM_JUMBO_STRINGS strings
        while (mJumboStrings.size()<MIN_NUM_JUMBO_STRINGS) {
            for (int pos=stringBuilder.length()-1;pos>=0;pos--) {
                for (char ch='a';ch<='z';ch++) {
                    stringBuilder.setCharAt(pos, ch);
                    mStringPool.intern(stringBuilder.toString(), index++);
                    if (mStringPool.getNumItems()>0xFFFF) {
                        mJumboStrings.add(stringBuilder.toString());
                    }
                }
            }

            stringBuilder.setLength(stringBuilder.length()+1);
            for (int pos=0;pos<stringBuilder.length();pos++) {
                stringBuilder.setCharAt(pos, 'a');
            }
        }
    }

    @Test
    public void testInstruction21c() {
        ArrayList<ImmutableInstruction> instructions = Lists.newArrayList();

        ImmutableStringReference reference = new ImmutableStringReference(mJumboStrings.get(0));
        ImmutableInstruction21c instruction = new ImmutableInstruction21c(Opcode.CONST_STRING, 0, reference);
        instructions.add(instruction);

        ImmutableMethodImplementation methodImplementation = new ImmutableMethodImplementation(1, instructions, null, null);
        InstructionWriteUtil writeUtil = new InstructionWriteUtil(methodImplementation, mStringPool);

        for (Instruction instr: writeUtil.getInstructions()) {
            Assert.assertEquals("Jumbo string conversion was not performed!", instr.getOpcode(), Opcode.CONST_STRING_JUMBO);
        }
    }

    private ArrayList<ImmutableInstruction> createSimpleInstructionList() {
        ArrayList<ImmutableInstruction> instructions = Lists.newArrayList();

        ImmutableStringReference reference = new ImmutableStringReference(mJumboStrings.get(0));
        ImmutableInstruction21c stringInstr = new ImmutableInstruction21c(Opcode.CONST_STRING, 0, reference);
        instructions.add(stringInstr);

        ImmutableInstruction10x nopInstr = new ImmutableInstruction10x(Opcode.NOP);
        instructions.add(nopInstr);

        return instructions;
    }

    @Test
    public void testInstruction10tSimple() {
        ArrayList<ImmutableInstruction> instructions = createSimpleInstructionList();

        ImmutableInstruction10t gotoInstr = new ImmutableInstruction10t(Opcode.GOTO, 3);
        instructions.add(0, gotoInstr);

        ImmutableMethodImplementation methodImplementation = new ImmutableMethodImplementation(1, instructions, null, null);
        InstructionWriteUtil writeUtil = new InstructionWriteUtil(methodImplementation, mStringPool);

        Instruction10t instruction = (Instruction10t) writeUtil.getInstructions().iterator().next();
        Assert.assertEquals("goto (Format10t) target was not modified properly", instruction.getCodeOffset(), 4);
    }

    @Test
    public void testInstruction20tSimple() {
        ArrayList<ImmutableInstruction> instructions = createSimpleInstructionList();

        ImmutableInstruction20t gotoInstr = new ImmutableInstruction20t(Opcode.GOTO_16, 4);
        instructions.add(0, gotoInstr);

        ImmutableMethodImplementation methodImplementation = new ImmutableMethodImplementation(1, instructions, null, null);
        InstructionWriteUtil writeUtil = new InstructionWriteUtil(methodImplementation, mStringPool);

        Instruction20t instruction = (Instruction20t) writeUtil.getInstructions().iterator().next();
        Assert.assertEquals("goto/16 (Format20t) target was not modified properly", instruction.getCodeOffset(), 5);
    }

    @Test
    public void testInstruction30t() {
        ArrayList<ImmutableInstruction> instructions = createSimpleInstructionList();

        ImmutableInstruction30t gotoInstr = new ImmutableInstruction30t(Opcode.GOTO_32, 5);
        instructions.add(0, gotoInstr);

        ImmutableMethodImplementation methodImplementation = new ImmutableMethodImplementation(1, instructions, null, null);
        InstructionWriteUtil writeUtil = new InstructionWriteUtil(methodImplementation, mStringPool);

        Instruction30t instruction = (Instruction30t) writeUtil.getInstructions().iterator().next();
        Assert.assertEquals("goto/32 (Format30t) target was not modified properly", instruction.getCodeOffset(), 6);
    }

    @Test
    public void testInstruction21t() {
        ArrayList<ImmutableInstruction> instructions = createSimpleInstructionList();

        ImmutableInstruction21t branchInstr = new ImmutableInstruction21t(Opcode.IF_EQZ, 0, 4);
        instructions.add(0, branchInstr);

        // jam a jumbo string before the branch target instruction, to make sure that relative offset is modified
        ImmutableStringReference reference = new ImmutableStringReference(mJumboStrings.get(1));
        ImmutableInstruction21c stringInstr = new ImmutableInstruction21c(Opcode.CONST_STRING, 0, reference);
        instructions.add(0, stringInstr);

        ImmutableMethodImplementation methodImplementation = new ImmutableMethodImplementation(1, instructions, null, null);
        InstructionWriteUtil writeUtil = new InstructionWriteUtil(methodImplementation, mStringPool);

        for (Instruction instr: writeUtil.getInstructions()) {
            if (instr instanceof Instruction21t) {
                Instruction21t instruction = (Instruction21t) instr;
                Assert.assertEquals("branch instruction (Format21t) target was not modified properly", instruction.getCodeOffset(), 5);
                break;
            }
        }
    }

    @Test
    public void testInstruction22t() {
        ArrayList<ImmutableInstruction> instructions = createSimpleInstructionList();

        ImmutableInstruction22t branchInstr = new ImmutableInstruction22t(Opcode.IF_EQ, 0, 1, 4);
        instructions.add(0, branchInstr);

        // jam a jumbo string before the branch target instruction, to make sure that relative offset is modified
        ImmutableStringReference reference = new ImmutableStringReference(mJumboStrings.get(1));
        ImmutableInstruction21c stringInstr = new ImmutableInstruction21c(Opcode.CONST_STRING, 0, reference);
        instructions.add(0, stringInstr);

        ImmutableMethodImplementation methodImplementation = new ImmutableMethodImplementation(1, instructions, null, null);
        InstructionWriteUtil writeUtil = new InstructionWriteUtil(methodImplementation, mStringPool);

        for (Instruction instr: writeUtil.getInstructions()) {
            if (instr instanceof Instruction22t) {
                Instruction22t instruction = (Instruction22t) instr;
                Assert.assertEquals("branch instruction (Format22t) target was not modified properly", instruction.getCodeOffset(), 5);
                break;
            }
        }
    }

    @Test
    public void testInstruction31t() {
        ArrayList<ImmutableInstruction> instructions = createSimpleInstructionList();

        ImmutableInstruction31t branchInstr = new ImmutableInstruction31t(Opcode.PACKED_SWITCH, 0, 5);
        instructions.add(0, branchInstr);

        // jam a jumbo string before the branch target instruction, to make sure that relative offset is modified
        ImmutableStringReference reference = new ImmutableStringReference(mJumboStrings.get(1));
        ImmutableInstruction21c stringInstr = new ImmutableInstruction21c(Opcode.CONST_STRING, 0, reference);
        instructions.add(0, stringInstr);

        ImmutableMethodImplementation methodImplementation = new ImmutableMethodImplementation(1, instructions, null, null);
        InstructionWriteUtil writeUtil = new InstructionWriteUtil(methodImplementation, mStringPool);

        for (Instruction instr: writeUtil.getInstructions()) {
            if (instr instanceof Instruction31t) {
                Instruction31t instruction = (Instruction31t) instr;
                Assert.assertEquals("branch instruction (Format31t) target was not modified properly", instruction.getCodeOffset(), 6);
                break;
            }
        }
    }
}
