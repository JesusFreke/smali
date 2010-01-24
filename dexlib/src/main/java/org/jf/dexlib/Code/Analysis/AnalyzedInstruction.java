package org.jf.dexlib.Code.Analysis;

import org.jf.dexlib.Code.*;
import org.jf.dexlib.Util.ExceptionWithContext;

import java.util.LinkedList;

public class AnalyzedInstruction {
    /**
     * The actual instruction
     */
    protected final Instruction instruction;

    /**
     * The index of the instruction, where the first instruction in the method is at index 0, and so on
     */
    protected final int instructionIndex;

    /**
     * Instructions that can pass on execution to this one during normal execution
     */
    protected final LinkedList<AnalyzedInstruction> predecessors = new LinkedList<AnalyzedInstruction>();

    /**
     * Instructions that can execution could pass on to next during normal execution
     */
    protected final LinkedList<AnalyzedInstruction> successors = new LinkedList<AnalyzedInstruction>();

    /**
     * This contains the register types *after* the instruction has executed
     */
    protected final RegisterType[] postRegisterMap;

    /**
     * This is set to true when this instruction follows an odexed instruction that couldn't be deodexed. In this case
     * the unodexable instruction is guaranteed to throw an NPE, so anything following it is dead, up until a non-dead
     * code path merges in. And more importantly, the code following the unodexable instruction isn't verifiable in
     * some cases, if it depends on the return/field type of the unodexeable instruction. Meaning that if the "dead"
     * code was left in, dalvik would reject it because it couldn't verify the register types. In some cases, this
     * dead code could be left in without ill-effect, but it's easier to always remove it, which is always valid. Since
     * it is dead code, removing it won't have any effect.
     */
    protected boolean dead = false;

    public AnalyzedInstruction(Instruction instruction, int instructionIndex, int registerCount) {
        this.instruction = instruction;
        this.instructionIndex = instructionIndex;
        this.postRegisterMap = new RegisterType[registerCount];
    }

    public int getInstructionIndex() {
        return instructionIndex;
    }

    protected void addPredecessor(AnalyzedInstruction predecessor) {
        predecessors.add(predecessor);
    }

    /**
     * @return true if the successor was added or false if it wasn't added because it already existed
     */
    protected boolean addSuccessor(AnalyzedInstruction successor) {
        for (AnalyzedInstruction instruction: successors) {
            if (instruction == successor) {
                return false;
            }
        }
        successors.add(successor);
        return true;
    }

    /*
     * Sets the "post-instruction" register type as indicated. This should only be used to set
     * the method parameter types for the "start of method" instruction, or to set the register
     * type of the destination register during verification. The change to the register type
     * will
     * @param registerNumber Which register to set
     * @param registerType The "post-instruction" register type
     */
    protected boolean setPostRegisterType(int registerNumber, RegisterType registerType) {
        assert registerNumber >= 0 && registerNumber < postRegisterMap.length;
        assert registerType != null;

        RegisterType oldRegisterType = postRegisterMap[registerNumber];
        if (oldRegisterType == registerType) {
            return false;
        }

        postRegisterMap[registerNumber] = registerType;
        return true;
    }

    protected RegisterType getMergedRegisterTypeFromPredecessors(int registerNumber) {
        RegisterType mergedRegisterType = null;
        for (AnalyzedInstruction predecessor: predecessors) {
            RegisterType predecessorRegisterType = predecessor.postRegisterMap[registerNumber];
            assert predecessorRegisterType != null;
            mergedRegisterType = predecessorRegisterType.merge(mergedRegisterType);
        }
        return mergedRegisterType;
    }

    public boolean setsRegister() {
        return instruction.opcode.setsRegister();
    }

    public boolean setsWideRegister() {
        return instruction.opcode.setsWideRegister();
    }

    public boolean setsRegister(int registerNumber) {
        if (!setsRegister()) {
            return false;
        }
        int destinationRegister = getDestinationRegister();
        if (registerNumber == destinationRegister) {
            return true;
        }
        if (setsWideRegister() && registerNumber == (destinationRegister + 1)) {
            return true;
        }
        return false;
    }

    public int getDestinationRegister() {
        if (!this.instruction.opcode.setsRegister()) {
            throw new ExceptionWithContext("Cannot call getDestinationRegister() for an instruction that doesn't " +
                    "store a value");
        }
        return ((SingleRegisterInstruction)instruction).getRegisterA();
    }

    public RegisterType getPreInstructionRegisterType(int registerNumber) {
        //if the specific register is not a destination register, then the stored post-instruction register type will
        //be the same as the pre-instruction regsiter type, so we can use that.
        //otherwise, we need to merge the predecessor's post-instruction register types

        if (this.setsRegister(registerNumber)) {
            return getMergedRegisterTypeFromPredecessors(registerNumber);
        } else {
            return postRegisterMap[registerNumber];
        }
    }
}

