/*
 * Copyright 2012, Google Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.dexlib2.writer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jf.dexlib2.Format;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.ReferenceType;
import org.jf.dexlib2.iface.ExceptionHandler;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.TryBlock;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.OffsetInstruction;
import org.jf.dexlib2.iface.instruction.ReferenceInstruction;
import org.jf.dexlib2.iface.instruction.SwitchElement;
import org.jf.dexlib2.iface.instruction.formats.*;
import org.jf.dexlib2.iface.reference.*;
import org.jf.dexlib2.util.InstructionUtil;
import org.jf.dexlib2.util.MethodUtil;
import org.jf.dexlib2.util.ReferenceUtil;
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CodeItemPool {
    @Nonnull private final Map<Method, Integer> codeItemOffsetMap = Maps.newHashMap();
    @Nonnull private final DexFile dexFile;
    private int sectionOffset = -1;

    public CodeItemPool(@Nonnull DexFile dexFile) {
        this.dexFile = dexFile;
    }

    public void intern(@Nonnull Method method) {
        // TODO: can we have parameter names (in the debug_info_item), without having any other sort of method implementation
        // this also handles parameter names, which aren't directly tied to the MethodImplementation, even though the debug items are
        boolean hasDebugInfo = dexFile.debugInfoPool.intern(method);
        boolean hasInstruction = false;

        MethodImplementation methodImpl = method.getImplementation();
        if (methodImpl != null) {
            for (Instruction instruction: methodImpl.getInstructions()) {
                hasInstruction = true;
                if (instruction instanceof ReferenceInstruction) {
                    Reference reference = ((ReferenceInstruction)instruction).getReference();
                    switch (instruction.getOpcode().referenceType) {
                        case ReferenceType.STRING:
                            dexFile.stringPool.intern((StringReference) reference);
                            break;
                        case ReferenceType.TYPE:
                            dexFile.typePool.intern((TypeReference)reference);
                            break;
                        case ReferenceType.FIELD:
                            dexFile.fieldPool.intern((FieldReference) reference);
                            break;
                        case ReferenceType.METHOD:
                            dexFile.methodPool.intern((MethodReference)reference);
                            break;
                        default:
                            throw new ExceptionWithContext("Unrecognized reference type: %d",
                                    instruction.getOpcode().referenceType);
                    }
                }
            }

            List<? extends TryBlock> tryBlocks = methodImpl.getTryBlocks();
            if (!hasInstruction && tryBlocks.size() > 0) {
                throw new ExceptionWithContext("Method %s has no instructions, but has try blocks.",
                        ReferenceUtil.getMethodDescriptor(method));
            }

            for (TryBlock tryBlock: methodImpl.getTryBlocks()) {
                for (ExceptionHandler handler: tryBlock.getExceptionHandlers()) {
                    dexFile.typePool.internNullable(handler.getExceptionType());
                }
            }
        }

        if (hasDebugInfo || hasInstruction) {
            codeItemOffsetMap.put(method, 0);
        }
    }

    public int getOffset(@Nonnull Method method) {
        Integer offset = codeItemOffsetMap.get(method);
        if (offset == null) {
            return 0;
        }
        return offset;
    }

    public int getNumItems() {
        return codeItemOffsetMap.size();
    }

    public int getSectionOffset() {
        if (sectionOffset < 0) {
            throw new ExceptionWithContext("Section offset has not been set yet!");
        }
        return sectionOffset;
    }

    public void write(@Nonnull DexWriter writer) throws IOException {
        ByteArrayOutputStream ehBuf = new ByteArrayOutputStream();

        writer.align();
        sectionOffset = writer.getPosition();
        List<Method> methods = Lists.newArrayList(codeItemOffsetMap.keySet());
        Collections.sort(methods);
        for (Method method: methods) {
            writer.align();
            codeItemOffsetMap.put(method, writer.getPosition());

            MethodImplementation methodImpl = method.getImplementation();
            if (methodImpl != null) {
                writer.writeUshort(methodImpl.getRegisterCount());
                writer.writeUshort(MethodUtil.getParameterRegisterCount(method, MethodUtil.isStatic(method)));

                int maxOutParamCount = 0;
                int codeUnitCount = 0;
                for (Instruction instruction: methodImpl.getInstructions()) {
                    codeUnitCount += instruction.getCodeUnits();
                    if (instruction.getOpcode().referenceType == ReferenceType.METHOD) {
                        ReferenceInstruction refInsn = (ReferenceInstruction)instruction;
                        MethodReference methodRef = (MethodReference)refInsn.getReference();
                        int paramCount = MethodUtil.getParameterRegisterCount(methodRef,
                                InstructionUtil.isInvokeStatic(instruction.getOpcode()));
                        if (paramCount > maxOutParamCount) {
                            maxOutParamCount = paramCount;
                        }
                    }
                }
                writer.writeUshort(maxOutParamCount);

                ArrayList<Integer> codeOffsetShifts = findCodeOffsetShifts(methodImpl);
                if (codeOffsetShifts != null) {
                    codeUnitCount += codeOffsetShifts.size();
                }

                List<? extends TryBlock> tryBlocks = methodImpl.getTryBlocks();
                writer.writeUshort(tryBlocks.size());
                writer.writeInt(dexFile.debugInfoPool.getOffset(method));
                writer.writeInt(codeUnitCount);

                // TODO: need to fix up instructions. Add alignment nops, etc.

                for (Instruction instruction: methodImpl.getInstructions()) {
                    switch (instruction.getOpcode().format) {
                        case Format10t:
                            int codeOffsetDelta = codeOffsetShift(codeOffsetShifts, ((OffsetInstruction) instruction).getCodeOffset());
                            writeFormat10t(writer, ((Instruction10t)instruction), codeOffsetDelta);
                            break;
                        case Format10x:
                            writeFormat10x(writer, (Instruction10x)instruction);
                            break;
                        case Format11n:
                            writeFormat11n(writer, (Instruction11n)instruction);
                            break;
                        case Format11x:
                            writeFormat11x(writer, (Instruction11x)instruction);
                            break;
                        case Format12x:
                            writeFormat12x(writer, (Instruction12x)instruction);
                            break;
                        case Format20t:
                            codeOffsetDelta = codeOffsetShift(codeOffsetShifts, ((OffsetInstruction) instruction).getCodeOffset());
                            writeFormat20t(writer, (Instruction20t)instruction, codeOffsetDelta);
                            break;
                        case Format21c:
                            writeFormat21c(writer, (Instruction21c)instruction);
                            break;
                        case Format21ih:
                            writeFormat21ih(writer, (Instruction21ih)instruction);
                            break;
                        case Format21lh:
                            writeFormat21lh(writer, (Instruction21lh)instruction);
                            break;
                        case Format21s:
                            writeFormat21s(writer, (Instruction21s)instruction);
                            break;
                        case Format21t:
                            codeOffsetDelta = codeOffsetShift(codeOffsetShifts, ((OffsetInstruction) instruction).getCodeOffset());
                            writeFormat21t(writer, (Instruction21t)instruction, codeOffsetDelta);
                            break;
                        case Format22b:
                            writeFormat22b(writer, (Instruction22b)instruction);
                            break;
                        case Format22c:
                            writeFormat22c(writer, (Instruction22c)instruction);
                            break;
                        case Format22s:
                            writeFormat22s(writer, (Instruction22s)instruction);
                            break;
                        case Format22t:
                            codeOffsetDelta = codeOffsetShift(codeOffsetShifts, ((OffsetInstruction) instruction).getCodeOffset());
                            writeFormat22t(writer, (Instruction22t)instruction, codeOffsetDelta);
                            break;
                        case Format22x:
                            writeFormat22x(writer, (Instruction22x)instruction);
                            break;
                        case Format23x:
                            writeFormat23x(writer, (Instruction23x)instruction);
                            break;
                        case Format30t:
                            codeOffsetDelta = codeOffsetShift(codeOffsetShifts, ((OffsetInstruction) instruction).getCodeOffset());
                            writeFormat30t(writer, (Instruction30t)instruction, codeOffsetDelta);
                            break;
                        case Format31c:
                            writeFormat31c(writer, (Instruction31c)instruction);
                            break;
                        case Format31i:
                            writeFormat31i(writer, (Instruction31i)instruction);
                            break;
                        case Format31t:
                            codeOffsetDelta = codeOffsetShift(codeOffsetShifts, ((OffsetInstruction) instruction).getCodeOffset());
                            writeFormat31t(writer, (Instruction31t)instruction, codeOffsetDelta);
                            break;
                        case Format32x:
                            writeFormat32x(writer, (Instruction32x)instruction);
                            break;
                        case Format35c:
                            writeFormat35c(writer, (Instruction35c)instruction);
                            break;
                        case Format3rc:
                            writeFormat3rc(writer, (Instruction3rc)instruction);
                            break;
                        case Format51l:
                            writeFormat51l(writer, (Instruction51l)instruction);
                            break;
                        case ArrayPayload:
                            writeArrayPayload(writer, (ArrayPayload)instruction);
                            break;
                        case SparseSwitchPayload:
                            writeSparseSwitchPayload(writer, (SparseSwitchPayload)instruction);
                            break;
                        case PackedSwitchPayload:
                            writePackedSwitchPayload(writer, (PackedSwitchPayload)instruction);
                            break;
                        default:
                            throw new ExceptionWithContext("Unexpected format: %s", instruction.getOpcode().format);
                    }
                }

                if (tryBlocks.size() > 0) {
                    writer.align();

                    // filter out unique lists of exception handlers
                    Map<List<? extends ExceptionHandler>, Integer> exceptionHandlerOffsetMap = Maps.newHashMap();
                    for (TryBlock tryBlock: tryBlocks) {
                        exceptionHandlerOffsetMap.put(tryBlock.getExceptionHandlers(), 0);
                    }
                    DexWriter.writeUleb128(ehBuf, exceptionHandlerOffsetMap.size());

                    for (TryBlock tryBlock: tryBlocks) {
                        int startAddress = tryBlock.getStartCodeAddress();
                        int endAddress = startAddress + tryBlock.getCodeUnitCount();

                        startAddress += codeOffsetShift(codeOffsetShifts, startAddress);
                        endAddress += codeOffsetShift(codeOffsetShifts, endAddress);
                        int tbCodeUnitCount = endAddress - startAddress;

                        writer.writeInt(startAddress);
                        writer.writeUshort(tbCodeUnitCount);

                        if (tryBlock.getExceptionHandlers().size() == 0) {
                            throw new ExceptionWithContext("No exception handlers for the try block!");
                        }

                        Integer offset = exceptionHandlerOffsetMap.get(tryBlock.getExceptionHandlers());
                        if (offset != 0) {
                            // exception handler has already been written out, just use it
                            writer.writeUshort(offset);
                        } else {
                            // if offset has not been set yet, we are about to write out a new exception handler
                            offset = ehBuf.size();
                            writer.writeUshort(offset);
                            exceptionHandlerOffsetMap.put(tryBlock.getExceptionHandlers(), offset);

                            // check if the last exception handler is a catch-all and adjust the size accordingly
                            int ehSize = tryBlock.getExceptionHandlers().size();
                            ExceptionHandler ehLast = tryBlock.getExceptionHandlers().get(ehSize-1);
                            if (ehLast.getExceptionType() == null) {
                                ehSize = ehSize * (-1) + 1;
                            }

                            // now let's layout the exception handlers, assuming that catch-all is always last
                            DexWriter.writeSleb128(ehBuf, ehSize);
                            for (ExceptionHandler eh : tryBlock.getExceptionHandlers()) {
                                String exceptionType = eh.getExceptionType();
                                int codeAddress = eh.getHandlerCodeAddress();
                                codeAddress += codeOffsetShift(codeOffsetShifts, codeAddress);

                                if (exceptionType != null) {
                                    //regular exception handling
                                    DexWriter.writeUleb128(ehBuf, dexFile.typePool.getIndex(exceptionType));
                                    DexWriter.writeUleb128(ehBuf, codeAddress);
                                } else {
                                    //catch-all
                                    DexWriter.writeUleb128(ehBuf, codeAddress);
                                }
                            }
                        }
                    }

                    if (ehBuf.size() > 0) {
                        ehBuf.writeTo(writer);
                        ehBuf.reset();
                    }
                }
            }
        }
    }

    private int codeOffsetShift(ArrayList<Integer> codeOffsetShifts, int offset) {
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
    private ArrayList<Integer> findCodeOffsetShifts(MethodImplementation methodImpl) {
        ArrayList<Integer> codeOffsetShifts = null;

        // first, process const-string to const-string/jumbo conversions
        int currentCodeOffset = 0;
        for (Instruction instruction: methodImpl.getInstructions()) {
            if (instruction.getOpcode().equals(Opcode.CONST_STRING)) {
                int referenceIndex = getReferenceIndex((Instruction21c)instruction);
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
            return null;
        }

        // now, let's check if this caused any conversions in goto instructions due to changes in offset values
        // since code offset delta is equivalent to the position of instruction's code offset in the shift list,
        // we use it as a position here
        currentCodeOffset = 0;
        for (Instruction instruction: methodImpl.getInstructions()) {
            if (instruction.getOpcode().format.equals(Format.Format10t)) {
                int codeOffset = ((Instruction10t)instruction).getCodeOffset();
                int codeOffsetDelta = codeOffsetShift(codeOffsetShifts, codeOffset);
                if (codeOffset+codeOffsetDelta > 0xFFFF) {
                    // handling very small (negligible) possiblity of goto becoming goto/32
                    // we insert extra 1 code unit shift referring to the same position
                    // this will cause subsequent code offsets to be shifted by 2 code units
                    codeOffsetShifts.add(codeOffsetDelta, currentCodeOffset);
                }
                if (codeOffset+codeOffsetDelta > 0xFF) {
                    codeOffsetShifts.add(codeOffsetDelta, currentCodeOffset);
                }
            } else if (instruction.getOpcode().format.equals(Format.Format20t)) {
                int codeOffset = ((Instruction20t)instruction).getCodeOffset();
                int codeOffsetDelta = codeOffsetShift(codeOffsetShifts, codeOffset);
                if (codeOffset+codeOffsetDelta > 0xFFFF) {
                    codeOffsetShifts.add(codeOffsetDelta, currentCodeOffset);
                }
            }
            currentCodeOffset += instruction.getCodeUnits();
        }

        return codeOffsetShifts;
    }

    private static int packNibbles(int a, int b) {
        return (b << 4) | a;
    }

    private int getReferenceIndex(ReferenceInstruction referenceInstruction) {
        switch (referenceInstruction.getOpcode().referenceType) {
            case ReferenceType.FIELD:
                return dexFile.fieldPool.getIndex((FieldReference)referenceInstruction.getReference());
            case ReferenceType.METHOD:
                return dexFile.methodPool.getIndex((MethodReference)referenceInstruction.getReference());
            case ReferenceType.STRING:
                return dexFile.stringPool.getIndex((StringReference)referenceInstruction.getReference());
            case ReferenceType.TYPE:
                return dexFile.typePool.getIndex((TypeReference)referenceInstruction.getReference());
            default:
                throw new ExceptionWithContext("Unknown reference type: %d",
                        referenceInstruction.getOpcode().referenceType);
        }
    }

    public void writeFormat10t(@Nonnull DexWriter writer, @Nonnull Instruction10t instruction, int codeOffsetDelta) throws IOException {
        int codeOffset = instruction.getCodeOffset() + codeOffsetDelta;
        if (codeOffset > 0xFFFF) {
            writer.write(Opcode.GOTO_32.value);
            writer.writeInt(codeOffset);
        } else if (codeOffset > 0xFF) {
            writer.write(Opcode.GOTO_16.value);
            writer.writeUshort(codeOffset);
        } else {
            writer.write(instruction.getOpcode().value);
            writer.write(codeOffset);
        }
    }

    public void writeFormat10x(@Nonnull DexWriter writer, @Nonnull Instruction10x instruction) throws IOException {
        writer.write(instruction.getOpcode().value);
        writer.write(0);
    }

    public void writeFormat11n(@Nonnull DexWriter writer, @Nonnull Instruction11n instruction) throws IOException {
        writer.write(instruction.getOpcode().value);
        writer.write(packNibbles(instruction.getRegisterA(), instruction.getNarrowLiteral()));
    }

    public void writeFormat11x(@Nonnull DexWriter writer, @Nonnull Instruction11x instruction) throws IOException {
        writer.write(instruction.getOpcode().value);
        writer.write(instruction.getRegisterA());
    }

    public void writeFormat12x(@Nonnull DexWriter writer, @Nonnull Instruction12x instruction) throws IOException {
        writer.write(instruction.getOpcode().value);
        writer.write(packNibbles(instruction.getRegisterA(), instruction.getRegisterB()));
    }

    public void writeFormat20t(@Nonnull DexWriter writer, @Nonnull Instruction20t instruction, int codeOffsetDelta) throws IOException {
        int codeOffset = instruction.getCodeOffset() + codeOffsetDelta;
        if (codeOffset > 0xFFFF) {
            writer.write(Opcode.GOTO_32.value);
            writer.write(0);
            writer.writeInt(codeOffset);
        } else {
            writer.write(instruction.getOpcode().value);
            writer.write(0);
            writer.writeShort(codeOffset);
        }
    }

    public void writeFormat21c(@Nonnull DexWriter writer, @Nonnull Instruction21c instruction) throws IOException {
        int referenceIndex = getReferenceIndex(instruction);
        if (referenceIndex > 0xFFFF && instruction.getOpcode().hasJumboOpcode()) {
            // convert to jumbo instruction
            writer.write(instruction.getOpcode().getJumboOpcode().value);
            writer.write(instruction.getRegisterA());
            writer.writeInt(referenceIndex);
        } else {
            writer.write(instruction.getOpcode().value);
            writer.write(instruction.getRegisterA());
            writer.writeUshort(referenceIndex);
        }
    }

    public void writeFormat21ih(@Nonnull DexWriter writer, @Nonnull Instruction21ih instruction) throws IOException {
        writer.write(instruction.getOpcode().value);
        writer.write(instruction.getRegisterA());
        writer.writeShort(instruction.getHatLiteral());
    }

    public void writeFormat21lh(@Nonnull DexWriter writer, @Nonnull Instruction21lh instruction) throws IOException {
        writer.write(instruction.getOpcode().value);
        writer.write(instruction.getRegisterA());
        writer.writeShort(instruction.getHatLiteral());
    }

    public void writeFormat21s(@Nonnull DexWriter writer, @Nonnull Instruction21s instruction) throws IOException {
        writer.write(instruction.getOpcode().value);
        writer.write(instruction.getRegisterA());
        writer.writeShort(instruction.getNarrowLiteral());
    }

    public void writeFormat21t(@Nonnull DexWriter writer, @Nonnull Instruction21t instruction, int codeOffsetDelta) throws IOException {
        writer.write(instruction.getOpcode().value);
        writer.write(instruction.getRegisterA());
        writer.writeShort(instruction.getCodeOffset() + codeOffsetDelta);
    }

    public void writeFormat22b(@Nonnull DexWriter writer, @Nonnull Instruction22b instruction) throws IOException {
        writer.write(instruction.getOpcode().value);
        writer.write(instruction.getRegisterA());
        writer.write(instruction.getRegisterB());
        writer.write(instruction.getNarrowLiteral());
    }

    public void writeFormat22c(@Nonnull DexWriter writer, @Nonnull Instruction22c instruction) throws IOException {
        writer.write(instruction.getOpcode().value);
        writer.write(packNibbles(instruction.getRegisterA(), instruction.getRegisterB()));
        writer.writeUshort(getReferenceIndex(instruction));
    }

    public void writeFormat22s(@Nonnull DexWriter writer, @Nonnull Instruction22s instruction) throws IOException {
        writer.write(instruction.getOpcode().value);
        writer.write(packNibbles(instruction.getRegisterA(), instruction.getRegisterB()));
        writer.writeShort(instruction.getNarrowLiteral());
    }

    public void writeFormat22t(@Nonnull DexWriter writer, @Nonnull Instruction22t instruction, int codeOffsetDelta) throws IOException {
        writer.write(instruction.getOpcode().value);
        writer.write(packNibbles(instruction.getRegisterA(), instruction.getRegisterB()));
        writer.writeShort(instruction.getCodeOffset() + codeOffsetDelta);
    }

    public void writeFormat22x(@Nonnull DexWriter writer, @Nonnull Instruction22x instruction) throws IOException {
        writer.write(instruction.getOpcode().value);
        writer.write(instruction.getRegisterA());
        writer.writeUshort(instruction.getRegisterB());
    }

    public void writeFormat23x(@Nonnull DexWriter writer, @Nonnull Instruction23x instruction) throws IOException {
        writer.write(instruction.getOpcode().value);
        writer.write(instruction.getRegisterA());
        writer.write(instruction.getRegisterB());
        writer.write(instruction.getRegisterC());
    }

    public void writeFormat30t(@Nonnull DexWriter writer, @Nonnull Instruction30t instruction, int codeOffsetDelta) throws IOException {
        writer.write(instruction.getOpcode().value);
        writer.write(0);
        writer.writeInt(instruction.getCodeOffset() + codeOffsetDelta);
    }

    public void writeFormat31c(@Nonnull DexWriter writer, @Nonnull Instruction31c instruction) throws IOException {
        writer.write(instruction.getOpcode().value);
        writer.write(instruction.getRegisterA());
        writer.writeInt(getReferenceIndex(instruction));
    }

    public void writeFormat31i(@Nonnull DexWriter writer, @Nonnull Instruction31i instruction) throws IOException {
        writer.write(instruction.getOpcode().value);
        writer.write(instruction.getRegisterA());
        writer.writeInt(instruction.getNarrowLiteral());
    }

    public void writeFormat31t(@Nonnull DexWriter writer, @Nonnull Instruction31t instruction, int codeOffsetDelta) throws IOException {
        writer.write(instruction.getOpcode().value);
        writer.write(instruction.getRegisterA());
        writer.writeInt(instruction.getCodeOffset() + codeOffsetDelta);
    }

    public void writeFormat32x(@Nonnull DexWriter writer, @Nonnull Instruction32x instruction) throws IOException {
        writer.write(instruction.getOpcode().value);
        writer.write(0);
        writer.writeUshort(instruction.getRegisterA());
        writer.writeUshort(instruction.getRegisterB());
    }

    public void writeFormat35c(@Nonnull DexWriter writer, @Nonnull Instruction35c instruction) throws IOException {
        writer.write(instruction.getOpcode().value);
        writer.write(packNibbles(instruction.getRegisterG(), instruction.getRegisterCount()));
        writer.writeUshort(getReferenceIndex(instruction));
        writer.write(packNibbles(instruction.getRegisterC(), instruction.getRegisterD()));
        writer.write(packNibbles(instruction.getRegisterE(), instruction.getRegisterF()));
    }

    public void writeFormat3rc(@Nonnull DexWriter writer, @Nonnull Instruction3rc instruction) throws IOException {
        writer.write(instruction.getOpcode().value);
        writer.write(instruction.getRegisterCount());
        writer.writeUshort(getReferenceIndex(instruction));
        writer.writeUshort(instruction.getStartRegister());
    }

    public void writeFormat51l(@Nonnull DexWriter writer, @Nonnull Instruction51l instruction) throws IOException {
        writer.write(instruction.getOpcode().value);
        writer.write(instruction.getRegisterA());
        writer.writeLong(instruction.getWideLiteral());
    }

    public void writeArrayPayload(@Nonnull DexWriter writer, @Nonnull ArrayPayload instruction) throws IOException {
        writer.writeUshort(instruction.getOpcode().value);
        writer.writeUshort(instruction.getElementWidth());
        List<Number> elements = instruction.getArrayElements();
        writer.writeInt(elements.size());
        // TODO: validate that dalvik only allows these element widths
        switch (instruction.getElementWidth()) {
            case 1:
                for (Number element: elements) {
                    writer.write(element.byteValue());
                }
                break;
            case 2:
                for (Number element: elements) {
                    writer.writeShort(element.shortValue());
                }
                break;
            case 4:
                for (Number element: elements) {
                    writer.writeInt(element.intValue());
                }
                break;
            case 8:
                for (Number element: elements) {
                    writer.writeLong(element.longValue());
                }
                break;
        }
        if ((writer.getPosition() & 1) != 0) {
            writer.write(0);
        }
    }

    public void writeSparseSwitchPayload(@Nonnull DexWriter writer, @Nonnull SparseSwitchPayload instruction)
            throws IOException {
        writer.writeUshort(instruction.getOpcode().value);
        List<? extends SwitchElement> elements = instruction.getSwitchElements();
        writer.writeUshort(elements.size());
        for (SwitchElement element: elements) {
            writer.writeInt(element.getKey());
        }
        for (SwitchElement element: elements) {
            writer.writeInt(element.getOffset());
        }
    }

    public void writePackedSwitchPayload(@Nonnull DexWriter writer, @Nonnull PackedSwitchPayload instruction)
            throws IOException {
        writer.writeUshort(instruction.getOpcode().value);
        List<? extends SwitchElement> elements = instruction.getSwitchElements();
        writer.writeUshort(elements.size());
        writer.writeInt(elements.get(0).getKey());
        for (SwitchElement element: elements) {
            writer.writeInt(element.getOffset());
        }
    }
}
