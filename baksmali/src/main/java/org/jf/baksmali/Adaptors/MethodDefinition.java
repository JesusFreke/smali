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
import org.jf.dexlib.Code.OffsetInstruction;
import org.jf.dexlib.Util.AccessFlags;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.antlr.stringtemplate.StringTemplate;

import java.util.*;

public class MethodDefinition {
    public static StringTemplate createTemplate(StringTemplateGroup stg, ClassDataItem.EncodedMethod encodedMethod,
                                              AnnotationSetItem annotationSet,
                                              AnnotationSetRefList parameterAnnotations) {

        CodeItem codeItem = encodedMethod.codeItem;

        StringTemplate template = stg.getInstanceOf("method");

        template.setAttribute("AccessFlags", getAccessFlags(encodedMethod));
        template.setAttribute("MethodName", encodedMethod.method.getMethodName().getStringValue());
        template.setAttribute("Prototype", encodedMethod.method.getPrototype().getPrototypeString());
        template.setAttribute("HasCode", codeItem != null);
        template.setAttribute("RegistersDirective", baksmali.useLocalsDirective?".locals":".registers");
        template.setAttribute("RegisterCount", codeItem==null?"0":Integer.toString(getRegisterCount(encodedMethod)));
        template.setAttribute("Parameters", getParameters(stg, codeItem, parameterAnnotations));
        template.setAttribute("Annotations", getAnnotations(stg, annotationSet));
        template.setAttribute("MethodItems", getMethodItems(encodedMethod.method.getDexFile(), stg, codeItem));

        return template;
    }

    private static int getRegisterCount(ClassDataItem.EncodedMethod encodedMethod)
    {
        int totalRegisters = encodedMethod.codeItem.getRegisterCount();
        if (baksmali.useLocalsDirective) {
            int parameterRegisters = encodedMethod.method.getPrototype().getParameterRegisterCount();
            if ((encodedMethod.accessFlags & AccessFlags.STATIC.getValue()) == 0) {
                parameterRegisters++;
            }
            return totalRegisters - parameterRegisters;
        }
        return totalRegisters;
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
        if (baksmali.outputDebugInfo && codeItem != null) {
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

            parameters.add(ParameterAdaptor.createTemplate(stg, parameterName, annotationSet));
        }

        return parameters;
    }

    private static List<StringTemplate> getAnnotations(StringTemplateGroup stg, AnnotationSetItem annotationSet) {
        if (annotationSet == null) {
            return null;
        }

        List<StringTemplate> annotationAdaptors = new ArrayList<StringTemplate>();

        for (AnnotationItem annotationItem: annotationSet.getAnnotations()) {
            annotationAdaptors.add(AnnotationAdaptor.createTemplate(stg, annotationItem));
        }
        return annotationAdaptors;
    }

    private static List<MethodItem> getMethodItems(DexFile dexFile, StringTemplateGroup stg, CodeItem codeItem) {
        List<MethodItem> methodItems = new ArrayList<MethodItem>();

        MethodItemList methodItemList = new MethodItemList(dexFile, stg, codeItem);
        methodItemList.generateMethodItemList();

        for (LabelMethodItem labelMethodItem: methodItemList.labels.getLabels()) {
            if (labelMethodItem.isCommentedOut()) {
                methodItems.add(new CommentedOutMethodItem(stg, labelMethodItem));
            } else {
                methodItems.add(labelMethodItem);
            }
        }

        methodItems.addAll(methodItemList.instructions);
        methodItems.addAll(methodItemList.blanks);
        methodItems.addAll(methodItemList.catches);
        if (baksmali.outputDebugInfo) {
            methodItems.addAll(methodItemList.debugItems);
        }
        Collections.sort(methodItems);

        return methodItems;
    }


    private static class MethodItemList {
        private final DexFile dexFile;
        private final StringTemplateGroup stg;
        private final CodeItem codeItem;

        public LabelCache labels = new LabelCache();

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

                    offset += instruction.getSize(offset*2)/2;
                }

                offset = 0;
                for (Instruction instruction: instructions) {
                    addMethodItemsForInstruction(offset, instruction, false);
                    blanks.add(new BlankMethodItem(stg, offset));

                    offset += instruction.getSize(offset*2)/2;
                }

                /*
                 * Look for the last uncommented instruction. If it is an UnresolvedNullReference,
                 * then set IsLastInstruction, so a goto will be added after it, to avoid validation
                 * issues
                 */
                for (int i=this.instructions.size()-1; i>=0; i--) {
                    MethodItem ins = this.instructions.get(i);
                    if (ins instanceof UnresolvedNullReferenceMethodItem) {
                        ((UnresolvedNullReferenceMethodItem)ins).setIsLastInstruction(true);
                        break;
                    }

                    if (!(ins instanceof CommentedOutMethodItem)) {
                        break;
                    }
                }
            } else {
                int currentCodeOffset = 0;
                for (Instruction instruction: codeItem.getInstructions()) {
                    if (instruction.opcode == Opcode.PACKED_SWITCH) {
                        OffsetInstruction offsetInstruction = (OffsetInstruction)instruction;
                        packedSwitchMap.put(currentCodeOffset/2 + offsetInstruction.getOffset(), currentCodeOffset/2);
                    } else if (instruction.opcode == Opcode.SPARSE_SWITCH) {
                        OffsetInstruction offsetInstruction = (OffsetInstruction)instruction;
                        sparseSwitchMap.put(currentCodeOffset/2 + offsetInstruction.getOffset(), currentCodeOffset/2);
                    }

                    currentCodeOffset += instruction.getSize(currentCodeOffset);
                }

                currentCodeOffset = 0;
                for (Instruction instruction: codeItem.getInstructions()) {
                    int offset = currentCodeOffset/2;
                    addMethodItemsForInstruction(offset, instruction, false);
                    blanks.add(new BlankMethodItem(stg, offset));
                    currentCodeOffset += instruction.getSize(currentCodeOffset);
                }

            }

            blanks.remove(blanks.size()-1);

            addTries();

            addDebugInfo();

            if (baksmali.useSequentialLabels) {
                setLabelSequentialNumbers();
            }
        }

        private void addOffsetInstructionMethodItem(OffsetInstructionFormatMethodItem methodItem,
                                                    boolean commentedOut) {
            if (commentedOut) {
                instructions.add(new CommentedOutMethodItem(stg, methodItem));
            } else {
                instructions.add(methodItem);
                LabelMethodItem label = methodItem.getLabel();
                label.setUncommented();
            }
        }


        private void addInstructionMethodItem(InstructionFormatMethodItem methodItem, boolean commentedOut) {
            if (commentedOut) {
                instructions.add(new CommentedOutMethodItem(stg, methodItem));
            } else {
                instructions.add(methodItem);
            }
        }

        private void addMethodItemsForInstruction(int offset, Instruction instruction, boolean commentedOut) {
            switch (instruction.getFormat()) {
                case Format10t:
                    addOffsetInstructionMethodItem(
                            new Instruction10tMethodItem(labels, codeItem, offset, stg,(Instruction10t)instruction),
                            commentedOut);
                    return;
                case Format10x:
                    addInstructionMethodItem(
                            new Instruction10xMethodItem(codeItem, offset, stg, (Instruction10x)instruction),
                            commentedOut);
                    return;
                case Format11n:
                    addInstructionMethodItem(
                            new Instruction11nMethodItem(codeItem, offset, stg, (Instruction11n)instruction),
                            commentedOut);
                    return;
                case Format11x:
                    addInstructionMethodItem(
                            new Instruction11xMethodItem(codeItem, offset, stg, (Instruction11x)instruction),
                            commentedOut);
                    return;
                case Format12x:
                    addInstructionMethodItem(
                            new Instruction12xMethodItem(codeItem, offset, stg, (Instruction12x)instruction),
                            commentedOut);
                    return;
                case Format20t:
                    addOffsetInstructionMethodItem(
                            new Instruction20tMethodItem(labels, codeItem, offset, stg, (Instruction20t)instruction),
                            commentedOut);
                    return;
                case Format21c:
                    addInstructionMethodItem(
                            new Instruction21cMethodItem(codeItem, offset, stg, (Instruction21c)instruction),
                            commentedOut);
                    return;
                case Format21h:
                    addInstructionMethodItem(
                            new Instruction21hMethodItem(codeItem, offset, stg, (Instruction21h)instruction),
                            commentedOut);
                    return;
                case Format21s:
                    addInstructionMethodItem(
                            new Instruction21sMethodItem(codeItem, offset, stg, (Instruction21s)instruction),
                            commentedOut);
                    return;
                case Format21t:
                    addOffsetInstructionMethodItem(
                            new Instruction21tMethodItem(labels, codeItem, offset, stg, (Instruction21t)instruction),
                            commentedOut);
                    return;
                case Format22b:
                    addInstructionMethodItem(
                            new Instruction22bMethodItem(codeItem, offset, stg, (Instruction22b)instruction),
                            commentedOut);
                    return;
                case Format22c:
                    addInstructionMethodItem(
                            new Instruction22cMethodItem(codeItem, offset, stg, (Instruction22c)instruction),
                            commentedOut);
                    return;
                case Format22cs:
                    addInstructionMethodItem(
                            new Instruction22csMethodItem(codeItem, offset, stg, (Instruction22cs)instruction),
                            commentedOut);
                    return;
                case Format22csf:
                    addInstructionMethodItem(
                            new Instruction22csfMethodItem(codeItem, offset, stg, (Instruction22csf)instruction),
                            commentedOut);
                    return;
                case Format22s:
                    addInstructionMethodItem(
                            new Instruction22sMethodItem(codeItem, offset, stg, (Instruction22s)instruction),
                            commentedOut);
                    return;
                case Format22t:
                    addOffsetInstructionMethodItem(
                            new Instruction22tMethodItem(labels, codeItem, offset, stg, (Instruction22t)instruction),
                            commentedOut);
                    return;
                case Format22x:
                    addInstructionMethodItem(
                            new Instruction22xMethodItem(codeItem, offset, stg, (Instruction22x)instruction),
                            commentedOut);
                    return;
                case Format23x:
                    addInstructionMethodItem(
                            new Instruction23xMethodItem(codeItem, offset, stg, (Instruction23x)instruction),
                            commentedOut);
                    return;
                case Format30t:
                    addOffsetInstructionMethodItem(
                            new Instruction30tMethodItem(labels, codeItem, offset, stg, (Instruction30t)instruction),
                            commentedOut);
                    return;
                case Format31c:
                    addInstructionMethodItem(
                            new Instruction31cMethodItem(codeItem, offset, stg, (Instruction31c)instruction),
                            commentedOut);
                    return;
                case Format31i:
                    addInstructionMethodItem(
                            new Instruction31iMethodItem(codeItem, offset, stg, (Instruction31i)instruction),
                            commentedOut);
                    return;
                case Format31t:
                    addOffsetInstructionMethodItem(
                            new Instruction31tMethodItem(labels, codeItem, offset, stg, (Instruction31t)instruction),
                            commentedOut);
                    return;
                case Format32x:
                    addInstructionMethodItem(
                            new Instruction32xMethodItem(codeItem, offset, stg, (Instruction32x)instruction),
                            commentedOut);
                    return;
                case Format35c:
                    addInstructionMethodItem(
                            new Instruction35cMethodItem(codeItem, offset, stg, (Instruction35c)instruction),
                            commentedOut);
                    return;
                case Format35s:
                    addInstructionMethodItem(
                            new Instruction35sMethodItem(codeItem, offset, stg, (Instruction35s)instruction),
                            commentedOut);
                    return;
                case Format35sf:
                    addInstructionMethodItem(
                            new Instruction35sfMethodItem(codeItem, offset, stg, (Instruction35sf)instruction),
                            commentedOut);
                    return;
                case Format35ms:
                    addInstructionMethodItem(
                            new Instruction35msMethodItem(codeItem, offset, stg, (Instruction35ms)instruction),
                            commentedOut);
                    return;
                case Format35msf:
                    addInstructionMethodItem(
                            new Instruction35msfMethodItem(codeItem, offset, stg, (Instruction35msf)instruction),
                            commentedOut);
                    return;
                case Format3rc:
                    addInstructionMethodItem(
                            new Instruction3rcMethodItem(codeItem, offset, stg, (Instruction3rc)instruction),
                            commentedOut);
                    return;
                case Format3rms:
                    addInstructionMethodItem(
                            new Instruction3rmsMethodItem(codeItem, offset, stg, (Instruction3rms)instruction),
                            commentedOut);
                    return;
                case Format3rmsf:
                    addInstructionMethodItem(
                            new Instruction3rmsfMethodItem(codeItem, offset, stg, (Instruction3rmsf)instruction),
                            commentedOut);
                    return;
                case Format51l:
                    addInstructionMethodItem(
                            new Instruction51lMethodItem(codeItem, offset, stg, (Instruction51l)instruction),
                            commentedOut);
                    return;
                case ArrayData:
                    addInstructionMethodItem(
                            new ArrayDataMethodItem(codeItem, offset, stg, (ArrayDataPseudoInstruction)instruction),
                            commentedOut);
                    return;
                case PackedSwitchData:
                {
                    final Integer baseAddress = packedSwitchMap.get(offset);

                    if (baseAddress != null) {
                        PackedSwitchDataPseudoInstruction packedSwitchInstruction =
                                (PackedSwitchDataPseudoInstruction)instruction;

                        PackedSwitchMethodItem packedSwitch = new PackedSwitchMethodItem(labels, codeItem, offset, stg,
                                packedSwitchInstruction, baseAddress);
                        addInstructionMethodItem(packedSwitch, commentedOut);

                        if (!commentedOut) {
                            for (LabelMethodItem label: packedSwitch) {
                                label.setUncommented();
                            }
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

                        SparseSwitchMethodItem sparseSwitch = new SparseSwitchMethodItem(labels, codeItem, offset, stg,
                                sparseSwitchInstruction, baseAddress);
                        addInstructionMethodItem(sparseSwitch, commentedOut);

                        if (!commentedOut) {
                            for (LabelMethodItem label: sparseSwitch) {
                                label.setUncommented();
                            }
                        }
                    }
                    return;
                }
                case UnresolvedNullReference:
                {
                    addInstructionMethodItem(new UnresolvedNullReferenceMethodItem(codeItem, offset, stg,
                            (UnresolvedNullReference)instruction), commentedOut);
                    addMethodItemsForInstruction(offset, ((UnresolvedNullReference)instruction).OriginalInstruction,
                            true);
                    return;
                }
                case DeadInstruction:
                {
                    //TODO: what about try/catch blocks inside the dead code? those will need to be commented out too. ugh.
                    addMethodItemsForInstruction(offset, ((DeadInstruction)instruction).OriginalInstruction, true);
                    return;
                }
            }
        }

        private void addTries() {
            if (codeItem.getTries() == null) {
                return;
            }
            for (CodeItem.TryItem tryItem: codeItem.getTries()) {
                int startAddress = tryItem.getStartAddress();
                int endAddress = tryItem.getStartAddress() + tryItem.getInstructionCount();

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
                //index should never be 0, so this should be safe
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
                int catchAllAddress = tryItem.encodedCatchHandler.getCatchAllHandlerAddress();
                if (catchAllAddress != -1) {
                    CatchMethodItem catchMethodItem = new CatchMethodItem(labels, lastInstructionOffset, stg, null,
                            startAddress, endAddress, catchAllAddress);
                    catches.add(catchMethodItem);
                }

                //add the rest of the handlers
                for (CodeItem.EncodedTypeAddrPair handler: tryItem.encodedCatchHandler.handlers) {
                    //use the offset from the last covered instruction
                    CatchMethodItem catchMethodItem = new CatchMethodItem(labels, lastInstructionOffset, stg,
                            handler.exceptionType, startAddress, endAddress, handler.getHandlerAddress());
                    catches.add(catchMethodItem);
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

        private void setLabelSequentialNumbers() {
            HashMap<String, Integer> nextLabelSequenceByType = new HashMap<String, Integer>();
            ArrayList<LabelMethodItem> sortedLabels = new ArrayList<LabelMethodItem>(labels.getLabels());

            //sort the labels by their location in the method
            Collections.sort(sortedLabels);

            for (LabelMethodItem labelMethodItem: sortedLabels) {
                Integer labelSequence = nextLabelSequenceByType.get(labelMethodItem.getLabelPrefix());
                if (labelSequence == null) {
                    labelSequence = 0;
                }
                labelMethodItem.setLabelSequence(labelSequence);
                nextLabelSequenceByType.put(labelMethodItem.getLabelPrefix(), labelSequence + 1);
            }
        }
    }

    public static class LabelCache {
        protected HashMap<LabelMethodItem, LabelMethodItem> labels = new HashMap<LabelMethodItem, LabelMethodItem>();

        public LabelCache() {
        }

        public LabelMethodItem internLabel(LabelMethodItem labelMethodItem) {
            LabelMethodItem internedLabelMethodItem = labels.get(labelMethodItem);
            if (internedLabelMethodItem != null) {
                if (!labelMethodItem.isCommentedOut()) {
                    internedLabelMethodItem.setUncommented();
                }
                return internedLabelMethodItem;
            }
            labels.put(labelMethodItem, labelMethodItem);
            return labelMethodItem;
        }


        public Collection<LabelMethodItem> getLabels() {
            return labels.values();
        }
    }
}
