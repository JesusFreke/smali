package org.jf.dexlib2.writer.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jf.dexlib2.Format;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.ReferenceType;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.ReferenceInstruction;
import org.jf.dexlib2.iface.instruction.SwitchElement;
import org.jf.dexlib2.iface.instruction.SwitchPayload;
import org.jf.dexlib2.iface.instruction.formats.*;
import org.jf.dexlib2.iface.reference.*;
import org.jf.dexlib2.immutable.instruction.*;
import org.jf.dexlib2.util.MethodUtil;
import org.jf.dexlib2.writer.StringPool;
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class InstructionWriteUtil {
    private final StringPool stringPool;
    MethodImplementation methodImplementation;

    private List<Instruction> instructions;
    private ArrayList<Integer> codeOffsetShifts;

    private int codeUnitCount;
    private int outParamCount;

    public InstructionWriteUtil(@Nonnull MethodImplementation methodImpl, @Nonnull StringPool stringPool) {
        this.stringPool = stringPool;
        methodImplementation = methodImpl;

        calculateMaxOutParamCount();
        findCodeOffsetShifts();
        modifyInstructions();
    }

    private void calculateMaxOutParamCount() {
        outParamCount = 0;
        for (Instruction instruction: methodImplementation.getInstructions()) {
            codeUnitCount += instruction.getCodeUnits();
            if (instruction.getOpcode().referenceType == ReferenceType.METHOD) {
                ReferenceInstruction refInsn = (ReferenceInstruction)instruction;
                MethodReference methodRef = (MethodReference)refInsn.getReference();
                int paramCount = MethodUtil.getParameterRegisterCount(methodRef,
                        org.jf.dexlib2.util.InstructionUtil.isInvokeStatic(instruction.getOpcode()));
                if (paramCount > outParamCount) {
                    outParamCount = paramCount;
                }
            }
        }
    }

    public Iterable<? extends Instruction> getInstructions() {
        if (instructions != null) {
            return instructions;
        } else {
            return methodImplementation.getInstructions();
        }
    }

    public int getCodeUnitCount() {
        return codeUnitCount;
    }

    public int getOutParamCount() {
        return outParamCount;
    }

    private int targetOffsetShift(int instrOffset, int targetOffset) {
        int targetOffsetShift = 0;
        if (codeOffsetShifts !=null) {
            int instrShift = codeOffsetShift(instrOffset);
            int targetShift = codeOffsetShift(instrOffset+targetOffset);
            targetOffsetShift = targetShift - instrShift;
        }
        return targetOffsetShift;
    }

    public int codeOffsetShift(int offset) {
        int shift = 0;
        if (codeOffsetShifts != null) {
            int numCodeOffsetShifts = codeOffsetShifts.size();
            if (numCodeOffsetShifts > 0) {
                if (offset > codeOffsetShifts.get(numCodeOffsetShifts-1)) {
                    shift = numCodeOffsetShifts;
                } else if (numCodeOffsetShifts>1) {
                    for (int i=1;i<numCodeOffsetShifts;i++) {
                        if (offset > codeOffsetShifts.get(i-1) && offset <= codeOffsetShifts.get(i)) {
                            shift = i;
                            break;
                        }
                    }
                }
            }
        }
        return shift;
    }

    /*
    * This method returns a list of code offsets of instructions that will create a code offset shift.
    * This happens when the instruction has to be changed to a larger sized one to fit new value.
     */
    private void findCodeOffsetShifts() {
        // first, process const-string to const-string/jumbo conversions
        int currentCodeOffset = 0;
        for (Instruction instruction: methodImplementation.getInstructions()) {
            if (instruction.getOpcode().equals(Opcode.CONST_STRING)) {
                ReferenceInstruction refInstr = (ReferenceInstruction) instruction;
                int referenceIndex = stringPool.getIndex((StringReference)refInstr.getReference());
                if (referenceIndex > 0xFFFF) {
                    if (codeOffsetShifts == null) {
                        codeOffsetShifts = new ArrayList<Integer>();
                    }
                    codeOffsetShifts.add(currentCodeOffset);
                }
            }
            currentCodeOffset += instruction.getCodeUnits();
        }

        if (codeOffsetShifts == null) {
            return;
        }

        // now, let's check if this caused any conversions in goto instructions due to changes in offset values
        // since code offset delta is equivalent to the position of instruction's code offset in the shift list,
        // we use it as a position here
        // we also check if we will have to insert no-ops to ensure 4-byte alignment for
        // switch statements and packed arrays
        currentCodeOffset = 0;
        for (Instruction instruction: methodImplementation.getInstructions()) {
            if (instruction.getOpcode().format.equals(Format.Format10t)) {
                int targetOffset = ((Instruction10t)instruction).getCodeOffset();
                int codeOffsetDelta = codeOffsetShift(currentCodeOffset);
                int newTargetOffset = targetOffset + targetOffsetShift(currentCodeOffset, targetOffset);
                if ((byte)newTargetOffset != newTargetOffset) {
                    if ((short)newTargetOffset != newTargetOffset) {
                        // handling very small (negligible) possiblity of goto becoming goto/32
                        // we insert extra 1 code unit shift referring to the same position
                        // this will cause subsequent code offsets to be shifted by 2 code units
                        codeOffsetShifts.add(codeOffsetDelta, currentCodeOffset);
                    }
                    codeOffsetShifts.add(codeOffsetDelta, currentCodeOffset);
                }
            } else if (instruction.getOpcode().format.equals(Format.Format20t)) {
                int targetOffset = ((Instruction20t)instruction).getCodeOffset();
                int codeOffsetDelta = codeOffsetShift(currentCodeOffset);
                int newTargetOffset = targetOffsetShift(currentCodeOffset, targetOffset);
                if ((short)newTargetOffset != newTargetOffset) {
                    codeOffsetShifts.add(codeOffsetDelta, currentCodeOffset);
                }
            } else if (instruction.getOpcode().format.equals(Format.ArrayPayload)
            		|| instruction.getOpcode().format.equals(Format.SparseSwitchPayload)
            		|| instruction.getOpcode().format.equals(Format.PackedSwitchPayload)) {
                int codeOffsetDelta = codeOffsetShift(currentCodeOffset);
                if ((currentCodeOffset+codeOffsetDelta)%2 != 0) {
                    codeOffsetShifts.add(codeOffsetDelta, currentCodeOffset);
                }
            } 
            currentCodeOffset += instruction.getCodeUnits();
        }

        codeUnitCount += codeOffsetShifts.size();
    }

    private void modifyInstructions() {
        if (codeOffsetShifts == null) {
            return;
        }

        instructions = Lists.newArrayList();
        int currentCodeOffset = 0;
        for (Instruction instruction: methodImplementation.getInstructions()) {
            instructions.add(instruction);
            switch (instruction.getOpcode().format) {
                case Format10t: {
                    Instruction10t instr = (Instruction10t)instruction;
                    int targetOffset = instr.getCodeOffset();
                    int newTargetOffset = targetOffset + targetOffsetShift(currentCodeOffset, targetOffset);
                    if (newTargetOffset != targetOffset) {
                        ImmutableInstruction immuInstr = null;
                        if ((byte)newTargetOffset != newTargetOffset) {
                            if ((short)newTargetOffset != newTargetOffset) {
                                immuInstr = new ImmutableInstruction30t(Opcode.GOTO_32, newTargetOffset);
                            } else {
                                immuInstr = new ImmutableInstruction20t(Opcode.GOTO_16, newTargetOffset);
                            }
                        } else {
                            immuInstr = new ImmutableInstruction10t(instr.getOpcode(), newTargetOffset);
                        }
                        instructions.set(instructions.size()-1, immuInstr);
                    }
                    break;
                }
                case Format20t: {
                    Instruction20t instr = (Instruction20t)instruction;
                    int targetOffset = instr.getCodeOffset();
                    int newTargetOffset = targetOffset + targetOffsetShift(currentCodeOffset, targetOffset);
                    if (newTargetOffset != targetOffset) {
                        ImmutableInstruction immuInstr = null;
                        if ((short)newTargetOffset != newTargetOffset) {
                            immuInstr = new ImmutableInstruction30t(Opcode.GOTO_32, newTargetOffset);
                        } else {
                            immuInstr = new ImmutableInstruction20t(Opcode.GOTO_16, newTargetOffset);
                        }
                        instructions.set(instructions.size()-1, immuInstr);
                    }
                    break;
                }
                case Format21c: {
                    Instruction21c instr = (Instruction21c)instruction;
                    if (instr.getOpcode().equals(Opcode.CONST_STRING)) {
                        int referenceIndex = stringPool.getIndex((StringReference)instr.getReference());
                        if (referenceIndex > 0xFFFF) {
                            ImmutableInstruction immuInstr
                                    = new ImmutableInstruction31c(Opcode.CONST_STRING_JUMBO, instr.getRegisterA(), instr.getReference());
                            instructions.set(instructions.size()-1, immuInstr);
                        }
                    }
                    break;
                }
                case Format21t: {
                    Instruction21t instr = (Instruction21t)instruction;
                    int targetOffset = instr.getCodeOffset();
                    int newTargetOffset = targetOffset + targetOffsetShift(currentCodeOffset, targetOffset);
                    if (newTargetOffset != targetOffset) {
                        ImmutableInstruction immuInstr
                                = new ImmutableInstruction21t(instr.getOpcode(), instr.getRegisterA(), newTargetOffset);
                        instructions.set(instructions.size()-1, immuInstr);
                    }
                    break;
                }
                case Format22t: {
                    Instruction22t instr = (Instruction22t)instruction;
                    int targetOffset = instr.getCodeOffset();
                    int newTargetOffset = targetOffset + targetOffsetShift(currentCodeOffset, targetOffset);
                    if (newTargetOffset != targetOffset) {
                        ImmutableInstruction immuInstr
                                = new ImmutableInstruction22t(instr.getOpcode(), instr.getRegisterA(), instr.getRegisterB(), newTargetOffset);
                        instructions.set(instructions.size()-1, immuInstr);
                    }
                    break;
                }
                case Format30t: {
                    Instruction30t instr = (Instruction30t)instruction;
                    int targetOffset = instr.getCodeOffset();
                    int newTargetOffset = targetOffset + targetOffsetShift(currentCodeOffset, targetOffset);
                    if (newTargetOffset != targetOffset) {
                        ImmutableInstruction immuInstr = new ImmutableInstruction30t(instr.getOpcode(), newTargetOffset);
                        instructions.set(instructions.size()-1, immuInstr);
                    }
                    break;
                }
                case Format31t: {
                    Instruction31t instr = (Instruction31t)instruction;
                    int targetOffset = instr.getCodeOffset();
                    int newTargetOffset = targetOffset + targetOffsetShift(currentCodeOffset, targetOffset);
                    if (newTargetOffset != targetOffset) {
                        ImmutableInstruction immuInstr
                                = new ImmutableInstruction31t(instr.getOpcode(), instr.getRegisterA(), newTargetOffset);
                        instructions.set(instructions.size()-1, immuInstr);
                    }
                    break;
                }
                case SparseSwitchPayload: {
                	int codeOffsetDelta = codeOffsetShift(currentCodeOffset);
                    if ((currentCodeOffset+codeOffsetDelta)%2 != 0) {
                		instructions.add(instructions.size()-1, new ImmutableInstruction10x(Opcode.NOP));
                	}
                    int switchInstructionOffset = findSwitchInstructionOffset(currentCodeOffset);
                    SwitchPayload payload = (SwitchPayload)instruction;
                    if (isSwitchTargetOffsetChanged(payload, switchInstructionOffset)) {
                        List<SwitchElement> newSwitchElements = modifySwitchElements(payload, switchInstructionOffset);
                        ImmutableSparseSwitchPayload immuInstr = new ImmutableSparseSwitchPayload(newSwitchElements);
                        instructions.set(instructions.size()-1, immuInstr);
                    }
                    break;
                }
                case PackedSwitchPayload: {
                	int codeOffsetDelta = codeOffsetShift(currentCodeOffset);
                    if ((currentCodeOffset+codeOffsetDelta)%2 != 0) {
                		instructions.add(instructions.size()-1, new ImmutableInstruction10x(Opcode.NOP));
                	}
                    int switchInstructionOffset = findSwitchInstructionOffset(currentCodeOffset);
                    SwitchPayload payload = (SwitchPayload)instruction;
                    if (isSwitchTargetOffsetChanged(payload, switchInstructionOffset)) {
                        List<SwitchElement> newSwitchElements = modifySwitchElements(payload, switchInstructionOffset);
                        ImmutablePackedSwitchPayload immuInstr = new ImmutablePackedSwitchPayload(newSwitchElements);
                        instructions.set(instructions.size()-1, immuInstr);
                    }
                    break;
                }
                case ArrayPayload: {
                	int codeOffsetDelta = codeOffsetShift(currentCodeOffset);
                    if ((currentCodeOffset+codeOffsetDelta)%2 != 0) {
                		instructions.add(instructions.size()-1, new ImmutableInstruction10x(Opcode.NOP));
                	}
                }
            }
            currentCodeOffset += instruction.getCodeUnits();
        }
    }

    private int findSwitchInstructionOffset(int payloadOffset) {
        int currentCodeOffset = 0;
        for (Instruction instruction: methodImplementation.getInstructions()) {
            if (instruction.getOpcode().equals(Opcode.PACKED_SWITCH)
                    || instruction.getOpcode().equals(Opcode.SPARSE_SWITCH)) {
                int targetOffset = currentCodeOffset + ((Instruction31t)instruction).getCodeOffset();
                if (targetOffset == payloadOffset) {
                    return currentCodeOffset;
                }
            }
            currentCodeOffset += instruction.getCodeUnits();
        }

        // we should never get here
        throw new ExceptionWithContext("Switch payload with no corresponding switch statement. Mailformed method implementation?");
    }

    private boolean isSwitchTargetOffsetChanged(SwitchPayload payload, int switchInstructionOffset) {
        for (SwitchElement switchElement: payload.getSwitchElements()) {
            if (targetOffsetShift(switchInstructionOffset, switchElement.getOffset()) != 0) {
                return true;
            }
        }
        return false;
    }

    private ArrayList<SwitchElement> modifySwitchElements(SwitchPayload payload, int switchInstructionOffset) {
        ArrayList<SwitchElement> switchElements = Lists.newArrayList();
        for (SwitchElement switchElement: payload.getSwitchElements()) {
            int targetOffset = switchElement.getOffset();
            int newTargetOffset = targetOffset + targetOffsetShift(switchInstructionOffset, targetOffset);
            if (newTargetOffset != targetOffset) {
                ImmutableSwitchElement immuSwitchElement = new ImmutableSwitchElement(switchElement.getKey(), newTargetOffset);
                switchElements.add(immuSwitchElement);
            } else {
                switchElements.add(switchElement);
            }
        }
        return switchElements;
    }
}


