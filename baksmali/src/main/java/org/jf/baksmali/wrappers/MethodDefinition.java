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

package org.jf.baksmali.wrappers;

import org.jf.dexlib.ClassDataItem;
import org.jf.dexlib.CodeItem;
import org.jf.dexlib.MethodIdItem;
import org.jf.dexlib.code.InstructionField;
import org.jf.dexlib.code.Instruction;
import org.jf.dexlib.code.Opcode;
import org.jf.dexlib.code.Format.*;
import org.jf.dexlib.util.AccessFlags;
import org.jf.baksmali.wrappers.format.*;

import java.util.*;

public class MethodDefinition {
    private ClassDataItem.EncodedMethod encodedMethod;
    private MethodIdItem methodIdItem;
    private CodeItem codeItem;

    public MethodDefinition(ClassDataItem.EncodedMethod encodedMethod) {
        this.encodedMethod = encodedMethod;
        this.methodIdItem = encodedMethod.getMethod();
        this.codeItem = encodedMethod.getCodeItem();
    }

    private String methodName = null;
    public String getMethodName() {
        if (methodName == null) {
            methodName = methodIdItem.getMethodName(); 
        }
        return methodName;
    }

    private List<String> accessFlags = null;
    public List<String> getAccessFlags() {
        if (accessFlags == null) {
            accessFlags = new ArrayList<String>();

            for (AccessFlags accessFlag: AccessFlags.getAccessFlagsForMethod(encodedMethod.getAccessFlags())) {
                accessFlags.add(accessFlag.toString());
            }
        }
        return accessFlags;
    }

    private String prototype = null;
    public String getPrototype() {
        if (prototype == null) {
            prototype = methodIdItem.getPrototype().getPrototypeString();
        }
        return prototype;
    }

    private Boolean hasCode = null;
    public boolean getHasCode() {
        if (hasCode == null) {
            hasCode = (codeItem != null);
        }
        return hasCode;
    }

    private String registerCount = null;
    public String getRegisterCount() {
        if (registerCount == null) {
            if (codeItem == null) {
                registerCount = "0";
            } else {
                registerCount = Integer.toString(codeItem.getRegisterCount());
            }
        }
        return registerCount;
    }


    private List<MethodItem> methodItems = null;
    public List<MethodItem> getMethodItems() {
        if (methodItems == null) {
            MethodItemList methodItemList = new MethodItemList();
            methodItemList.generateMethodItemList();

            methodItems = new ArrayList<MethodItem>();

            methodItems.addAll(methodItemList.labels);
            methodItems.addAll(methodItemList.instructions);
            methodItems.addAll(methodItemList.blanks);
            Collections.sort(methodItems);
        }
        return methodItems;
    }


    private class MethodItemList {
        public HashSet<LabelMethodItem> labels = new HashSet<LabelMethodItem>();
        public List<InstructionFormatMethodItem> instructions = new ArrayList<InstructionFormatMethodItem>();
        public List<BlankMethodItem> blanks = new ArrayList<BlankMethodItem>();

        private HashMap<Integer, Integer> packedSwitchMap = new HashMap<Integer, Integer>();
        private HashMap<Integer, Integer> sparseSwitchMap = new HashMap<Integer, Integer>();


        public void generateMethodItemList() {
            if (codeItem == null) {
                return;
            }

            int offset = 0;
            for (InstructionField instructionField: codeItem.getInstructions()) {
                Instruction instruction = instructionField.getInstruction();
                if (instruction.getOpcode() == Opcode.PACKED_SWITCH) {
                    packedSwitchMap.put(offset+((Instruction31t)instruction).getOffset(), offset);
                } else if (instruction.getOpcode() == Opcode.SPARSE_SWITCH) {
                    sparseSwitchMap.put(offset+((Instruction31t)instruction).getOffset(), offset);
                }

                offset += instructionField.getSize(offset * 2) / 2;
            }

            offset = 0;
            for (InstructionField instructionField: codeItem.getInstructions()) {
                addMethodItemsForInstruction(offset, instructionField);
                blanks.add(new BlankMethodItem(offset));
                offset += instructionField.getSize(offset*2) / 2;
            }
            blanks.remove(blanks.size()-1);
        }

        private void addMethodItemsForInstruction(int offset, InstructionField instructionField) {
            Instruction instruction = instructionField.getInstruction();

            switch (instruction.getFormat()) {
                case Format10t:
                    instructions.add(new Instruction10tMethodItem(offset, (Instruction10t)instruction));
                    labels.add(new LabelMethodItem(offset + ((Instruction10t)instruction).getOffset(), "goto_"));
                    return;
                case Format10x:
                    instructions.add(new Instruction10xMethodItem(offset, (Instruction10x)instruction));
                    return;
                case Format11n:
                    instructions.add(new Instruction11nMethodItem(offset, (Instruction11n)instruction));
                    return;
                case Format11x:
                    instructions.add(new Instruction11xMethodItem(offset, (Instruction11x)instruction));
                    return;                
                case Format12x:
                    instructions.add(new Instruction12xMethodItem(offset, (Instruction12x)instruction));
                    return;
                case Format20t:
                    instructions.add(new Instruction20tMethodItem(offset, (Instruction20t)instruction));
                    labels.add(new LabelMethodItem(offset + ((Instruction20t)instruction).getOffset(), "goto_"));
                    return;
                case Format21c:
                    instructions.add(new Instruction21cMethodItem(offset, (Instruction21c)instruction));
                    return;
                case Format21h:
                    instructions.add(new Instruction21hMethodItem(offset, (Instruction21h)instruction));
                    return;
                case Format21s:
                    instructions.add(new Instruction21sMethodItem(offset, (Instruction21s)instruction));
                    return;
                case Format21t:
                    instructions.add(new Instruction21tMethodItem(offset, (Instruction21t)instruction));
                    labels.add(new LabelMethodItem(offset + ((Instruction21t)instruction).getOffset(), "cond_"));
                    return;
                case Format22b:
                    instructions.add(new Instruction22bMethodItem(offset, (Instruction22b)instruction));
                    return;
                case Format22c:
                    instructions.add(new Instruction22cMethodItem(offset, (Instruction22c)instruction));
                    return;
                case Format22s:
                    instructions.add(new Instruction22sMethodItem(offset, (Instruction22s)instruction));
                    return;
                case Format22t:
                    instructions.add(new Instruction22tMethodItem(offset, (Instruction22t)instruction));
                    labels.add(new LabelMethodItem(offset + ((Instruction22t)instruction).getOffset(), "cond_"));
                    return;
                case Format22x:
                    instructions.add(new Instruction22xMethodItem(offset, (Instruction22x)instruction));
                    return;
                case Format23x:
                    instructions.add(new Instruction23xMethodItem(offset, (Instruction23x)instruction));
                    return;
                case Format30t:
                    instructions.add(new Instruction30tMethodItem(offset, (Instruction30t)instruction));
                    labels.add(new LabelMethodItem(offset + ((Instruction30t)instruction).getOffset(), "goto_"));
                    return;
                case Format31c:
                    instructions.add(new Instruction31cMethodItem(offset, (Instruction31c)instruction));
                    return;
                case Format31i:
                    instructions.add(new Instruction31iMethodItem(offset, (Instruction31i)instruction));
                    return;
                case Format31t:
                    instructions.add(new Instruction31tMethodItem(offset, (Instruction31t)instruction));
                    if (instruction.getOpcode() == Opcode.FILL_ARRAY_DATA) {
                        labels.add(new LabelMethodItem(offset + ((Instruction31t)instruction).getOffset(), "array_"));
                    } else if (instruction.getOpcode() == Opcode.PACKED_SWITCH) {
                        labels.add(new LabelMethodItem(offset + ((Instruction31t)instruction).getOffset(), "pswitch_data_"));
                    } else if (instruction.getOpcode() == Opcode.SPARSE_SWITCH) {
                        labels.add(new LabelMethodItem(offset + ((Instruction31t)instruction).getOffset(), "sswitch_data_"));
                    }
                    return;
                case Format32x:
                    instructions.add(new Instruction32xMethodItem(offset, (Instruction32x)instruction));
                    return;
                case Format35c:
                    instructions.add(new Instruction35cMethodItem(offset, (Instruction35c)instruction));
                    return;
                case Format3rc:
                    instructions.add(new Instruction3rcMethodItem(offset, (Instruction3rc)instruction));
                    return;
                case Format51l:
                    instructions.add(new Instruction51lMethodItem(offset, (Instruction51l)instruction));
                    return;
                case ArrayData:
                    instructions.add(new ArrayDataMethodItem(offset, (ArrayDataPseudoInstruction)instruction));
                    return;
                case PackedSwitchData:
                {
                    Integer baseAddress = packedSwitchMap.get(offset);

                    if (baseAddress != null) {
                        instructions.add(new PackedSwitchMethodItem(offset,
                                (PackedSwitchDataPseudoInstruction)instruction, baseAddress));
                        for (int target: ((PackedSwitchDataPseudoInstruction)instruction).getTargets()) {
                            labels.add(new LabelMethodItem(baseAddress + target, "pswitch_"));
                        }
                    }
                    return;
                }
                case SparseSwitchData:
                {
                    Integer baseAddress = sparseSwitchMap.get(offset);
                    if (baseAddress != null) {
                        instructions.add(new SparseSwitchMethodItem(offset,
                                (SparseSwitchDataPseudoInstruction)instruction, baseAddress));
                        for (int target: ((SparseSwitchDataPseudoInstruction)instruction).getTargets()) {
                            labels.add(new LabelMethodItem(baseAddress + target, "sswitch_"));
                        }
                    }
                    return;
                }
            }
        }
    }
}
