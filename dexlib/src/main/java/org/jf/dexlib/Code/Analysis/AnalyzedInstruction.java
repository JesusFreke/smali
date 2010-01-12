package org.jf.dexlib.Code.Analysis;

import org.jf.dexlib.Code.Instruction;

import java.util.LinkedList;

public class AnalyzedInstruction {
    /**
     * The actual instruction
     */
    protected final Instruction instruction;

    /**
     * The address of the instruction, in 2-byte code blocks
     */
    protected final int codeAddress;

    /**
     * Instructions that can pass on execution to this one during normal execution
     */
    protected LinkedList<AnalyzedInstruction> predecessors = new LinkedList<AnalyzedInstruction>();

    /**
     * Instructions that can execution could pass on to next during normal execution
     */
    protected LinkedList<AnalyzedInstruction> successors = new LinkedList<AnalyzedInstruction>();

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

    public AnalyzedInstruction(Instruction instruction, int codeAddress) {
        this.instruction = instruction;
        this.codeAddress = codeAddress;
    }

    public int getCodeAddress() {
        return codeAddress;
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
}

