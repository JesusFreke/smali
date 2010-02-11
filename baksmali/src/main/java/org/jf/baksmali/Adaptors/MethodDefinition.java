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
import org.jf.dexlib.Code.Analysis.AnalyzedInstruction;
import org.jf.dexlib.Code.Analysis.MethodAnalyzer;
import org.jf.dexlib.Code.Analysis.RegisterType;
import org.jf.dexlib.Code.Analysis.ValidationException;
import org.jf.dexlib.Debug.DebugInstructionIterator;
import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.Opcode;
import org.jf.dexlib.Code.OffsetInstruction;
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
        if (baksmali.verboseRegisterInfo) {
            instructions = methodAnalyzer.analyze();

            ValidationException validationException = methodAnalyzer.getValidationException();
            if (validationException != null) {
                methodItems.add(new CommentMethodItem(stg, validationException.getMessage(),
                        validationException.getCodeAddress(), Integer.MIN_VALUE));
            }
        } else {
            instructions = methodAnalyzer.makeInstructionArray();
        }

        int currentCodeAddress = 0;
        for (int i=0; i<instructions.length; i++) {
            AnalyzedInstruction instruction = instructions[i];

            methodItems.add(InstructionMethodItemFactory.makeInstructionFormatMethodItem(this,
                    encodedMethod.codeItem, currentCodeAddress, stg, instruction.instruction));

            if (i != instructions.length - 1) {
                methodItems.add(new BlankMethodItem(stg, currentCodeAddress));
            }

            if (baksmali.verboseRegisterInfo) {
                if (instruction.getPredecessorCount() > 1 || i == 0) {
                    methodItems.add(new CommentMethodItem(stg, getPreInstructionRegisterString(instruction),
                            currentCodeAddress, Integer.MIN_VALUE));
                }
                methodItems.add(new CommentMethodItem(stg, getPostInstructionRegisterString(instruction),
                        currentCodeAddress, Integer.MAX_VALUE-1));
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

    private String getPreInstructionRegisterString(AnalyzedInstruction instruction) {
        StringBuilder sb = new StringBuilder();

        for (int i=0; i<instruction.getRegisterCount(); i++) {
            RegisterType registerType = instruction.getPreInstructionRegisterType(i);
            sb.append("v");
            sb.append(i);
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

    private String getPostInstructionRegisterString(AnalyzedInstruction instruction) {
        StringBuilder sb = new StringBuilder();

        for (int i=0; i<instruction.getRegisterCount(); i++) {
            RegisterType registerType = instruction.getPostInstructionRegisterType(i);
            sb.append("v");
            sb.append(i);
            sb.append("=");
            sb.append(registerType.toString());
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
