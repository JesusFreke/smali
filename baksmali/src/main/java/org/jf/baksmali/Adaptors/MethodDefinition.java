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

package org.jf.baksmali.Adaptors;

import org.jf.baksmali.Adaptors.Format.*;
import org.jf.dexlib.*;
import org.jf.dexlib.Debug.DebugInstructionIterator;
import org.jf.dexlib.Code.Format.*;
import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.Opcode;
import org.jf.dexlib.Code.InstructionIterator;
import org.jf.dexlib.Util.AccessFlags;

import java.util.*;

public class MethodDefinition {
    private ClassDataItem.EncodedMethod encodedMethod;
    private MethodIdItem methodIdItem;
    private CodeItem codeItem;
    private AnnotationSetItem annotationSet;
    private AnnotationSetRefList parameterAnnotations;

    public MethodDefinition(ClassDataItem.EncodedMethod encodedMethod, AnnotationSetItem annotationSet,
                            AnnotationSetRefList parameterAnnotations) {
        this.encodedMethod = encodedMethod;
        this.methodIdItem = encodedMethod.method;
        this.codeItem = encodedMethod.codeItem;
        this.annotationSet = annotationSet;
        this.parameterAnnotations = parameterAnnotations;
    }

    public String getMethodName() {
        return methodIdItem.getMethodName().getStringValue(); 
    }

    private List<String> accessFlags = null;
    public List<String> getAccessFlags() {
        if (accessFlags == null) {
            accessFlags = new ArrayList<String>();

            for (AccessFlags accessFlag: AccessFlags.getAccessFlagsForMethod(encodedMethod.accessFlags)) {
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

    public List<AnnotationAdaptor> getAnnotations() {
        if (annotationSet == null) {
            return null;
        }

        List<AnnotationAdaptor> annotationAdaptors = new ArrayList<AnnotationAdaptor>();

        for (AnnotationItem annotationItem: annotationSet.getAnnotations()) {
            annotationAdaptors.add(new AnnotationAdaptor(annotationItem));
        }
        return annotationAdaptors;
    }

    public List<ParameterAdaptor> getParameters() {
        DebugInfoItem debugInfoItem = null;
        if (codeItem != null) {
            debugInfoItem = codeItem.getDebugInfo();
        }

        int parameterCount = 0;

        List<AnnotationSetItem> annotations = new ArrayList<AnnotationSetItem>();
        if (parameterAnnotations != null) {
            AnnotationSetItem[] _annotations = parameterAnnotations.getAnnotationSets();
            if (_annotations != null) {
                annotations.addAll(Arrays.asList(_annotations));
            }

            parameterCount = annotations.size();
        }

        List<String> parameterNames = new ArrayList<String>();
        if (debugInfoItem != null) {
            StringIdItem[] _parameterNames = debugInfoItem.getParameterNames();
            if (_parameterNames != null) {
                for (StringIdItem parameterName: _parameterNames) {
                    parameterNames.add(parameterName==null?null:parameterName.getStringValue());
                }
            }

            if (parameterCount < parameterNames.size()) {
                parameterCount = parameterNames.size();
            }
        }

        List<ParameterAdaptor> parameterAdaptors = new ArrayList<ParameterAdaptor>();
        for (int i=0; i<parameterCount; i++) {
            AnnotationSetItem annotationSet = null;
            if (i < annotations.size()) {
                annotationSet = annotations.get(i);
            }

            String parameterName = null;
            if (i < parameterNames.size()) {
                parameterName = parameterNames.get(i);
            }

            parameterAdaptors.add(new ParameterAdaptor(parameterName, annotationSet));
        }

        return parameterAdaptors;
    }

    public StringIdItem[] getParameterNames() {
        if (codeItem == null) {
            return null;
        }

        DebugInfoItem debugInfoItem = codeItem.getDebugInfo();
        if (debugInfoItem == null) {
            return null;
        }

        return debugInfoItem.getParameterNames();
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
            methodItems.addAll(methodItemList.catches);
            methodItems.addAll(methodItemList.debugItems);
            Collections.sort(methodItems);
        }
        return methodItems;
    }


    private class MethodItemList {
        public HashSet<LabelMethodItem> labels = new HashSet<LabelMethodItem>();
        public List<MethodItem> instructions = new ArrayList<MethodItem>();
        public List<BlankMethodItem> blanks = new ArrayList<BlankMethodItem>();
        public List<CatchMethodItem> catches = new ArrayList<CatchMethodItem>();
        public List<MethodItem> debugItems = new ArrayList<MethodItem>();

        private HashMap<Integer, Integer> packedSwitchMap = new HashMap<Integer, Integer>();
        private HashMap<Integer, Integer> sparseSwitchMap = new HashMap<Integer, Integer>();


        public void generateMethodItemList() {
            if (codeItem == null) {
                return;
            }

            final byte[] encodedInstructions = codeItem.getEncodedInstructions();

            InstructionIterator.IterateInstructions(encodedInstructions,
                    new InstructionIterator.ProcessRawInstructionDelegate() {
                        public void ProcessNormalInstruction(Opcode opcode, int index) {
                            if (opcode == Opcode.PACKED_SWITCH) {
                                Instruction31t ins = (Instruction31t)opcode.format.Factory.makeInstruction(
                                        methodIdItem.getDexFile(), opcode, encodedInstructions, index);
                                packedSwitchMap.put(ins.getOffset(), index/2);
                            } else if (opcode == Opcode.SPARSE_SWITCH) {
                                Instruction31t ins = (Instruction31t)opcode.format.Factory.makeInstruction(
                                        methodIdItem.getDexFile(), opcode, encodedInstructions, index);
                                sparseSwitchMap.put(ins.getOffset(),  index/2);
                            }
                        }

                        public void ProcessReferenceInstruction(Opcode opcode, int index) {
                        }

                        public void ProcessPackedSwitchInstruction(int index, int targetCount, int instructionLength) {
                        }

                        public void ProcessSparseSwitchInstruction(int index, int targetCount, int instructionLength) {
                        }

                        public void ProcessFillArrayDataInstruction(int index, int elementWidth, int elementCount, int instructionLength) {
                        }
                    });

            InstructionIterator.IterateInstructions(methodIdItem.getDexFile(), encodedInstructions,
                    new InstructionIterator.ProcessInstructionDelegate() {
                        public void ProcessInstruction(int index, Instruction instruction) {
                            int offset = index/2;
                            addMethodItemsForInstruction(offset, instruction);
                            blanks.add(new BlankMethodItem(offset));
                        }
                    });
            
            blanks.remove(blanks.size()-1);

            addTries();

            addDebugInfo();
        }

        private void addMethodItemsForInstruction(int offset, Instruction instruction) {
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
                    if (instruction.opcode == Opcode.FILL_ARRAY_DATA) {
                        labels.add(new LabelMethodItem(offset + ((Instruction31t)instruction).getOffset(), "array_"));
                    } else if (instruction.opcode == Opcode.PACKED_SWITCH) {
                        labels.add(new LabelMethodItem(offset + ((Instruction31t)instruction).getOffset(), "pswitch_data_"));
                    } else if (instruction.opcode == Opcode.SPARSE_SWITCH) {
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
                    final Integer baseAddress = packedSwitchMap.get(offset);

                    if (baseAddress != null) {
                        PackedSwitchDataPseudoInstruction packedSwitchInstruction =
                                (PackedSwitchDataPseudoInstruction)instruction;

                        instructions.add(new PackedSwitchMethodItem(offset,
                                packedSwitchInstruction, baseAddress));

                        Iterator<PackedSwitchDataPseudoInstruction.PackedSwitchTarget> iterator =
                                packedSwitchInstruction.getTargets();
                        while (iterator.hasNext()) {
                            PackedSwitchDataPseudoInstruction.PackedSwitchTarget target = iterator.next();
                            labels.add(new LabelMethodItem(baseAddress + target.target, "pswitch_"));
                        }
                    }
                    return;
                }
                case SparseSwitchData:
                {
                    final Integer baseAddress = sparseSwitchMap.get(offset);

                    if (baseAddress != null) {
                        SparseSwitchDataPseudoInstruction sparseSwitchInstruction =
                                (SparseSwitchDataPseudoInstruction)instruction;

                        instructions.add(new SparseSwitchMethodItem(offset,
                                sparseSwitchInstruction, baseAddress));

                        Iterator<SparseSwitchDataPseudoInstruction.SparseSwitchTarget> iterator =
                                sparseSwitchInstruction.getTargets();
                        while (iterator.hasNext()) {
                            SparseSwitchDataPseudoInstruction.SparseSwitchTarget target = iterator.next();
                            labels.add(new LabelMethodItem(baseAddress + target.target, "sswitch_"));
                        }
                    }
                }
            }
        }

        private void addTries() {
            if (codeItem.getTries() == null) {
                return;
            }
            for (CodeItem.TryItem tryItem: codeItem.getTries()) {
                int startAddress = tryItem.startAddress;
                int endAddress = tryItem.startAddress + tryItem.instructionCount;

                /**
                 * The end address points to the address immediately after the end of the last
                 * instruction that the try block covers. We want the .catch directive and end_try
                 * label to be associated with the last covered instruction, so we need to get
                 * the offset for that instruction
                 */
                int index = Collections.binarySearch(instructions, new BlankMethodItem(endAddress));
                if (index < 0) {
                    index = (index * -1) - 1;
                }
                //index should never by 0, so this should be safe
                if (index == instructions.size()) {
                    //if the end address is the same as the address of the last instruction, then
                    //this try item ends at the next to last instruction.
                    //otherwise, if the end address is past the address of the last instruction,
                    //thin this try item ends at the last instruction
                    if (instructions.get(instructions.size() - 1).getOffset() == endAddress) {
                        //get the address for the next to last instruction
                        index -= 2;
                    } else {
                        //get the address for the last instruction
                        index--;
                    }
                } else {
                    index -= 2;
                }

                int lastInstructionOffset = instructions.get(index).getOffset();

                //add the catch all handler if it exists
                int catchAllAddress = tryItem.encodedCatchHandler.catchAllHandlerAddress;
                if (catchAllAddress != -1) {
                    CatchMethodItem catchMethodItem = new CatchMethodItem(lastInstructionOffset, null, startAddress,
                            endAddress, catchAllAddress) {
                        public String getTemplate() {
                            return "CatchAll";
                        }
                    };
                    catches.add(catchMethodItem);

                    labels.add(new LabelMethodItem(startAddress, "try_start_"));
                    //use the offset from the last covered instruction, but make the label
                    //name refer to the address of the next instruction
                    labels.add(new EndTryLabelMethodItem(lastInstructionOffset, endAddress));
                    labels.add(new LabelMethodItem(catchAllAddress, "handler_"));

                }

                //add the rest of the handlers
                //TODO: find adjacent handlers for the same type and combine them
                for (CodeItem.EncodedTypeAddrPair handler: tryItem.encodedCatchHandler.handlers) {
                    //use the offset from the last covered instruction
                    CatchMethodItem catchMethodItem = new CatchMethodItem(lastInstructionOffset,
                            handler.exceptionType, startAddress, endAddress, handler.handlerAddress);
                    catches.add(catchMethodItem);

                    labels.add(new LabelMethodItem(startAddress, "try_start_"));
                    //use the offset from the last covered instruction, but make the label
                    //name refer to the address of the next instruction
                    labels.add(new EndTryLabelMethodItem(lastInstructionOffset, endAddress));
                    labels.add(new LabelMethodItem(handler.handlerAddress, "handler_"));
                }
            }
        }

        private void addDebugInfo() {
            DebugInfoItem debugInfoItem = codeItem.getDebugInfo();
            if (debugInfoItem == null) {
                return;
            }

            DebugInstructionIterator.DecodeInstructions(debugInfoItem, codeItem.getRegisterCount(),
                    new DebugInstructionIterator.ProcessDecodedDebugInstructionDelegate() {
                        @Override
                        public void ProcessStartLocal(int codeAddress, int length, int registerNum, StringIdItem name,
                                                      TypeIdItem type) {
                            debugItems.add(new LocalDebugMethodItem(codeAddress, "StartLocal", -1, registerNum, name,
                                    type, null));
                        }

                        @Override
                        public void ProcessStartLocalExtended(int codeAddress, int length, int registerNum,
                                                              StringIdItem name, TypeIdItem type,
                                                              StringIdItem signature) {
                            debugItems.add(new LocalDebugMethodItem(codeAddress, "StartLocal", -1, registerNum, name,
                                    type, signature));
                        }

                        @Override
                        public void ProcessEndLocal(int codeAddress, int length, int registerNum, StringIdItem name,
                                                    TypeIdItem type, StringIdItem signature) {
                            debugItems.add(new LocalDebugMethodItem(codeAddress, "EndLocal", -1, registerNum, name,
                                    type, signature));
                        }

                        @Override
                        public void ProcessRestartLocal(int codeAddress, int length, int registerNum, StringIdItem name,
                                                        TypeIdItem type, StringIdItem signature) {
                            debugItems.add(new LocalDebugMethodItem(codeAddress, "RestartLocal", -1, registerNum, name,
                                    type, signature));
                        }

                        @Override
                        public void ProcessSetPrologueEnd(int codeAddress) {
                            debugItems.add(new DebugMethodItem(codeAddress, "EndPrologue", -4));
                        }

                        @Override
                        public void ProcessSetEpilogueBegin(int codeAddress) {
                            debugItems.add(new DebugMethodItem(codeAddress, "StartEpilogue", -4));
                        }

                        @Override
                        public void ProcessSetFile(int codeAddress, int length, final StringIdItem name) {
                            debugItems.add(new DebugMethodItem(codeAddress, "SetFile", -3) {
                                    public String getFileName() {
                                        return name.getStringValue();
                                    }
                            });
                        }

                        @Override
                        public void ProcessLineEmit(int codeAddress, final int line) {
                             debugItems.add(new DebugMethodItem(codeAddress, "Line", -2) {
                                public int getLine() {
                                    return line;
                                }
                            });
                        }
                    });
        }
    }
}
