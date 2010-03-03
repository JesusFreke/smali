/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver (JesusFreke)
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

package org.jf.dexlib.Code.Analysis;

import org.jf.dexlib.*;
import org.jf.dexlib.Code.*;
import org.jf.dexlib.Code.Format.*;
import org.jf.dexlib.Util.*;


import java.util.*;

public class MethodAnalyzer {
    private final ClassDataItem.EncodedMethod encodedMethod;

    private final DeodexUtil deodexUtil;

    private SparseArray<AnalyzedInstruction> instructions;

    private static final int NOT_ANALYZED = 0;
    private static final int ANALYZED = 1;
    private static final int VERIFIED = 2;
    private int analyzerState = NOT_ANALYZED;

    private BitSet verifiedInstructions;

    private ValidationException validationException = null;

    //This is a dummy instruction that occurs immediately before the first real instruction. We can initialize the
    //register types for this instruction to the parameter types, in order to have them propagate to all of its
    //successors, e.g. the first real instruction, the first instructions in any exception handlers covering the first
    //instruction, etc.
    private AnalyzedInstruction startOfMethod;

    public MethodAnalyzer(ClassDataItem.EncodedMethod encodedMethod, boolean deodex) {
        if (encodedMethod == null) {
            throw new IllegalArgumentException("encodedMethod cannot be null");
        }
        if (encodedMethod.codeItem == null || encodedMethod.codeItem.getInstructions().length == 0) {
            throw new IllegalArgumentException("The method has no code");
        }
        this.encodedMethod = encodedMethod;

        if (deodex) {
            this.deodexUtil = new DeodexUtil(encodedMethod.method.getDexFile());
        } else {
            this.deodexUtil = null;
        }

        //override AnalyzedInstruction and provide custom implementations of some of the methods, so that we don't
        //have to handle the case this special case of instruction being null, in the main class
        startOfMethod = new AnalyzedInstruction(null, -1, encodedMethod.codeItem.getRegisterCount()) {
            public boolean setsRegister() {
                return false;
            }

            @Override
            public boolean setsWideRegister() {
                return false;
            }

            @Override
            public boolean setsRegister(int registerNumber) {
                return false;
            }

            @Override
            public int getDestinationRegister() {
                assert false;
                return -1;
            };
        };

        buildInstructionList();

        verifiedInstructions = new BitSet(instructions.size());
    }

    public boolean isAnalyzed() {
        return analyzerState >= ANALYZED;
    }

    public boolean isVerified() {
        return analyzerState == VERIFIED;
    }

    public AnalyzedInstruction[] analyze() {
        assert encodedMethod != null;
        assert encodedMethod.codeItem != null;

        if (analyzerState >= ANALYZED) {
            return makeInstructionArray();
        }

        CodeItem codeItem = encodedMethod.codeItem;
        MethodIdItem methodIdItem = encodedMethod.method;

        int totalRegisters = codeItem.getRegisterCount();
        int parameterRegisters = methodIdItem.getPrototype().getParameterRegisterCount();

        int nonParameterRegisters = totalRegisters - parameterRegisters;

        //if this isn't a static method, determine which register is the "this" register and set the type to the
        //current class
        if ((encodedMethod.accessFlags & AccessFlags.STATIC.getValue()) == 0) {
            nonParameterRegisters--;
            int thisRegister = totalRegisters - parameterRegisters - 1;

            //if this is a constructor, then set the "this" register to an uninitialized reference of the current class
            if ((encodedMethod.accessFlags & AccessFlags.CONSTRUCTOR.getValue()) != 0) {
                //TODO: it would probably make more sense to validate this somewhere else, and just put an assert here. Also, need to do a similar check for static constructor
                if (!encodedMethod.method.getMethodName().getStringValue().equals("<init>")) {
                    throw new ValidationException("The constructor flag can only be used with an <init> method.");
                }

                setPostRegisterTypeAndPropagateChanges(startOfMethod, thisRegister,
                        RegisterType.getRegisterType(RegisterType.Category.UninitThis,
                            ClassPath.getClassDef(methodIdItem.getContainingClass())));
            } else {
                if (encodedMethod.method.getMethodName().getStringValue().equals("<init>")) {
                    throw new ValidationException("An <init> method must have the \"constructor\" access flag");
                }

                setPostRegisterTypeAndPropagateChanges(startOfMethod, thisRegister,
                        RegisterType.getRegisterType(RegisterType.Category.Reference,
                            ClassPath.getClassDef(methodIdItem.getContainingClass())));
            }
        }

        TypeListItem parameters = methodIdItem.getPrototype().getParameters();
        if (parameters != null) {
            RegisterType[] parameterTypes = getParameterTypes(parameters, parameterRegisters);
            for (int i=0; i<parameterTypes.length; i++) {
                RegisterType registerType = parameterTypes[i];
                int registerNum = (totalRegisters - parameterRegisters) + i;
                setPostRegisterTypeAndPropagateChanges(startOfMethod, registerNum, registerType);
            }
        }

        RegisterType uninit = RegisterType.getRegisterType(RegisterType.Category.Uninit, null);
        for (int i=0; i<nonParameterRegisters; i++) {
            setPostRegisterTypeAndPropagateChanges(startOfMethod, i, uninit);
        }

        BitSet instructionsToAnalyze = new BitSet(verifiedInstructions.size());

        //make sure all of the "first instructions" are marked for processing
        for (AnalyzedInstruction successor: startOfMethod.successors) {
            instructionsToAnalyze.set(successor.instructionIndex);
        }

        BitSet odexedInstructions = new BitSet(verifiedInstructions.size());

        do {
            boolean didSomething = false;

            while (!instructionsToAnalyze.isEmpty()) {
                for(int i=instructionsToAnalyze.nextSetBit(0); i>=0; i=instructionsToAnalyze.nextSetBit(i+1)) {
                    instructionsToAnalyze.clear(i);
                    if (verifiedInstructions.get(i)) {
                        continue;
                    }
                    AnalyzedInstruction instructionToVerify = instructions.valueAt(i);
                    try {
                        if (instructionToVerify.originalInstruction.opcode.odexOnly()) {
                            instructionToVerify.restoreOdexedInstruction();
                        }

                        if (!analyzeInstruction(instructionToVerify)) {
                            odexedInstructions.set(i);
                            continue;
                        } else {
                            didSomething = true;
                            odexedInstructions.clear(i);
                        }
                    } catch (ValidationException ex) {
                        this.validationException = ex;
                        int codeAddress = getInstructionAddress(instructionToVerify);
                        ex.setCodeAddress(codeAddress);
                        ex.addContext(String.format("opcode: %s", instructionToVerify.instruction.opcode.name));
                        ex.addContext(String.format("CodeAddress: %d", codeAddress));
                        ex.addContext(String.format("Method: %s", encodedMethod.method.getMethodString()));
                        break;
                    }

                    verifiedInstructions.set(instructionToVerify.getInstructionIndex());

                    for (AnalyzedInstruction successor: instructionToVerify.successors) {
                        instructionsToAnalyze.set(successor.getInstructionIndex());
                    }
                }
                if (validationException != null) {
                    break;
                }
            }

            if (!didSomething) {
                break;
            }

            if (!odexedInstructions.isEmpty()) {
                for (int i=odexedInstructions.nextSetBit(0); i>=0; i=odexedInstructions.nextSetBit(i+1)) {
                    instructionsToAnalyze.set(i);
                }
            }
        } while (true);

        for (int i=0; i<instructions.size(); i++) {
            AnalyzedInstruction instruction = instructions.valueAt(i);

            if (!odexedInstructions.get(i)) {

                //TODO: We probably need to re-verify everything after changing unknown-uninit. Better yet, maybe we should split the register propagation logic and the verification logic, and only do the verification after all the register info is known

                //We don't want to change unknown register types to uninit for unreachable instructions, as the register
                //types for the unreachable instruction shouldn't be taken into account when merging registers for any
                //reachable predecessor (for example, the predecessor of an unreachable goto instruction)
                //So we keep the unreachable register types as unknown, because anything else + unknown = anything else
                if (verifiedInstructions.get(i)) {
                    for (int j=0; j<instruction.postRegisterMap.length; j++) {
                        if (instruction.postRegisterMap[j].category == RegisterType.Category.Unknown) {
                            instruction.postRegisterMap[j] = uninit;
                        }
                    }
                }
            } else {
                Instruction odexedInstruction = instruction.instruction;
                int objectRegisterNumber;

                if (odexedInstruction.getFormat() == Format.Format22cs) {
                    objectRegisterNumber = ((Instruction22cs)odexedInstruction).getRegisterB();
                } else if (odexedInstruction.getFormat() == Format.Format35ms) {
                    objectRegisterNumber = ((Instruction35ms)odexedInstruction).getRegisterD();
                } else if (odexedInstruction.getFormat() == Format.Format3rms) {
                    objectRegisterNumber = ((Instruction3rms)odexedInstruction).getStartRegister();
                } else {
                    assert false;
                    throw new ExceptionWithContext(String.format("Unexpected format %s for odexed instruction",
                            odexedInstruction.getFormat().name()));
                }

                instruction.setDeodexedInstruction(new UnresolvedNullReference(odexedInstruction,
                        objectRegisterNumber));

                setAndPropagateDeadness(instruction);
            }
        }

        analyzerState = ANALYZED;
        return makeInstructionArray();
    }

    private int getThisRegister() {
        assert (encodedMethod.accessFlags & AccessFlags.STATIC.getValue()) == 0;

        CodeItem codeItem = encodedMethod.codeItem;
        assert codeItem != null;

        MethodIdItem methodIdItem = encodedMethod.method;
        assert methodIdItem != null;

        int totalRegisters = codeItem.getRegisterCount();
        if (totalRegisters == 0) {
            throw new ValidationException("A non-static method must have at least 1 register");
        }

        int parameterRegisters = methodIdItem.getPrototype().getParameterRegisterCount();

        return totalRegisters - parameterRegisters - 1;
    }

    private boolean isInstanceConstructor() {
        return (encodedMethod.accessFlags & AccessFlags.STATIC.getValue()) == 0 &&
               (encodedMethod.accessFlags & AccessFlags.CONSTRUCTOR.getValue()) != 0;
    }

    private boolean isStaticConstructor() {
        return (encodedMethod.accessFlags & AccessFlags.STATIC.getValue()) != 0 &&
               (encodedMethod.accessFlags & AccessFlags.CONSTRUCTOR.getValue()) != 0;
    }

    public AnalyzedInstruction getStartOfMethod() {
        return startOfMethod;
    }

    public AnalyzedInstruction[] makeInstructionArray() {
        AnalyzedInstruction[] instructionArray = new AnalyzedInstruction[instructions.size()];
        for (int i=0; i<instructions.size(); i++) {
            instructionArray[i] = instructions.valueAt(i);
        }
        return instructionArray;
    }

    public ValidationException getValidationException() {
        return validationException;
    }

    private static RegisterType[] getParameterTypes(TypeListItem typeListItem, int parameterRegisterCount) {
        assert typeListItem != null;
        assert parameterRegisterCount == typeListItem.getRegisterCount();

        RegisterType[] registerTypes = new RegisterType[parameterRegisterCount];

        int registerNum = 0;
        for (TypeIdItem type: typeListItem.getTypes()) {
            if (type.getRegisterCount() == 2) {
                registerTypes[registerNum++] = RegisterType.getWideRegisterTypeForTypeIdItem(type, true);
                registerTypes[registerNum++] = RegisterType.getWideRegisterTypeForTypeIdItem(type, false);
            } else {
                registerTypes[registerNum++] = RegisterType.getRegisterTypeForTypeIdItem(type);
            }
        }

        return registerTypes;
    }

    public int getInstructionAddress(AnalyzedInstruction instruction) {
        return instructions.keyAt(instruction.instructionIndex);
    }

    private void setDestinationRegisterTypeAndPropagateChanges(AnalyzedInstruction analyzedInstruction,
                                                               RegisterType registerType) {
        setPostRegisterTypeAndPropagateChanges(analyzedInstruction, analyzedInstruction.getDestinationRegister(),
                registerType);
    }


    private void setAndPropagateDeadness(AnalyzedInstruction analyzedInstruction) {
        BitSet instructionsToProcess = new BitSet(instructions.size());

        //temporarily set the undeodexeble instruction as dead, so that the "set dead if all predecessors are dead"
        //operation works
        analyzedInstruction.dead = true;

        for (AnalyzedInstruction successor: analyzedInstruction.successors) {
            instructionsToProcess.set(successor.instructionIndex);
        }

        instructionsToProcess.set(analyzedInstruction.instructionIndex);

        while (!instructionsToProcess.isEmpty()) {
            for (int i=instructionsToProcess.nextSetBit(0); i>=0; i=instructionsToProcess.nextSetBit(i+1)) {
                AnalyzedInstruction currentInstruction = instructions.valueAt(i);
                instructionsToProcess.clear(i);

                if (currentInstruction.dead) {
                    continue;
                }

                boolean isDead = true;

                for (AnalyzedInstruction predecessor: currentInstruction.predecessors) {
                    if (!predecessor.dead) {
                        isDead = false;
                        break;
                    }
                }

                if (isDead) {
                    currentInstruction.dead = true;

                    for (AnalyzedInstruction successor: currentInstruction.successors) {
                        instructionsToProcess.set(successor.instructionIndex);
                    }
                }
            }
        }

        analyzedInstruction.dead = false;
    }

    private void setPostRegisterTypeAndPropagateChanges(AnalyzedInstruction analyzedInstruction, int registerNumber,
                                                RegisterType registerType) {

        BitSet changedInstructions = new BitSet(instructions.size());

        if (!analyzedInstruction.setPostRegisterType(registerNumber, registerType)) {
            return;
        }

        propagateRegisterToSuccessors(analyzedInstruction, registerNumber, changedInstructions);

        //Using a for loop inside the while loop optimizes for the common case of the successors of an instruction
        //occurring after the instruction. Any successors that occur prior to the instruction will be picked up on
        //the next iteration of the while loop.
        //This could also be done recursively, but in large methods it would likely cause very deep recursion,
        //which requires the user to specify a larger stack size. This isn't really a problem, but it is slightly
        //annoying.
        while (!changedInstructions.isEmpty()) {
            for (int instructionIndex=changedInstructions.nextSetBit(0);
                     instructionIndex>=0;
                     instructionIndex=changedInstructions.nextSetBit(instructionIndex+1)) {

                changedInstructions.clear(instructionIndex);

                propagateRegisterToSuccessors(instructions.valueAt(instructionIndex), registerNumber,
                        changedInstructions);
            }
        }

        if (registerType.category == RegisterType.Category.LongLo) {
            checkWidePair(registerNumber, analyzedInstruction);
            setPostRegisterTypeAndPropagateChanges(analyzedInstruction, registerNumber+1,
                    RegisterType.getRegisterType(RegisterType.Category.LongHi, null));
        } else if (registerType.category == RegisterType.Category.DoubleLo) {
            checkWidePair(registerNumber, analyzedInstruction);
            setPostRegisterTypeAndPropagateChanges(analyzedInstruction, registerNumber+1,
                    RegisterType.getRegisterType(RegisterType.Category.DoubleHi, null));
        }
    }

    private void propagateRegisterToSuccessors(AnalyzedInstruction instruction, int registerNumber,
                                               BitSet changedInstructions) {
        RegisterType postRegisterType = instruction.getPostInstructionRegisterType(registerNumber);
        for (AnalyzedInstruction successor: instruction.successors) {
            if (successor.mergeRegister(registerNumber, postRegisterType, verifiedInstructions)) {
                changedInstructions.set(successor.instructionIndex);
            }
        }
    }

    private void buildInstructionList() {
        assert encodedMethod != null;
        assert encodedMethod.codeItem != null;
        int registerCount = encodedMethod.codeItem.getRegisterCount();

        Instruction[] insns = encodedMethod.codeItem.getInstructions();

        instructions = new SparseArray<AnalyzedInstruction>(insns.length);

        //first, create all the instructions and populate the instructionAddresses array
        int currentCodeAddress = 0;
        for (int i=0; i<insns.length; i++) {
            instructions.append(currentCodeAddress, new AnalyzedInstruction(insns[i], i, registerCount));
            assert instructions.indexOfKey(currentCodeAddress) == i;
            currentCodeAddress += insns[i].getSize(currentCodeAddress);
        }

        //next, populate the exceptionHandlers array. The array item for each instruction that can throw an exception
        //and is covered by a try block should be set to a list of the first instructions of each exception handler
        //for the try block covering the instruction
        CodeItem.TryItem[] tries = encodedMethod.codeItem.getTries();
        int triesIndex = 0;
        CodeItem.TryItem currentTry = null;
        AnalyzedInstruction[] currentExceptionHandlers = null;
        AnalyzedInstruction[][] exceptionHandlers = new AnalyzedInstruction[insns.length][];

        if (tries != null) {
            for (int i=0; i<instructions.size(); i++) {
                AnalyzedInstruction instruction = instructions.valueAt(i);
                Opcode instructionOpcode = instruction.instruction.opcode;
                currentCodeAddress = getInstructionAddress(instruction);

                //check if we have gone past the end of the current try
                if (currentTry != null) {
                    if (currentTry.getStartCodeAddress() + currentTry.getTryLength() <= currentCodeAddress) {
                        currentTry = null;
                        triesIndex++;
                    }
                }

                //check if the next try is applicable yet
                if (currentTry == null && triesIndex < tries.length) {
                    CodeItem.TryItem tryItem = tries[triesIndex];
                    if (tryItem.getStartCodeAddress() <= currentCodeAddress) {
                        assert(tryItem.getStartCodeAddress() + tryItem.getTryLength() > currentCodeAddress);

                        currentTry = tryItem;

                        currentExceptionHandlers = buildExceptionHandlerArray(tryItem);
                    }
                }

                //if we're inside a try block, and the instruction can throw an exception, then add the exception handlers
                //for the current instruction
                if (currentTry != null && instructionOpcode.canThrow()) {
                    exceptionHandlers[i] = currentExceptionHandlers;
                }
            }
        }

        //finally, populate the successors and predecessors for each instruction. We start at the fake "StartOfMethod"
        //instruction and follow the execution path. Any unreachable code won't have any predecessors or successors,
        //and no reachable code will have an unreachable predessor or successor
        assert instructions.size() > 0;
        BitSet instructionsToProcess = new BitSet(insns.length);

        addPredecessorSuccessor(startOfMethod, instructions.valueAt(0), exceptionHandlers, instructionsToProcess);
        while (!instructionsToProcess.isEmpty()) {
            int currentInstructionIndex = instructionsToProcess.nextSetBit(0);
            instructionsToProcess.clear(currentInstructionIndex);

            AnalyzedInstruction instruction = instructions.valueAt(currentInstructionIndex);
            Opcode instructionOpcode = instruction.instruction.opcode;
            int instructionCodeAddress = getInstructionAddress(instruction);

            if (instruction.instruction.opcode.canContinue()) {
                if (instruction.instruction.opcode != Opcode.NOP ||
                    !instruction.instruction.getFormat().variableSizeFormat) {

                    if (currentInstructionIndex == instructions.size() - 1) {
                        throw new ValidationException("Execution can continue past the last instruction");
                    }

                    AnalyzedInstruction nextInstruction = instructions.valueAt(currentInstructionIndex+1);
                    addPredecessorSuccessor(instruction, nextInstruction, exceptionHandlers, instructionsToProcess);
                }
            }

            if (instruction.instruction instanceof OffsetInstruction) {
                OffsetInstruction offsetInstruction = (OffsetInstruction)instruction.instruction;

                if (instructionOpcode == Opcode.PACKED_SWITCH || instructionOpcode == Opcode.SPARSE_SWITCH) {
                    MultiOffsetInstruction switchDataInstruction =
                            (MultiOffsetInstruction)instructions.get(instructionCodeAddress +
                                    offsetInstruction.getTargetAddressOffset()).instruction;
                    for (int targetAddressOffset: switchDataInstruction.getTargets()) {
                        AnalyzedInstruction targetInstruction = instructions.get(instructionCodeAddress +
                                targetAddressOffset);

                        addPredecessorSuccessor(instruction, targetInstruction, exceptionHandlers,
                                instructionsToProcess);
                    }
                } else {
                    int targetAddressOffset = offsetInstruction.getTargetAddressOffset();
                    AnalyzedInstruction targetInstruction = instructions.get(instructionCodeAddress +
                            targetAddressOffset);
                    addPredecessorSuccessor(instruction, targetInstruction, exceptionHandlers, instructionsToProcess);
                }
            }
        }
    }

    private void addPredecessorSuccessor(AnalyzedInstruction predecessor, AnalyzedInstruction successor,
                                                AnalyzedInstruction[][] exceptionHandlers,
                                                BitSet instructionsToProcess) {
        addPredecessorSuccessor(predecessor, successor, exceptionHandlers, instructionsToProcess, false);
    }

    private void addPredecessorSuccessor(AnalyzedInstruction predecessor, AnalyzedInstruction successor,
                                                AnalyzedInstruction[][] exceptionHandlers,
                                                BitSet instructionsToProcess, boolean allowMoveException) {

        if (!allowMoveException && successor.instruction.opcode == Opcode.MOVE_EXCEPTION) {
            throw new ValidationException("Execution can pass from the " + predecessor.instruction.opcode.name +
                    " instruction at code address 0x" + Integer.toHexString(getInstructionAddress(predecessor)) +
                    " to the move-exception instruction at address 0x" +
                    Integer.toHexString(getInstructionAddress(successor)));
        }

        if (!successor.addPredecessor(predecessor)) {
            return;
        }

        predecessor.addSuccessor(successor);
        instructionsToProcess.set(successor.getInstructionIndex());


        //if the successor can throw an instruction, then we need to add the exception handlers as additional
        //successors to the predecessor (and then apply this same logic recursively if needed)
        //Technically, we should handle the monitor-exit instruction as a special case. The exception is actually
        //thrown *after* the instruction executes, instead of "before" the instruction executes, lke for any other
        //instruction. But since it doesn't modify any registers, we can treat it like any other instruction.
        AnalyzedInstruction[] exceptionHandlersForSuccessor = exceptionHandlers[successor.instructionIndex];
        if (exceptionHandlersForSuccessor != null) {
            //the item for this instruction in exceptionHandlersForSuccessor should only be set if this instruction
            //can throw an exception
            assert successor.instruction.opcode.canThrow();

            for (AnalyzedInstruction exceptionHandler: exceptionHandlersForSuccessor) {
                addPredecessorSuccessor(predecessor, exceptionHandler, exceptionHandlers, instructionsToProcess, true);
            }
        }
    }

    private AnalyzedInstruction[] buildExceptionHandlerArray(CodeItem.TryItem tryItem) {
        int exceptionHandlerCount = tryItem.encodedCatchHandler.handlers.length;
        int catchAllHandler = tryItem.encodedCatchHandler.getCatchAllHandlerAddress();
        if (catchAllHandler != -1) {
            exceptionHandlerCount++;
        }

        AnalyzedInstruction[] exceptionHandlers = new AnalyzedInstruction[exceptionHandlerCount];
        for (int i=0; i<tryItem.encodedCatchHandler.handlers.length; i++) {
            exceptionHandlers[i] = instructions.get(tryItem.encodedCatchHandler.handlers[i].getHandlerAddress());
        }

        if (catchAllHandler != -1) {
            exceptionHandlers[exceptionHandlers.length - 1] = instructions.get(catchAllHandler);
        }

        return exceptionHandlers;
    }

    private boolean analyzeInstruction(AnalyzedInstruction analyzedInstruction) {
        Instruction instruction = analyzedInstruction.instruction;

        switch (instruction.opcode) {
            case NOP:
                return true;
            case MOVE:
            case MOVE_FROM16:
            case MOVE_16:
                handleMove(analyzedInstruction, Primitive32BitCategories);
                return true;
            case MOVE_WIDE:
            case MOVE_WIDE_FROM16:
            case MOVE_WIDE_16:
                handleMove(analyzedInstruction, WideLowCategories);
                return true;
            case MOVE_OBJECT:
            case MOVE_OBJECT_FROM16:
            case MOVE_OBJECT_16:
                handleMove(analyzedInstruction, ReferenceOrUninitCategories);
                return true;
            case MOVE_RESULT:
                handleMoveResult(analyzedInstruction, Primitive32BitCategories);
                return true;
            case MOVE_RESULT_WIDE:
                handleMoveResult(analyzedInstruction, WideLowCategories);
                return true;
            case MOVE_RESULT_OBJECT:
                handleMoveResult(analyzedInstruction, ReferenceCategories);
                return true;
            case MOVE_EXCEPTION:
                handleMoveException(analyzedInstruction);
                return true;
            case RETURN_VOID:
                handleReturnVoid(analyzedInstruction);
                return true;
            case RETURN:
                handleReturn(analyzedInstruction, Primitive32BitCategories);
                return true;
            case RETURN_WIDE:
                handleReturn(analyzedInstruction, WideLowCategories);
                return true;
            case RETURN_OBJECT:
                handleReturn(analyzedInstruction, ReferenceCategories);
                return true;
            case CONST_4:
            case CONST_16:
            case CONST:
                handleConst(analyzedInstruction);
                return true;
            case CONST_HIGH16:
                handleConstHigh16(analyzedInstruction);
                return true;
            case CONST_WIDE_16:
            case CONST_WIDE_32:
            case CONST_WIDE:
            case CONST_WIDE_HIGH16:
                handleWideConst(analyzedInstruction);
                return true;
            case CONST_STRING:
            case CONST_STRING_JUMBO:
                handleConstString(analyzedInstruction);
                return true;
            case CONST_CLASS:
                handleConstClass(analyzedInstruction);
                return true;
            case MONITOR_ENTER:
            case MONITOR_EXIT:
                handleMonitor(analyzedInstruction);
                return true;
            case CHECK_CAST:
                handleCheckCast(analyzedInstruction);
                return true;
            case INSTANCE_OF:
                handleInstanceOf(analyzedInstruction);
                return true;
            case ARRAY_LENGTH:
                handleArrayLength(analyzedInstruction);
                return true;
            case NEW_INSTANCE:
                handleNewInstance(analyzedInstruction);
                return true;
            case NEW_ARRAY:
                handleNewArray(analyzedInstruction);
                return true;
            case FILLED_NEW_ARRAY:
                handleFilledNewArray(analyzedInstruction);
                return true;
            case FILLED_NEW_ARRAY_RANGE:
                handleFilledNewArrayRange(analyzedInstruction);
                return true;
            case FILL_ARRAY_DATA:
                handleFillArrayData(analyzedInstruction);
                return true;
            case THROW:
                handleThrow(analyzedInstruction);
                return true;
            case GOTO:
            case GOTO_16:
            case GOTO_32:
                //nothing to do
                return true;
            case PACKED_SWITCH:
                handleSwitch(analyzedInstruction, Format.PackedSwitchData);
                return true;
            case SPARSE_SWITCH:
                handleSwitch(analyzedInstruction, Format.SparseSwitchData);
                return true;
            case CMPL_FLOAT:
            case CMPG_FLOAT:
                handleFloatWideCmp(analyzedInstruction, Primitive32BitCategories);
                return true;
            case CMPL_DOUBLE:
            case CMPG_DOUBLE:
            case CMP_LONG:
                handleFloatWideCmp(analyzedInstruction, WideLowCategories);
                return true;
            case IF_EQ:
            case IF_NE:
                handleIfEqNe(analyzedInstruction);
                return true;
            case IF_LT:
            case IF_GE:
            case IF_GT:
            case IF_LE:
                handleIf(analyzedInstruction);
                return true;
            case IF_EQZ:
            case IF_NEZ:
                handleIfEqzNez(analyzedInstruction);
                return true;
            case IF_LTZ:
            case IF_GEZ:
            case IF_GTZ:
            case IF_LEZ:
                handleIfz(analyzedInstruction);
                return true;
            case AGET:
                handle32BitPrimitiveAget(analyzedInstruction, RegisterType.Category.Integer);
                return true;
            case AGET_BOOLEAN:
                handle32BitPrimitiveAget(analyzedInstruction, RegisterType.Category.Boolean);
                return true;
            case AGET_BYTE:
                handle32BitPrimitiveAget(analyzedInstruction, RegisterType.Category.Byte);
                return true;
            case AGET_CHAR:
                handle32BitPrimitiveAget(analyzedInstruction, RegisterType.Category.Char);
                return true;
            case AGET_SHORT:
                handle32BitPrimitiveAget(analyzedInstruction, RegisterType.Category.Short);
                return true;
            case AGET_WIDE:
                handleAgetWide(analyzedInstruction);
                return true;
            case AGET_OBJECT:
                handleAgetObject(analyzedInstruction);
                return true;
            case APUT:
                handle32BitPrimitiveAput(analyzedInstruction, RegisterType.Category.Integer);
                return true;
            case APUT_BOOLEAN:
                handle32BitPrimitiveAput(analyzedInstruction, RegisterType.Category.Boolean);
                return true;
            case APUT_BYTE:
                handle32BitPrimitiveAput(analyzedInstruction, RegisterType.Category.Byte);
                return true;
            case APUT_CHAR:
                handle32BitPrimitiveAput(analyzedInstruction, RegisterType.Category.Char);
                return true;
            case APUT_SHORT:
                handle32BitPrimitiveAput(analyzedInstruction, RegisterType.Category.Short);
                return true;
            case APUT_WIDE:
                handleAputWide(analyzedInstruction);
                return true;
            case APUT_OBJECT:
                handleAputObject(analyzedInstruction);
                return true;
            case IGET:
                handle32BitPrimitiveIget(analyzedInstruction, RegisterType.Category.Integer);
                return true;
            case IGET_BOOLEAN:
                handle32BitPrimitiveIget(analyzedInstruction, RegisterType.Category.Boolean);
                return true;
            case IGET_BYTE:
                handle32BitPrimitiveIget(analyzedInstruction, RegisterType.Category.Byte);
                return true;
            case IGET_CHAR:
                handle32BitPrimitiveIget(analyzedInstruction, RegisterType.Category.Char);
                return true;
            case IGET_SHORT:
                handle32BitPrimitiveIget(analyzedInstruction, RegisterType.Category.Short);
                return true;
            case IGET_WIDE:
                handleIgetWide(analyzedInstruction);
                return true;
            case IGET_OBJECT:
                handleIgetObject(analyzedInstruction);
                return true;
            case IPUT:
                handle32BitPrimitiveIput(analyzedInstruction, RegisterType.Category.Integer);
                return true;
            case IPUT_BOOLEAN:
                handle32BitPrimitiveIput(analyzedInstruction, RegisterType.Category.Boolean);
                return true;
            case IPUT_BYTE:
                handle32BitPrimitiveIput(analyzedInstruction, RegisterType.Category.Byte);
                return true;
            case IPUT_CHAR:
                handle32BitPrimitiveIput(analyzedInstruction, RegisterType.Category.Char);
                return true;
            case IPUT_SHORT:
                handle32BitPrimitiveIput(analyzedInstruction, RegisterType.Category.Short);
                return true;
            case IPUT_WIDE:
                handleIputWide(analyzedInstruction);
                return true;
            case IPUT_OBJECT:
                handleIputObject(analyzedInstruction);
                return true;
            case SGET:
                handle32BitPrimitiveSget(analyzedInstruction, RegisterType.Category.Integer);
                return true;
            case SGET_BOOLEAN:
                handle32BitPrimitiveSget(analyzedInstruction, RegisterType.Category.Boolean);
                return true;
            case SGET_BYTE:
                handle32BitPrimitiveSget(analyzedInstruction, RegisterType.Category.Byte);
                return true;
            case SGET_CHAR:
                handle32BitPrimitiveSget(analyzedInstruction, RegisterType.Category.Char);
                return true;
            case SGET_SHORT:
                handle32BitPrimitiveSget(analyzedInstruction, RegisterType.Category.Short);
                return true;
            case SGET_WIDE:
                handleSgetWide(analyzedInstruction);
                return true;
            case SGET_OBJECT:
                handleSgetObject(analyzedInstruction);
                return true;
            case SPUT:
                handle32BitPrimitiveSput(analyzedInstruction, RegisterType.Category.Integer);
                return true;
            case SPUT_BOOLEAN:
                handle32BitPrimitiveSput(analyzedInstruction, RegisterType.Category.Boolean);
                return true;
            case SPUT_BYTE:
                handle32BitPrimitiveSput(analyzedInstruction, RegisterType.Category.Byte);
                return true;
            case SPUT_CHAR:
                handle32BitPrimitiveSput(analyzedInstruction, RegisterType.Category.Char);
                return true;
            case SPUT_SHORT:
                handle32BitPrimitiveSput(analyzedInstruction, RegisterType.Category.Short);
                return true;
            case SPUT_WIDE:
                handleSputWide(analyzedInstruction);
                return true;
            case SPUT_OBJECT:
                handleSputObject(analyzedInstruction);
                return true;
            case INVOKE_VIRTUAL:
                handleInvoke(analyzedInstruction, INVOKE_VIRTUAL);
                return true;
            case INVOKE_SUPER:
                handleInvoke(analyzedInstruction, INVOKE_SUPER);
                return true;
            case INVOKE_DIRECT:
                handleInvoke(analyzedInstruction, INVOKE_DIRECT);
                return true;
            case INVOKE_STATIC:
                handleInvoke(analyzedInstruction, INVOKE_STATIC);
                return true;
            case INVOKE_INTERFACE:
                handleInvoke(analyzedInstruction, INVOKE_INTERFACE);
                return true;
            case INVOKE_VIRTUAL_RANGE:
                handleInvokeRange(analyzedInstruction, INVOKE_VIRTUAL);
                return true;
            case INVOKE_SUPER_RANGE:
                handleInvokeRange(analyzedInstruction, INVOKE_SUPER);
                return true;
            case INVOKE_DIRECT_RANGE:
                handleInvokeRange(analyzedInstruction, INVOKE_DIRECT);
                return true;
            case INVOKE_STATIC_RANGE:
                handleInvokeRange(analyzedInstruction, INVOKE_STATIC);
                return true;
            case INVOKE_INTERFACE_RANGE:
                handleInvokeRange(analyzedInstruction, INVOKE_INTERFACE);
                return true;
            case NEG_INT:
            case NOT_INT:
                handleUnaryOp(analyzedInstruction, Primitive32BitCategories, RegisterType.Category.Integer);
                return true;
            case NEG_LONG:
            case NOT_LONG:
                handleUnaryOp(analyzedInstruction, WideLowCategories, RegisterType.Category.LongLo);
                return true;
            case NEG_FLOAT:
                handleUnaryOp(analyzedInstruction, Primitive32BitCategories, RegisterType.Category.Float);
                return true;
            case NEG_DOUBLE:
                handleUnaryOp(analyzedInstruction, WideLowCategories, RegisterType.Category.DoubleLo);
                return true;
            case INT_TO_LONG:
                handleUnaryOp(analyzedInstruction, Primitive32BitCategories, RegisterType.Category.LongLo);
                return true;
            case INT_TO_FLOAT:
                handleUnaryOp(analyzedInstruction, Primitive32BitCategories, RegisterType.Category.Float);
                return true;
            case INT_TO_DOUBLE:
                handleUnaryOp(analyzedInstruction, Primitive32BitCategories, RegisterType.Category.DoubleLo);
                return true;
            case LONG_TO_INT:
            case DOUBLE_TO_INT:
                handleUnaryOp(analyzedInstruction, WideLowCategories, RegisterType.Category.Integer);
                return true;
            case LONG_TO_FLOAT:
            case DOUBLE_TO_FLOAT:
                handleUnaryOp(analyzedInstruction, WideLowCategories, RegisterType.Category.Float);
                return true;
            case LONG_TO_DOUBLE:
                handleUnaryOp(analyzedInstruction, WideLowCategories, RegisterType.Category.DoubleLo);
                return true;
            case FLOAT_TO_INT:
                handleUnaryOp(analyzedInstruction, Primitive32BitCategories, RegisterType.Category.Integer);
                return true;
            case FLOAT_TO_LONG:
                handleUnaryOp(analyzedInstruction, Primitive32BitCategories, RegisterType.Category.LongLo);
                return true;
            case FLOAT_TO_DOUBLE:
                handleUnaryOp(analyzedInstruction, Primitive32BitCategories, RegisterType.Category.DoubleLo);
                return true;
            case DOUBLE_TO_LONG:
                handleUnaryOp(analyzedInstruction, WideLowCategories, RegisterType.Category.LongLo);
                return true;
            case INT_TO_BYTE:
                handleUnaryOp(analyzedInstruction, Primitive32BitCategories, RegisterType.Category.Byte);
                return true;
            case INT_TO_CHAR:
                handleUnaryOp(analyzedInstruction, Primitive32BitCategories, RegisterType.Category.Char);
                return true;
            case INT_TO_SHORT:
                handleUnaryOp(analyzedInstruction, Primitive32BitCategories, RegisterType.Category.Short);
                return true;
            case ADD_INT:
            case SUB_INT:
            case MUL_INT:
            case DIV_INT:
            case REM_INT:
            case SHL_INT:
            case SHR_INT:
            case USHR_INT:
                handleBinaryOp(analyzedInstruction, Primitive32BitCategories, Primitive32BitCategories,
                        RegisterType.Category.Integer, false);
                return true;
            case AND_INT:
            case OR_INT:
            case XOR_INT:
                handleBinaryOp(analyzedInstruction, Primitive32BitCategories, Primitive32BitCategories,
                        RegisterType.Category.Integer, true);
                return true;
            case ADD_LONG:
            case SUB_LONG:
            case MUL_LONG:
            case DIV_LONG:
            case REM_LONG:
            case AND_LONG:
            case OR_LONG:
            case XOR_LONG:
                handleBinaryOp(analyzedInstruction, WideLowCategories, WideLowCategories, RegisterType.Category.LongLo,
                        false);
                return true;
            case SHL_LONG:
            case SHR_LONG:
            case USHR_LONG:
                handleBinaryOp(analyzedInstruction, WideLowCategories, Primitive32BitCategories,
                        RegisterType.Category.LongLo, false);
                return true;
            case ADD_FLOAT:
            case SUB_FLOAT:
            case MUL_FLOAT:
            case DIV_FLOAT:
            case REM_FLOAT:
                handleBinaryOp(analyzedInstruction, Primitive32BitCategories, Primitive32BitCategories,
                        RegisterType.Category.Float, false);
                return true;
            case ADD_DOUBLE:
            case SUB_DOUBLE:
            case MUL_DOUBLE:
            case DIV_DOUBLE:
            case REM_DOUBLE:
                handleBinaryOp(analyzedInstruction, WideLowCategories, WideLowCategories,
                        RegisterType.Category.DoubleLo, false);
                return true;
            case ADD_INT_2ADDR:
            case SUB_INT_2ADDR:
            case MUL_INT_2ADDR:
            case DIV_INT_2ADDR:
            case REM_INT_2ADDR:
            case SHL_INT_2ADDR:
            case SHR_INT_2ADDR:
            case USHR_INT_2ADDR:
                handleBinary2AddrOp(analyzedInstruction, Primitive32BitCategories, Primitive32BitCategories,
                        RegisterType.Category.Integer, false);
                return true;
            case AND_INT_2ADDR:
            case OR_INT_2ADDR:
            case XOR_INT_2ADDR:
                handleBinary2AddrOp(analyzedInstruction, Primitive32BitCategories, Primitive32BitCategories,
                        RegisterType.Category.Integer, true);
                return true;
            case ADD_LONG_2ADDR:
            case SUB_LONG_2ADDR:
            case MUL_LONG_2ADDR:
            case DIV_LONG_2ADDR:
            case REM_LONG_2ADDR:
            case AND_LONG_2ADDR:
            case OR_LONG_2ADDR:
            case XOR_LONG_2ADDR:
                handleBinary2AddrOp(analyzedInstruction, WideLowCategories, WideLowCategories,
                        RegisterType.Category.LongLo, false);
                return true;
            case SHL_LONG_2ADDR:
            case SHR_LONG_2ADDR:
            case USHR_LONG_2ADDR:
                handleBinary2AddrOp(analyzedInstruction, WideLowCategories, Primitive32BitCategories,
                        RegisterType.Category.LongLo, false);
                return true;
            case ADD_FLOAT_2ADDR:
            case SUB_FLOAT_2ADDR:
            case MUL_FLOAT_2ADDR:
            case DIV_FLOAT_2ADDR:
            case REM_FLOAT_2ADDR:
                handleBinary2AddrOp(analyzedInstruction, Primitive32BitCategories, Primitive32BitCategories,
                        RegisterType.Category.Float, false);
                return true;
            case ADD_DOUBLE_2ADDR:
            case SUB_DOUBLE_2ADDR:
            case MUL_DOUBLE_2ADDR:
            case DIV_DOUBLE_2ADDR:
            case REM_DOUBLE_2ADDR:
                handleBinary2AddrOp(analyzedInstruction, WideLowCategories, WideLowCategories,
                        RegisterType.Category.DoubleLo, false);
                return true;
            case ADD_INT_LIT16:
            case RSUB_INT:
            case MUL_INT_LIT16:
            case DIV_INT_LIT16:
            case REM_INT_LIT16:
                handleLiteralBinaryOp(analyzedInstruction, Primitive32BitCategories, RegisterType.Category.Integer,
                        false);
                return true;
            case AND_INT_LIT16:
            case OR_INT_LIT16:
            case XOR_INT_LIT16:
                handleLiteralBinaryOp(analyzedInstruction, Primitive32BitCategories, RegisterType.Category.Integer,
                        true);
                return true;
            case ADD_INT_LIT8:
            case RSUB_INT_LIT8:
            case MUL_INT_LIT8:
            case DIV_INT_LIT8:
            case REM_INT_LIT8:
            case SHL_INT_LIT8:
                handleLiteralBinaryOp(analyzedInstruction, Primitive32BitCategories, RegisterType.Category.Integer,
                        false);
                return true;
            case AND_INT_LIT8:
            case OR_INT_LIT8:
            case XOR_INT_LIT8:
                handleLiteralBinaryOp(analyzedInstruction, Primitive32BitCategories, RegisterType.Category.Integer,
                        true);
                return true;
            case SHR_INT_LIT8:
                handleLiteralBinaryOp(analyzedInstruction, Primitive32BitCategories,
                        getDestTypeForLiteralShiftRight(analyzedInstruction, true), false);
                return true;
            case USHR_INT_LIT8:
                handleLiteralBinaryOp(analyzedInstruction, Primitive32BitCategories,
                        getDestTypeForLiteralShiftRight(analyzedInstruction, false), false);
                return true;
            case EXECUTE_INLINE:
                handleExecuteInline(analyzedInstruction);
                return true;
            case EXECUTE_INLINE_RANGE:
                handleExecuteInlineRange(analyzedInstruction);
                return true;
            case INVOKE_DIRECT_EMPTY:
                handleInvokeDirectEmpty(analyzedInstruction);
                return true;
            case IGET_QUICK:
            case IGET_WIDE_QUICK:
            case IGET_OBJECT_QUICK:
                return handleIputIgetQuick(analyzedInstruction, false);
            case IPUT_QUICK:
            case IPUT_WIDE_QUICK:
            case IPUT_OBJECT_QUICK:
                return handleIputIgetQuick(analyzedInstruction, true);
            case INVOKE_VIRTUAL_QUICK:
                return handleInvokeVirtualQuick(analyzedInstruction, false, false);
            case INVOKE_SUPER_QUICK:
                return handleInvokeVirtualQuick(analyzedInstruction, true, false);
            case INVOKE_VIRTUAL_QUICK_RANGE:
                return handleInvokeVirtualQuick(analyzedInstruction, false, true);
            case INVOKE_SUPER_QUICK_RANGE:
                return handleInvokeVirtualQuick(analyzedInstruction, true, true);
            default:
                assert false;
                return true;
        }
    }

    private static final EnumSet<RegisterType.Category> Primitive32BitCategories = EnumSet.of(
            RegisterType.Category.Null,
            RegisterType.Category.One,
            RegisterType.Category.Boolean,
            RegisterType.Category.Byte,
            RegisterType.Category.PosByte,
            RegisterType.Category.Short,
            RegisterType.Category.PosShort,
            RegisterType.Category.Char,
            RegisterType.Category.Integer,
            RegisterType.Category.Float);

    private static final EnumSet<RegisterType.Category> WideLowCategories = EnumSet.of(
            RegisterType.Category.LongLo,
            RegisterType.Category.DoubleLo);

    private static final EnumSet<RegisterType.Category> WideHighCategories = EnumSet.of(
            RegisterType.Category.LongHi,
            RegisterType.Category.DoubleHi);

    private static final EnumSet<RegisterType.Category> ReferenceCategories = EnumSet.of(
            RegisterType.Category.Null,
            RegisterType.Category.Reference);

    private static final EnumSet<RegisterType.Category> ReferenceOrUninitThisCategories = EnumSet.of(
            RegisterType.Category.Null,
            RegisterType.Category.UninitThis,
            RegisterType.Category.Reference);

    private static final EnumSet<RegisterType.Category> ReferenceOrUninitCategories = EnumSet.of(
            RegisterType.Category.Null,
            RegisterType.Category.UninitRef,
            RegisterType.Category.UninitThis,
            RegisterType.Category.Reference);

    private static final EnumSet<RegisterType.Category> ReferenceAndPrimitive32BitCategories = EnumSet.of(
            RegisterType.Category.Null,
            RegisterType.Category.One,
            RegisterType.Category.Boolean,
            RegisterType.Category.Byte,
            RegisterType.Category.PosByte,
            RegisterType.Category.Short,
            RegisterType.Category.PosShort,
            RegisterType.Category.Char,
            RegisterType.Category.Integer,
            RegisterType.Category.Float,
            RegisterType.Category.Reference);

    private static final EnumSet<RegisterType.Category> BooleanCategories = EnumSet.of(
            RegisterType.Category.Null,
            RegisterType.Category.One,
            RegisterType.Category.Boolean);

    private void handleMove(AnalyzedInstruction analyzedInstruction, EnumSet validCategories) {
        TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;

        RegisterType sourceRegisterType = getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterB(),
                validCategories);

        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, sourceRegisterType);
    }

    private void handleMoveResult(AnalyzedInstruction analyzedInstruction,
                                     EnumSet<RegisterType.Category> allowedCategories) {

        //TODO: handle the case when the previous instruction is an odexed instruction

        if (analyzedInstruction.instructionIndex == 0) {
            throw new ValidationException(analyzedInstruction.instruction.opcode.name + " cannot be the first " +
                    "instruction in a method. It must occur after an invoke-*/fill-new-array instruction");
        }

        AnalyzedInstruction previousInstruction = instructions.valueAt(analyzedInstruction.instructionIndex-1);

        if (!previousInstruction.instruction.opcode.setsResult()) {
            throw new ValidationException(analyzedInstruction.instruction.opcode.name + " must occur after an " +
                    "invoke-*/fill-new-array instruction");
        }

        //TODO: does dalvik allow a move-result after an invoke with a void return type?
        RegisterType resultRegisterType;

        InstructionWithReference invokeInstruction = (InstructionWithReference)previousInstruction.instruction;
        Item item = invokeInstruction.getReferencedItem();

        if (item instanceof MethodIdItem) {
            resultRegisterType = RegisterType.getRegisterTypeForTypeIdItem(
                    ((MethodIdItem)item).getPrototype().getReturnType());
        } else {
            assert item instanceof TypeIdItem;
            resultRegisterType = RegisterType.getRegisterTypeForTypeIdItem((TypeIdItem)item);
        }

        if (!allowedCategories.contains(resultRegisterType.category)) {
            throw new ValidationException(String.format("Wrong move-result* instruction for return value %s",
                    resultRegisterType.toString()));
        }

        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, resultRegisterType);
    }

    private void handleMoveException(AnalyzedInstruction analyzedInstruction) {
        CodeItem.TryItem[] tries = encodedMethod.codeItem.getTries();
        int instructionAddress = getInstructionAddress(analyzedInstruction);

        if (tries == null) {
            throw new ValidationException("move-exception must be the first instruction in an exception handler block");
        }

        RegisterType exceptionType = null;

        for (CodeItem.TryItem tryItem: encodedMethod.codeItem.getTries()) {
            if (tryItem.encodedCatchHandler.getCatchAllHandlerAddress() == instructionAddress) {
                exceptionType = RegisterType.getRegisterType(RegisterType.Category.Reference,
                        ClassPath.getClassDef("Ljava/lang/Throwable;"));
                break;
            }
            for (CodeItem.EncodedTypeAddrPair handler: tryItem.encodedCatchHandler.handlers) {
                if (handler.getHandlerAddress() == instructionAddress) {
                    exceptionType = RegisterType.getRegisterTypeForTypeIdItem(handler.exceptionType)
                            .merge(exceptionType);
                }
            }
        }

        if (exceptionType == null) {
            throw new ValidationException("move-exception must be the first instruction in an exception handler block");
        }

        //TODO: check if the type is a throwable. Should we throw a ValidationException or print a warning? (does dalvik validate that it's a throwable? It doesn't in CodeVerify.c, but it might check in DexSwapVerify.c)
        if (exceptionType.category != RegisterType.Category.Reference) {
            throw new ValidationException(String.format("Exception type %s is not a reference type",
                    exceptionType.toString()));
        }

        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, exceptionType);
    }

    private void handleReturnVoid(AnalyzedInstruction analyzedInstruction) {
        TypeIdItem returnType = encodedMethod.method.getPrototype().getReturnType();
        if (returnType.getTypeDescriptor().charAt(0) != 'V') {
            //TODO: could add which return-* variation should be used instead
            throw new ValidationException("Cannot use return-void with a non-void return type (" +
                returnType.getTypeDescriptor() + ")");
        }
    }

    private void handleReturn(AnalyzedInstruction analyzedInstruction, EnumSet validCategories) {
        /*if (this.isInstanceConstructor()) {
            checkConstructorReturn(analyzedInstruction);
        }*/

        SingleRegisterInstruction instruction = (SingleRegisterInstruction)analyzedInstruction.instruction;
        int returnRegister = instruction.getRegisterA();
        RegisterType returnRegisterType = getAndCheckSourceRegister(analyzedInstruction, returnRegister,
                validCategories);

        TypeIdItem returnType = encodedMethod.method.getPrototype().getReturnType();
        if (returnType.getTypeDescriptor().charAt(0) == 'V') {
            throw new ValidationException("Cannot use return with a void return type. Use return-void instead");
        }

        RegisterType methodReturnRegisterType = RegisterType.getRegisterTypeForTypeIdItem(returnType);

        if (!validCategories.contains(methodReturnRegisterType.category)) {
            //TODO: could add which return-* variation should be used instead
            throw new ValidationException(String.format("Cannot use %s with return type %s",
                    analyzedInstruction.instruction.opcode.name, returnType.getTypeDescriptor()));
        }

        if (validCategories == ReferenceCategories) {
            if (methodReturnRegisterType.type.isInterface()) {
                if (returnRegisterType.category != RegisterType.Category.Null &&
                    !returnRegisterType.type.implementsInterface(methodReturnRegisterType.type)) {
                    //TODO: how to handle warnings?
                }
            } else {
                if (returnRegisterType.category == RegisterType.Category.Reference &&
                    !returnRegisterType.type.extendsClass(methodReturnRegisterType.type)) {

                    throw new ValidationException(String.format("The return value in register v%d (%s) is not " +
                            "compatible with the method's return type %s", returnRegister,
                            returnRegisterType.type.getClassType(), methodReturnRegisterType.type.getClassType()));
                }
            }
        }
    }

    private void handleConst(AnalyzedInstruction analyzedInstruction) {
        LiteralInstruction instruction = (LiteralInstruction)analyzedInstruction.instruction;

        RegisterType newDestinationRegisterType = RegisterType.getRegisterTypeForLiteral(instruction.getLiteral());

        //we assume that the literal value is a valid value for the given instruction type, because it's impossible
        //to store an invalid literal with the instruction. so we don't need to check the type of the literal
        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, newDestinationRegisterType);
    }

    private void handleConstHigh16(AnalyzedInstruction analyzedInstruction) {
        LiteralInstruction instruction = (LiteralInstruction)analyzedInstruction.instruction;

        long literalValue = instruction.getLiteral() << 16;
        RegisterType newDestinationRegisterType = RegisterType.getRegisterTypeForLiteral(literalValue);

        //we assume that the literal value is a valid value for the given instruction type, because it's impossible
        //to store an invalid literal with the instruction. so we don't need to check the type of the literal
        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, newDestinationRegisterType);
    }

    private void handleWideConst(AnalyzedInstruction analyzedInstruction) {
        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction,
                RegisterType.getRegisterType(RegisterType.Category.LongLo, null));
    }

    private void handleConstString(AnalyzedInstruction analyzedInstruction) {
        ClassPath.ClassDef stringClassDef = ClassPath.getClassDef("Ljava/lang/String;");
        RegisterType stringType = RegisterType.getRegisterType(RegisterType.Category.Reference, stringClassDef);
        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, stringType);
    }

    private void handleConstClass(AnalyzedInstruction analyzedInstruction) {
        ClassPath.ClassDef classClassDef = ClassPath.getClassDef("Ljava/lang/Class;");
        RegisterType classType = RegisterType.getRegisterType(RegisterType.Category.Reference, classClassDef);

        InstructionWithReference instruction = (InstructionWithReference)analyzedInstruction.instruction;
        Item item = instruction.getReferencedItem();
        assert item.getItemType() == ItemType.TYPE_TYPE_ID_ITEM;

        //TODO: need to check class access
        //make sure the referenced class is resolvable
        ClassPath.getClassDef((TypeIdItem)item);

        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, classType);
    }

    private void handleMonitor(AnalyzedInstruction analyzedInstruction) {
        SingleRegisterInstruction instruction = (SingleRegisterInstruction)analyzedInstruction.instruction;
        getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterA(), ReferenceCategories);
    }

    private void handleCheckCast(AnalyzedInstruction analyzedInstruction) {
        {
            //ensure the "source" register is a reference type
            SingleRegisterInstruction instruction = (SingleRegisterInstruction)analyzedInstruction.instruction;

            RegisterType registerType = getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterA(),
                    ReferenceCategories);
        }

        {
            //resolve and verify the class that we're casting to
            InstructionWithReference instruction = (InstructionWithReference)analyzedInstruction.instruction;

            Item item = instruction.getReferencedItem();
            assert item.getItemType() == ItemType.TYPE_TYPE_ID_ITEM;

            //TODO: need to check class access
            RegisterType castRegisterType = RegisterType.getRegisterTypeForTypeIdItem((TypeIdItem)item);
            if (castRegisterType.category != RegisterType.Category.Reference) {
                //TODO: verify that dalvik allows a non-reference type..
                //TODO: print a warning, but don't re-throw the exception. dalvik allows a non-reference type during validation (but throws an exception at runtime)
            }

            setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, castRegisterType);
        }
    }

    private void handleInstanceOf(AnalyzedInstruction analyzedInstruction) {
        {
            //ensure the register that is being checks is a reference type
            TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;

            getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterB(), ReferenceCategories);
        }

        {
            //resolve and verify the class that we're checking against
            InstructionWithReference instruction = (InstructionWithReference)analyzedInstruction.instruction;

            Item item = instruction.getReferencedItem();
            assert  item.getItemType() == ItemType.TYPE_TYPE_ID_ITEM;
            RegisterType registerType = RegisterType.getRegisterTypeForTypeIdItem((TypeIdItem)item);
            if (registerType.category != RegisterType.Category.Reference) {
                throw new ValidationException(String.format("Cannot use instance-of with a non-reference type %s",
                        registerType.toString()));
            }

            //TODO: is it valid to use an array type?

            //TODO: could probably do an even more sophisticated check, where we check the possible register types against the specified type. In some cases, we could determine that it always fails, and print a warning to that effect.
            setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction,
                    RegisterType.getRegisterType(RegisterType.Category.Boolean, null));
        }
    }

    private void handleArrayLength(AnalyzedInstruction analyzedInstruction) {
        TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;

        int arrayRegisterNumber = instruction.getRegisterB();
        RegisterType arrayRegisterType = getAndCheckSourceRegister(analyzedInstruction, arrayRegisterNumber,
                ReferenceCategories);

        if (arrayRegisterType.type != null) {
            if (arrayRegisterType.type.getClassType().charAt(0) != '[') {
                throw new ValidationException(String.format("Cannot use array-length with non-array type %s",
                        arrayRegisterType.type.getClassType()));
            }
            assert arrayRegisterType.type instanceof ClassPath.ArrayClassDef;
        }

        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction,
                RegisterType.getRegisterType(RegisterType.Category.Integer, null));
    }

    private void handleNewInstance(AnalyzedInstruction analyzedInstruction) {
        InstructionWithReference instruction = (InstructionWithReference)analyzedInstruction.instruction;

        int register = ((SingleRegisterInstruction)analyzedInstruction.instruction).getRegisterA();
        RegisterType destRegisterType = analyzedInstruction.postRegisterMap[register];
        if (destRegisterType.category != RegisterType.Category.Unknown) {
            assert destRegisterType.category == RegisterType.Category.UninitRef;

            //the "post-instruction" destination register will only be set if we've gone over
            //this instruction at least once before. If this is the case, then we need to check
            //all the other registers, and make sure that none of them contain the same
            //uninitialized reference that is in the destination register.

            for (int i=0; i<analyzedInstruction.postRegisterMap.length; i++) {
                if (i==register) {
                    continue;
                }

                if (analyzedInstruction.getPreInstructionRegisterType(i) == destRegisterType) {
                    throw new ValidationException(String.format("Register v%d contains an uninitialized reference " +
                            "that was created by this new-instance instruction.", i));
                }
            }

            return;
        }

        Item item = instruction.getReferencedItem();
        assert item.getItemType() == ItemType.TYPE_TYPE_ID_ITEM;

        //TODO: need to check class access
        RegisterType classType = RegisterType.getRegisterTypeForTypeIdItem((TypeIdItem)item);
        if (classType.category != RegisterType.Category.Reference) {
            throw new ValidationException(String.format("Cannot use new-instance with a non-reference type %s",
                    classType.toString()));
        }

        if (((TypeIdItem)item).getTypeDescriptor().charAt(0) == '[') {
            throw new ValidationException("Cannot use array type \"" + ((TypeIdItem)item).getTypeDescriptor() +
                    "\" with new-instance. Use new-array instead.");
        }

        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction,
                RegisterType.getUnitializedReference(classType.type));
    }

    private void handleNewArray(AnalyzedInstruction analyzedInstruction) {
        {
            TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;
            getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterB(), Primitive32BitCategories);
        }

        InstructionWithReference instruction = (InstructionWithReference)analyzedInstruction.instruction;

        Item item = instruction.getReferencedItem();
        assert item.getItemType() == ItemType.TYPE_TYPE_ID_ITEM;

        RegisterType arrayType = RegisterType.getRegisterTypeForTypeIdItem((TypeIdItem)item);
        assert arrayType.type instanceof ClassPath.ArrayClassDef;

        if (arrayType.category != RegisterType.Category.Reference) {
            throw new ValidationException(String.format("Cannot use new-array with a non-reference type %s",
                    arrayType.toString()));
        }
        if (arrayType.type.getClassType().charAt(0) != '[') {
            throw new ValidationException("Cannot use non-array type \"" + arrayType.type.getClassType() +
                    "\" with new-array. Use new-instance instead.");
        }

        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, arrayType);
    }

    private static interface RegisterIterator {
        int getRegister();
        boolean moveNext();
        int getCount();
        boolean pastEnd();
    }

    private static class Format35cRegisterIterator implements RegisterIterator {
        private final int registerCount;
        private final int[] registers;
        private int currentRegister = 0;

        public Format35cRegisterIterator(FiveRegisterInstruction instruction) {
            registerCount = instruction.getRegCount();
            registers = new int[]{instruction.getRegisterD(), instruction.getRegisterE(),
                                  instruction.getRegisterF(), instruction.getRegisterG(),
                                  instruction.getRegisterA()};
        }

        public int getRegister() {
            return registers[currentRegister];
        }

        public boolean moveNext() {
            currentRegister++;
            return !pastEnd();
        }

        public int getCount() {
            return registerCount;
        }

        public boolean pastEnd() {
            return currentRegister >= registerCount;
        }
    }

    private static class Format3rcRegisterIterator implements RegisterIterator {
        private final int startRegister;
        private final int registerCount;
        private int currentRegister = 0;

        public Format3rcRegisterIterator(RegisterRangeInstruction instruction) {
            startRegister = instruction.getStartRegister();
            registerCount = instruction.getRegCount();
        }

        public int getRegister() {
            return startRegister + currentRegister;
        }

        public boolean moveNext() {
            currentRegister++;
            return !pastEnd();
        }

        public int getCount() {
            return registerCount;
        }

        public boolean pastEnd() {
            return currentRegister >= registerCount;
        }
    }

    private void handleFilledNewArrayCommon(AnalyzedInstruction analyzedInstruction,
                                               RegisterIterator registerIterator) {
        InstructionWithReference instruction = (InstructionWithReference)analyzedInstruction.instruction;

        RegisterType arrayType;
        RegisterType arrayImmediateElementType;

        Item item = instruction.getReferencedItem();
        assert  item.getItemType() == ItemType.TYPE_TYPE_ID_ITEM;

        ClassPath.ClassDef classDef = ClassPath.getClassDef((TypeIdItem)item);

        if (classDef.getClassType().charAt(0) != '[') {
            throw new ValidationException("Cannot use non-array type \"" + classDef.getClassType() +
                "\" with new-array. Use new-instance instead.");
        }

        ClassPath.ArrayClassDef arrayClassDef = (ClassPath.ArrayClassDef)classDef;
        arrayType = RegisterType.getRegisterType(RegisterType.Category.Reference, classDef);
        arrayImmediateElementType = RegisterType.getRegisterTypeForType(
                arrayClassDef.getImmediateElementClass().getClassType());
        String baseElementType = arrayClassDef.getBaseElementClass().getClassType();
        if (baseElementType.charAt(0) == 'J' || baseElementType.charAt(0) == 'D') {
            throw new ValidationException("Cannot use filled-new-array to create an array of wide values " +
                    "(long or double)");
        }

        do {
            int register = registerIterator.getRegister();
            RegisterType elementType = analyzedInstruction.getPreInstructionRegisterType(register);
            assert elementType != null;

            if (!elementType.canBeAssignedTo(arrayImmediateElementType)) {
                throw new ValidationException("Register v" + Integer.toString(register) + " is of type " +
                        elementType.toString() + " and is incompatible with the array type " +
                        arrayType.type.getClassType());
            }
        } while (registerIterator.moveNext());
    }

    private void handleFilledNewArray(AnalyzedInstruction analyzedInstruction) {
        FiveRegisterInstruction instruction = (FiveRegisterInstruction)analyzedInstruction.instruction;
        handleFilledNewArrayCommon(analyzedInstruction, new Format35cRegisterIterator(instruction));
    }

    private void handleFilledNewArrayRange(AnalyzedInstruction analyzedInstruction) {
        RegisterRangeInstruction instruction = (RegisterRangeInstruction)analyzedInstruction.instruction;

        //instruction.getStartRegister() and instruction.getRegCount() both return an int value, but are actually
        //unsigned 16 bit values, so we don't have to worry about overflowing an int when adding them together
        if (instruction.getStartRegister() + instruction.getRegCount() >= 1<<16) {
            throw new ValidationException(String.format("Invalid register range {v%d .. v%d}. The ending register " +
                    "is larger than the largest allowed register of v65535.",
                    instruction.getStartRegister(),
                    instruction.getStartRegister() + instruction.getRegCount() - 1));
        }

        handleFilledNewArrayCommon(analyzedInstruction, new Format3rcRegisterIterator(instruction));
    }

    private void handleFillArrayData(AnalyzedInstruction analyzedInstruction) {
        SingleRegisterInstruction instruction = (SingleRegisterInstruction)analyzedInstruction.instruction;

        int register = instruction.getRegisterA();
        RegisterType registerType = analyzedInstruction.getPreInstructionRegisterType(register);
        assert registerType != null;

        if (registerType.category == RegisterType.Category.Null) {
            return;
        }

        if (registerType.category != RegisterType.Category.Reference) {
            throw new ValidationException(String.format("Cannot use fill-array-data with non-array register v%d of " +
                    "type %s", register, registerType.toString()));
        }

        assert registerType.type instanceof ClassPath.ArrayClassDef;
        ClassPath.ArrayClassDef arrayClassDef = (ClassPath.ArrayClassDef)registerType.type;

        if (arrayClassDef.getArrayDimensions() != 1) {
            throw new ValidationException(String.format("Cannot use fill-array-data with array type %s. It can only " +
                    "be used with a one-dimensional array of primitives.", arrayClassDef.getClassType()));
        }

        int elementWidth;
        switch (arrayClassDef.getBaseElementClass().getClassType().charAt(0)) {
            case 'Z':
            case 'B':
                elementWidth = 1;
                break;
            case 'C':
            case 'S':
                elementWidth = 2;
                break;
            case 'I':
            case 'F':
                elementWidth = 4;
                break;
            case 'J':
            case 'D':
                elementWidth = 8;
                break;
            default:
                throw new ValidationException(String.format("Cannot use fill-array-data with array type %s. It can " +
                        "only be used with a one-dimensional array of primitives.", arrayClassDef.getClassType()));
        }


        int arrayDataAddressOffset = ((OffsetInstruction)analyzedInstruction.instruction).getTargetAddressOffset();
        int arrayDataCodeAddress = getInstructionAddress(analyzedInstruction) + arrayDataAddressOffset;
        AnalyzedInstruction arrayDataInstruction = this.instructions.get(arrayDataCodeAddress);
        if (arrayDataInstruction == null || arrayDataInstruction.instruction.getFormat() != Format.ArrayData) {
            throw new ValidationException(String.format("Could not find an array data structure at code address 0x%x",
                    arrayDataCodeAddress));
        }

        ArrayDataPseudoInstruction arrayDataPseudoInstruction =
                (ArrayDataPseudoInstruction)arrayDataInstruction.instruction;

        if (elementWidth != arrayDataPseudoInstruction.getElementWidth()) {
            throw new ValidationException(String.format("The array data at code address 0x%x does not have the " +
                    "correct element width for array type %s. Expecting element width %d, got element width %d.",
                    arrayDataCodeAddress, arrayClassDef.getClassType(), elementWidth,
                    arrayDataPseudoInstruction.getElementWidth()));
        }
    }

    private void handleThrow(AnalyzedInstruction analyzedInstruction) {
        int register = ((SingleRegisterInstruction)analyzedInstruction.instruction).getRegisterA();

        RegisterType registerType = analyzedInstruction.getPreInstructionRegisterType(register);
        assert registerType != null;

        if (registerType.category == RegisterType.Category.Null) {
            return;
        }

        if (registerType.category != RegisterType.Category.Reference) {
            throw new ValidationException(String.format("Cannot use throw with non-reference type %s in register v%d",
                    registerType.toString(), register));
        }

        assert registerType.type != null;

        if (!registerType.type.extendsClass(ClassPath.getClassDef("Ljava/lang/Throwable;"))) {
            throw new ValidationException(String.format("Cannot use throw with non-throwable type %s in register v%d",
                    registerType.type.getClassType(), register));
        }
    }

    private void handleSwitch(AnalyzedInstruction analyzedInstruction, Format expectedSwitchDataFormat) {
        int register = ((SingleRegisterInstruction)analyzedInstruction.instruction).getRegisterA();
        int switchCodeAddressOffset = ((OffsetInstruction)analyzedInstruction.instruction).getTargetAddressOffset();

        getAndCheckSourceRegister(analyzedInstruction, register, Primitive32BitCategories);

        int switchDataCodeAddress = this.getInstructionAddress(analyzedInstruction) + switchCodeAddressOffset;
        AnalyzedInstruction switchDataAnalyzedInstruction = instructions.get(switchDataCodeAddress);

        if (switchDataAnalyzedInstruction == null ||
            switchDataAnalyzedInstruction.instruction.getFormat() != expectedSwitchDataFormat) {
            throw new ValidationException(String.format("There is no %s structure at code address 0x%x",
                    expectedSwitchDataFormat.name(), switchDataCodeAddress));
        }
    }

    private void handleFloatWideCmp(AnalyzedInstruction analyzedInstruction, EnumSet validCategories) {
        ThreeRegisterInstruction instruction = (ThreeRegisterInstruction)analyzedInstruction.instruction;

        getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterB(), validCategories);
        getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterC(), validCategories);

        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction,
                RegisterType.getRegisterType(RegisterType.Category.Byte, null));
    }

    private void handleIfEqNe(AnalyzedInstruction analyzedInstruction) {
        TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;

        RegisterType registerType1 = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterA());
        assert registerType1 != null;

        RegisterType registerType2 = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterB());
        assert registerType2 != null;

        if (!(
                (ReferenceCategories.contains(registerType1.category) &&
                ReferenceCategories.contains(registerType2.category))
                    ||
                (Primitive32BitCategories.contains(registerType1.category) &&
                Primitive32BitCategories.contains(registerType2.category))
              )) {

            throw new ValidationException(String.format("%s cannot be used on registers of dissimilar types %s and " +
                    "%s. They must both be a reference type or a primitive 32 bit type.",
                    analyzedInstruction.instruction.opcode.name, registerType1.toString(), registerType2.toString()));
        }
    }

    private void handleIf(AnalyzedInstruction analyzedInstruction) {
        TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;

        getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterA(), Primitive32BitCategories);
        getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterB(), Primitive32BitCategories);
    }

    private void handleIfEqzNez(AnalyzedInstruction analyzedInstruction) {
        SingleRegisterInstruction instruction = (SingleRegisterInstruction)analyzedInstruction.instruction;

        getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterA(),
                ReferenceAndPrimitive32BitCategories);
    }

    private void handleIfz(AnalyzedInstruction analyzedInstruction) {
        SingleRegisterInstruction instruction = (SingleRegisterInstruction)analyzedInstruction.instruction;

        getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterA(), Primitive32BitCategories);
    }

    private void handle32BitPrimitiveAget(AnalyzedInstruction analyzedInstruction,
                                             RegisterType.Category instructionCategory) {
        ThreeRegisterInstruction instruction = (ThreeRegisterInstruction)analyzedInstruction.instruction;

        getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterC(), Primitive32BitCategories);

        RegisterType arrayRegisterType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterB());
        assert arrayRegisterType != null;

        if (arrayRegisterType.category != RegisterType.Category.Null) {
            if (arrayRegisterType.category != RegisterType.Category.Reference) {
                throw new ValidationException(String.format("Cannot use %s with non-array type %s",
                        analyzedInstruction.instruction.opcode.name, arrayRegisterType.category.toString()));
            }

            assert arrayRegisterType.type != null;
            if (arrayRegisterType.type.getClassType().charAt(0) != '[') {
                throw new ValidationException(String.format("Cannot use %s with non-array type %s",
                        analyzedInstruction.instruction.opcode.name, arrayRegisterType.type.getClassType()));
            }

            assert arrayRegisterType.type instanceof ClassPath.ArrayClassDef;
            ClassPath.ArrayClassDef arrayClassDef = (ClassPath.ArrayClassDef)arrayRegisterType.type;

            if (arrayClassDef.getArrayDimensions() != 1) {
                throw new ValidationException(String.format("Cannot use %s with multi-dimensional array type %s",
                        analyzedInstruction.instruction.opcode.name, arrayRegisterType.type.getClassType()));
            }

            RegisterType arrayBaseType =
                    RegisterType.getRegisterTypeForType(arrayClassDef.getBaseElementClass().getClassType());
            if (!checkArrayFieldAssignment(arrayBaseType.category, instructionCategory)) {
                throw new ValidationException(String.format("Cannot use %s with array type %s. Incorrect array type " +
                        "for the instruction.", analyzedInstruction.instruction.opcode.name,
                        arrayRegisterType.type.getClassType()));
            }
        }

        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction,
                RegisterType.getRegisterType(instructionCategory, null));
    }

    private void handleAgetWide(AnalyzedInstruction analyzedInstruction) {
        ThreeRegisterInstruction instruction = (ThreeRegisterInstruction)analyzedInstruction.instruction;

        getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterC(), Primitive32BitCategories);

        RegisterType arrayRegisterType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterB());
        assert arrayRegisterType != null;

        if (arrayRegisterType.category != RegisterType.Category.Null) {
            if (arrayRegisterType.category != RegisterType.Category.Reference) {
                throw new ValidationException(String.format("Cannot use aget-wide with non-array type %s",
                        arrayRegisterType.category.toString()));
            }

            assert arrayRegisterType.type != null;
            if (arrayRegisterType.type.getClassType().charAt(0) != '[') {
                throw new ValidationException(String.format("Cannot use aget-wide with non-array type %s",
                        arrayRegisterType.type.getClassType()));
            }

            assert arrayRegisterType.type instanceof ClassPath.ArrayClassDef;
            ClassPath.ArrayClassDef arrayClassDef = (ClassPath.ArrayClassDef)arrayRegisterType.type;

            if (arrayClassDef.getArrayDimensions() != 1) {
                throw new ValidationException(String.format("Cannot use aget-wide with multi-dimensional array type %s",
                        arrayRegisterType.type.getClassType()));
            }

            char arrayBaseType = arrayClassDef.getBaseElementClass().getClassType().charAt(0);
            if (arrayBaseType == 'J') {
                setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction,
                        RegisterType.getRegisterType(RegisterType.Category.LongLo, null));
            } else if (arrayBaseType == 'D') {
                setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction,
                        RegisterType.getRegisterType(RegisterType.Category.DoubleLo, null));
            } else {
                throw new ValidationException(String.format("Cannot use aget-wide with array type %s. Incorrect " +
                        "array type for the instruction.", arrayRegisterType.type.getClassType()));
            }
        } else {
            setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction,
                        RegisterType.getRegisterType(RegisterType.Category.LongLo, null));
        }
    }

    private void handleAgetObject(AnalyzedInstruction analyzedInstruction) {
        ThreeRegisterInstruction instruction = (ThreeRegisterInstruction)analyzedInstruction.instruction;

        getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterC(), Primitive32BitCategories);

        RegisterType arrayRegisterType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterB());
        assert arrayRegisterType != null;

        if (arrayRegisterType.category != RegisterType.Category.Null) {
            if (arrayRegisterType.category != RegisterType.Category.Reference) {
                throw new ValidationException(String.format("Cannot use aget-object with non-array type %s",
                        arrayRegisterType.category.toString()));
            }

            assert arrayRegisterType.type != null;
            if (arrayRegisterType.type.getClassType().charAt(0) != '[') {
                throw new ValidationException(String.format("Cannot use aget-object with non-array type %s",
                        arrayRegisterType.type.getClassType()));
            }

            assert arrayRegisterType.type instanceof ClassPath.ArrayClassDef;
            ClassPath.ArrayClassDef arrayClassDef = (ClassPath.ArrayClassDef)arrayRegisterType.type;

            ClassPath.ClassDef elementClassDef = arrayClassDef.getImmediateElementClass();
            char elementTypePrefix = elementClassDef.getClassType().charAt(0);
            if (elementTypePrefix != 'L' && elementTypePrefix != '[') {
                throw new ValidationException(String.format("Cannot use aget-object with array type %s. Incorrect " +
                        "array type for the instruction.", arrayRegisterType.type.getClassType()));
            }

            setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction,
                    RegisterType.getRegisterType(RegisterType.Category.Reference, elementClassDef));
        } else {
            setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction,
                    RegisterType.getRegisterType(RegisterType.Category.Null, null));
        }
    }

    private void handle32BitPrimitiveAput(AnalyzedInstruction analyzedInstruction,
                                             RegisterType.Category instructionCategory) {
        ThreeRegisterInstruction instruction = (ThreeRegisterInstruction)analyzedInstruction.instruction;

        getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterC(), Primitive32BitCategories);

        RegisterType sourceRegisterType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterA());
        assert sourceRegisterType != null;
        RegisterType instructionRegisterType = RegisterType.getRegisterType(instructionCategory, null);
        if (!sourceRegisterType.canBeAssignedTo(instructionRegisterType)) {
            throw new ValidationException(String.format("Cannot use %s with source register type %s.",
                    analyzedInstruction.instruction.opcode.name, sourceRegisterType.toString()));
        }


        RegisterType arrayRegisterType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterB());
        assert arrayRegisterType != null;

        if (arrayRegisterType.category != RegisterType.Category.Null) {
            if (arrayRegisterType.category != RegisterType.Category.Reference) {
                throw new ValidationException(String.format("Cannot use %s with non-array type %s",
                        analyzedInstruction.instruction.opcode.name, arrayRegisterType.category.toString()));
            }

            assert arrayRegisterType.type != null;
            if (arrayRegisterType.type.getClassType().charAt(0) != '[') {
                throw new ValidationException(String.format("Cannot use %s with non-array type %s",
                        analyzedInstruction.instruction.opcode.name, arrayRegisterType.type.getClassType()));
            }

            assert arrayRegisterType.type instanceof ClassPath.ArrayClassDef;
            ClassPath.ArrayClassDef arrayClassDef = (ClassPath.ArrayClassDef)arrayRegisterType.type;

            if (arrayClassDef.getArrayDimensions() != 1) {
                throw new ValidationException(String.format("Cannot use %s with multi-dimensional array type %s",
                        analyzedInstruction.instruction.opcode.name, arrayRegisterType.type.getClassType()));
            }

            RegisterType arrayBaseType =
                    RegisterType.getRegisterTypeForType(arrayClassDef.getBaseElementClass().getClassType());
            if (!checkArrayFieldAssignment(arrayBaseType.category, instructionCategory)) {
                throw new ValidationException(String.format("Cannot use %s with array type %s. Incorrect array type " +
                        "for the instruction.", analyzedInstruction.instruction.opcode.name,
                        arrayRegisterType.type.getClassType()));
            }
        }
    }

    private void handleAputWide(AnalyzedInstruction analyzedInstruction) {
        ThreeRegisterInstruction instruction = (ThreeRegisterInstruction)analyzedInstruction.instruction;

        getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterC(), Primitive32BitCategories);
        getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterA(), WideLowCategories);

        RegisterType arrayRegisterType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterB());
        assert arrayRegisterType != null;

        if (arrayRegisterType.category != RegisterType.Category.Null) {
            if (arrayRegisterType.category != RegisterType.Category.Reference) {
                throw new ValidationException(String.format("Cannot use aput-wide with non-array type %s",
                        arrayRegisterType.category.toString()));
            }

            assert arrayRegisterType.type != null;
            if (arrayRegisterType.type.getClassType().charAt(0) != '[') {
                throw new ValidationException(String.format("Cannot use aput-wide with non-array type %s",
                        arrayRegisterType.type.getClassType()));
            }

            assert arrayRegisterType.type instanceof ClassPath.ArrayClassDef;
            ClassPath.ArrayClassDef arrayClassDef = (ClassPath.ArrayClassDef)arrayRegisterType.type;

            if (arrayClassDef.getArrayDimensions() != 1) {
                throw new ValidationException(String.format("Cannot use aput-wide with multi-dimensional array type %s",
                        arrayRegisterType.type.getClassType()));
            }

            char arrayBaseType = arrayClassDef.getBaseElementClass().getClassType().charAt(0);
            if (arrayBaseType != 'J' && arrayBaseType != 'D') {
                throw new ValidationException(String.format("Cannot use aput-wide with array type %s. Incorrect " +
                        "array type for the instruction.", arrayRegisterType.type.getClassType()));
            }
        }
    }

    private void handleAputObject(AnalyzedInstruction analyzedInstruction) {
        ThreeRegisterInstruction instruction = (ThreeRegisterInstruction)analyzedInstruction.instruction;

        getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterC(), Primitive32BitCategories);

        RegisterType sourceRegisterType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterA());
        assert sourceRegisterType != null;

        //TODO: ensure sourceRegisterType is a Reference type?

        RegisterType arrayRegisterType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterB());
        assert arrayRegisterType != null;

        if (arrayRegisterType.category != RegisterType.Category.Null) {
            //don't check the source type against the array type, just make sure it is an array of reference types

            if (arrayRegisterType.category != RegisterType.Category.Reference) {
                throw new ValidationException(String.format("Cannot use aget-object with non-array type %s",
                        arrayRegisterType.category.toString()));
            }

            assert arrayRegisterType.type != null;
            if (arrayRegisterType.type.getClassType().charAt(0) != '[') {
                throw new ValidationException(String.format("Cannot use aget-object with non-array type %s",
                        arrayRegisterType.type.getClassType()));
            }

            assert arrayRegisterType.type instanceof ClassPath.ArrayClassDef;
            ClassPath.ArrayClassDef arrayClassDef = (ClassPath.ArrayClassDef)arrayRegisterType.type;

            ClassPath.ClassDef elementClassDef = arrayClassDef.getImmediateElementClass();
            char elementTypePrefix = elementClassDef.getClassType().charAt(0);
            if (elementTypePrefix != 'L' && elementTypePrefix != '[') {
                throw new ValidationException(String.format("Cannot use aget-object with array type %s. Incorrect " +
                        "array type for the instruction.", arrayRegisterType.type.getClassType()));
            }
        }
    }

    private void handle32BitPrimitiveIget(AnalyzedInstruction analyzedInstruction,
                                             RegisterType.Category instructionCategory) {
        TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;

        RegisterType objectRegisterType = getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterB(),
                ReferenceOrUninitThisCategories);

        //TODO: check access
        Item referencedItem = ((InstructionWithReference)analyzedInstruction.instruction).getReferencedItem();
        assert referencedItem instanceof FieldIdItem;
        FieldIdItem field = (FieldIdItem)referencedItem;

        if (objectRegisterType.category != RegisterType.Category.Null &&
            !objectRegisterType.type.extendsClass(ClassPath.getClassDef(field.getContainingClass()))) {
            throw new ValidationException(String.format("Cannot access field %s through type %s",
                    field.getFieldString(), objectRegisterType.type.getClassType()));
        }

        RegisterType fieldType = RegisterType.getRegisterTypeForTypeIdItem(field.getFieldType());

        if (!checkArrayFieldAssignment(fieldType.category, instructionCategory)) {
                throw new ValidationException(String.format("Cannot use %s with field %s. Incorrect field type " +
                        "for the instruction.", analyzedInstruction.instruction.opcode.name,
                        field.getFieldString()));
        }

        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction,
                RegisterType.getRegisterType(instructionCategory, null));
    }

    private void handleIgetWide(AnalyzedInstruction analyzedInstruction) {
        TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;

        RegisterType objectRegisterType = getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterB(),
                ReferenceOrUninitThisCategories);

        //TODO: check access
        Item referencedItem = ((InstructionWithReference)analyzedInstruction.instruction).getReferencedItem();
        assert referencedItem instanceof FieldIdItem;
        FieldIdItem field = (FieldIdItem)referencedItem;

        if (objectRegisterType.category != RegisterType.Category.Null &&
            !objectRegisterType.type.extendsClass(ClassPath.getClassDef(field.getContainingClass()))) {
            throw new ValidationException(String.format("Cannot access field %s through type %s",
                    field.getFieldString(), objectRegisterType.type.getClassType()));
        }

        RegisterType fieldType = RegisterType.getRegisterTypeForTypeIdItem(field.getFieldType());

        if (!WideLowCategories.contains(fieldType.category)) {
            throw new ValidationException(String.format("Cannot use %s with field %s. Incorrect field type " +
                    "for the instruction.", analyzedInstruction.instruction.opcode.name,
                    field.getFieldString()));
        }

        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, fieldType);
    }

    private void handleIgetObject(AnalyzedInstruction analyzedInstruction) {
        TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;

        RegisterType objectRegisterType = getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterB(),
                ReferenceOrUninitThisCategories);

        //TODO: check access
        Item referencedItem = ((InstructionWithReference)analyzedInstruction.instruction).getReferencedItem();
        assert referencedItem instanceof FieldIdItem;
        FieldIdItem field = (FieldIdItem)referencedItem;

        if (objectRegisterType.category != RegisterType.Category.Null &&
            !objectRegisterType.type.extendsClass(ClassPath.getClassDef(field.getContainingClass()))) {
            throw new ValidationException(String.format("Cannot access field %s through type %s",
                    field.getFieldString(), objectRegisterType.type.getClassType()));
        }

        RegisterType fieldType = RegisterType.getRegisterTypeForTypeIdItem(field.getFieldType());

        if (fieldType.category != RegisterType.Category.Reference) {
            throw new ValidationException(String.format("Cannot use %s with field %s. Incorrect field type " +
                        "for the instruction.", analyzedInstruction.instruction.opcode.name,
                        field.getFieldString()));
        }

        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, fieldType);
    }

    private void handle32BitPrimitiveIput(AnalyzedInstruction analyzedInstruction,
                                             RegisterType.Category instructionCategory) {
        TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;

        RegisterType objectRegisterType = getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterB(),
                ReferenceOrUninitThisCategories);

        RegisterType sourceRegisterType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterA());
        assert sourceRegisterType != null;

        //per CodeVerify.c in dalvik:
        //java generates synthetic functions that write byte values into boolean fields
        if (sourceRegisterType.category == RegisterType.Category.Byte &&
            instructionCategory == RegisterType.Category.Boolean) {

            sourceRegisterType = RegisterType.getRegisterType(RegisterType.Category.Boolean, null);
        }

        RegisterType instructionRegisterType = RegisterType.getRegisterType(instructionCategory, null);
        if (!sourceRegisterType.canBeAssignedTo(instructionRegisterType)) {
            throw new ValidationException(String.format("Cannot use %s with source register type %s.",
                    analyzedInstruction.instruction.opcode.name, sourceRegisterType.toString()));
        }


        //TODO: check access
        Item referencedItem = ((InstructionWithReference)analyzedInstruction.instruction).getReferencedItem();
        assert referencedItem instanceof FieldIdItem;
        FieldIdItem field = (FieldIdItem)referencedItem;

        if (objectRegisterType.category != RegisterType.Category.Null &&
            !objectRegisterType.type.extendsClass(ClassPath.getClassDef(field.getContainingClass()))) {
            throw new ValidationException(String.format("Cannot access field %s through type %s",
                    field.getFieldString(), objectRegisterType.type.getClassType()));
        }

        RegisterType fieldType = RegisterType.getRegisterTypeForTypeIdItem(field.getFieldType());

        if (!checkArrayFieldAssignment(fieldType.category, instructionCategory)) {
                throw new ValidationException(String.format("Cannot use %s with field %s. Incorrect field type " +
                        "for the instruction.", analyzedInstruction.instruction.opcode.name,
                        field.getFieldString()));
        }
    }

    private void handleIputWide(AnalyzedInstruction analyzedInstruction) {
        TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;

        RegisterType objectRegisterType = getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterB(),
                ReferenceOrUninitThisCategories);

        getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterA(), WideLowCategories);

        //TODO: check access
        Item referencedItem = ((InstructionWithReference)analyzedInstruction.instruction).getReferencedItem();
        assert referencedItem instanceof FieldIdItem;
        FieldIdItem field = (FieldIdItem)referencedItem;

        if (objectRegisterType.category != RegisterType.Category.Null &&
                !objectRegisterType.type.extendsClass(ClassPath.getClassDef(field.getContainingClass()))) {
            throw new ValidationException(String.format("Cannot access field %s through type %s",
                    field.getFieldString(), objectRegisterType.type.getClassType()));
        }

        RegisterType fieldType = RegisterType.getRegisterTypeForTypeIdItem(field.getFieldType());

        if (!WideLowCategories.contains(fieldType.category)) {
            throw new ValidationException(String.format("Cannot use %s with field %s. Incorrect field type " +
                    "for the instruction.", analyzedInstruction.instruction.opcode.name,
                    field.getFieldString()));
        }
    }

    private void handleIputObject(AnalyzedInstruction analyzedInstruction) {
        TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;

        RegisterType objectRegisterType = getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterB(),
                ReferenceOrUninitThisCategories);

        RegisterType sourceRegisterType = getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterA(),
                ReferenceCategories);

        //TODO: check access
        Item referencedItem = ((InstructionWithReference)analyzedInstruction.instruction).getReferencedItem();
        assert referencedItem instanceof FieldIdItem;
        FieldIdItem field = (FieldIdItem)referencedItem;

        if (objectRegisterType.category != RegisterType.Category.Null &&
            !objectRegisterType.type.extendsClass(ClassPath.getClassDef(field.getContainingClass()))) {
            throw new ValidationException(String.format("Cannot access field %s through type %s",
                    field.getFieldString(), objectRegisterType.type.getClassType()));
        }

        RegisterType fieldType = RegisterType.getRegisterTypeForTypeIdItem(field.getFieldType());

        if (fieldType.category != RegisterType.Category.Reference) {
            throw new ValidationException(String.format("Cannot use %s with field %s. Incorrect field type " +
                        "for the instruction.", analyzedInstruction.instruction.opcode.name,
                        field.getFieldString()));
        }

        if (sourceRegisterType.category != RegisterType.Category.Null &&
            !fieldType.type.isInterface() &&
            !sourceRegisterType.type.extendsClass(fieldType.type)) {

            throw new ValidationException(String.format("Cannot store a value of type %s into a field of type %s",
                    sourceRegisterType.type.getClassType(), fieldType.type.getClassType()));
        }
    }

    private void handle32BitPrimitiveSget(AnalyzedInstruction analyzedInstruction,
                                             RegisterType.Category instructionCategory) {
        //TODO: check access
        Item referencedItem = ((InstructionWithReference)analyzedInstruction.instruction).getReferencedItem();
        assert referencedItem instanceof FieldIdItem;
        FieldIdItem field = (FieldIdItem)referencedItem;

        RegisterType fieldType = RegisterType.getRegisterTypeForTypeIdItem(field.getFieldType());

        if (!checkArrayFieldAssignment(fieldType.category, instructionCategory)) {
                throw new ValidationException(String.format("Cannot use %s with field %s. Incorrect field type " +
                        "for the instruction.", analyzedInstruction.instruction.opcode.name,
                        field.getFieldString()));
        }

        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction,
                RegisterType.getRegisterType(instructionCategory, null));
    }

    private void handleSgetWide(AnalyzedInstruction analyzedInstruction) {
        //TODO: check access
        Item referencedItem = ((InstructionWithReference)analyzedInstruction.instruction).getReferencedItem();
        assert referencedItem instanceof FieldIdItem;
        FieldIdItem field = (FieldIdItem)referencedItem;

        RegisterType fieldType = RegisterType.getRegisterTypeForTypeIdItem(field.getFieldType());


        if (fieldType.category != RegisterType.Category.LongLo &&
            fieldType.category != RegisterType.Category.DoubleLo) {

            throw new ValidationException(String.format("Cannot use %s with field %s. Incorrect field type " +
                    "for the instruction.", analyzedInstruction.instruction.opcode.name,
                    field.getFieldString()));
        }

        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, fieldType);
    }

    private void handleSgetObject(AnalyzedInstruction analyzedInstruction) {
        //TODO: check access
        Item referencedItem = ((InstructionWithReference)analyzedInstruction.instruction).getReferencedItem();
        assert referencedItem instanceof FieldIdItem;
        FieldIdItem field = (FieldIdItem)referencedItem;

        RegisterType fieldType = RegisterType.getRegisterTypeForTypeIdItem(field.getFieldType());

        if (fieldType.category != RegisterType.Category.Reference) {
                throw new ValidationException(String.format("Cannot use %s with field %s. Incorrect field type " +
                        "for the instruction.", analyzedInstruction.instruction.opcode.name,
                        field.getFieldString()));
        }

        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, fieldType);
    }

    private void handle32BitPrimitiveSput(AnalyzedInstruction analyzedInstruction,
                                             RegisterType.Category instructionCategory) {
        SingleRegisterInstruction instruction = (SingleRegisterInstruction)analyzedInstruction.instruction;

        RegisterType sourceRegisterType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterA());
        assert sourceRegisterType != null;

        //per CodeVerify.c in dalvik:
        //java generates synthetic functions that write byte values into boolean fields
        if (sourceRegisterType.category == RegisterType.Category.Byte &&
            instructionCategory == RegisterType.Category.Boolean) {

            sourceRegisterType = RegisterType.getRegisterType(RegisterType.Category.Boolean, null);
        }

        RegisterType instructionRegisterType = RegisterType.getRegisterType(instructionCategory, null);
        if (!sourceRegisterType.canBeAssignedTo(instructionRegisterType)) {
            throw new ValidationException(String.format("Cannot use %s with source register type %s.",
                    analyzedInstruction.instruction.opcode.name, sourceRegisterType.toString()));
        }

        //TODO: check access
        Item referencedItem = ((InstructionWithReference)analyzedInstruction.instruction).getReferencedItem();
        assert referencedItem instanceof FieldIdItem;
        FieldIdItem field = (FieldIdItem)referencedItem;

        RegisterType fieldType = RegisterType.getRegisterTypeForTypeIdItem(field.getFieldType());

        if (!checkArrayFieldAssignment(fieldType.category, instructionCategory)) {
                throw new ValidationException(String.format("Cannot use %s with field %s. Incorrect field type " +
                        "for the instruction.", analyzedInstruction.instruction.opcode.name,
                        field.getFieldString()));
        }
    }

    private void handleSputWide(AnalyzedInstruction analyzedInstruction) {
        SingleRegisterInstruction instruction = (SingleRegisterInstruction)analyzedInstruction.instruction;


        getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterA(), WideLowCategories);

        //TODO: check access
        Item referencedItem = ((InstructionWithReference)analyzedInstruction.instruction).getReferencedItem();
        assert referencedItem instanceof FieldIdItem;
        FieldIdItem field = (FieldIdItem)referencedItem;

        RegisterType fieldType = RegisterType.getRegisterTypeForTypeIdItem(field.getFieldType());

        if (!WideLowCategories.contains(fieldType.category)) {
                throw new ValidationException(String.format("Cannot use %s with field %s. Incorrect field type " +
                        "for the instruction.", analyzedInstruction.instruction.opcode.name,
                        field.getFieldString()));
        }
    }

    private void handleSputObject(AnalyzedInstruction analyzedInstruction) {
        SingleRegisterInstruction instruction = (SingleRegisterInstruction)analyzedInstruction.instruction;

        RegisterType sourceRegisterType = getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterA(),
                ReferenceCategories);

        //TODO: check access
        Item referencedItem = ((InstructionWithReference)analyzedInstruction.instruction).getReferencedItem();
        assert referencedItem instanceof FieldIdItem;
        FieldIdItem field = (FieldIdItem)referencedItem;

        RegisterType fieldType = RegisterType.getRegisterTypeForTypeIdItem(field.getFieldType());

        if (fieldType.category != RegisterType.Category.Reference) {
            throw new ValidationException(String.format("Cannot use %s with field %s. Incorrect field type " +
                        "for the instruction.", analyzedInstruction.instruction.opcode.name,
                        field.getFieldString()));
        }

        if (sourceRegisterType.category != RegisterType.Category.Null &&
            !fieldType.type.isInterface() &&
            !sourceRegisterType.type.extendsClass(fieldType.type)) {

            throw new ValidationException(String.format("Cannot store a value of type %s into a field of type %s",
                    sourceRegisterType.type.getClassType(), fieldType.type.getClassType()));
        }
    }

    private void handleInvoke(AnalyzedInstruction analyzedInstruction, int invokeType) {
        FiveRegisterInstruction instruction = (FiveRegisterInstruction)analyzedInstruction.instruction;
        handleInvokeCommon(analyzedInstruction, false, invokeType, new Format35cRegisterIterator(instruction));
    }

    private void handleInvokeRange(AnalyzedInstruction analyzedInstruction, int invokeType) {
        RegisterRangeInstruction instruction = (RegisterRangeInstruction)analyzedInstruction.instruction;
        handleInvokeCommon(analyzedInstruction, true, invokeType, new Format3rcRegisterIterator(instruction));
    }

    private static final int INVOKE_VIRTUAL = 0x01;
    private static final int INVOKE_SUPER = 0x02;
    private static final int INVOKE_DIRECT = 0x04;
    private static final int INVOKE_INTERFACE = 0x08;
    private static final int INVOKE_STATIC = 0x10;

    private void handleInvokeCommon(AnalyzedInstruction analyzedInstruction, boolean isRange, int invokeType,
                                       RegisterIterator registers) {
        InstructionWithReference instruction = (InstructionWithReference)analyzedInstruction.instruction;

        //TODO: check access

        Item item = instruction.getReferencedItem();
        assert item.getItemType() == ItemType.TYPE_METHOD_ID_ITEM;
        MethodIdItem methodIdItem = (MethodIdItem)item;

        TypeIdItem methodClass = methodIdItem.getContainingClass();
        boolean isInit = false;

        if (methodIdItem.getMethodName().getStringValue().charAt(0) == '<') {
            if ((invokeType & INVOKE_DIRECT) != 0) {
                isInit = true;
            } else {
                throw new ValidationException(String.format("Cannot call constructor %s with %s",
                        methodIdItem.getMethodString(), analyzedInstruction.instruction.opcode.name));
            }
        }

        ClassPath.ClassDef methodClassDef = ClassPath.getClassDef(methodClass);
        if ((invokeType & INVOKE_INTERFACE) != 0) {
            if (!methodClassDef.isInterface()) {
                throw new ValidationException(String.format("Cannot call method %s with %s. %s is not an interface " +
                        "class.", methodIdItem.getMethodString(), analyzedInstruction.instruction.opcode.name,
                        methodClassDef.getClassType()));
            }
        } else {
            if (methodClassDef.isInterface()) {
                throw new ValidationException(String.format("Cannot call method %s with %s. %s is an interface class." +
                        " Use invoke-interface or invoke-interface/range instead.", methodIdItem.getMethodString(),
                        analyzedInstruction.instruction.opcode.name, methodClassDef.getClassType()));
            }
        }

        if ((invokeType & INVOKE_SUPER) != 0) {
            ClassPath.ClassDef currentMethodClassDef = ClassPath.getClassDef(encodedMethod.method.getContainingClass());
            if (currentMethodClassDef.getSuperclass() == null) {
                throw new ValidationException(String.format("Cannot call method %s with %s. %s has no superclass",
                        methodIdItem.getMethodString(), analyzedInstruction.instruction.opcode.name,
                        methodClassDef.getSuperclass().getClassType()));
            }

            if (!currentMethodClassDef.getSuperclass().extendsClass(methodClassDef)) {
                throw new ValidationException(String.format("Cannot call method %s with %s. %s is not an ancestor " +
                        "of the current class %s", methodIdItem.getMethodString(),
                        analyzedInstruction.instruction.opcode.name, methodClass.getTypeDescriptor(),
                        encodedMethod.method.getContainingClass().getTypeDescriptor()));
            }

            if (!currentMethodClassDef.getSuperclass().hasVirtualMethod(methodIdItem.getVirtualMethodString())) {
                throw new ValidationException(String.format("Cannot call method %s with %s. The superclass %s has" +
                        "no such method", methodIdItem.getMethodString(),
                        analyzedInstruction.instruction.opcode.name, methodClassDef.getSuperclass().getClassType()));
            }
        }

        assert isRange || registers.getCount() <= 5;

        TypeListItem typeListItem = methodIdItem.getPrototype().getParameters();
        int methodParameterRegisterCount;
        if (typeListItem == null) {
            methodParameterRegisterCount = 0;
        } else {
            methodParameterRegisterCount = typeListItem.getRegisterCount();
        }

        if ((invokeType & INVOKE_STATIC) == 0) {
            methodParameterRegisterCount++;
        }

        if (methodParameterRegisterCount != registers.getCount()) {
            throw new ValidationException(String.format("The number of registers does not match the number of " +
                    "parameters for method %s. Expecting %d registers, got %d.", methodIdItem.getMethodString(),
                    methodParameterRegisterCount + 1, registers.getCount()));
        }

        RegisterType objectRegisterType = null;
        int objectRegister = 0;
        if ((invokeType & INVOKE_STATIC) == 0) {
            objectRegister = registers.getRegister();
            registers.moveNext();

            objectRegisterType = analyzedInstruction.getPreInstructionRegisterType(objectRegister);
            assert objectRegisterType != null;
            if (objectRegisterType.category == RegisterType.Category.UninitRef ||
                    objectRegisterType.category == RegisterType.Category.UninitThis) {

                if (!isInit) {
                    throw new ValidationException(String.format("Cannot invoke non-<init> method %s on uninitialized " +
                            "reference type %s", methodIdItem.getMethodString(),
                            objectRegisterType.type.getClassType()));
                }
            } else if (objectRegisterType.category == RegisterType.Category.Reference) {
                if (isInit) {
                    throw new ValidationException(String.format("Cannot invoke %s on initialized reference type %s",
                            methodIdItem.getMethodString(), objectRegisterType.type.getClassType()));
                }
            } else if (objectRegisterType.category == RegisterType.Category.Null) {
                if (isInit) {
                    throw new ValidationException(String.format("Cannot invoke %s on a null reference",
                            methodIdItem.getMethodString()));
                }
            }
            else {
                throw new ValidationException(String.format("Cannot invoke %s on non-reference type %s",
                        methodIdItem.getMethodString(), objectRegisterType.toString()));
            }

            if (isInit) {
                if (objectRegisterType.type.getSuperclass() == methodClassDef) {
                    if (!encodedMethod.method.getMethodName().getStringValue().equals("<init>")) {
                        throw new ValidationException(String.format("Cannot call %s on type %s. The object type must " +
                                "match the method type exactly", methodIdItem.getMethodString(),
                                objectRegisterType.type.getClassType()));
                    }
                }
            }

            if ((invokeType & INVOKE_INTERFACE) == 0 && objectRegisterType.category != RegisterType.Category.Null &&
                    !objectRegisterType.type.extendsClass(methodClassDef)) {

               throw new ValidationException(String.format("Cannot call method %s on an object of type %s, which " +
                       "does not extend %s.", methodIdItem.getMethodString(), objectRegisterType.type.getClassType(),
                        methodClassDef.getClassType()));
            }
        }

        if (typeListItem != null) {
            List<TypeIdItem> parameterTypes = typeListItem.getTypes();
            int parameterTypeIndex = 0;
            while (!registers.pastEnd()) {
                assert parameterTypeIndex < parameterTypes.size();
                RegisterType parameterType =
                        RegisterType.getRegisterTypeForTypeIdItem(parameterTypes.get(parameterTypeIndex));

                int register = registers.getRegister();

                RegisterType parameterRegisterType;
                if (WideLowCategories.contains(parameterType.category)) {
                    parameterRegisterType = getAndCheckSourceRegister(analyzedInstruction, register, WideLowCategories);

                    if (!registers.moveNext()) {
                        throw new ValidationException(String.format("No 2nd register specified for wide register pair v%d",
                                parameterTypeIndex+1));
                    }
                    int nextRegister = registers.getRegister();

                    if (nextRegister != register + 1) {
                        throw new ValidationException(String.format("Invalid wide register pair (v%d, v%d). Registers " +
                                "must be consecutive.", register, nextRegister));
                    }
                } else {
                    parameterRegisterType = analyzedInstruction.getPreInstructionRegisterType(register);
                }

                assert parameterRegisterType != null;

                if (!parameterRegisterType.canBeAssignedTo(parameterType)) {
                    throw new ValidationException(
                            String.format("Invalid register type %s for parameter %d %s.",
                                    parameterRegisterType.toString(), parameterTypeIndex+1,
                                    parameterType.toString()));
                }

                parameterTypeIndex++;
                registers.moveNext();
            }
        }


        //TODO: need to ensure the "this" register is initialized, in a constructor method
        if (isInit) {
            setPostRegisterTypeAndPropagateChanges(analyzedInstruction, objectRegister,
                    RegisterType.getRegisterType(RegisterType.Category.Reference, objectRegisterType.type));

            for (int i=0; i<analyzedInstruction.postRegisterMap.length; i++) {
                RegisterType postInstructionRegisterType = analyzedInstruction.postRegisterMap[i];
                if (postInstructionRegisterType.category == RegisterType.Category.Unknown) {
                    RegisterType preInstructionRegisterType =
                            analyzedInstruction.getPreInstructionRegisterType(i);

                    if (preInstructionRegisterType.category == RegisterType.Category.UninitRef ||
                        preInstructionRegisterType.category == RegisterType.Category.UninitThis) {

                        RegisterType registerType;
                        if (preInstructionRegisterType == objectRegisterType) {
                            registerType = analyzedInstruction.postRegisterMap[objectRegister];
                        } else {
                            registerType = preInstructionRegisterType;
                        }

                        setPostRegisterTypeAndPropagateChanges(analyzedInstruction, i, registerType);
                    }
                }
            }
        }
    }

    private void handleUnaryOp(AnalyzedInstruction analyzedInstruction, EnumSet validSourceCategories,
                            RegisterType.Category destRegisterCategory) {
        TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;

        getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterB(), validSourceCategories);

        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction,
                RegisterType.getRegisterType(destRegisterCategory, null));
    }

    private void handleBinaryOp(AnalyzedInstruction analyzedInstruction, EnumSet validSource1Categories,
                                EnumSet validSource2Categories, RegisterType.Category destRegisterCategory,
                                boolean checkForBoolean) {
        ThreeRegisterInstruction instruction = (ThreeRegisterInstruction)analyzedInstruction.instruction;

        RegisterType source1RegisterType = getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterB(),
                validSource1Categories);
        RegisterType source2RegisterType = getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterC(),
                validSource2Categories);

        if (checkForBoolean) {
            if (BooleanCategories.contains(source1RegisterType.category) &&
                BooleanCategories.contains(source2RegisterType.category)) {

                destRegisterCategory = RegisterType.Category.Boolean;
            }
        }

        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction,
                RegisterType.getRegisterType(destRegisterCategory, null));
    }

    private void handleBinary2AddrOp(AnalyzedInstruction analyzedInstruction, EnumSet validSource1Categories,
                                EnumSet validSource2Categories, RegisterType.Category destRegisterCategory,
                                boolean checkForBoolean) {
        TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;

        RegisterType source1RegisterType = getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterA(),
                validSource1Categories);
        RegisterType source2RegisterType = getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterB(),
                validSource2Categories);

        if (checkForBoolean) {
            if (BooleanCategories.contains(source1RegisterType.category) &&
                BooleanCategories.contains(source2RegisterType.category)) {

                destRegisterCategory = RegisterType.Category.Boolean;
            }
        }

        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction,
                RegisterType.getRegisterType(destRegisterCategory, null));
    }

    private void handleLiteralBinaryOp(AnalyzedInstruction analyzedInstruction, EnumSet validSourceCategories,
                                RegisterType.Category destRegisterCategory, boolean checkForBoolean) {
        TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;

        RegisterType sourceRegisterType = getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterB(),
                validSourceCategories);

        if (checkForBoolean) {
            if (BooleanCategories.contains(sourceRegisterType.category)) {
                long literal = ((LiteralInstruction)analyzedInstruction.instruction).getLiteral();
                if (literal == 0 || literal == 1) {
                    destRegisterCategory = RegisterType.Category.Boolean;
                }
            }
        }

        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction,
                RegisterType.getRegisterType(destRegisterCategory, null));
    }

    private RegisterType.Category getDestTypeForLiteralShiftRight(AnalyzedInstruction analyzedInstruction,
                                                                  boolean signedShift) {
        TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;

        RegisterType sourceRegisterType = getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterB(),
                Primitive32BitCategories);
        long literalShift = ((LiteralInstruction)analyzedInstruction.instruction).getLiteral();

        if (literalShift == 0) {
            return sourceRegisterType.category;
        }

        RegisterType.Category destRegisterCategory;
        if (!signedShift) {
            destRegisterCategory = RegisterType.Category.Integer;
        } else {
            destRegisterCategory = sourceRegisterType.category;
        }

        if (literalShift >= 32) {
            //TODO: add warning
            return destRegisterCategory;
        }

        switch (sourceRegisterType.category) {
            case Integer:
            case Float:
                if (!signedShift) {
                    if (literalShift > 24) {
                        return RegisterType.Category.PosByte;
                    }
                    if (literalShift >= 16) {
                        return RegisterType.Category.Char;
                    }
                } else {
                    if (literalShift >= 24) {
                        return RegisterType.Category.Byte;
                    }
                    if (literalShift >= 16) {
                        return RegisterType.Category.Short;
                    }
                }
                break;
            case Short:
                if (signedShift && literalShift >= 8) {
                    return RegisterType.Category.Byte;
                }
                break;
            case PosShort:
                if (literalShift >= 8) {
                    return RegisterType.Category.PosByte;
                }
                break;
            case Char:
                if (literalShift > 8) {
                    return RegisterType.Category.PosByte;
                }
                break;
            case Byte:
                break;
            case PosByte:
                return RegisterType.Category.PosByte;
            case Null:
            case One:
            case Boolean:
                return RegisterType.Category.Null;
            default:
                assert false;
        }

        return destRegisterCategory;
    }


    private void handleExecuteInline(AnalyzedInstruction analyzedInstruction) {
        if (deodexUtil == null) {
            throw new ValidationException("Cannot analyze an odexed instruction unless we are deodexing");
        }

        Instruction35ms instruction = (Instruction35ms)analyzedInstruction.instruction;

        int methodIndex = instruction.getMethodIndex();
        DeodexUtil.InlineMethod inlineMethod = deodexUtil.lookupInlineMethod(methodIndex);
        MethodIdItem inlineMethodIdItem = inlineMethod.getMethodIdItem();
        if (inlineMethodIdItem == null) {
            throw new ValidationException(String.format("Cannot load inline method with index %d", methodIndex));
        }

        Opcode deodexedOpcode = null;
        switch (inlineMethod.methodType) {
            case DeodexUtil.Direct:
                deodexedOpcode = Opcode.INVOKE_DIRECT;
                break;
            case DeodexUtil.Static:
                deodexedOpcode = Opcode.INVOKE_STATIC;
                break;
            case DeodexUtil.Virtual:
                deodexedOpcode = Opcode.INVOKE_VIRTUAL;
                break;
            default:
                assert false;
        }

        Instruction35c deodexedInstruction = new Instruction35c(deodexedOpcode, instruction.getRegCount(),
                instruction.getRegisterD(), instruction.getRegisterE(), instruction.getRegisterF(),
                instruction.getRegisterG(), instruction.getRegisterA(), inlineMethodIdItem);

        analyzedInstruction.setDeodexedInstruction(deodexedInstruction);

        analyzeInstruction(analyzedInstruction);
    }

    private void handleExecuteInlineRange(AnalyzedInstruction analyzedInstruction) {
        if (deodexUtil == null) {
            throw new ValidationException("Cannot analyze an odexed instruction unless we are deodexing");
        }

        Instruction3rms instruction = (Instruction3rms)analyzedInstruction.instruction;

        int methodIndex = instruction.getMethodIndex();
        DeodexUtil.InlineMethod inlineMethod = deodexUtil.lookupInlineMethod(methodIndex);
        MethodIdItem inlineMethodIdItem = inlineMethod.getMethodIdItem();
        if (inlineMethodIdItem == null) {
            throw new ValidationException(String.format("Cannot load inline method with index %d", methodIndex));
        }

        Opcode deodexedOpcode = null;
        switch (inlineMethod.methodType) {
            case DeodexUtil.Direct:
                deodexedOpcode = Opcode.INVOKE_DIRECT;
                break;
            case DeodexUtil.Static:
                deodexedOpcode = Opcode.INVOKE_STATIC;
                break;
            case DeodexUtil.Virtual:
                deodexedOpcode = Opcode.INVOKE_VIRTUAL;
                break;
            default:
                assert false;
        }

        Instruction3rc deodexedInstruction = new Instruction3rc(deodexedOpcode, instruction.getRegCount(),
                instruction.getStartRegister(), inlineMethodIdItem);

        analyzedInstruction.setDeodexedInstruction(deodexedInstruction);

        analyzeInstruction(analyzedInstruction);
    }

    private void handleInvokeDirectEmpty(AnalyzedInstruction analyzedInstruction) {
        Instruction35s instruction = (Instruction35s)analyzedInstruction.instruction;

        Instruction35c deodexedInstruction = new Instruction35c(Opcode.INVOKE_DIRECT, instruction.getRegCount(),
                instruction.getRegisterD(), instruction.getRegisterE(), instruction.getRegisterF(),
                instruction.getRegisterG(), instruction.getRegisterA(), instruction.getReferencedItem());

        analyzedInstruction.setDeodexedInstruction(deodexedInstruction);

        analyzeInstruction(analyzedInstruction);
    }

    private boolean handleIputIgetQuick(AnalyzedInstruction analyzedInstruction, boolean isIput) {
        Instruction22cs instruction = (Instruction22cs)analyzedInstruction.instruction;

        int fieldOffset = instruction.getFieldOffset();
        RegisterType objectRegisterType = getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterB(),
                ReferenceOrUninitCategories);

        if (objectRegisterType.category == RegisterType.Category.Null) {
            return false;
        }

        FieldIdItem fieldIdItem = deodexUtil.lookupField(objectRegisterType.type, fieldOffset);
        if (fieldIdItem == null) {
            throw new ValidationException(String.format("Could not resolve the field in class %s at offset %d",
                    objectRegisterType.type.getClassType(), fieldOffset));
        }

        String fieldType = fieldIdItem.getFieldType().getTypeDescriptor();

        Opcode opcode = getAndCheckIgetIputOpcodeForType(fieldType, instruction.opcode, isIput);

        Instruction22c deodexedInstruction = new Instruction22c(opcode, (byte)instruction.getRegisterA(),
                (byte)instruction.getRegisterB(), fieldIdItem);
        analyzedInstruction.setDeodexedInstruction(deodexedInstruction);

        analyzeInstruction(analyzedInstruction);

        return true;
    }

    private boolean handleInvokeVirtualQuick(AnalyzedInstruction analyzedInstruction, boolean isSuper, boolean isRange) {
        int methodIndex;
        int objectRegister;


        if (isRange) {
            Instruction3rms instruction = (Instruction3rms)analyzedInstruction.instruction;
            methodIndex = instruction.getMethodIndex();
            objectRegister = instruction.getStartRegister();
        } else {
            Instruction35ms instruction = (Instruction35ms)analyzedInstruction.instruction;
            methodIndex = instruction.getMethodIndex();
            objectRegister = instruction.getRegisterD();
        }

        RegisterType objectRegisterType = getAndCheckSourceRegister(analyzedInstruction, objectRegister,
                ReferenceOrUninitCategories);

        if (objectRegisterType.category == RegisterType.Category.Null) {
            return false;
        }

        MethodIdItem methodIdItem = null;
        if (isSuper) {
            ClassPath.ClassDef classDef = ClassPath.getClassDef(this.encodedMethod.method.getContainingClass(), false);
            assert classDef != null;

            if (classDef.getSuperclass() != null) {
                methodIdItem = deodexUtil.lookupVirtualMethod(classDef.getSuperclass(), methodIndex);
            }

            if (methodIdItem == null) {
                //it's possible that the pre-odexed instruction had used the method from the current class instead
                //of from the superclass (although the superclass method is still what would actually be called).
                //And so the MethodIdItem for the superclass method may not be in the dex file. Let's try to get the
                //MethodIdItem for the method in the current class instead
                methodIdItem = deodexUtil.lookupVirtualMethod(classDef, methodIndex);
            }
        } else{
            methodIdItem = deodexUtil.lookupVirtualMethod(objectRegisterType.type, methodIndex);
        }

        if (methodIdItem == null) {
            throw new ValidationException(String.format("Could not resolve the method in class %s at index %d",
                    objectRegisterType.type.getClassType(), methodIndex));
        }


        Instruction deodexedInstruction;
        if (isRange) {
            Instruction3rms instruction = (Instruction3rms)analyzedInstruction.instruction;
            Opcode opcode;
            if (isSuper) {
                opcode = Opcode.INVOKE_SUPER_RANGE;
            } else {
                opcode = Opcode.INVOKE_VIRTUAL_RANGE;
            }

            deodexedInstruction = new Instruction3rc(opcode, instruction.getRegCount(),
                    instruction.getStartRegister(), methodIdItem);
        } else {
            Instruction35ms instruction = (Instruction35ms)analyzedInstruction.instruction;
            Opcode opcode;
            if (isSuper) {
                opcode = Opcode.INVOKE_SUPER;
            } else {
                opcode = Opcode.INVOKE_VIRTUAL;
            }

            deodexedInstruction = new Instruction35c(opcode, instruction.getRegCount(),
                    instruction.getRegisterD(), instruction.getRegisterE(), instruction.getRegisterF(),
                    instruction.getRegisterG(), instruction.getRegisterA(), methodIdItem);
        }

        analyzedInstruction.setDeodexedInstruction(deodexedInstruction);
        analyzeInstruction(analyzedInstruction);

        return true;
    }

    private static Opcode getAndCheckIgetIputOpcodeForType(String fieldType, Opcode odexedOpcode, boolean isIput) {
        Opcode opcode;
        Opcode validOdexedOpcode;
        switch (fieldType.charAt(0)) {
            case 'Z':
                if (isIput) {
                    validOdexedOpcode = Opcode.IPUT_QUICK;
                    opcode = Opcode.IPUT_BOOLEAN;
                } else {
                    validOdexedOpcode = Opcode.IGET_QUICK;
                    opcode = Opcode.IGET_BOOLEAN;
                }
                break;
            case 'B':
                if (isIput) {
                    validOdexedOpcode = Opcode.IPUT_QUICK;
                    opcode = Opcode.IPUT_BYTE;
                } else {
                    validOdexedOpcode = Opcode.IGET_QUICK;
                    opcode = Opcode.IGET_BYTE;
                }
                break;
            case 'S':
                if (isIput) {
                    validOdexedOpcode = Opcode.IPUT_QUICK;
                    opcode = Opcode.IPUT_SHORT;
                } else {
                    validOdexedOpcode = Opcode.IGET_QUICK;
                    opcode = Opcode.IGET_SHORT;
                }
                break;
            case 'C':
                if (isIput) {
                    validOdexedOpcode = Opcode.IPUT_QUICK;
                    opcode = Opcode.IPUT_CHAR;
                } else {
                    validOdexedOpcode = Opcode.IGET_QUICK;
                    opcode = Opcode.IGET_CHAR;
                }
                break;
            case 'I':
            case 'F':
                if (isIput) {
                    validOdexedOpcode = Opcode.IPUT_QUICK;
                    opcode = Opcode.IPUT;
                } else {
                    validOdexedOpcode = Opcode.IGET_QUICK;
                    opcode = Opcode.IGET;
                }
                break;
            case 'J':
            case 'D':
                if (isIput) {
                    validOdexedOpcode = Opcode.IPUT_WIDE_QUICK;
                    opcode = Opcode.IPUT_WIDE;
                } else {
                    validOdexedOpcode = Opcode.IGET_WIDE_QUICK;
                    opcode = Opcode.IGET_WIDE;
                }
                break;
            case 'L':
            case '[':
                if (isIput) {
                    validOdexedOpcode = Opcode.IPUT_OBJECT_QUICK;
                    opcode = Opcode.IPUT_OBJECT;
                } else {
                    validOdexedOpcode = Opcode.IGET_OBJECT_QUICK;
                    opcode = Opcode.IGET_OBJECT;
                }
                break;
            default:
                throw new RuntimeException(String.format("Unexpected field type %s for %s: ", fieldType,
                        odexedOpcode.name));
        }

        if (odexedOpcode != validOdexedOpcode) {
            throw new ValidationException(String.format("Incorrect field type \"%s\" for %s", fieldType,
                    odexedOpcode.name));
        }

        return opcode;
    }

    private static boolean checkArrayFieldAssignment(RegisterType.Category arrayFieldCategory,
                                                  RegisterType.Category instructionCategory) {
        if (arrayFieldCategory == instructionCategory) {
            return true;
        }

        if ((arrayFieldCategory == RegisterType.Category.Integer &&
             instructionCategory == RegisterType.Category.Float) ||
            (arrayFieldCategory == RegisterType.Category.Float &&
             instructionCategory == RegisterType.Category.Integer)) {
            return true;
        }
        return false;
    }

    private static RegisterType getAndCheckSourceRegister(AnalyzedInstruction analyzedInstruction, int registerNumber,
                                            EnumSet validCategories) {
        assert registerNumber >= 0 && registerNumber < analyzedInstruction.postRegisterMap.length;

        RegisterType registerType = analyzedInstruction.getPreInstructionRegisterType(registerNumber);
        assert registerType != null;

        checkRegister(registerType, registerNumber, validCategories);

        if (validCategories == WideLowCategories) {
            checkRegister(registerType, registerNumber, WideLowCategories);
            checkWidePair(registerNumber, analyzedInstruction);

            RegisterType secondRegisterType = analyzedInstruction.getPreInstructionRegisterType(registerNumber + 1);
            assert secondRegisterType != null;
            checkRegister(secondRegisterType, registerNumber+1, WideHighCategories);
        }

        return registerType;
    }

    private static void checkRegister(RegisterType registerType, int registerNumber, EnumSet validCategories) {
        if (!validCategories.contains(registerType.category)) {
            throw new ValidationException(String.format("Invalid register type %s for register v%d.",
                    registerType.toString(), registerNumber));
        }
    }

    private static void checkWidePair(int registerNumber, AnalyzedInstruction analyzedInstruction) {
        if (registerNumber + 1 >= analyzedInstruction.postRegisterMap.length) {
            throw new ValidationException(String.format("v%d is the last register and not a valid wide register " +
                    "pair.", registerNumber));
        }
    }
}
