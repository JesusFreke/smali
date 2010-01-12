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

package org.jf.baksmali.Deodex;

import org.jf.dexlib.*;
import org.jf.dexlib.Code.*;
import org.jf.dexlib.Code.Format.*;
import org.jf.dexlib.Util.AccessFlags;
import org.jf.dexlib.Util.SparseArray;

import java.util.*;

public class DeodexUtil {
    private final Deodexerant deodexerant;

    //a table that reflects which instructions can throw an exception
    public static final BitSet instructionThrowTable = new BitSet(256);

    {
        //mark the instructions that can throw an exception
        instructionThrowTable.set(Opcode.CONST_STRING.value & 0xFF);
        instructionThrowTable.set(Opcode.CONST_STRING_JUMBO.value & 0xFF);
        instructionThrowTable.set(Opcode.CONST_CLASS.value & 0xFF);
        instructionThrowTable.set(Opcode.MONITOR_ENTER.value & 0xFF);
        instructionThrowTable.set(Opcode.MONITOR_EXIT.value & 0xFF);
        instructionThrowTable.set(Opcode.CHECK_CAST.value & 0xFF);
        instructionThrowTable.set(Opcode.INSTANCE_OF.value & 0xFF);
        instructionThrowTable.set(Opcode.ARRAY_LENGTH.value & 0xFF);
        instructionThrowTable.set(Opcode.NEW_INSTANCE.value & 0xFF);
        instructionThrowTable.set(Opcode.NEW_ARRAY.value & 0xFF);
        instructionThrowTable.set(Opcode.FILLED_NEW_ARRAY.value & 0xFF);
        instructionThrowTable.set(Opcode.FILLED_NEW_ARRAY_RANGE.value & 0xFF);
        instructionThrowTable.set(Opcode.AGET.value & 0xFF);
        instructionThrowTable.set(Opcode.AGET_BOOLEAN.value & 0xFF);
        instructionThrowTable.set(Opcode.AGET_BYTE.value & 0xFF);
        instructionThrowTable.set(Opcode.AGET_CHAR.value & 0xFF);
        instructionThrowTable.set(Opcode.AGET_SHORT.value & 0xFF);
        instructionThrowTable.set(Opcode.AGET_WIDE.value & 0xFF);
        instructionThrowTable.set(Opcode.AGET_OBJECT.value & 0xFF);
        instructionThrowTable.set(Opcode.APUT.value & 0xFF);
        instructionThrowTable.set(Opcode.APUT_BOOLEAN.value & 0xFF);
        instructionThrowTable.set(Opcode.APUT_BYTE.value & 0xFF);
        instructionThrowTable.set(Opcode.APUT_CHAR.value & 0xFF);
        instructionThrowTable.set(Opcode.APUT_SHORT.value & 0xFF);
        instructionThrowTable.set(Opcode.APUT_WIDE.value & 0xFF);
        instructionThrowTable.set(Opcode.APUT_OBJECT.value & 0xFF);
        instructionThrowTable.set(Opcode.IGET.value & 0xFF);
        instructionThrowTable.set(Opcode.IGET_BOOLEAN.value & 0xFF);
        instructionThrowTable.set(Opcode.IGET_BYTE.value & 0xFF);
        instructionThrowTable.set(Opcode.IGET_CHAR.value & 0xFF);
        instructionThrowTable.set(Opcode.IGET_SHORT.value & 0xFF);
        instructionThrowTable.set(Opcode.IGET_WIDE.value & 0xFF);
        instructionThrowTable.set(Opcode.IGET_OBJECT.value & 0xFF);
        instructionThrowTable.set(Opcode.IPUT.value & 0xFF);
        instructionThrowTable.set(Opcode.IPUT_BOOLEAN.value & 0xFF);
        instructionThrowTable.set(Opcode.IPUT_BYTE.value & 0xFF);
        instructionThrowTable.set(Opcode.IPUT_CHAR.value & 0xFF);
        instructionThrowTable.set(Opcode.IPUT_SHORT.value & 0xFF);
        instructionThrowTable.set(Opcode.IPUT_WIDE.value & 0xFF);
        instructionThrowTable.set(Opcode.IPUT_OBJECT.value & 0xFF);
        instructionThrowTable.set(Opcode.SGET.value & 0xFF);
        instructionThrowTable.set(Opcode.SGET_BOOLEAN.value & 0xFF);
        instructionThrowTable.set(Opcode.SGET_BYTE.value & 0xFF);
        instructionThrowTable.set(Opcode.SGET_CHAR.value & 0xFF);
        instructionThrowTable.set(Opcode.SGET_SHORT.value & 0xFF);
        instructionThrowTable.set(Opcode.SGET_WIDE.value & 0xFF);
        instructionThrowTable.set(Opcode.SGET_OBJECT.value & 0xFF);
        instructionThrowTable.set(Opcode.SPUT.value & 0xFF);
        instructionThrowTable.set(Opcode.SPUT_BOOLEAN.value & 0xFF);
        instructionThrowTable.set(Opcode.SPUT_BYTE.value & 0xFF);
        instructionThrowTable.set(Opcode.SPUT_CHAR.value & 0xFF);
        instructionThrowTable.set(Opcode.SPUT_SHORT.value & 0xFF);
        instructionThrowTable.set(Opcode.SPUT_WIDE.value & 0xFF);
        instructionThrowTable.set(Opcode.SPUT_OBJECT.value & 0xFF);
        instructionThrowTable.set(Opcode.INVOKE_VIRTUAL.value & 0xFF);
        instructionThrowTable.set(Opcode.INVOKE_VIRTUAL_RANGE.value & 0xFF);
        instructionThrowTable.set(Opcode.INVOKE_SUPER.value & 0xFF);
        instructionThrowTable.set(Opcode.INVOKE_SUPER_RANGE.value & 0xFF);
        instructionThrowTable.set(Opcode.INVOKE_DIRECT.value & 0xFF);
        instructionThrowTable.set(Opcode.INVOKE_DIRECT_RANGE.value & 0xFF);
        instructionThrowTable.set(Opcode.INVOKE_STATIC.value & 0xFF);
        instructionThrowTable.set(Opcode.INVOKE_STATIC_RANGE.value & 0xFF);
        instructionThrowTable.set(Opcode.INVOKE_INTERFACE.value & 0xFF);
        instructionThrowTable.set(Opcode.INVOKE_INTERFACE_RANGE.value & 0xFF);
        instructionThrowTable.set(Opcode.DIV_INT.value & 0xFF);
        instructionThrowTable.set(Opcode.REM_INT.value & 0xFF);
        instructionThrowTable.set(Opcode.DIV_LONG.value & 0xFF);
        instructionThrowTable.set(Opcode.REM_LONG.value & 0xFF);
        instructionThrowTable.set(Opcode.DIV_INT_2ADDR.value & 0xFF);
        instructionThrowTable.set(Opcode.REM_INT_2ADDR.value & 0xFF);
        instructionThrowTable.set(Opcode.DIV_LONG_2ADDR.value & 0xFF);
        instructionThrowTable.set(Opcode.REM_LONG_2ADDR.value & 0xFF);
        instructionThrowTable.set(Opcode.DIV_INT_LIT16.value & 0xFF);
        instructionThrowTable.set(Opcode.REM_INT_LIT16.value & 0xFF);
        instructionThrowTable.set(Opcode.DIV_INT_LIT8.value & 0xFF);
        instructionThrowTable.set(Opcode.REM_INT_LIT8.value & 0xFF);
        instructionThrowTable.set(Opcode.THROW.value & 0xFF);
        instructionThrowTable.set(Opcode.EXECUTE_INLINE.value & 0xFF);
        instructionThrowTable.set(Opcode.EXECUTE_INLINE_RANGE.value & 0xFF);
        instructionThrowTable.set(Opcode.IGET_QUICK.value & 0xFF);
        instructionThrowTable.set(Opcode.IGET_WIDE_QUICK.value & 0xFF);
        instructionThrowTable.set(Opcode.IGET_OBJECT_QUICK.value & 0xFF);
        instructionThrowTable.set(Opcode.IPUT_QUICK.value & 0xFF);
        instructionThrowTable.set(Opcode.IPUT_WIDE_QUICK.value & 0xFF);
        instructionThrowTable.set(Opcode.IPUT_OBJECT_QUICK.value & 0xFF);
        instructionThrowTable.set(Opcode.INVOKE_VIRTUAL_QUICK.value & 0xFF);
        instructionThrowTable.set(Opcode.INVOKE_VIRTUAL_QUICK_RANGE.value & 0xFF);
        instructionThrowTable.set(Opcode.INVOKE_SUPER_QUICK.value & 0xFF);
        instructionThrowTable.set(Opcode.INVOKE_SUPER_QUICK_RANGE.value & 0xFF);
        instructionThrowTable.set(Opcode.INVOKE_DIRECT_EMPTY.value & 0xFF);
    }

    public DeodexUtil(Deodexerant deodexerant) {
        this.deodexerant = deodexerant;
        deodexerant.dexFile.disableInterning();
    }

    private List<insn> makeInsnList(final CodeItem codeItem) {

        final ArrayList<insn> insns = new ArrayList<insn>();
        final SparseArray<insn> insnsMap = new SparseArray<insn>();

        int currentCodeAddress = 0;
        for (Instruction instruction: codeItem.getInstructions()) {
            insn ins = new insn(codeItem, instruction, insnsMap, currentCodeAddress);
            insns.add(ins);
            insnsMap.append(currentCodeAddress, ins);
            currentCodeAddress += instruction.getSize(currentCodeAddress);
        }

        if (codeItem.getTries() != null) {
            for (CodeItem.TryItem tryItem: codeItem.getTries()) {
                insn[] handlers;

                if (tryItem.encodedCatchHandler.getCatchAllHandlerAddress() != -1) {
                    handlers = new insn[tryItem.encodedCatchHandler.handlers.length + 1];
                    handlers[handlers.length - 1] =
                            insnsMap.get(tryItem.encodedCatchHandler.getCatchAllHandlerAddress());
                } else {
                    handlers = new insn[tryItem.encodedCatchHandler.handlers.length];
                }

                for (int i=0; i<tryItem.encodedCatchHandler.handlers.length; i++) {
                    handlers[i] = insnsMap.get(tryItem.encodedCatchHandler.handlers[i].getHandlerAddress());
                }

                int currentInsnAddress = tryItem.getStartCodeAddress();
                while (currentInsnAddress < tryItem.getStartCodeAddress() + tryItem.getTryLength()) {
                    insn i = insnsMap.get(currentInsnAddress);

                    i.exceptionHandlers = handlers;

                    currentInsnAddress += i.instruction.getSize(currentInsnAddress);
                }
            }
        }

        insns.get(0).initializeRegistersFromParams();

        insn prevInsn = null;
        for (insn i: insns) {
            i.init(prevInsn);
            prevInsn = i;
        }

        insns.get(0).propagateRegisters();

        return insns;
    }

    public List<Instruction> deodexerizeCode(CodeItem codeItem) {
        List<insn> insns = makeInsnList(codeItem);

        boolean didSomething;
        boolean somethingLeftToDo;
        do {
            didSomething = false;
            somethingLeftToDo = false;
            for (insn i: insns) {
                if (i.instruction.opcode.odexOnly && i.fixedInstruction == null) {
                    if (deodexInstruction(i)) {
                        didSomething = true;
                    } else {
                        if (!i.dead) {
                            somethingLeftToDo = true;
                        }
                    }
                }
            }
        } while (didSomething);
        if (somethingLeftToDo) {
            System.err.println("warning: could not fully deodex the method " +
                    codeItem.getParent().method.getMethodString());
        }

        List<Instruction> instructions = new ArrayList<Instruction>(insns.size());
        for (insn i: insns) {
            if (i.dead) {
                if (i.fixedInstruction != null) {
                    instructions.add(new DeadInstruction(i.fixedInstruction));
                } else {
                    instructions.add(new DeadInstruction(i.instruction));
                }
            } else if (i.instruction.opcode.odexOnly) {
                assert i.fixedInstruction != null;
                instructions.add(i.fixedInstruction);
            } else {
                instructions.add(i.instruction);
            }
        }
        return instructions;
    }

    private boolean deodexInstruction(insn i) {
        switch (i.instruction.opcode) {
            case EXECUTE_INLINE:
            {
                int inlineMethodIndex = ((Instruction35ms)i.instruction).getMethodIndex();
                Deodexerant.InlineMethod inlineMethod =
                        deodexerant.lookupInlineMethod(inlineMethodIndex);
                if (inlineMethod == null) {
                    throw new RuntimeException("Could not find the inline method with index " + inlineMethodIndex);
                }
                assert inlineMethod != null;
                assert inlineMethod.getMethodIdItem() != null;

                Opcode opcode = null;
                switch (inlineMethod.getMethodType()) {
                    case Direct:
                        opcode = Opcode.INVOKE_DIRECT;
                        break;
                    case Static:
                        opcode = Opcode.INVOKE_STATIC;
                        break;
                    case Virtual:
                        opcode = Opcode.INVOKE_VIRTUAL;
                        break;
                }

                i.fixedInstruction = new Instruction35msf(opcode, (Instruction35ms)i.instruction,
                        inlineMethod.getMethodIdItem());

                insn nextInstruction = i.getInstructionAtAddress(i.address + i.instruction.getSize(i.address));
                assert nextInstruction != null;
                if (nextInstruction.instruction.opcode == Opcode.MOVE_RESULT_OBJECT) {
                    nextInstruction.registerReferenceType =
                            inlineMethod.getMethodIdItem().getPrototype().getReturnType().getTypeDescriptor();
                }

                return true;
            }
            case EXECUTE_INLINE_RANGE:
            {
                int inlineMethodIndex = ((Instruction3rms)i.instruction).getMethodIndex();
                Deodexerant.InlineMethod inlineMethod =
                        deodexerant.lookupInlineMethod(inlineMethodIndex);
                if (inlineMethod == null) {
                    throw new RuntimeException("Could not find the inline method with index " + inlineMethodIndex);
                }
                assert inlineMethod != null;
                assert inlineMethod.getMethodIdItem() != null;

                Opcode opcode = null;
                switch (inlineMethod.getMethodType()) {
                    case Direct:
                        opcode = Opcode.INVOKE_DIRECT_RANGE;
                        break;
                    case Static:
                        opcode = Opcode.INVOKE_STATIC_RANGE;
                        break;
                    case Virtual:
                        opcode = Opcode.INVOKE_VIRTUAL_RANGE;
                        break;
                }

                i.fixedInstruction = new Instruction3rmsf(opcode, (Instruction3rms)i.instruction,
                        inlineMethod.getMethodIdItem());

                insn nextInstruction = i.getInstructionAtAddress(i.address + i.instruction.getSize(i.address));
                assert nextInstruction != null;
                if (nextInstruction.instruction.opcode == Opcode.MOVE_RESULT_OBJECT) {
                    nextInstruction.registerReferenceType =
                            inlineMethod.getMethodIdItem().getPrototype().getReturnType().getTypeDescriptor();
                }

                return true;
            }
            case INVOKE_DIRECT_EMPTY:
            {
                i.fixedInstruction = new Instruction35sf((Instruction35s)i.instruction);
                return true;
            }
            case IGET_QUICK:
            {
                Instruction22cs ins = (Instruction22cs)i.instruction;
                int registerNum = ins.getRegisterB();

                RegisterType regType = i.registerMap[registerNum];

                assert regType != RegisterType.NonReference && regType != RegisterType.Conflicted;

                //if the register type is Null, we can't determine the type of the register, and so we can't determine
                //what field is being accessed. What will really happen is that when it tries to access the field, it
                //will obviously throw a NPE. We can get this same effect by replacing this opcode with a call to
                //a method on java.lang.Object.
                //We actually just create an Instruction2csn, which doesn't have any method/field info associated with
                //it, and let the caller choose which "default" method to call in this case
                if (regType == RegisterType.Null) {
                    i.fixedInstruction = new UnresolvedNullReference(i.instruction, registerNum);
                    i.propogateDeadness();
                    return true;
                }

                if (regType != RegisterType.Reference) {
                    return false;
                }

                String type = i.registerTypes[registerNum];
                if (type == null) {
                    return false;
                }

                FieldIdItem field = deodexerant.lookupField(type, ins.getFieldOffset());
                if (field == null) {
                    throw new RuntimeException("Could not find the field with offset " + ins.getFieldOffset() +
                            " for class: " + type);
                }
                String fieldType = field.getFieldType().getTypeDescriptor();

                Opcode opcode;
                switch (fieldType.charAt(0)) {
                    case 'Z':
                        opcode = Opcode.IGET_BOOLEAN;
                        break;
                    case 'B':
                        opcode = Opcode.IGET_BYTE;
                        break;
                    case 'S':
                        opcode = Opcode.IGET_SHORT;
                        break;
                    case 'C':
                        opcode = Opcode.IGET_CHAR;
                        break;
                    case 'I':
                    case 'F':
                        opcode = Opcode.IGET;
                        break;
                    default:
                        throw new RuntimeException("Unexpected field type for iget-quick opcode: " + fieldType);
                }

                i.fixedInstruction = new Instruction22csf(opcode, (Instruction22cs)i.instruction, field);

                return true;
            }
            case IGET_WIDE_QUICK:
            {
                Instruction22cs ins = (Instruction22cs)i.instruction;
                int registerNum = ins.getRegisterB();

                RegisterType regType = i.registerMap[registerNum];

                assert regType != RegisterType.NonReference && regType != RegisterType.Conflicted;

                //if the register type is Null, we can't determine the type of the register, and so we can't determine
                //what field is being accessed. What will really happen is that when it tries to access the field, it
                //will obviously throw a NPE. We can get this same effect by replacing this opcode with a call to
                //a method on java.lang.Object.
                //We actually just create an Instruction2csn, which doesn't have any method/field info associated with
                //it, and let the caller choose which "default" method to call in this case
                if (regType == RegisterType.Null) {
                    i.fixedInstruction = new UnresolvedNullReference(i.instruction, registerNum);
                    i.propogateDeadness();
                    return true;
                }

                if (regType != RegisterType.Reference) {
                    return false;
                }

                String type = i.registerTypes[registerNum];
                if (type == null) {
                    return false;
                }

                FieldIdItem field = deodexerant.lookupField(type, ins.getFieldOffset());
                if (field == null) {
                    throw new RuntimeException("Could not find the field with offset " + ins.getFieldOffset() +
                            " for class: " + type);
                }

                assert field.getFieldType().getTypeDescriptor().charAt(0) == 'J' ||
                       field.getFieldType().getTypeDescriptor().charAt(0) == 'D';

                i.fixedInstruction = new Instruction22csf(Opcode.IGET_WIDE, (Instruction22cs)i.instruction, field);
                return true;
            }
            case IGET_OBJECT_QUICK:
            {
                Instruction22cs ins = (Instruction22cs)i.instruction;
                int registerNum = ins.getRegisterB();

                RegisterType regType = i.registerMap[registerNum];

                assert regType != RegisterType.NonReference && regType != RegisterType.Conflicted;

                //if the register type is Null, we can't determine the type of the register, and so we can't determine
                //what field is being accessed. What will really happen is that when it tries to access the field, it
                //will obviously throw a NPE. We can get this same effect by replacing this opcode with a call to
                //a method on java.lang.Object.
                //We actually just create an UnresolvedNullReference, which doesn't have any method/field info
                //associated with it, and let the caller choose which "default" method to call in this case
                if (regType == RegisterType.Null) {
                    i.fixedInstruction = new UnresolvedNullReference(i.instruction, registerNum);
                    i.propogateDeadness();
                    return true;
                }

                if (regType != RegisterType.Reference) {
                    return false;
                }

                String type = i.registerTypes[registerNum];
                if (type == null) {
                    return false;
                }

                FieldIdItem field = deodexerant.lookupField(type, ins.getFieldOffset());
                if (field == null) {
                    throw new RuntimeException("Could not find the field with offset " + ins.getFieldOffset() +
                            " for class: " + type);
                }

                assert field.getFieldType().getTypeDescriptor().charAt(0) == 'L' ||
                       field.getFieldType().getTypeDescriptor().charAt(0) == '[';

                i.fixedInstruction = new Instruction22csf(Opcode.IGET_OBJECT, (Instruction22cs)i.instruction, field);

                i.updateRegisterReferenceType(field.getFieldType().getTypeDescriptor());
                return true;
            }
            case IPUT_QUICK:
            {
                Instruction22cs ins = (Instruction22cs)i.instruction;
                int registerNum = ins.getRegisterB();

                RegisterType regType = i.registerMap[registerNum];

                assert regType != RegisterType.NonReference && regType != RegisterType.Conflicted;

                //if the register type is Null, we can't determine the type of the register, and so we can't determine
                //what field is being accessed. What will really happen is that when it tries to access the field, it
                //will obviously throw a NPE. We can get this same effect by replacing this opcode with a call to
                //a method on java.lang.Object.
                //We actually just create an Instruction2csn, which doesn't have any method/field info associated with
                //it, and let the caller choose which "default" method to call in this case
                if (regType == RegisterType.Null) {
                    i.fixedInstruction = new UnresolvedNullReference(i.instruction, registerNum);
                    return true;
                }

                if (regType != RegisterType.Reference) {
                    return false;
                }

                String type = i.registerTypes[registerNum];
                if (type == null) {
                    return false;
                }

                FieldIdItem field = deodexerant.lookupField(type, ins.getFieldOffset());
                if (field == null) {
                    throw new RuntimeException("Could not find the field with offset " + ins.getFieldOffset() +
                            " for class: " + type);
                }
                String fieldType = field.getFieldType().getTypeDescriptor();

                Opcode opcode;
                switch (fieldType.charAt(0)) {
                    case 'Z':
                        opcode = Opcode.IPUT_BOOLEAN;
                        break;
                    case 'B':
                        opcode = Opcode.IPUT_BYTE;
                        break;
                    case 'S':
                        opcode = Opcode.IPUT_SHORT;
                        break;
                    case 'C':
                        opcode = Opcode.IPUT_CHAR;
                        break;
                    case 'I':
                    case 'F':
                        opcode = Opcode.IPUT;
                        break;
                    default:
                        throw new RuntimeException("Unexpected field type for iput-quick opcode: " + fieldType);
                }

                i.fixedInstruction = new Instruction22csf(opcode, (Instruction22cs)i.instruction, field);

                return true;
            }
            case IPUT_WIDE_QUICK:
            {
                Instruction22cs ins = (Instruction22cs)i.instruction;
                int registerNum = ins.getRegisterB();

                RegisterType regType = i.registerMap[registerNum];

                assert regType != RegisterType.NonReference && regType != RegisterType.Conflicted;

                //if the register type is Null, we can't determine the type of the register, and so we can't determine
                //what field is being accessed. What will really happen is that when it tries to access the field, it
                //will obviously throw a NPE. We can get this same effect by replacing this opcode with a call to
                //a method on java.lang.Object.
                //We actually just create an Instruction2csn, which doesn't have any method/field info associated with
                //it, and let the caller choose which "default" method to call in this case
                if (regType == RegisterType.Null) {
                    i.fixedInstruction = new UnresolvedNullReference(i.instruction, registerNum);
                    return true;
                }

                if (regType != RegisterType.Reference) {
                    return false;
                }

                String type = i.registerTypes[registerNum];
                if (type == null) {
                    return false;
                }

                FieldIdItem field = deodexerant.lookupField(type, ins.getFieldOffset());
                if (field == null) {
                    throw new RuntimeException("Could not find the field with offset " + ins.getFieldOffset() +
                            " for class: " + type);
                }

                assert field.getFieldType().getTypeDescriptor().charAt(0) == 'J' ||
                       field.getFieldType().getTypeDescriptor().charAt(0) == 'D';

                i.fixedInstruction = new Instruction22csf(Opcode.IPUT_WIDE, (Instruction22cs)i.instruction, field);

                return true;
            }
            case IPUT_OBJECT_QUICK:
            {
                Instruction22cs ins = (Instruction22cs)i.instruction;
                int registerNum = ins.getRegisterB();

                RegisterType regType = i.registerMap[registerNum];

                assert regType != RegisterType.NonReference && regType != RegisterType.Conflicted;

                //if the register type is Null, we can't determine the type of the register, and so we can't determine
                //what field is being accessed. What will really happen is that when it tries to access the field, it
                //will obviously throw a NPE. We can get this same effect by replacing this opcode with a call to
                //a method on java.lang.Object.
                //We actually just create an Instruction2csn, which doesn't have any method/field info associated with
                //it, and let the caller choose which "default" method to call in this case
                if (regType == RegisterType.Null) {
                    i.fixedInstruction = new UnresolvedNullReference(i.instruction, registerNum);
                    return true;
                }

                if (regType != RegisterType.Reference) {
                    return false;
                }

                String type = i.registerTypes[registerNum];
                if (type == null) {
                    return false;
                }

                FieldIdItem field = deodexerant.lookupField(type, ins.getFieldOffset());
                if (field == null) {
                    throw new RuntimeException("Could not find the field with offset " + ins.getFieldOffset() +
                            " for class: " + type);
                }

                assert field.getFieldType().getTypeDescriptor().charAt(0) == 'L' ||
                       field.getFieldType().getTypeDescriptor().charAt(0) == '[';

                i.fixedInstruction = new Instruction22csf(Opcode.IPUT_OBJECT, (Instruction22cs)i.instruction, field);

                return true;
            }
            case INVOKE_VIRTUAL_QUICK:
            {
                Instruction35ms ins = ((Instruction35ms)i.instruction);
                int registerNum = ins.getRegisterD();

                RegisterType regType = i.registerMap[registerNum];

                assert regType != RegisterType.NonReference && regType != RegisterType.Conflicted;

                //if the register type is Null, we can't determine the type of the register, and so we can't determine
                //what method to call. What will really happen is that when it tries to call the method, it will
                //obviously throw a NPE. We can get this same effect by replacing this opcode with a call to
                //a method on java.lang.Object.
                //We actually just create an Instruction3msn, which doesn't have any method info associated with it,
                //and let the caller choose which "default" method to call in this case
                if (regType == RegisterType.Null) {
                    i.fixedInstruction = new UnresolvedNullReference(i.instruction, registerNum);
                    i.propogateDeadness();
                    return true;
                }

                if (regType != RegisterType.Reference) {
                    return false;
                }

                String type = i.registerTypes[registerNum];
                if (type == null) {
                    return false;
                }

                MethodIdItem method = deodexerant.lookupVirtualMethod(type, ins.getMethodIndex(), false);
                if (method == null) {
                    throw new RuntimeException("Could not find the virtual method with vtable index " +
                            ins.getMethodIndex() + " for class: " + type);
                }

                i.fixedInstruction = new Instruction35msf(Opcode.INVOKE_VIRTUAL, (Instruction35ms)i.instruction,
                        method);

                insn nextInstruction = i.getInstructionAtAddress(i.address + i.instruction.getSize(i.address));
                assert nextInstruction != null;
                if (nextInstruction.instruction.opcode == Opcode.MOVE_RESULT_OBJECT) {
                    nextInstruction.updateRegisterReferenceType(
                            method.getPrototype().getReturnType().getTypeDescriptor());
                }
                return true;
            }
            case INVOKE_VIRTUAL_QUICK_RANGE:
            {
                Instruction3rms ins = ((Instruction3rms)i.instruction);
                int registerNum = ins.getStartRegister();

                RegisterType regType = i.registerMap[registerNum];

                assert regType != RegisterType.NonReference && regType != RegisterType.Conflicted;

                //if the register type is Null, we can't determine the type of the register, and so we can't determine
                //what method to call. What will really happen is that when it tries to call the method, it will
                //obviously throw a NPE. We can get this same effect by replacing this opcode with a call to
                //a method on java.lang.Object.
                //We actually just create an Instruction3msn, which doesn't have any method info associated with it,
                //and let the caller choose which "default" method to call in this case
                if (regType == RegisterType.Null) {
                    i.fixedInstruction = new UnresolvedNullReference(i.instruction, registerNum);
                    i.propogateDeadness();
                    return true;
                }

                if (regType != RegisterType.Reference) {
                    return false;
                }

                String type = i.registerTypes[registerNum];
                if (type == null) {
                    return false;
                }

                MethodIdItem method = deodexerant.lookupVirtualMethod(type, ins.getMethodIndex(), false);
                if (method == null) {
                    throw new RuntimeException("Could not find the virtual method with vtable index " +
                            ins.getMethodIndex() + " for class: " + type);
                }

                i.fixedInstruction = new Instruction3rmsf(Opcode.INVOKE_VIRTUAL_RANGE, (Instruction3rms)i.instruction,
                        method);

                insn nextInstruction = i.getInstructionAtAddress(i.address + i.instruction.getSize(i.address));
                assert nextInstruction != null;
                if (nextInstruction.instruction.opcode == Opcode.MOVE_RESULT_OBJECT) {
                    nextInstruction.updateRegisterReferenceType(
                            method.getPrototype().getReturnType().getTypeDescriptor());
                }
                return true;
            }
            case INVOKE_SUPER_QUICK:
            {
                Instruction35ms ins = ((Instruction35ms)i.instruction);
                int registerNum = ins.getRegisterD();

                RegisterType regType = i.registerMap[registerNum];

                assert regType != RegisterType.NonReference && regType != RegisterType.Conflicted;

                //if the register type is Null, we can't determine the type of the register, and so we can't determine
                //what method to call. What will really happen is that when it tries to call the method, it will
                //obviously throw a NPE. We can get this same effect by replacing this opcode with a call to
                //a method on java.lang.Object.
                //We actually just create an Instruction3msn, which doesn't have any method info associated with it,
                //and let the caller choose which "default" method to call in this case
                if (regType == RegisterType.Null) {
                    i.fixedInstruction = new UnresolvedNullReference(i.instruction, registerNum);
                    //we need to mark any following instructions as dead
                    i.propogateDeadness();
                    return true;
                }

                if (regType != RegisterType.Reference) {
                    return false;
                }

                String type = i.registerTypes[registerNum];
                if (type == null) {
                    return false;
                }

                MethodIdItem method = deodexerant.lookupVirtualMethod(type, ins.getMethodIndex(), true);
                if (method == null) {
                    throw new RuntimeException("Could not find the super method with vtable index " +
                            ins.getMethodIndex() + " for class: " + type);
                }

                i.fixedInstruction = new Instruction35msf(Opcode.INVOKE_SUPER, (Instruction35ms)i.instruction,
                        method);

                insn nextInstruction = i.getInstructionAtAddress(i.address + i.instruction.getSize(i.address));
                assert nextInstruction != null;
                if (nextInstruction.instruction.opcode == Opcode.MOVE_RESULT_OBJECT) {
                    nextInstruction.updateRegisterReferenceType(
                            method.getPrototype().getReturnType().getTypeDescriptor());
                }
                return true;
            }
            case INVOKE_SUPER_QUICK_RANGE:
            {
                Instruction3rms ins = ((Instruction3rms)i.instruction);
                int registerNum = ins.getStartRegister();

                RegisterType regType = i.registerMap[registerNum];

                assert regType != RegisterType.NonReference && regType != RegisterType.Conflicted;

                //if the register type is Null, we can't determine the type of the register, and so we can't determine
                //what method to call. What will really happen is that when it tries to call the method, it will
                //obviously throw a NPE. We can get this same effect by replacing this opcode with a call to
                //a method on java.lang.Object.
                //We actually just create an Instruction3msn, which doesn't have any method info associated with it,
                //and let the caller choose which "default" method to call in this case
                if (regType == RegisterType.Null) {
                    i.fixedInstruction = new UnresolvedNullReference(i.instruction, registerNum);
                    i.propogateDeadness();
                    return true;
                }

                if (regType != RegisterType.Reference) {
                    return false;
                }

                String type = i.registerTypes[registerNum];
                if (type == null) {
                    return false;
                }

                MethodIdItem method = deodexerant.lookupVirtualMethod(type, ins.getMethodIndex(), true);
                if (method == null) {
                    throw new RuntimeException("Could not find the super method with vtable index " +
                            ins.getMethodIndex() + " for class: " + type);
                }

                i.fixedInstruction = new Instruction3rmsf(Opcode.INVOKE_SUPER_RANGE, (Instruction3rms)i.instruction,
                        method);

                insn nextInstruction = i.getInstructionAtAddress(i.address + i.instruction.getSize(i.address));
                assert nextInstruction != null;
                if (nextInstruction.instruction.opcode == Opcode.MOVE_RESULT_OBJECT) {
                    nextInstruction.updateRegisterReferenceType(
                            method.getPrototype().getReturnType().getTypeDescriptor());
                }
                return true;
            }
            default:
                throw new RuntimeException("Unexpected opcode " + i.instruction.opcode);
        }
    }

    public enum RegisterType {
        Unknown,
        Null,
        NonReference,
        Reference,
        Conflicted;

        private static RegisterType[][] mergeTable  =
                {
                       //Unknown        Null            Nonreference    Reference   Conflicted
                        {Unknown,       Null,           NonReference,   Reference,  Conflicted}, //Unknown
                        {Null,          Null,           NonReference,   Reference,  Conflicted}, //Null
                        {NonReference,  NonReference,   NonReference,   Conflicted, Conflicted}, //NonReference
                        {Reference,     Reference,      Conflicted,     Reference,  Conflicted}, //Referenced
                        {Conflicted,    Conflicted,     Conflicted,     Conflicted, Conflicted}, //Conflicted
                };

        public static RegisterType mergeRegisterTypes(RegisterType type1, RegisterType type2) {
            return mergeTable[type1.ordinal()][type2.ordinal()];
        }
    }

    public class insn {
        /**
         * The CodeItem that this instruction is a part of
         */
        public final CodeItem codeItem;
        /**
         * The actual instruction
         */
        public final Instruction instruction;
        /**
         * The code address of the instruction, in 2-byte instruction blocks
         */
        public final int address;
        /**
         * True if this instruction can throw an exception
         */
        public final boolean canThrow;

        /**
         * maps a code address to an insn
         */
        public final SparseArray<insn> insnsMap;

        /**
         * Instructions that execution could pass on to next
         */
        public LinkedList<insn> successors = new LinkedList<insn>();

        /**
         * Instructions that can pass on execution to this one
         */
        public LinkedList<insn> predecessors = new LinkedList<insn>();

        /**
         * If this instruction is in a try block, these are the first instructions for each
         * exception handler
         */
        public insn[] exceptionHandlers = null;

        /**
         * true if this instruction stores a value in a register
         */
        public boolean setsRegister = false;

        /**
         * true if this instruction sets a wide register. In this case, registerNum is the first of the
         * 2 registers
         */
        public boolean setsWideRegister = false;

        /**
         * If setsRegister is true, this is the instruction that is modified
         */
        public int registerNum;

        /**
         * If setsRegister is true, this is the register type of register that is modified
         */
        public RegisterType registerType;

        /**
         * if setsRegister is true, and the register type is a reference, this is the
         * reference type of the register, or null if not known yet.
         */
        public String registerReferenceType;

        /**
         * Stores a "fake" fixed instruction, which is included in the instruction list that deodexerizeCode produces
         */
        public Instruction fixedInstruction;

        /**
         * This is only used for odexed instructions, and should contain the register num of the object reference
         * that the instruction acts on. More specifically, it's only for odexed instructions that require the
         * type of the object register in order to look up the correct information.
         */
        public int objectRegisterNum = -1;

        /**
         * Whether this instruction can be the first instruction to successfully execute. This could be the first
         * instruction in the method, or if that instruction is covered by a try block, then the first instruction
         * in any of the exception handlers. Or if they are covered by try blocks... you get the idea
         */
        public boolean firstInstruction = false;

        /**
         * If this instruction has been visited in the course of determining the type of a register
         */
        public boolean visited = false;

        /**
         * If this is an odex instruction, and has been fixed.
         */
        public boolean fixed = false;

        /**
         * If this code is dead. Note that not all dead code is marked. Only the dead code that comes after an odexed
         * instruction can't be resolved because its object register is always null.
         */
        public boolean dead = false;

        public final RegisterType[] registerMap;
        public final String[] registerTypes;

        public insn(CodeItem codeItem, Instruction instruction, SparseArray<insn> insnsMap, int address) {
            this.codeItem = codeItem;
            this.instruction = instruction;
            this.address = address;
            this.canThrow = DeodexUtil.instructionThrowTable.get(instruction.opcode.value & 0xFF);
            this.insnsMap = insnsMap;

            if (instruction.opcode.odexOnly) {
                //we don't need INVOKE_EXECUTE_INLINE or EXECUTE_INLINE_RANGE here, because we don't need to know
                //the type of the object register in order to resolve which method is being called
                switch (instruction.opcode) {
                    case IGET_QUICK:
                    case IGET_WIDE_QUICK:
                    case IGET_OBJECT_QUICK:
                    case IPUT_QUICK:
                    case IPUT_WIDE_QUICK:
                    case IPUT_OBJECT_QUICK:
                        objectRegisterNum = ((Instruction22cs)instruction).getRegisterB();
                        break;
                    case INVOKE_VIRTUAL_QUICK:
                    case INVOKE_SUPER_QUICK:
                        objectRegisterNum = ((Instruction35ms)instruction).getRegisterD();
                        break;
                    case INVOKE_VIRTUAL_QUICK_RANGE:
                    case INVOKE_SUPER_QUICK_RANGE:
                        objectRegisterNum = ((Instruction3rms)instruction).getStartRegister();
                        break;
                    default:
                        break;
                }
            }

            registerMap = new RegisterType[codeItem.getRegisterCount()];
            registerTypes = new String[codeItem.getRegisterCount()];

            for (int i=0; i<registerMap.length; i++) {
                registerMap[i] = RegisterType.Unknown;
            }
        }

        private insn getInstructionAtAddress(int address) {
            insn i = insnsMap.get(address);
            assert i != null;
            return i;
        }

        public void init(insn previousInsn) {
            switch (instruction.opcode) {
                case NOP:
                    if (instruction instanceof ArrayDataPseudoInstruction ||
                        instruction instanceof PackedSwitchDataPseudoInstruction ||
                        instruction instanceof SparseSwitchDataPseudoInstruction) {
                        return;
                    }
                    break;
                case THROW:
                case RETURN:
                case RETURN_OBJECT:
                case RETURN_VOID:
                case RETURN_WIDE:
                    return;
                case GOTO:
                case GOTO_16:
                case GOTO_32:
                    addSuccessor(getInstructionAtAddress(address + ((OffsetInstruction)instruction).getTargetAddressOffset()));
                    return;
                case IF_EQ:
                case IF_GE:
                case IF_GT:
                case IF_LE:
                case IF_LT:
                case IF_NE:
                case IF_EQZ:
                case IF_GEZ:
                case IF_GTZ:
                case IF_LEZ:
                case IF_LTZ:
                case IF_NEZ:
                    addSuccessor(getInstructionAtAddress(address + ((OffsetInstruction)instruction).getTargetAddressOffset()));
                    break;
                case PACKED_SWITCH:
                case SPARSE_SWITCH:
                {
                    insn packedSwitchDataInsn =
                            getInstructionAtAddress(address + ((OffsetInstruction)instruction).getTargetAddressOffset());
                    assert packedSwitchDataInsn.instruction instanceof MultiOffsetInstruction;
                    MultiOffsetInstruction switchData =
                            (MultiOffsetInstruction)(packedSwitchDataInsn.instruction);
                    int[] packedSwitchTargets = switchData.getTargets();
                    for (int i=0; i<packedSwitchTargets.length; i++) {
                        addSuccessor(getInstructionAtAddress(address + packedSwitchTargets[i]));
                    }
                    break;
                }
                case MOVE_WIDE:
                case MOVE_WIDE_FROM16:
                case MOVE_WIDE_16:
                case NEG_LONG:
                case NOT_LONG:
                case NEG_DOUBLE:
                case INT_TO_LONG:
                case INT_TO_DOUBLE:
                case LONG_TO_DOUBLE:
                case FLOAT_TO_LONG:
                case FLOAT_TO_DOUBLE:
                case DOUBLE_TO_LONG:
                case ADD_LONG:
                case SUB_LONG:
                case MUL_LONG:
                case DIV_LONG:
                case REM_LONG:
                case AND_LONG:
                case OR_LONG:
                case XOR_LONG:
                case SHL_LONG:
                case SHR_LONG:
                case USHR_LONG:
                case ADD_DOUBLE:
                case SUB_DOUBLE:
                case MUL_DOUBLE:
                case DIV_DOUBLE:
                case REM_DOUBLE:
                case MOVE_RESULT_WIDE:
                case AGET_WIDE:
                case IGET_WIDE:
                case SGET_WIDE:
                case IGET_WIDE_QUICK:
                {
                    setsWideRegister = true;
                    //fall through
                }
                case MOVE:
                case MOVE_FROM16:
                case MOVE_16:
                case ARRAY_LENGTH:
                case NEG_INT:
                case NOT_INT:
                case NEG_FLOAT:
                case INT_TO_FLOAT:
                case LONG_TO_INT:
                case LONG_TO_FLOAT:
                case FLOAT_TO_INT:
                case DOUBLE_TO_INT:
                case DOUBLE_TO_FLOAT:
                case INT_TO_BYTE:
                case INT_TO_CHAR:
                case INT_TO_SHORT:
                case ADD_INT:
                case SUB_INT:
                case MUL_INT:
                case DIV_INT:
                case REM_INT:
                case AND_INT:
                case OR_INT:
                case XOR_INT:
                case SHL_INT:
                case SHR_INT:
                case USHR_INT:
                case ADD_FLOAT:
                case SUB_FLOAT:
                case MUL_FLOAT:
                case DIV_FLOAT:
                case REM_FLOAT:
                case ADD_INT_LIT16:
                case RSUB_INT:
                case MUL_INT_LIT16:
                case DIV_INT_LIT16:
                case REM_INT_LIT16:
                case AND_INT_LIT16:
                case OR_INT_LIT16:
                case XOR_INT_LIT16:
                case ADD_INT_LIT8:
                case RSUB_INT_LIT8:
                case MUL_INT_LIT8:
                case DIV_INT_LIT8:
                case REM_INT_LIT8:
                case AND_INT_LIT8:
                case OR_INT_LIT8:
                case XOR_INT_LIT8:
                case SHL_INT_LIT8:
                case SHR_INT_LIT8:
                case USHR_INT_LIT8:
                case MOVE_RESULT:
                case CMPL_FLOAT:
                case CMPG_FLOAT:
                case CMPL_DOUBLE:
                case CMPG_DOUBLE:
                case CMP_LONG:
                case AGET:
                case AGET_BOOLEAN:
                case AGET_BYTE:
                case AGET_CHAR:
                case AGET_SHORT:
                case IGET:
                case IGET_BOOLEAN:
                case IGET_BYTE:
                case IGET_CHAR:
                case IGET_SHORT:
                case SGET:
                case SGET_BOOLEAN:
                case SGET_BYTE:
                case SGET_CHAR:
                case SGET_SHORT:
                case IGET_QUICK:
                {
                    setsRegister = true;
                    registerNum = ((SingleRegisterInstruction)instruction).getRegisterA();
                    registerType = RegisterType.NonReference;
                    break;
                }
                case MOVE_OBJECT:
                case MOVE_OBJECT_FROM16:
                case MOVE_OBJECT_16:
                case AGET_OBJECT:
                case IGET_OBJECT_QUICK:
                {
                    setsRegister = true;
                    registerNum = ((SingleRegisterInstruction)instruction).getRegisterA();
                    registerType = RegisterType.Reference;
                    break;
                }
                case MOVE_RESULT_OBJECT:
                {
                    setsRegister = true;
                    if (previousInsn == null) {
                        throw new RuntimeException("This move-result-object instruction does not have an invoke" +
                        " instruction immediately prior to it");
                    }
                    registerNum = ((SingleRegisterInstruction)instruction).getRegisterA();
                    registerType = RegisterType.Reference;
                    registerReferenceType = getReturnType(previousInsn.instruction);
                    break;
                }
                case MOVE_EXCEPTION:
                {
                    setsRegister = true;
                    registerNum = ((SingleRegisterInstruction)instruction).getRegisterA();
                    registerType = RegisterType.Reference;
                    //typically, there will only be a single exception type for a particular handler block, so optimize
                    //the array size for that case, but support the case of multiple exception types as well
                    List<String> exceptionTypes = new ArrayList<String>(1);
                    for (CodeItem.TryItem tryItem: codeItem.getTries()) {
                        if (tryItem.encodedCatchHandler.getCatchAllHandlerAddress() == this.address) {
                            //if this is a catch all handler, the only possible type is Ljava/lang/Throwable;
                            registerReferenceType = "Ljava/lang/Throwable;";

                            //it's possible that Ljava/lang/Throwable; hasn't been interned into the dex file. Since
                            //we've turned off interning for the current dex file, we will just get a null back.
                            //This "shouldn't" be a problem, because if the type hasn't been interned, it's safe to
                            //say that there were no method/field accesses for that type, so we won't need to know
                            //the specific type of this register.

                            break;
                        }

                        for (CodeItem.EncodedTypeAddrPair handler: tryItem.encodedCatchHandler.handlers) {
                            if (handler.getHandlerAddress() == this.address) {
                                exceptionTypes.add(handler.exceptionType.getTypeDescriptor());
                            }
                        }
                    }
                    if (registerReferenceType == null) {
                        //optimize for the case when there is only a single exception type
                        if (exceptionTypes.size() == 1) {
                            registerReferenceType = exceptionTypes.get(0);
                        } else {
                            registerReferenceType = findCommonSuperclass(exceptionTypes);
                        }
                    }
                    break;
                }
                case CONST_WIDE_16:
                case CONST_WIDE_32:
                case CONST_WIDE:
                case CONST_WIDE_HIGH16:
                {
                    setsRegister = true;
                    registerNum = ((SingleRegisterInstruction)instruction).getRegisterA();
                    registerType = RegisterType.NonReference;
                    setsWideRegister = true;
                    break;
                }
                case CONST_4:
                case CONST_16:
                case CONST:
                case CONST_HIGH16:
                {
                    setsRegister = true;
                    registerNum = ((SingleRegisterInstruction)instruction).getRegisterA();
                    if (((LiteralInstruction)instruction).getLiteral() == 0) {
                        registerType = RegisterType.Null;
                    } else {
                        registerType = RegisterType.NonReference;
                    }
                    break;
                }
                case CONST_STRING:
                {
                    setsRegister = true;
                    registerNum = ((SingleRegisterInstruction)instruction).getRegisterA();
                    registerType = RegisterType.Reference;
                    registerReferenceType = "Ljava/lang/String;";
                    break;
                }
                case CONST_CLASS:
                {
                    setsRegister = true;
                    registerNum = ((SingleRegisterInstruction)instruction).getRegisterA();
                    registerType = RegisterType.Reference;
                    registerReferenceType = "Ljava/lang/Class;";
                    break;
                }
                case CHECK_CAST:
                case NEW_INSTANCE:
                case NEW_ARRAY:
                {
                    setsRegister = true;
                    registerNum = ((SingleRegisterInstruction)instruction).getRegisterA();
                    registerType = RegisterType.Reference;
                    registerReferenceType =
                         ((TypeIdItem)((InstructionWithReference)instruction).getReferencedItem()).getTypeDescriptor();
                    break;
                }
                case IGET_OBJECT:
                case SGET_OBJECT:
                {
                    setsRegister = true;
                    registerNum = ((SingleRegisterInstruction)instruction).getRegisterA();
                    registerType = RegisterType.Reference;
                    registerReferenceType = ((FieldIdItem)((InstructionWithReference)instruction).getReferencedItem())
                            .getFieldType().getTypeDescriptor();
                    break;
                }
            }

            //if we got here, then we can assume that it's possible for execution to continue on to the next
            //instruction. Otherwise, we would have returned from within the switch statement
            addSuccessor(getInstructionAtAddress(address + instruction.getSize(address)));
        }

        private String findCommonSuperclass(String type1, String type2) {
            if (type1 == null) {
                return type2;
            }
            if (type2 == null) {
                return type1;
            }

            if (type1.equals(type2)) {
                return type1;
            }

            return deodexerant.lookupCommonSuperclass(type1, type2);
        }

        private String findCommonSuperclass(List<String> exceptionTypes) {
            assert exceptionTypes.size() > 1;

            String supertype = exceptionTypes.get(0);

            for (int i=1; i<exceptionTypes.size(); i++) {
                supertype = deodexerant.lookupCommonSuperclass(supertype, exceptionTypes.get(i));
            }

            return supertype;
        }

        private void addSuccessor(insn i) {
            successors.add(i);
            i.predecessors.add(this);

            //if the next instruction can throw an exception, and is covered by exception handlers,
            //then the execution can in effect go directly from this instruction into the handler
            if (i.canThrow && i.exceptionHandlers != null) {
                for (insn handler: i.exceptionHandlers) {
                    addSuccessor(handler);
                }
            }
        }

        private String getReturnType(Instruction instruction) {
            switch (instruction.opcode) {
                case INVOKE_DIRECT:
                case INVOKE_INTERFACE:
                case INVOKE_STATIC:
                case INVOKE_SUPER:
                case INVOKE_VIRTUAL:
                case INVOKE_DIRECT_RANGE:
                case INVOKE_INTERFACE_RANGE:
                case INVOKE_STATIC_RANGE:
                case INVOKE_SUPER_RANGE:
                case INVOKE_VIRTUAL_RANGE:
                    return ((MethodIdItem)((InstructionWithReference)instruction).getReferencedItem()).getPrototype().
                            getReturnType().getTypeDescriptor();
                case FILLED_NEW_ARRAY:
                case FILLED_NEW_ARRAY_RANGE:
                    return ((TypeIdItem)((InstructionWithReference)instruction).getReferencedItem()).getTypeDescriptor();
            }
            return null;
        }

        private void initializeRegistersFromParams() {
            firstInstruction = true;
            MethodIdItem method = codeItem.getParent().method;

            int methodRegisters = codeItem.getRegisterCount();
            int paramRegisters = method.getPrototype().getParameterRegisterCount();

            if ((codeItem.getParent().accessFlags & AccessFlags.STATIC.getValue()) == 0) {
                int thisRegister = methodRegisters - paramRegisters - 1;
                registerMap[thisRegister] = RegisterType.Reference;
                registerTypes[thisRegister] = method.getContainingClass().getTypeDescriptor();
            }

            int paramRegister = methodRegisters - paramRegisters;
            TypeListItem parameters = method.getPrototype().getParameters();
            if (parameters != null) {
                for (TypeIdItem paramType: parameters.getTypes()) {
                    switch (paramType.getTypeDescriptor().charAt(0)) {
                        case 'Z':
                        case 'B':
                        case 'S':
                        case 'C':
                        case 'I':
                        case 'F':
                            registerMap[paramRegister++] = RegisterType.NonReference;
                            break;
                        case 'J':
                        case 'D':
                            registerMap[paramRegister++] = RegisterType.NonReference;
                            registerMap[paramRegister++] = RegisterType.NonReference;
                            break;
                        case 'L':
                        case '[':
                            registerMap[paramRegister] = RegisterType.Reference;
                            registerTypes[paramRegister++] = paramType.getTypeDescriptor();
                            break;
                        default:
                            assert false;
                            throw new RuntimeException("Unexpected type descriptor: " + paramType.getTypeDescriptor());
                    }
                }
            }

            if (exceptionHandlers != null && canThrow) {
                for (insn handlerinsn: exceptionHandlers) {
                    handlerinsn.initializeRegistersFromParams();
                }
            }
        }

        public void updateRegisterReferenceType(String type) {
            this.registerReferenceType = type;
            this.propagateRegisters();
        }

        /**
         * This is initially called when this instruction is an odexed instruction that can't be resolved because
         * the object register is always null. For each of its successor instructions, it checks if all of the other
         * predecessors of that successor are also "dead". If so, it marks it as dead and recursively calls
         * propogateDeadness on it. The effect is that all the code that follows after the initial unresolved
         * instruction is marked dead, until a non-dead code path merges with the dead code path (and it becomes...
         * undead. mmMMmmMMmm brains)
         */
        public void propogateDeadness() {
            for (insn successor: successors) {
                //the first instruction of the method (or the first instruction of any exception handlers covering
                //the first instruction) can never be dead
                if (successor.firstInstruction) {
                    continue;
                }

                boolean allSucessorsPredecessorsDead = true;
                for (insn successorPredecessor: successor.predecessors) {
                    if (successorPredecessor != this) {
                        if (!successorPredecessor.dead) {
                            allSucessorsPredecessorsDead = false;
                            break;
                        }
                    }
                }
                if (allSucessorsPredecessorsDead) {
                    successor.dead = true;
                    successor.propogateDeadness();
                }
            }
        }

        public void propagateRegisters() {
            visited = true;

            //if this is the first instruction, we're in a try block, then if the first instruction throws an
            //exception, execution could go directly into one of the handlers. We need to handle this recursively,
            //i.e. if the first instruction in the exception handler is also covered by a try block..
            if (this.firstInstruction && canThrow && exceptionHandlers != null) {
                for (insn handler: exceptionHandlers) {
                    handler.propagateRegisters();
                }
            }

            //if the next instruction is an odexed instruction and requires the type of it's object
            //register to figure out the correct method/field to use, then objectRegisterNum will
            //be set to the register number containing the object reference that it uses.
            //if that instruction has already been fixed, but we have newer information and update
            //the register type, we need to clear out the fixed instruction, so it gets re-fixed,
            //with the new register information

            for (insn nextInsn: successors) {
                boolean somethingChanged = false;

                for (int i=0; i<registerMap.length; i++) {
                    boolean skipReg = (setsRegister && i == this.registerNum) ||
                            (setsWideRegister & i == registerNum+1);

                    if (!skipReg)
                    {
                        RegisterType regType = RegisterType.mergeRegisterTypes(registerMap[i],
                                nextInsn.registerMap[i]);
                        if (regType != nextInsn.registerMap[i]) {
                            somethingChanged = true;
                            nextInsn.registerMap[i] = regType;
                        }
                        if (regType == RegisterType.Reference) {
                            String regReferenceType = findCommonSuperclass(registerTypes[i],
                                    nextInsn.registerTypes[i]);
                            if (regReferenceType != null && !regReferenceType.equals(nextInsn.registerTypes[i])) {
                                //see comment above for loop
                                if (i == nextInsn.objectRegisterNum) {
                                    nextInsn.fixedInstruction = null;
                                }
                                somethingChanged = true;
                                nextInsn.registerTypes[i] = regReferenceType;
                            }
                        }
                    }
                }

                if (this.setsRegister) {
                    if (nextInsn.registerMap[registerNum] != registerType) {
                        somethingChanged = true;
                        nextInsn.registerMap[registerNum] = registerType;
                    }
                    if (registerType == RegisterType.Reference) {
                        if (registerReferenceType != null) {
                            if (!registerReferenceType.equals(nextInsn.registerTypes[registerNum])) {
                                //see comment above for loop
                                if (registerNum == nextInsn.objectRegisterNum) {
                                    nextInsn.fixedInstruction = null;
                                }

                                somethingChanged = true;
                                nextInsn.registerTypes[registerNum] = registerReferenceType;
                            }
                        } else {
                            String type = destRegisterType();

                            if (type != null && !type.equals(nextInsn.registerTypes[registerNum])) {
                                //see comment above for loop
                                if (registerNum == nextInsn.objectRegisterNum) {
                                    nextInsn.fixedInstruction = null;
                                }

                                somethingChanged = true;
                                nextInsn.registerTypes[registerNum] = type;
                            }
                        }
                    }

                    if (this.setsWideRegister) {
                        if (nextInsn.registerMap[registerNum + 1] != RegisterType.NonReference) {
                            somethingChanged = true;
                            nextInsn.registerMap[registerNum + 1] = RegisterType.NonReference;
                        }
                    }
                }

                if (somethingChanged || !nextInsn.visited) {
                    nextInsn.propagateRegisters();
                }
            }
        }

        private String destRegisterType() {
            if (registerReferenceType != null) {
                return registerReferenceType;
            }

            switch (instruction.opcode) {
                case MOVE_OBJECT:
                case MOVE_OBJECT_FROM16:
                case MOVE_OBJECT_16:
                {
                    int registerNum = ((TwoRegisterInstruction)instruction).getRegisterB();
                    assert registerMap[registerNum] == RegisterType.Reference ||
                           registerMap[registerNum] == RegisterType.Null;
                    return registerTypes[registerNum];
                }
                case AGET_OBJECT:
                {
                    int registerNum = ((TwoRegisterInstruction)instruction).getRegisterB();
                    assert registerMap[registerNum] == RegisterType.Reference ||
                           registerMap[registerNum] == RegisterType.Null;

                    String type = registerTypes[registerNum];
                    if (type == null) {
                        return null;
                    }

                    assert type.charAt(0) == '[';
                    return type.substring(1);
                }
                case MOVE_RESULT_OBJECT:
                case IGET_OBJECT_QUICK:
                    return null;
                default:
                    assert false;
                    return null;
            }
        }
    }
}