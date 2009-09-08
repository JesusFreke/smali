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
import org.jf.baksmali.baksmali;
import org.jf.dexlib.*;
import org.jf.dexlib.Debug.DebugInstructionIterator;
import org.jf.dexlib.Code.Format.*;
import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.Opcode;
import org.jf.dexlib.Code.InstructionIterator;
import org.jf.dexlib.Util.AccessFlags;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.antlr.stringtemplate.StringTemplate;

import java.util.*;

public class MethodDefinition {
    public static StringTemplate makeTemplate(StringTemplateGroup stg, ClassDataItem.EncodedMethod encodedMethod,
                                              AnnotationSetItem annotationSet,
                                              AnnotationSetRefList parameterAnnotations) {

        CodeItem codeItem = encodedMethod.codeItem;

        StringTemplate template = stg.getInstanceOf("method");

        template.setAttribute("AccessFlags", getAccessFlags(encodedMethod));
        template.setAttribute("MethodName", encodedMethod.method.getMethodName().getStringValue());
        template.setAttribute("Prototype", encodedMethod.method.getPrototype().getPrototypeString());
        template.setAttribute("HasCode", codeItem != null);
        template.setAttribute("RegisterCount", codeItem==null?"0":Integer.toString(codeItem.getRegisterCount()));
        template.setAttribute("Parameters", getParameters(stg, codeItem, parameterAnnotations));
        template.setAttribute("Annotations", getAnnotations(stg, annotationSet));
        template.setAttribute("MethodItems", getMethodItems(encodedMethod.method.getDexFile(), stg, codeItem));

        return template;        
    }

    private static List<String> getAccessFlags(ClassDataItem.EncodedMethod encodedMethod) {
        List<String> accessFlags = new ArrayList<String>();

        for (AccessFlags accessFlag: AccessFlags.getAccessFlagsForMethod(encodedMethod.accessFlags)) {
            accessFlags.add(accessFlag.toString());
        }
        
        return accessFlags;
    }

    private static List<StringTemplate> getParameters(StringTemplateGroup stg, CodeItem codeItem,
                                                               AnnotationSetRefList parameterAnnotations) {
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

        List<StringTemplate> parameters = new ArrayList<StringTemplate>();
        for (int i=0; i<parameterCount; i++) {
            AnnotationSetItem annotationSet = null;
            if (i < annotations.size()) {
                annotationSet = annotations.get(i);
            }

            String parameterName = null;
            if (i < parameterNames.size()) {
                parameterName = parameterNames.get(i);
            }

            parameters.add(ParameterAdaptor.makeTemplate(stg, parameterName, annotationSet));
        }

        return parameters;
    }

    private static List<StringTemplate> getAnnotations(StringTemplateGroup stg, AnnotationSetItem annotationSet) {
        if (annotationSet == null) {
            return null;
        }

        List<StringTemplate> annotationAdaptors = new ArrayList<StringTemplate>();

        for (AnnotationItem annotationItem: annotationSet.getAnnotations()) {
            annotationAdaptors.add(AnnotationAdaptor.makeTemplate(stg, annotationItem));
        }
        return annotationAdaptors;
    }

    private static List<MethodItem> getMethodItems(DexFile dexFile, StringTemplateGroup stg, CodeItem codeItem) {
        List<MethodItem> methodItems = new ArrayList<MethodItem>();

        MethodItemList methodItemList = new MethodItemList(dexFile, stg, codeItem);
        methodItemList.generateMethodItemList();

        methodItems.addAll(methodItemList.labels);
        methodItems.addAll(methodItemList.instructions);
        methodItems.addAll(methodItemList.blanks);
        methodItems.addAll(methodItemList.catches);
        methodItems.addAll(methodItemList.debugItems);
        Collections.sort(methodItems);

        return methodItems;
    }


    private static class MethodItemList {
        private final DexFile dexFile;
        private final StringTemplateGroup stg;
        private final CodeItem codeItem;

        public HashSet<LabelMethodItem> labels = new HashSet<LabelMethodItem>();
        public List<MethodItem> instructions = new ArrayList<MethodItem>();
        public List<BlankMethodItem> blanks = new ArrayList<BlankMethodItem>();
        public List<CatchMethodItem> catches = new ArrayList<CatchMethodItem>();
        public List<MethodItem> debugItems = new ArrayList<MethodItem>();

        private HashMap<Integer, Integer> packedSwitchMap = new HashMap<Integer, Integer>();
        private HashMap<Integer, Integer> sparseSwitchMap = new HashMap<Integer, Integer>();

        public MethodItemList(DexFile dexFile, StringTemplateGroup stg, CodeItem codeItem) {
            this.dexFile = dexFile;
            this.stg = stg;
            this.codeItem = codeItem;
        }

        public void generateMethodItemList() {
            if (codeItem == null) {
                return;
            }

            if (baksmali.deodexUtil != null && dexFile.isOdex()) {
                List<Instruction> instructions = baksmali.deodexUtil.deodexerizeCode(codeItem);

                int offset = 0;
                for (Instruction instruction: instructions) {
                    if (instruction.opcode == Opcode.PACKED_SWITCH) {
                        Instruction31t ins = (Instruction31t)instruction;
                        packedSwitchMap.put(offset + ins.getOffset(), offset);
                    } else if (instruction.opcode == Opcode.SPARSE_SWITCH) {
                        Instruction31t ins = (Instruction31t)instruction;
                        sparseSwitchMap.put(offset + ins.getOffset(), offset);
                    }

                    offset += instruction.getSize()/2;
                }

                offset = 0;
                for (Instruction instruction: instructions) {
                    addMethodItemsForInstruction(offset, instruction);
                    blanks.add(new BlankMethodItem(stg, offset));

                    offset += instruction.getSize()/2;
                }
            } else {
                final byte[] encodedInstructions = codeItem.getEncodedInstructions();

                InstructionIterator.IterateInstructions(encodedInstructions,
                        new InstructionIterator.ProcessRawInstructionDelegate() {
                            public void ProcessNormalInstruction(Opcode opcode, int index) {
                                if (opcode == Opcode.PACKED_SWITCH) {
                                    Instruction31t ins = (Instruction31t)opcode.format.Factory.makeInstruction(
                                            dexFile, opcode, encodedInstructions, index);
                                    packedSwitchMap.put(index/2 + ins.getOffset(), index/2);
                                } else if (opcode == Opcode.SPARSE_SWITCH) {
                                    Instruction31t ins = (Instruction31t)opcode.format.Factory.makeInstruction(
                                            dexFile, opcode, encodedInstructions, index);
                                    sparseSwitchMap.put(index/2 + ins.getOffset(),  index/2);
                                }
                            }

                            public void ProcessReferenceInstruction(Opcode opcode, int index) {
                            }

                            public void ProcessPackedSwitchInstruction(int index, int targetCount, int instructionLength) {
                            }

                            public void ProcessSparseSwitchInstruction(int index, int targetCount, int instructionLength) {
                            }

                            public void ProcessFillArrayDataInstruction(int index, int elementWidth, int elementCount,
                                                                        int instructionLength) {
                            }
                        });

                InstructionIterator.IterateInstructions(dexFile, encodedInstructions,
                        new InstructionIterator.ProcessInstructionDelegate() {
                            public void ProcessInstruction(int index, Instruction instruction) {
                                int offset = index/2;
                                addMethodItemsForInstruction(offset, instruction);
                                blanks.add(new BlankMethodItem(stg, offset));
                            }
                        });
            }
            
            blanks.remove(blanks.size()-1);

            addTries();

            addDebugInfo();
        }

        private void addMethodItemsForInstruction(int offset, Instruction instruction) {
            switch (instruction.getFormat()) {
                case Format10t:
                    instructions.add(new Instruction10tMethodItem(codeItem, offset, stg,(Instruction10t)instruction));
                    labels.add(new LabelMethodItem(offset + ((Instruction10t)instruction).getOffset(), stg, "goto_"));
                    return;
                case Format10x:
                    instructions.add(new Instruction10xMethodItem(codeItem, offset, stg, (Instruction10x)instruction));
                    return;
                case Format11n:
                    instructions.add(new Instruction11nMethodItem(codeItem, offset, stg, (Instruction11n)instruction));
                    return;
                case Format11x:
                    instructions.add(new Instruction11xMethodItem(codeItem, offset, stg, (Instruction11x)instruction));
                    return;                
                case Format12x:
                    instructions.add(new Instruction12xMethodItem(codeItem, offset, stg, (Instruction12x)instruction));
                    return;
                case Format20t:
                    instructions.add(new Instruction20tMethodItem(codeItem, offset, stg, (Instruction20t)instruction));
                    labels.add(new LabelMethodItem(offset + ((Instruction20t)instruction).getOffset(), stg, "goto_"));
                    return;
                case Format21c:
                    instructions.add(new Instruction21cMethodItem(codeItem, offset, stg, (Instruction21c)instruction));
                    return;
                case Format21h:
                    instructions.add(new Instruction21hMethodItem(codeItem, offset, stg, (Instruction21h)instruction));
                    return;
                case Format21s:
                    instructions.add(new Instruction21sMethodItem(codeItem, offset, stg, (Instruction21s)instruction));
                    return;
                case Format21t:
                    instructions.add(new Instruction21tMethodItem(codeItem, offset, stg, (Instruction21t)instruction));
                    labels.add(new LabelMethodItem(offset + ((Instruction21t)instruction).getOffset(), stg, "cond_"));
                    return;
                case Format22b:
                    instructions.add(new Instruction22bMethodItem(codeItem, offset, stg, (Instruction22b)instruction));
                    return;
                case Format22c:
                    instructions.add(new Instruction22cMethodItem(codeItem, offset, stg, (Instruction22c)instruction));
                    return;
                case Format22cs:
                    instructions.add(new Instruction22csMethodItem(codeItem, offset, stg,
                            (Instruction22cs)instruction));
                    return;
                case Format22csf:
                    instructions.add(new Instruction22csfMethodItem(codeItem, offset, stg,
                            (Instruction22csf)instruction));
                    return;
                case Format22csn:
                    instructions.add(new Instruction22csnMethodItem(codeItem, offset, stg,
                            (Instruction22csn)instruction));
                    return;
                case Format22s:
                    instructions.add(new Instruction22sMethodItem(codeItem, offset, stg, (Instruction22s)instruction));
                    return;
                case Format22t:
                    instructions.add(new Instruction22tMethodItem(codeItem, offset, stg, (Instruction22t)instruction));
                    labels.add(new LabelMethodItem(offset + ((Instruction22t)instruction).getOffset(), stg, "cond_"));
                    return;
                case Format22x:
                    instructions.add(new Instruction22xMethodItem(codeItem, offset, stg, (Instruction22x)instruction));
                    return;
                case Format23x:
                    instructions.add(new Instruction23xMethodItem(codeItem, offset, stg, (Instruction23x)instruction));
                    return;
                case Format30t:
                    instructions.add(new Instruction30tMethodItem(codeItem, offset, stg, (Instruction30t)instruction));
                    labels.add(new LabelMethodItem(offset + ((Instruction30t)instruction).getOffset(), stg, "goto_"));
                    return;
                case Format31c:
                    instructions.add(new Instruction31cMethodItem(codeItem, offset, stg, (Instruction31c)instruction));
                    return;
                case Format31i:
                    instructions.add(new Instruction31iMethodItem(codeItem, offset, stg, (Instruction31i)instruction));
                    return;
                case Format31t:
                    instructions.add(new Instruction31tMethodItem(codeItem, offset, stg, (Instruction31t)instruction));
                    if (instruction.opcode == Opcode.FILL_ARRAY_DATA) {
                        labels.add(new LabelMethodItem(offset + ((Instruction31t)instruction).getOffset(), stg,
                                "array_"));
                    } else if (instruction.opcode == Opcode.PACKED_SWITCH) {
                        labels.add(new LabelMethodItem(offset + ((Instruction31t)instruction).getOffset(), stg,
                                "pswitch_data_"));
                    } else if (instruction.opcode == Opcode.SPARSE_SWITCH) {
                        labels.add(new LabelMethodItem(offset + ((Instruction31t)instruction).getOffset(), stg,
                                "sswitch_data_"));
                    }
                    return;
                case Format32x:
                    instructions.add(new Instruction32xMethodItem(codeItem, offset, stg, (Instruction32x)instruction));
                    return;
                case Format35c:
                    instructions.add(new Instruction35cMethodItem(codeItem, offset, stg, (Instruction35c)instruction));
                    return;
                case Format35s:
                    instructions.add(new Instruction35sMethodItem(codeItem, offset, stg, (Instruction35s)instruction));
                    return;
                case Format35sf:
                    instructions.add(new Instruction35sfMethodItem(codeItem, offset, stg,
                            (Instruction35sf)instruction));
                    return;
                case Format35ms:
                    instructions.add(new Instruction35msMethodItem(codeItem, offset, stg,
                            (Instruction35ms)instruction));
                    return;
                case Format35msf:
                    instructions.add(new Instruction35msfMethodItem(codeItem, offset, stg,
                            (Instruction35msf)instruction));
                    return;
                case Format35msn:
                    instructions.add(new Instruction35msnMethodItem(codeItem, offset, stg,
                            (Instruction35msn)instruction));
                    return;
                case Format3rc:
                    instructions.add(new Instruction3rcMethodItem(codeItem, offset, stg, (Instruction3rc)instruction));
                    return;
                case Format3rms:
                    instructions.add(new Instruction3rmsMethodItem(codeItem, offset, stg,
                            (Instruction3rms)instruction));
                    return;
                case Format3rmsf:
                    instructions.add(new Instruction3rmsfMethodItem(codeItem, offset, stg,
                            (Instruction3rmsf)instruction));
                    return;
                case Format51l:
                    instructions.add(new Instruction51lMethodItem(codeItem, offset, stg, (Instruction51l)instruction));
                    return;
                case ArrayData:
                    instructions.add(new ArrayDataMethodItem(codeItem, offset, stg, (ArrayDataPseudoInstruction)instruction));
                    return;
                case PackedSwitchData:
                {
                    final Integer baseAddress = packedSwitchMap.get(offset);

                    if (baseAddress != null) {
                        PackedSwitchDataPseudoInstruction packedSwitchInstruction =
                                (PackedSwitchDataPseudoInstruction)instruction;

                        instructions.add(new PackedSwitchMethodItem(codeItem, offset, stg,
                                packedSwitchInstruction, baseAddress));

                        Iterator<PackedSwitchDataPseudoInstruction.PackedSwitchTarget> iterator =
                                packedSwitchInstruction.getTargets();
                        while (iterator.hasNext()) {
                            PackedSwitchDataPseudoInstruction.PackedSwitchTarget target = iterator.next();
                            labels.add(new LabelMethodItem(baseAddress + target.target, stg, "pswitch_"));
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

                        instructions.add(new SparseSwitchMethodItem(codeItem, offset, stg,
                                sparseSwitchInstruction, baseAddress));

                        Iterator<SparseSwitchDataPseudoInstruction.SparseSwitchTarget> iterator =
                                sparseSwitchInstruction.getTargets();
                        while (iterator.hasNext()) {
                            SparseSwitchDataPseudoInstruction.SparseSwitchTarget target = iterator.next();
                            labels.add(new LabelMethodItem(baseAddress + target.target, stg, "sswitch_"));
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
                int index = Collections.binarySearch(instructions, new BlankMethodItem(stg, endAddress));
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
                    CatchMethodItem catchMethodItem = new CatchMethodItem(lastInstructionOffset, stg, null,
                            startAddress, endAddress, catchAllAddress) {
                        public String getTemplateName() {
                            return "CatchAll";
                        }
                    };
                    catches.add(catchMethodItem);

                    labels.add(new LabelMethodItem(startAddress, stg, "try_start_"));
                    //use the offset from the last covered instruction, but make the label
                    //name refer to the address of the next instruction
                    labels.add(new EndTryLabelMethodItem(lastInstructionOffset, stg, endAddress));
                    labels.add(new LabelMethodItem(catchAllAddress, stg, "handler_"));

                }

                //add the rest of the handlers
                //TODO: find adjacent handlers for the same type and combine them
                for (CodeItem.EncodedTypeAddrPair handler: tryItem.encodedCatchHandler.handlers) {
                    //use the offset from the last covered instruction
                    CatchMethodItem catchMethodItem = new CatchMethodItem(lastInstructionOffset, stg,
                            handler.exceptionType, startAddress, endAddress, handler.handlerAddress);
                    catches.add(catchMethodItem);

                    labels.add(new LabelMethodItem(startAddress, stg, "try_start_"));
                    //use the offset from the last covered instruction, but make the label
                    //name refer to the address of the next instruction
                    labels.add(new EndTryLabelMethodItem(lastInstructionOffset, stg, endAddress));
                    labels.add(new LabelMethodItem(handler.handlerAddress, stg, "handler_"));
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
                            debugItems.add(new LocalDebugMethodItem(codeItem, codeAddress, stg, "StartLocal", -1,
                                    registerNum, name, type, null));
                        }

                        @Override
                        public void ProcessStartLocalExtended(int codeAddress, int length, int registerNum,
                                                              StringIdItem name, TypeIdItem type,
                                                              StringIdItem signature) {
                            debugItems.add(new LocalDebugMethodItem(codeItem, codeAddress, stg, "StartLocal", -1,
                                    registerNum, name, type, signature));
                        }

                        @Override
                        public void ProcessEndLocal(int codeAddress, int length, int registerNum, StringIdItem name,
                                                    TypeIdItem type, StringIdItem signature) {
                            debugItems.add(new LocalDebugMethodItem(codeItem, codeAddress, stg, "EndLocal", -1,
                                    registerNum, name, type, signature));
                        }

                        @Override
                        public void ProcessRestartLocal(int codeAddress, int length, int registerNum, StringIdItem name,
                                                        TypeIdItem type, StringIdItem signature) {
                            debugItems.add(new LocalDebugMethodItem(codeItem, codeAddress, stg, "RestartLocal", -1,
                                    registerNum, name, type, signature));
                        }

                        @Override
                        public void ProcessSetPrologueEnd(int codeAddress) {
                            debugItems.add(new DebugMethodItem(codeAddress, stg, "EndPrologue", -4));
                        }

                        @Override
                        public void ProcessSetEpilogueBegin(int codeAddress) {
                            debugItems.add(new DebugMethodItem(codeAddress, stg, "StartEpilogue", -4));
                        }

                        @Override
                        public void ProcessSetFile(int codeAddress, int length, final StringIdItem name) {
                            debugItems.add(new DebugMethodItem(codeAddress, stg, "SetFile", -3) {
                                @Override
                                protected void setAttributes(StringTemplate template) {
                                    template.setAttribute("FileName", name.getStringValue());
                                }
                            });
                        }

                        @Override
                        public void ProcessLineEmit(int codeAddress, final int line) {
                             debugItems.add(new DebugMethodItem(codeAddress, stg, "Line", -2) {
                                 @Override
                                 protected void setAttributes(StringTemplate template) {
                                     template.setAttribute("Line", line);
                                 }
                             });
                        }
                    });
        }
    }
}
