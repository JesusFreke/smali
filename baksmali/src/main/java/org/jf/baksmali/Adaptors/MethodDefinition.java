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
import org.jf.baksmali.main;
import org.jf.dexlib.*;
import org.jf.dexlib.Code.*;
import org.jf.dexlib.Code.Analysis.AnalyzedInstruction;
import org.jf.dexlib.Code.Analysis.MethodAnalyzer;
import org.jf.dexlib.Code.Analysis.RegisterType;
import org.jf.dexlib.Code.Analysis.ValidationException;
import org.jf.dexlib.Debug.DebugInstructionIterator;
import org.jf.dexlib.Util.AccessFlags;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.antlr.stringtemplate.StringTemplate;
import org.jf.dexlib.Util.SparseIntArray;

import java.util.*;

public class MethodDefinition {
    private final StringTemplateGroup stg;
    private final ClassDataItem.EncodedMethod encodedMethod;
    private final MethodAnalyzer methodAnalyzer;

    private final LabelCache labelCache = new LabelCache();

    private final SparseIntArray packedSwitchMap;
    private final SparseIntArray sparseSwitchMap;
    private final SparseIntArray instructionMap;

    private final int registerCount;

    public MethodDefinition(StringTemplateGroup stg, ClassDataItem.EncodedMethod encodedMethod) {
        this.stg = stg;
        this.encodedMethod = encodedMethod;

        //TODO: what about try/catch blocks inside the dead code? those will need to be commented out too. ugh.

        if (encodedMethod.codeItem != null) {
            methodAnalyzer = new MethodAnalyzer(encodedMethod);
            AnalyzedInstruction[] instructions = methodAnalyzer.makeInstructionArray();

            packedSwitchMap = new SparseIntArray(1);
            sparseSwitchMap = new SparseIntArray(1);
            instructionMap = new SparseIntArray(instructions.length);

            registerCount = encodedMethod.codeItem.getRegisterCount();

            int currentCodeAddress = 0;
            for (int i=0; i<instructions.length; i++) {
                AnalyzedInstruction instruction = instructions[i];
                if (instruction.instruction.opcode == Opcode.PACKED_SWITCH) {
                    packedSwitchMap.append(
                            currentCodeAddress + ((OffsetInstruction)instruction.instruction).getTargetAddressOffset(),
                            currentCodeAddress);
                } else if (instruction.instruction.opcode == Opcode.SPARSE_SWITCH) {
                    sparseSwitchMap.append(
                            currentCodeAddress + ((OffsetInstruction)instruction.instruction).getTargetAddressOffset(),
                            currentCodeAddress);
                }
                instructionMap.append(currentCodeAddress, i);
                currentCodeAddress += instruction.instruction.getSize(currentCodeAddress);
            }
        } else {
            packedSwitchMap = null;
            sparseSwitchMap = null;
            instructionMap = null;
            methodAnalyzer = null;
            registerCount = 0;
        }
    }

    public StringTemplate createTemplate(AnnotationSetItem annotationSet,
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
        template.setAttribute("MethodItems", getMethodItems());

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

    public LabelCache getLabelCache() {
        return labelCache;
    }

    public ValidationException getValidationException() {
        if (methodAnalyzer == null) {
            return null;
        }

        return methodAnalyzer.getValidationException();
    }

    public int getPackedSwitchBaseAddress(int packedSwitchDataAddress) {
        int packedSwitchBaseAddress = this.packedSwitchMap.get(packedSwitchDataAddress, -1);

        if (packedSwitchBaseAddress == -1) {
            throw new RuntimeException("Could not find the packed switch statement corresponding to the packed " +
                    "switch data at address " + packedSwitchDataAddress);
        }

        return packedSwitchBaseAddress;
    }

    public int getSparseSwitchBaseAddress(int sparseSwitchDataAddress) {
        int sparseSwitchBaseAddress = this.sparseSwitchMap.get(sparseSwitchDataAddress, -1);

        if (sparseSwitchBaseAddress == -1) {
            throw new RuntimeException("Could not find the sparse switch statement corresponding to the sparse " +
                    "switch data at address " + sparseSwitchDataAddress);
        }

        return sparseSwitchBaseAddress;
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

    private List<MethodItem> getMethodItems() {
        List<MethodItem> methodItems = new ArrayList<MethodItem>();

        if (encodedMethod.codeItem == null) {
            return methodItems;
        }

        AnalyzedInstruction[] instructions;
        if (baksmali.registerInfo != 0) {
            instructions = methodAnalyzer.analyze();

            ValidationException validationException = methodAnalyzer.getValidationException();
            if (validationException != null) {
                methodItems.add(new CommentMethodItem(stg,
                        String.format("ValidationException: %s" ,validationException.getMessage()),
                        validationException.getCodeAddress(), Integer.MIN_VALUE));
            }
        } else {
            instructions = methodAnalyzer.makeInstructionArray();
        }

        BitSet printPreRegister = new BitSet(registerCount);
        BitSet printPostRegister = new BitSet(registerCount);

        int currentCodeAddress = 0;
        for (int i=0; i<instructions.length; i++) {
            AnalyzedInstruction instruction = instructions[i];

            methodItems.add(InstructionMethodItemFactory.makeInstructionFormatMethodItem(this,
                    encodedMethod.codeItem, currentCodeAddress, stg, instruction.instruction));

            if (i != instructions.length - 1) {
                methodItems.add(new BlankMethodItem(stg, currentCodeAddress));
            }

            if (baksmali.registerInfo != 0 && !instruction.instruction.getFormat().variableSizeFormat) {
                printPreRegister.clear();
                printPostRegister.clear();

                if ((baksmali.registerInfo & main.ALL) != 0) {
                    printPreRegister.set(0, registerCount);
                    printPostRegister.set(0, registerCount);
                } else {
                    if ((baksmali.registerInfo & main.ALLPRE) != 0) {
                        printPreRegister.set(0, registerCount);
                    } else {
                        if ((baksmali.registerInfo & main.ARGS) != 0) {
                            addArgsRegs(printPreRegister, instruction);
                        }
                        if ((baksmali.registerInfo & main.MERGE) != 0) {
                            addMergeRegs(printPreRegister, instructions, i);
                        } else if ((baksmali.registerInfo & main.FULLMERGE) != 0 &&
                                (i == 0 || instruction.isBeginningInstruction())) {
                            addParamRegs(printPreRegister);
                        }
                    }

                    if ((baksmali.registerInfo & main.ALLPOST) != 0) {
                        printPostRegister.set(0, registerCount);
                    } else if ((baksmali.registerInfo & main.DEST) != 0) {
                        addDestRegs(printPostRegister, instruction);
                    }
                }

                if ((baksmali.registerInfo & main.FULLMERGE) != 0) {
                    addFullMergeRegs(printPreRegister, methodItems, methodAnalyzer, i, currentCodeAddress);
                }

                if (!printPreRegister.isEmpty()) {
                    methodItems.add(new CommentMethodItem(stg,
                            getPreInstructionRegisterString(instruction, printPreRegister),
                            currentCodeAddress, 99.9));
                }

                if (!printPostRegister.isEmpty()) {
                    methodItems.add(new CommentMethodItem(stg,
                            getPostInstructionRegisterString(instruction, printPostRegister),
                            currentCodeAddress, 100.1));
                }
            }

            currentCodeAddress += instruction.instruction.getSize(currentCodeAddress);
        }

        addTries(methodItems);
        addDebugInfo(methodItems);

        if (baksmali.useSequentialLabels) {
            setLabelSequentialNumbers();
        }


        for (LabelMethodItem labelMethodItem: labelCache.getLabels()) {
            if (labelMethodItem.isCommentedOut()) {
                methodItems.add(new CommentedOutMethodItem(stg, labelMethodItem));
            } else {
                methodItems.add(labelMethodItem);
            }
        }

        Collections.sort(methodItems);

        return methodItems;
    }

    private void addArgsRegs(BitSet printPreRegister, AnalyzedInstruction analyzedInstruction) {
        if (analyzedInstruction.instruction instanceof RegisterRangeInstruction) {
            RegisterRangeInstruction instruction = (RegisterRangeInstruction)analyzedInstruction.instruction;

            printPreRegister.set(instruction.getStartRegister(),
                    instruction.getStartRegister() + instruction.getRegCount());
        } else if (analyzedInstruction.instruction instanceof FiveRegisterInstruction) {
            FiveRegisterInstruction instruction = (FiveRegisterInstruction)analyzedInstruction.instruction;
            int regCount = instruction.getRegCount();
            switch (regCount) {
                case 5:
                    printPreRegister.set(instruction.getRegisterA());
                    //fall through
                case 4:
                    printPreRegister.set(instruction.getRegisterG());
                    //fall through
                case 3:
                    printPreRegister.set(instruction.getRegisterF());
                    //fall through
                case 2:
                    printPreRegister.set(instruction.getRegisterE());
                    //fall through
                case 1:
                    printPreRegister.set(instruction.getRegisterD());
            }
        } else if (analyzedInstruction.instruction instanceof ThreeRegisterInstruction) {
            ThreeRegisterInstruction instruction = (ThreeRegisterInstruction)analyzedInstruction.instruction;
            printPreRegister.set(instruction.getRegisterA());
            printPreRegister.set(instruction.getRegisterB());
            printPreRegister.set(instruction.getRegisterC());
        } else if (analyzedInstruction.instruction instanceof TwoRegisterInstruction) {
            TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;
            printPreRegister.set(instruction.getRegisterA());
            printPreRegister.set(instruction.getRegisterB());
        } else if (analyzedInstruction.instruction instanceof SingleRegisterInstruction) {
            SingleRegisterInstruction instruction = (SingleRegisterInstruction)analyzedInstruction.instruction;
            printPreRegister.set(instruction.getRegisterA());
        }
    }

    private void addFullMergeRegs(BitSet printPreRegister, List<MethodItem> methodItems, MethodAnalyzer methodAnalyzer,
                                  int instructionNum, int currentCodeAddress) {
        if (instructionNum == 0) {
            return;
        }

        //MethodAnalyzer cached the analyzedInstructions, so we're not actually re-analyzing, just getting the
        //instructions that have already been analyzed
        AnalyzedInstruction[] instructions = methodAnalyzer.analyze();
        AnalyzedInstruction instruction = instructions[instructionNum];

        if (instruction.getPredecessorCount() <= 1) {
            return;
        }

        StringBuffer sb = new StringBuffer();

        for (int registerNum=0; registerNum<registerCount; registerNum++) {
            sb.setLength(0);
            sb.append(RegisterFormatter.formatRegister(encodedMethod.codeItem, registerNum));
            sb.append('=');
            sb.append(instruction.getPreInstructionRegisterType(registerNum));
            sb.append(":merge{");

            RegisterType mergedRegisterType = instruction.getPreInstructionRegisterType(registerNum);
            boolean addRegister = false;

            boolean first = true;
            for (AnalyzedInstruction predecessor: instruction.getPredecessors()) {
                RegisterType predecessorRegisterType = predecessor.getPostInstructionRegisterType(registerNum);

                if (!first) {
                    sb.append(',');
                    if (!addRegister && mergedRegisterType != predecessorRegisterType) {
                        addRegister = true;
                    }
                }

                if (predecessor.getInstructionIndex() == -1) {
                    //the fake "StartOfMethod" instruction
                    sb.append("Start:");
                } else {
                    sb.append("0x");
                    sb.append(Integer.toHexString(methodAnalyzer.getInstructionAddress(predecessor)));
                    sb.append(':');
                }
                sb.append(predecessor.getPostInstructionRegisterType(registerNum).toString());
                first = false;
            }

            if (!addRegister) {
                continue;
            }

            sb.append("}");

            methodItems.add(new CommentMethodItem(stg, sb.toString(),  currentCodeAddress, 99.8));
            printPreRegister.clear(registerNum);
        }
    }

    private void addMergeRegs(BitSet printPreRegister, AnalyzedInstruction instructions[], int instructionNum) {
        //MethodAnalyzer cached the analyzedInstructions, so we're not actually re-analyzing, just getting the
        //instructions that have already been analyzed
        AnalyzedInstruction instruction = instructions[instructionNum];

        if (instructionNum == 0) {
            addParamRegs(printPreRegister);
        } else {
            if (instruction.isBeginningInstruction()) {
                addParamRegs(printPreRegister);
            }

            if (instruction.getPredecessorCount() <= 1) {
                //in the common case of an instruction that only has a single predecessor which is the previous
                //instruction, the pre-instruction registers will always match the previous instruction's
                //post-instruction registers
                return;
            }

            for (int registerNum=0; registerNum<registerCount; registerNum++) {
                RegisterType mergedRegisterType = instruction.getPreInstructionRegisterType(registerNum);

                for (AnalyzedInstruction predecessor: instruction.getPredecessors()) {
                    if (predecessor.getPostInstructionRegisterType(registerNum) != mergedRegisterType) {
                        printPreRegister.set(registerNum);
                        continue;
                    }
                }
            }
        }
    }

    private void addParamRegs(BitSet printPreRegister) {
        int registerCount = encodedMethod.codeItem.getRegisterCount();

        int parameterRegisterCount = encodedMethod.method.getPrototype().getParameterRegisterCount();
        if ((encodedMethod.accessFlags & AccessFlags.STATIC.getValue()) == 0) {
            parameterRegisterCount++;
        }

        printPreRegister.set(registerCount-parameterRegisterCount, registerCount);
    }

    private void addDestRegs(BitSet printPostRegister, AnalyzedInstruction analyzedInstruction) {
        for (int registerNum=0; registerNum<registerCount; registerNum++) {
            if (analyzedInstruction.getPreInstructionRegisterType(registerNum) !=
                    analyzedInstruction.getPostInstructionRegisterType(registerNum)) {
                printPostRegister.set(registerNum);
            }
        }
    }


    private String getPreInstructionRegisterString(AnalyzedInstruction instruction, BitSet registers) {
        StringBuilder sb = new StringBuilder();

        for (int registerNum = registers.nextSetBit(0); registerNum >= 0;
             registerNum = registers.nextSetBit(registerNum + 1)) {

            RegisterType registerType = instruction.getPreInstructionRegisterType(registerNum);
            sb.append(RegisterFormatter.formatRegister(encodedMethod.codeItem,registerNum));
            sb.append("=");
            if (registerType == null) {
                sb.append("null");
            } else {
                sb.append(registerType.toString());
            }
            sb.append(";");
        }

        return sb.toString();
    }

    private String getPostInstructionRegisterString(AnalyzedInstruction instruction, BitSet registers) {
        StringBuilder sb = new StringBuilder();

        for (int registerNum = registers.nextSetBit(0); registerNum >= 0;
             registerNum = registers.nextSetBit(registerNum + 1)) {

            RegisterType registerType = instruction.getPostInstructionRegisterType(registerNum);
            sb.append(RegisterFormatter.formatRegister(encodedMethod.codeItem,registerNum));
            sb.append("=");
            if (registerType == null) {
                sb.append("null");
            } else {
                sb.append(registerType.toString());
            }
            sb.append(";");
        }

        return sb.toString();
    }

    private void addTries(List<MethodItem> methodItems) {
        if (encodedMethod.codeItem == null || encodedMethod.codeItem.getTries() == null) {
            return;
        }

        Instruction[] instructions = encodedMethod.codeItem.getInstructions();

        for (CodeItem.TryItem tryItem: encodedMethod.codeItem.getTries()) {
            int startAddress = tryItem.getStartCodeAddress();
            int endAddress = tryItem.getStartCodeAddress() + tryItem.getTryLength();

            /**
             * The end address points to the address immediately after the end of the last
             * instruction that the try block covers. We want the .catch directive and end_try
             * label to be associated with the last covered instruction, so we need to get
             * the address for that instruction
             */

            int index = instructionMap.get(endAddress, -1);
            int lastInstructionAddress;

            /**
             * If we couldn't find the index, then the try block probably extends to the last instruction in the
             * method, and so endAddress would be the address immediately after the end of the last instruction.
             * Check to make sure this is the case, if not, throw an exception.
             */
            if (index == -1) {
                Instruction lastInstruction = instructions[instructions.length - 1];
                lastInstructionAddress = instructionMap.keyAt(instructionMap.size() - 1);

                if (endAddress != lastInstructionAddress + lastInstruction.getSize(lastInstructionAddress)) {
                    throw new RuntimeException("Invalid code offset " + endAddress + " for the try block end address");
                }
            } else {
                if (index == 0) {
                    throw new RuntimeException("Unexpected instruction index");
                }
                Instruction lastInstruction = instructions[index - 1];

                if (lastInstruction.getFormat().variableSizeFormat) {
                    throw new RuntimeException("This try block unexpectedly ends on a switch/array data block.");
                }

                //getSize for non-variable size formats should return the same size regardless of code address, so just
                //use a dummy address of "0"
                lastInstructionAddress = endAddress - lastInstruction.getSize(0);
            }

            //add the catch all handler if it exists
            int catchAllAddress = tryItem.encodedCatchHandler.getCatchAllHandlerAddress();
            if (catchAllAddress != -1) {
                CatchMethodItem catchAllMethodItem = new CatchMethodItem(labelCache, lastInstructionAddress, stg, null,
                        startAddress, endAddress, catchAllAddress);
                methodItems.add(catchAllMethodItem);
            }

            //add the rest of the handlers
            for (CodeItem.EncodedTypeAddrPair handler: tryItem.encodedCatchHandler.handlers) {
                //use the address from the last covered instruction
                CatchMethodItem catchMethodItem = new CatchMethodItem(labelCache, lastInstructionAddress, stg,
                        handler.exceptionType, startAddress, endAddress, handler.getHandlerAddress());
                methodItems.add(catchMethodItem);
            }
        }
    }

    private void addDebugInfo(final List<MethodItem> methodItems) {
        if (encodedMethod.codeItem == null || encodedMethod.codeItem.getDebugInfo() == null) {
            return;
        }

        final CodeItem codeItem = encodedMethod.codeItem;
        DebugInfoItem debugInfoItem = codeItem.getDebugInfo();

        DebugInstructionIterator.DecodeInstructions(debugInfoItem, codeItem.getRegisterCount(),
                new DebugInstructionIterator.ProcessDecodedDebugInstructionDelegate() {
                    @Override
                    public void ProcessStartLocal(int codeAddress, int length, int registerNum, StringIdItem name,
                                                  TypeIdItem type) {
                        methodItems.add(new LocalDebugMethodItem(codeItem, codeAddress, stg, "StartLocal",
                                -1, registerNum, name, type, null));
                    }

                    @Override
                    public void ProcessStartLocalExtended(int codeAddress, int length, int registerNum,
                                                          StringIdItem name, TypeIdItem type,
                                                          StringIdItem signature) {
                        methodItems.add(new LocalDebugMethodItem(codeItem, codeAddress, stg, "StartLocal",
                                -1, registerNum, name, type, signature));
                    }

                    @Override
                    public void ProcessEndLocal(int codeAddress, int length, int registerNum, StringIdItem name,
                                                TypeIdItem type, StringIdItem signature) {
                        methodItems.add(new LocalDebugMethodItem(codeItem, codeAddress, stg, "EndLocal", -1,
                                registerNum, name, type, signature));
                    }

                    @Override
                    public void ProcessRestartLocal(int codeAddress, int length, int registerNum, StringIdItem name,
                                                    TypeIdItem type, StringIdItem signature) {
                        methodItems.add(new LocalDebugMethodItem(codeItem, codeAddress, stg, "RestartLocal", -1,
                                registerNum, name, type, signature));
                    }

                    @Override
                    public void ProcessSetPrologueEnd(int codeAddress) {
                        methodItems.add(new DebugMethodItem(codeAddress, stg, "EndPrologue", -4));
                    }

                    @Override
                    public void ProcessSetEpilogueBegin(int codeAddress) {
                        methodItems.add(new DebugMethodItem(codeAddress, stg, "StartEpilogue", -4));
                    }

                    @Override
                    public void ProcessSetFile(int codeAddress, int length, final StringIdItem name) {
                        methodItems.add(new DebugMethodItem(codeAddress, stg, "SetFile", -3) {
                            @Override
                            protected void setAttributes(StringTemplate template) {
                                template.setAttribute("FileName", name.getStringValue());
                            }
                        });
                    }

                    @Override
                    public void ProcessLineEmit(int codeAddress, final int line) {
                         methodItems.add(new DebugMethodItem(codeAddress, stg, "Line", -2) {
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
        ArrayList<LabelMethodItem> sortedLabels = new ArrayList<LabelMethodItem>(labelCache.getLabels());

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
