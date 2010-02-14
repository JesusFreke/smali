package org.jf.dexlib.Code.Analysis;

import org.jf.dexlib.Code.*;
import org.jf.dexlib.Item;
import org.jf.dexlib.ItemType;
import org.jf.dexlib.MethodIdItem;
import org.jf.dexlib.Util.ExceptionWithContext;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class AnalyzedInstruction {
    /**
     * The actual instruction
     */
    public final Instruction instruction;

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
        RegisterType unknown = RegisterType.getRegisterType(RegisterType.Category.Unknown, null);
        for (int i=0; i<registerCount; i++) {
            postRegisterMap[i] = unknown;
        }
    }

    public int getInstructionIndex() {
        return instructionIndex;
    }

    public int getPredecessorCount() {
        return predecessors.size();
    }

    public List<AnalyzedInstruction> getPredecessors() {
        return Collections.unmodifiableList(predecessors);
    }

    private boolean checkPredecessorSorted(AnalyzedInstruction predecessor) {
        if (predecessors.size() == 0) {
            return true;
        }

        if (predecessor.getInstructionIndex() <= predecessors.getLast().getInstructionIndex()) {
            return false;
        }

        return true;
    }

    protected void addPredecessor(AnalyzedInstruction predecessor) {
        assert checkPredecessorSorted(predecessor);
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

    public int getSuccessorCount() {
        return successors.size();
    }

    public List<AnalyzedInstruction> getSuccesors() {
        return Collections.unmodifiableList(successors);
    }

    /**
     * Is this instruction a "beginning instruction". A beginning instruction is defined to be an instruction
     * that can be the first successfully executed instruction in the method. The first instruction is always a
     * beginning instruction. If the first instruction can throw an exception, and is covered by a try block, then
     * the first instruction of any exception handler for that try block is also a beginning instruction. And likewise,
     * if any of those instructions can throw an exception and are covered by try blocks, the first instruction of the
     * corresponding exception handler is a beginning instruction, etc.
     * @return a boolean value indicating whether this instruction is a beginning instruction
     */
    public boolean isBeginningInstruction() {
        if (predecessors.size() == 0) {
            return false;
        }

        if (predecessors.getFirst().instructionIndex == -1) {
            return true;
        }
        return false;
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

    protected boolean isInvokeInit() {
        if (instruction == null ||
                (instruction.opcode != Opcode.INVOKE_DIRECT && instruction.opcode != Opcode.INVOKE_DIRECT_RANGE)) {
            return false;
        }

        //TODO: check access flags instead of name?

        InstructionWithReference instruction = (InstructionWithReference)this.instruction;
        Item item = instruction.getReferencedItem();
        assert item.getItemType() == ItemType.TYPE_METHOD_ID_ITEM;
        MethodIdItem method = (MethodIdItem)item;

        if (!method.getMethodName().getStringValue().equals("<init>")) {
            return false;
        }

        return true;
    }

    public boolean setsRegister() {
        return instruction.opcode.setsRegister();
    }

    public boolean setsWideRegister() {
        return instruction.opcode.setsWideRegister();
    }

    public boolean setsRegister(int registerNumber) {

        //When constructing a new object, the register type will be an uninitialized reference after the new-instance
        //instruction, but becomes an initialized reference once the <init> method is called. So even though invoke
        //instructions don't normally change any registers, calling an <init> method will change the type of its
        //object register. If the uninitialized reference has been copied to other registers, they will be initialized
        //as well, so we need to check for that too
        if (isInvokeInit()) {
            int destinationRegister;
            if (instruction instanceof FiveRegisterInstruction) {
                destinationRegister = ((FiveRegisterInstruction)instruction).getRegisterD();
            } else {
                assert instruction instanceof RegisterRangeInstruction;
                RegisterRangeInstruction rangeInstruction = (RegisterRangeInstruction)instruction;
                assert rangeInstruction.getRegCount() > 0;
                destinationRegister = rangeInstruction.getStartRegister();
            }

            if (registerNumber == destinationRegister) {
                return true;
            }
            RegisterType preInstructionDestRegisterType = getMergedRegisterTypeFromPredecessors(registerNumber);
            if (preInstructionDestRegisterType.category != RegisterType.Category.UninitRef &&
                preInstructionDestRegisterType.category != RegisterType.Category.UninitThis) {

                return false;
            }
            //check if the uninit ref has been copied to another register
            if (getMergedRegisterTypeFromPredecessors(registerNumber) == preInstructionDestRegisterType) {
                return true;
            }
            return false;
        }

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

    public int getRegisterCount() {
        return postRegisterMap.length;
    }

    public RegisterType getPostInstructionRegisterType(int registerNumber) {
        return postRegisterMap[registerNumber];
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

