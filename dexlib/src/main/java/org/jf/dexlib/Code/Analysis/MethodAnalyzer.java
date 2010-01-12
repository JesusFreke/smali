package org.jf.dexlib.Code.Analysis;

import org.jf.dexlib.ClassDataItem;
import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.MultiOffsetInstruction;
import org.jf.dexlib.Code.OffsetInstruction;
import org.jf.dexlib.Code.Opcode;
import org.jf.dexlib.CodeItem;

import java.util.Arrays;
import java.util.HashSet;

public class MethodAnalyzer {
    private final HashSet<RegisterInfo> registerInfoCache = new HashSet<RegisterInfo>();
    private final ClassDataItem.EncodedMethod encodedMethod;

    private AnalyzedInstruction[] instructions;

    //This is a dummy instruction that occurs immediately before the first real instruction. We can initialize the
    //register types for this instruction to the parameter types, in order to have them propagate to all of its
    //successors, e.g. the first real instruction, the first instructions in any exception handlers covering the first
    //instruction, etc.
    private AnalyzedInstruction startOfMethod;

    public MethodAnalyzer(ClassDataItem.EncodedMethod encodedMethod) {
        if (encodedMethod == null) {
            throw new IllegalArgumentException("encodedMethod cannot be null");
        }
        if (encodedMethod.codeItem == null) {
            throw new IllegalArgumentException("The method has no code");
        }
        this.encodedMethod = encodedMethod;
        buildInstructionList();
    }

    public AnalyzedInstruction[] analyze() {
        return null;
    }

    private void buildInstructionList() {
        assert encodedMethod != null;
        assert encodedMethod.codeItem != null;

        startOfMethod = new AnalyzedInstruction(null, -1);

        Instruction[] insns = encodedMethod.codeItem.getInstructions();

        instructions = new AnalyzedInstruction[insns.length];

        //first, create all the instructions and populate the instructionAddresses array
        int currentCodeAddress = 0;
        for (int i=0; i<insns.length; i++) {
            instructions[i] = new AnalyzedInstruction(insns[i], currentCodeAddress);
            currentCodeAddress += insns[i].getSize(currentCodeAddress);
        }

        //next, populate the exceptionHandlers array. The array item for each instruction that can throw an exception
        //and is covered by a try block should be set to a list of the first instructions of each exception handler
        //for the try block covering the instruction
        CodeItem.TryItem[] tries = encodedMethod.codeItem.getTries();
        int triesIndex = 0;
        CodeItem.TryItem currentTry = null;
        int[] currentExceptionHandlers = null;
        int[][] exceptionHandlers = new int[insns.length][];

        for (int i=0; i<instructions.length; i++) {
            AnalyzedInstruction instruction = instructions[i];
            Opcode instructionOpcode = instruction.instruction.opcode;

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

        //finally, populate the successors and predecessors for each instruction
        for (int i=0; i<instructions.length; i++) {
            AnalyzedInstruction instruction = instructions[i];
            Opcode instructionOpcode = instruction.instruction.opcode;
            int currentCodeOffset = instruction.codeAddress;

            if (instruction.instruction.opcode.canContinue()) {
                if (i == instructions.length - 1) {
                    throw new ValidationException("Execution can continue past the last instruction");
                }
                AnalyzedInstruction nextInstruction = instructions[i+1];
                addPredecessorSuccessor(instruction, nextInstruction, exceptionHandlers, i+1);
            }

            if (instruction instanceof OffsetInstruction) {
                OffsetInstruction offsetInstruction = (OffsetInstruction)instruction;

                if (instructionOpcode == Opcode.PACKED_SWITCH || instructionOpcode == Opcode.SPARSE_SWITCH) {
                    MultiOffsetInstruction switchDataInstruction =
                            (MultiOffsetInstruction)getInstructionByAddress(currentCodeOffset +
                                    offsetInstruction.getTargetAddressOffset()).instruction;
                    for (int targetAddressOffset: switchDataInstruction.getTargets()) {
                        int targetInstructionIndex = getInstructionIndexByAddress(currentCodeOffset +
                                targetAddressOffset);
                        AnalyzedInstruction targetInstruction = instructions[targetInstructionIndex];

                        addPredecessorSuccessor(instruction, targetInstruction, exceptionHandlers,
                                targetInstructionIndex);
                    }
                } else {
                    int targetAddressOffset = offsetInstruction.getTargetAddressOffset();
                    int targetInstructionIndex = getInstructionIndexByAddress(currentCodeOffset + targetAddressOffset);
                    AnalyzedInstruction targetInstruction = instructions[targetInstructionIndex];

                    addPredecessorSuccessor(instruction, targetInstruction, exceptionHandlers, targetInstructionIndex);
                }
            }
        }
    }

    private void addPredecessorSuccessor(AnalyzedInstruction predecessor, AnalyzedInstruction successor,
                                                int[][] exceptionHandlers,
                                                int successorInstructionIndex) {

        if (!predecessor.addSuccessor(successor)) {
            //if predecessor already had successor as a successor, then there's nothing else to do
            return;
        }

        successor.addPredecessor(predecessor);

        //if the successor can throw an instruction, then we need to add the exception handlers as additional
        //successors to the predecessor (and then apply this same logic recursively if needed)
        int[] exceptionHandlersForSuccessor = exceptionHandlers[successorInstructionIndex];
        if (exceptionHandlersForSuccessor != null) {
            //the item for this instruction in exceptionHandlersForSuccessor should only be set if this instruction
            //can throw an exception
            assert predecessor.instruction.opcode.canThrow();

            for (int exceptionHandlerIndex: exceptionHandlersForSuccessor) {
                AnalyzedInstruction exceptionHandler = instructions[exceptionHandlerIndex];
                addPredecessorSuccessor(predecessor, exceptionHandler, exceptionHandlers, exceptionHandlerIndex);
            }
        }
    }

    private int[] buildExceptionHandlerArray(CodeItem.TryItem tryItem) {
        int exceptionHandlerCount = tryItem.encodedCatchHandler.handlers.length;
        int catchAllHandler = tryItem.encodedCatchHandler.getCatchAllHandlerAddress();
        if (catchAllHandler != -1) {
            exceptionHandlerCount++;
        }

        int[] exceptionHandlers = new int[exceptionHandlerCount];
        for (int i=0; i<tryItem.encodedCatchHandler.handlers.length; i++) {
            exceptionHandlers[i] = getInstructionIndexByAddress(
                    tryItem.encodedCatchHandler.handlers[i].getHandlerAddress());
        }

        if (catchAllHandler != -1) {
            exceptionHandlers[exceptionHandlers.length - 1] = getInstructionIndexByAddress(
                    catchAllHandler);
        }

        return exceptionHandlers;
    }

    private int getInstructionIndexByAddress(int address) {
        int start=0;
        int end=instructions.length;

        while (end > (start + 1)) {
            int index = (start + end) / 2;

            if (instructions[index].codeAddress < address) {
                start = index;
            } else {
                end = index;
            }
        }

        if (end < instructions.length && instructions[end].codeAddress == address) {
            return end;
        }

        throw new RuntimeException("No instruction at address " + address);
    }    

    private AnalyzedInstruction getInstructionByAddress(int address) {
        int index = getInstructionIndexByAddress(address);

        assert index < instructions.length;
        assert instructions[index] != null;
        return instructions[index];
    }
}
