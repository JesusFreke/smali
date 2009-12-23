/*
 * [The "BSD licence"]
 * Copyright (c) 2009 Ben Gruver
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

package org.jf.dexlib.Code;

import org.jf.dexlib.Util.Hex;
import org.jf.dexlib.Util.NumberUtils;
import org.jf.dexlib.Code.Format.*;
import org.jf.dexlib.DexFile;

public class InstructionIterator {
    /**
     * This method partially decodes the instructions in the given byte array and iterates over them, calling
     * the appropriate ProcessRawInstructionDelegate method for each instruction
     * @param insns a byte array containing the instructions
     * @param processRawInstruction a <code>ProcessInstructionDelegate</code> object containing the methods
     * that get called for each instruction that is encountered
     */
    public static void IterateInstructions(byte[] insns, ProcessRawInstructionDelegate processRawInstruction) {
        int insnsPosition = 0;

        while (insnsPosition < insns.length) {
            Opcode opcode = Opcode.getOpcodeByValue(insns[insnsPosition]);

            if (opcode == null) {
                throw new RuntimeException("Unknown opcode: " + Hex.u1(insns[insnsPosition]));
            }

            if (opcode.referenceType == ReferenceType.none) {
                byte secondByte = insns[insnsPosition+1];
                //if this is one of the "special" opcodes
                if (opcode == Opcode.NOP && secondByte > 0) {
                    switch (secondByte) {
                        case 1:
                        {
                            //packed-switch
                            int size = NumberUtils.decodeUnsignedShort(insns[insnsPosition+2], insns[insnsPosition+3]);
                            int end = insnsPosition + size * 4 + 8;
                            processRawInstruction.ProcessPackedSwitchInstruction(insnsPosition, size, end-insnsPosition);
                            insnsPosition = end;
                            break;
                        }
                        case 2:
                        {
                            //sparse-switch
                            int size = NumberUtils.decodeUnsignedShort(insns[insnsPosition+2], insns[insnsPosition+3]);
                            int end = insnsPosition + size * 8 + 4;
                            processRawInstruction.ProcessSparseSwitchInstruction(insnsPosition, size, end-insnsPosition);
                            insnsPosition = end;
                            break;
                        }
                        case 3:
                        {
                            //fill-array-data
                            int elementWidth = NumberUtils.decodeUnsignedShort(insns[insnsPosition+2],
                                    insns[insnsPosition+3]);
                            int size = NumberUtils.decodeInt(insns[insnsPosition+4], insns[insnsPosition+5],
                                    insns[insnsPosition+6], insns[insnsPosition+7]);
                            int end = insnsPosition + (size * elementWidth) + 8;
                            if (end % 2 == 1) {
                                end++;
                            }
                            processRawInstruction.ProcessFillArrayDataInstruction(insnsPosition, elementWidth, size,
                                    end-insnsPosition);
                            insnsPosition = end;
                            break;
                        }
                    }
                } else {
                    processRawInstruction.ProcessNormalInstruction(opcode, insnsPosition);
                    insnsPosition += opcode.format.size;
                }
            } else {
                processRawInstruction.ProcessReferenceInstruction(opcode, insnsPosition);
                insnsPosition += opcode.format.size;
            }
        }
    }

    public static void IterateInstructions(DexFile dexFile, byte[] insns, ProcessInstructionDelegate delegate) {
        int currentCodeOffset = 0;

        while (currentCodeOffset < insns.length) {
            Opcode opcode = Opcode.getOpcodeByValue(insns[currentCodeOffset]);

            Instruction instruction = null;

            if (opcode == null) {
                throw new RuntimeException("Unknown opcode: " + Hex.u1(insns[currentCodeOffset]));
            }

            if (opcode == Opcode.NOP) {
                byte secondByte = insns[currentCodeOffset+1];
                switch (secondByte) {
                    case 0:
                    {
                        instruction = new Instruction10x(Opcode.NOP, insns, currentCodeOffset);
                        break;
                    }
                    case 1:
                    {
                        instruction = new PackedSwitchDataPseudoInstruction(insns, currentCodeOffset);
                        break;
                    }
                    case 2:
                    {
                        instruction = new SparseSwitchDataPseudoInstruction(insns, currentCodeOffset);
                        break;
                    }
                    case 3:
                    {
                        instruction = new ArrayDataPseudoInstruction(insns, currentCodeOffset);
                        break;
                    }
                }
            } else {
                instruction = opcode.format.Factory.makeInstruction(dexFile, opcode, insns, currentCodeOffset);
            }

            assert instruction != null;

            delegate.ProcessInstruction(currentCodeOffset, instruction);
            currentCodeOffset += instruction.getSize(currentCodeOffset);
        }
    }

    public static interface ProcessRawInstructionDelegate {
        /**
         * The <code>InstructionIterator</code> calls this method when a "normal" instruction is encountered. I.e.
         * not a special or reference instruction
         * @param opcode the opcode of the instruction that was encountered
         * @param index the start index of the instruction in the byte array that the
         * <code>InstructionIterator</code> is iterating
         */
        public void ProcessNormalInstruction(Opcode opcode, int index);

        /**
         * The <code>InstructionIterator</code> calls this method when a "reference" instruction is encountered.
         * I.e. an instruction that contains an index that is a reference to a string, method, type or field.
         * @param opcode the opcode of the instruction that was encountered
         * @param index the start index of the instruction in the byte array that the
         * <code>InstructionIterator</code> is iterating
         */
        public void ProcessReferenceInstruction(Opcode opcode, int index);

        /**
         * The <code>InstructionIterator</code> calls this method when a packed switch instruction is encountered.
         * @param index the start index of the instruction in the byte array that the
         * <code>InstructionIterator</code> is iterating
         * @param targetCount the number of targets that this packed switch structure contains
         * @param instructionLength the length of this instruction in bytes
         */
        public void ProcessPackedSwitchInstruction(int index, int targetCount, int instructionLength);

        /**
         * The <code>InstructionIterator</code> calls this method when a sparse switch instruction is encountered.
         * @param index the start index of the instruction in the byte array that the
         * <code>InstructionIterator</code> is iterating
         * @param targetCount the number of targets that this sparse switch structure contains
         * @param instructionLength the length of this instruction in bytes
         */
        public void ProcessSparseSwitchInstruction(int index, int targetCount, int instructionLength);

        /**
         * The <code>InstructionIterator</code> calls this method when a fill-array-data instruction is encountered.
         * @param index the start index of the instruction in the byte array that the
         * <code>InstructionIterator</code> is iterating
         * @param elementWidth the width of the elements contained in this fill-array-data structure
         * @param elementCount the number of elements contained in this fill-array-data structure
         * @param instructionLength the length of this instruction in bytes
         */
        public void ProcessFillArrayDataInstruction(int index, int elementWidth, int elementCount,
                                                    int instructionLength);
    }

    public static interface ProcessInstructionDelegate {
        public void ProcessInstruction(int index, Instruction instruction);
    }
}
