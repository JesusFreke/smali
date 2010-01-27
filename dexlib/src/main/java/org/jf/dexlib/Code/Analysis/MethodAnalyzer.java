package org.jf.dexlib.Code.Analysis;

import org.jf.dexlib.*;
import org.jf.dexlib.Code.*;
import org.jf.dexlib.Code.Format.ArrayDataPseudoInstruction;
import org.jf.dexlib.Code.Format.Format;
import org.jf.dexlib.Util.*;

import java.util.*;

public class MethodAnalyzer {
    private final ClassDataItem.EncodedMethod encodedMethod;

    private SparseArray<AnalyzedInstruction> instructions;

    private boolean analyzed = false;

    //This is a dummy instruction that occurs immediately before the first real instruction. We can initialize the
    //register types for this instruction to the parameter types, in order to have them propagate to all of its
    //successors, e.g. the first real instruction, the first instructions in any exception handlers covering the first
    //instruction, etc.
    private AnalyzedInstruction startOfMethod;

    public MethodAnalyzer(ClassDataItem.EncodedMethod encodedMethod) {
        if (encodedMethod == null) {
            throw new IllegalArgumentException("encodedMethod cannot be null");
        }
        if (encodedMethod.codeItem == null || encodedMethod.codeItem.getInstructions().length == 0) {
            throw new IllegalArgumentException("The method has no code");
        }
        this.encodedMethod = encodedMethod;
        buildInstructionList();

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
    }

    public AnalyzedInstruction[] analyze() {
        assert encodedMethod != null;
        assert encodedMethod.codeItem != null;

        if (analyzed) {
            return makeInstructionArray();
        }

        CodeItem codeItem = encodedMethod.codeItem;
        MethodIdItem methodIdItem = encodedMethod.method;

        int totalRegisters = codeItem.getRegisterCount();
        int parameterRegisters = methodIdItem.getPrototype().getParameterRegisterCount();

        //if this isn't a static method, determine which register is the "this" register and set the type to the
        //current class
        if ((encodedMethod.accessFlags & AccessFlags.STATIC.getValue()) == 0) {
            int thisRegister = totalRegisters - parameterRegisters - 1;

            //if this is a constructor, then set the "this" register to an uninitialized reference of the current class
            if ((encodedMethod.accessFlags & AccessFlags.CONSTRUCTOR.getValue()) != 0) {
                //TODO: it would probably make more sense to validate this somewhere else, and just put an assert here. Also, need to do a similar check for static constructor
                if (!encodedMethod.method.getMethodName().equals("<init>")) {
                    throw new ValidationException("The constructor flag can only be used with an <init> method.");
                }

                setRegisterTypeAndPropagateChanges(startOfMethod, thisRegister,
                        RegisterType.getRegisterType(RegisterType.Category.UninitRef,
                            ClassPath.getClassDef(methodIdItem.getContainingClass())));
            } else {
                if (encodedMethod.method.getMethodName().equals("<init>")) {
                    throw new ValidationException("An <init> method must have the \"constructor\" access flag");
                }

                setRegisterTypeAndPropagateChanges(startOfMethod, thisRegister,
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
                setRegisterTypeAndPropagateChanges(startOfMethod, registerNum, registerType);
            }
        }

        analyzed = true;
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

    public AnalyzedInstruction[] makeInstructionArray() {
        AnalyzedInstruction[] instructionArray = new AnalyzedInstruction[instructions.size()];
        for (int i=0; i<instructions.size(); i++) {
            instructionArray[i] = instructions.valueAt(i);
        }
        return instructionArray;
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
                registerTypes[registerNum] = RegisterType.getRegisterTypeForTypeIdItem(type);
            }
        }

        return registerTypes;
    }

    private int getInstructionAddress(AnalyzedInstruction instruction) {
        return instructions.keyAt(instruction.instructionIndex);
    }

    private void setWideDestinationRegisterTypeAndPropagateChanges(AnalyzedInstruction analyzedInstruction,
                                                                   RegisterType registerType) {
        assert registerType.category == RegisterType.Category.LongLo ||
               registerType.category == RegisterType.Category.DoubleLo;

        checkWideDestinationPair(analyzedInstruction);

        setRegisterTypeAndPropagateChanges(analyzedInstruction, analyzedInstruction.getDestinationRegister(),
                registerType);
        if (registerType.category == RegisterType.Category.LongLo) {
            setRegisterTypeAndPropagateChanges(analyzedInstruction, analyzedInstruction.getDestinationRegister() + 1,
                RegisterType.getRegisterType(RegisterType.Category.LongHi, null));
        } else {
            setRegisterTypeAndPropagateChanges(analyzedInstruction, analyzedInstruction.getDestinationRegister() + 1,
                RegisterType.getRegisterType(RegisterType.Category.DoubleHi, null));
        }
    }

    private void setDestinationRegisterTypeAndPropagateChanges(AnalyzedInstruction analyzedInstruction,
                                                               RegisterType registerType) {
        setRegisterTypeAndPropagateChanges(analyzedInstruction, analyzedInstruction.getDestinationRegister(),
                registerType);
    }

    private void setRegisterTypeAndPropagateChanges(AnalyzedInstruction instruction, int registerNumber,
                                                RegisterType registerType) {

        BitSet changedInstructions = new BitSet(instructions.size());

        boolean changed = instruction.setPostRegisterType(registerNumber, registerType);

        if (!changed || instruction.setsRegister(registerNumber)) {
            return;
        }

        propagateRegisterToSuccessors(instruction, registerNumber, changedInstructions);

        //using a for loop inside the while loop optimizes for the common case of the successors of an instruction
        //occurring after the instruction. Any successors that occur prior to the instruction will be picked up on
        //the next iteration of the while loop.
        //this could also be done recursively, but in large methods it would likely cause very deep recursion,
        //which would requires the user to specify a larger stack size. This isn't really a problem, but it is
        //slightly annoying.
        while (!changedInstructions.isEmpty()) {
            for (int instructionIndex=changedInstructions.nextSetBit(0);
                     instructionIndex>=0;
                     instructionIndex=changedInstructions.nextSetBit(instructionIndex)) {

                changedInstructions.clear(instructionIndex);

                propagateRegisterToSuccessors(instructions.valueAt(instructionIndex), registerNumber,
                        changedInstructions);
            }
        }
    }

    private void propagateRegisterToSuccessors(AnalyzedInstruction instruction, int registerNumber,
                                               BitSet changedInstructions) {
        for (AnalyzedInstruction successor: instruction.successors) {
            if (!successor.setsRegister(registerNumber)) {
                RegisterType registerType = successor.getMergedRegisterTypeFromPredecessors(registerNumber);

                if (successor.setPostRegisterType(registerNumber, registerType)) {
                    changedInstructions.set(successor.instructionIndex);
                }
            }
        }
    }



    private void buildInstructionList() {
        assert encodedMethod != null;
        assert encodedMethod.codeItem != null;
        int registerCount = encodedMethod.codeItem.getRegisterCount();

        startOfMethod = new AnalyzedInstruction(null, -1, registerCount);

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

        for (int i=0; i<instructions.size(); i++) {
            AnalyzedInstruction instruction = instructions.valueAt(i);
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
        assert instructions.size() > 0;
        addPredecessorSuccessor(startOfMethod, instructions.valueAt(0), exceptionHandlers);
        startOfMethod.addSuccessor(instructions.valueAt(0));

        for (int i=0; i<instructions.size(); i++) {
            AnalyzedInstruction instruction = instructions.valueAt(i);
            Opcode instructionOpcode = instruction.instruction.opcode;
            int instructionCodeAddress = getInstructionAddress(instruction);

            if (instruction.instruction.opcode.canContinue()) {
                if (i == instructions.size() - 1) {
                    throw new ValidationException("Execution can continue past the last instruction");
                }
                AnalyzedInstruction nextInstruction = instructions.valueAt(i+1);
                addPredecessorSuccessor(instruction, nextInstruction, exceptionHandlers);
            }

            if (instruction instanceof OffsetInstruction) {
                OffsetInstruction offsetInstruction = (OffsetInstruction)instruction;

                if (instructionOpcode == Opcode.PACKED_SWITCH || instructionOpcode == Opcode.SPARSE_SWITCH) {
                    MultiOffsetInstruction switchDataInstruction =
                            (MultiOffsetInstruction)instructions.get(instructionCodeAddress +
                                    offsetInstruction.getTargetAddressOffset()).instruction;
                    for (int targetAddressOffset: switchDataInstruction.getTargets()) {
                        AnalyzedInstruction targetInstruction = instructions.get(instructionCodeAddress +
                                targetAddressOffset);

                        addPredecessorSuccessor(instruction, targetInstruction, exceptionHandlers);
                    }
                } else {
                    int targetAddressOffset = offsetInstruction.getTargetAddressOffset();
                    AnalyzedInstruction targetInstruction = instructions.get(instructionCodeAddress +
                            targetAddressOffset);
                    addPredecessorSuccessor(instruction, targetInstruction, exceptionHandlers);
                }
            }
        }
    }

    private void addPredecessorSuccessor(AnalyzedInstruction predecessor, AnalyzedInstruction successor,
                                                AnalyzedInstruction[][] exceptionHandlers) {
        addPredecessorSuccessor(predecessor, successor, exceptionHandlers, false);
    }

    private void addPredecessorSuccessor(AnalyzedInstruction predecessor, AnalyzedInstruction successor,
                                                AnalyzedInstruction[][] exceptionHandlers, boolean allowMoveException) {

        if (!allowMoveException && successor.instruction.opcode == Opcode.MOVE_EXCEPTION) {
            throw new ValidationException("Execution can pass from the " + predecessor.instruction.opcode.name +
                    " instruction at code address 0x" + Integer.toHexString(getInstructionAddress(predecessor)) +
                    " to the move-exception instruction at address 0x" +
                    Integer.toHexString(getInstructionAddress(successor)));
        }

        if (!predecessor.addSuccessor(successor)) {
            //if predecessor already had successor as a successor, then there's nothing else to do
            return;
        }

        successor.addPredecessor(predecessor);

        //TODO: need to handle the case of monitor-exit as a special case - the exception is thrown *after* the instruction executes
        //if the successor can throw an instruction, then we need to add the exception handlers as additional
        //successors to the predecessor (and then apply this same logic recursively if needed)
        AnalyzedInstruction[] exceptionHandlersForSuccessor = exceptionHandlers[successor.instructionIndex];
        if (exceptionHandlersForSuccessor != null) {
            //the item for this instruction in exceptionHandlersForSuccessor should only be set if this instruction
            //can throw an exception
            assert predecessor.instruction.opcode.canThrow();

            for (AnalyzedInstruction exceptionHandler: exceptionHandlersForSuccessor) {
                addPredecessorSuccessor(predecessor, exceptionHandler, exceptionHandlers, true);
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

    private boolean setDestinationRegisterTypeForInstruction(AnalyzedInstruction analyzedInstruction) {
        Instruction instruction = analyzedInstruction.instruction;

        switch (instruction.opcode) {
            case NOP:
                return true;
            case MOVE:
            case MOVE_FROM16:
            case MOVE_16:
                return handleMove(analyzedInstruction, Primitive32BitCategories);
            case MOVE_WIDE:
            case MOVE_WIDE_FROM16:
            case MOVE_WIDE_16:
                return handleMoveWide(analyzedInstruction);
            case MOVE_OBJECT:
            case MOVE_OBJECT_FROM16:
            case MOVE_OBJECT_16:
                return handleMove(analyzedInstruction, ReferenceCategories);
            case MOVE_RESULT:
                return handleMoveResult(analyzedInstruction, Primitive32BitCategories);
            case MOVE_RESULT_WIDE:
                return handleMoveResult(analyzedInstruction, WideLowCategories);
            case MOVE_RESULT_OBJECT:
                return handleMoveResult(analyzedInstruction, ReferenceCategories);
            case MOVE_EXCEPTION:
                return handleMoveException(analyzedInstruction);
            case RETURN_VOID:
                return handleReturnVoid(analyzedInstruction);
            case RETURN:
                return handleReturn(analyzedInstruction);
            case RETURN_WIDE:
                return handleReturnWide(analyzedInstruction);
            case RETURN_OBJECT:
                return handleReturnObject(analyzedInstruction);
            case CONST_4:
            case CONST_16:
            case CONST:
                return handleConst(analyzedInstruction);
            case CONST_HIGH16:
                return handleConstHigh16(analyzedInstruction);
            case CONST_WIDE_16:
            case CONST_WIDE_32:
            case CONST_WIDE:
            case CONST_WIDE_HIGH16:
                return handleWideConst(analyzedInstruction);
            case CONST_STRING:
            case CONST_STRING_JUMBO:
                return handleConstString(analyzedInstruction);
            case CONST_CLASS:
                return handleConstClass(analyzedInstruction);
            case MONITOR_ENTER:
            case MONITOR_EXIT:
                return handleMonitor(analyzedInstruction);
            case CHECK_CAST:
                return handleCheckCast(analyzedInstruction);
            case INSTANCE_OF:
                return handleInstanceOf(analyzedInstruction);
            case ARRAY_LENGTH:
                return handleArrayLength(analyzedInstruction);
            case NEW_INSTANCE:
                return handleNewInstance(analyzedInstruction);
            case NEW_ARRAY:
                return handleNewArray(analyzedInstruction);
            case FILLED_NEW_ARRAY:
                return handleFilledNewArray(analyzedInstruction);
            case FILLED_NEW_ARRAY_RANGE:
                return handleFilledNewArrayRange(analyzedInstruction);
            case FILL_ARRAY_DATA:
                return handleFillArrayData(analyzedInstruction);
            case THROW:
                return handleThrow(analyzedInstruction);
            case GOTO:
            case GOTO_16:
            case GOTO_32:
                //nothing to do
                return true;
            case PACKED_SWITCH:
                return handleSwitch(analyzedInstruction, Format.PackedSwitchData);
            case SPARSE_SWITCH:
                return handleSwitch(analyzedInstruction, Format.SparseSwitchData);
            case CMPL_FLOAT:
            case CMPG_FLOAT:
                return handleFloatCmp(analyzedInstruction);
            case CMPL_DOUBLE:
            case CMPG_DOUBLE:
            case CMP_LONG:
                return handleWideCmp(analyzedInstruction);
            case IF_EQ:
            case IF_NE:
                return handleIfEqNe(analyzedInstruction);
            case IF_LT:
            case IF_GE:
            case IF_GT:
            case IF_LE:
                return handleIf(analyzedInstruction);
            case IF_EQZ:
            case IF_NEZ:
                return handleIfEqzNez(analyzedInstruction);
            case IF_LTZ:
            case IF_GEZ:
            case IF_GTZ:
            case IF_LEZ:
                return handleIfz(analyzedInstruction);
            case AGET:
                return handle32BitPrimitiveAget(analyzedInstruction, RegisterType.Category.Integer);
            case AGET_BOOLEAN:
                return handle32BitPrimitiveAget(analyzedInstruction, RegisterType.Category.Boolean);
            case AGET_BYTE:
                return handle32BitPrimitiveAget(analyzedInstruction, RegisterType.Category.Byte);
            case AGET_CHAR:
                return handle32BitPrimitiveAget(analyzedInstruction, RegisterType.Category.Char);
            case AGET_SHORT:
                return handle32BitPrimitiveAget(analyzedInstruction, RegisterType.Category.Short);
            case AGET_WIDE:
                return handleAgetWide(analyzedInstruction);
            case AGET_OBJECT:
                return handleAgetObject(analyzedInstruction);
            case APUT:
                return handle32BitPrimitiveAput(analyzedInstruction, RegisterType.Category.Integer);
            case APUT_BOOLEAN:
                return handle32BitPrimitiveAput(analyzedInstruction, RegisterType.Category.Boolean);
            case APUT_BYTE:
                return handle32BitPrimitiveAput(analyzedInstruction, RegisterType.Category.Byte);
            case APUT_CHAR:
                return handle32BitPrimitiveAput(analyzedInstruction, RegisterType.Category.Char);
            case APUT_SHORT:
                return handle32BitPrimitiveAput(analyzedInstruction, RegisterType.Category.Short);
            case APUT_WIDE:
                return handleAputWide(analyzedInstruction);
            case APUT_OBJECT:
                return handleAputObject(analyzedInstruction);
            case IGET:
                return handle32BitPrimitiveIget(analyzedInstruction, RegisterType.Category.Integer);
            case IGET_BOOLEAN:
                return handle32BitPrimitiveIget(analyzedInstruction, RegisterType.Category.Boolean);
            case IGET_BYTE:
                return handle32BitPrimitiveIget(analyzedInstruction, RegisterType.Category.Byte);
            case IGET_CHAR:
                return handle32BitPrimitiveIget(analyzedInstruction, RegisterType.Category.Char);
            case IGET_SHORT:
                return handle32BitPrimitiveIget(analyzedInstruction, RegisterType.Category.Short);
            case IGET_WIDE:
                return handleIgetWide(analyzedInstruction);
            case IGET_OBJECT:
                return handleIgetObject(analyzedInstruction);
        }

        assert false;
        return false;
    }

    private static final EnumSet<RegisterType.Category> Primitive32BitCategories = EnumSet.of(
            RegisterType.Category.Null,
            RegisterType.Category.Boolean,
            RegisterType.Category.Byte,
            RegisterType.Category.Short,
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

    private boolean handleMove(AnalyzedInstruction analyzedInstruction,
                               EnumSet<RegisterType.Category> allowedCategories) {
        TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;

        //get the "pre-instruction" register type for the source register
        RegisterType sourceRegisterType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterB());
        assert sourceRegisterType != null;

        if (sourceRegisterType.category == RegisterType.Category.Unknown) {
            //we don't know the source register type yet, so we can't verify it. Return false, and we'll come back later
            return false;
        }

        checkRegister(sourceRegisterType, allowedCategories);

        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, sourceRegisterType);
        return true;
    }

    private boolean handleMoveWide(AnalyzedInstruction analyzedInstruction) {
        TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;

        RegisterType sourceRegisterType = getAndCheckWideSourcePair(analyzedInstruction,
                instruction.getRegisterB());
        assert sourceRegisterType != null;

        if (sourceRegisterType.category == RegisterType.Category.Unknown) {
            //we don't know the source register type yet, so we can't verify it. Return false, and we'll come back later
            return false;
        }

        checkWideDestinationPair(analyzedInstruction);

        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, sourceRegisterType);
        return true;
    }

    private boolean handleMoveResult(AnalyzedInstruction analyzedInstruction,
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

        if (analyzedInstruction.instruction.opcode.setsWideRegister()) {
            checkWideDestinationPair(analyzedInstruction);
        }

        //TODO: does dalvik allow a move-result after an invoke with a void return type?
        RegisterType destinationRegisterType;

        InstructionWithReference invokeInstruction = (InstructionWithReference)previousInstruction.instruction;
        Item item = invokeInstruction.getReferencedItem();

        if (item instanceof MethodIdItem) {
            destinationRegisterType = RegisterType.getRegisterTypeForTypeIdItem(
                    ((MethodIdItem)item).getPrototype().getReturnType());
        } else {
            assert item instanceof TypeIdItem;
            destinationRegisterType = RegisterType.getRegisterTypeForTypeIdItem((TypeIdItem)item);
        }

        checkRegister(destinationRegisterType, allowedCategories);
        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, destinationRegisterType);
        return true;
    }

    private boolean handleMoveException(AnalyzedInstruction analyzedInstruction) {
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

        //TODO: check if the type is a throwable. Should we throw a ValidationException or print a warning? (does dalvik validate that it's a throwable? It doesn't in CodeVerify.c, but it might check in DexSwapVerify.c)
        checkRegister(exceptionType, ReferenceCategories);
        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, exceptionType);
        return true;
    }

    private boolean checkConstructorReturn(AnalyzedInstruction analyzedInstruction) {
        assert this.isInstanceConstructor();

        //if we're in an instance constructor (an <init> method), then the superclass <init> must have been called.
        //When execution enters the method, the "this" register is set as an uninitialized reference to the containing
        //class. Once the superclass' <init> is called, the "this" register is upgraded to a full-blown reference type,
        //so we need to ensure that the "this" register isn't an uninitialized reference

        int thisRegister = getThisRegister();
        RegisterType thisRegisterType = analyzedInstruction.postRegisterMap[thisRegister];

        if (thisRegisterType.category == RegisterType.Category.Unknown) {
            //we don't have enough information yet, so return false. We'll come back later
            return false;
        }
        if (thisRegisterType.category == RegisterType.Category.UninitRef) {
            throw new ValidationException("Returning from constructor without calling the superclass' <init>");
        }
        assert thisRegisterType.category == RegisterType.Category.Reference;
        assert thisRegisterType.type == ClassPath.getClassDef(encodedMethod.method.getContainingClass());
        return true;
    }

    private boolean handleReturnVoid(AnalyzedInstruction analyzedInstruction) {
        if (this.isInstanceConstructor()) {
            if (!checkConstructorReturn(analyzedInstruction)) {
                return false;
            }
        }

        TypeIdItem returnType = encodedMethod.method.getPrototype().getReturnType();
        if (returnType.getTypeDescriptor().charAt(0) != 'V') {
            //TODO: could add which return-* variation should be used instead
            throw new ValidationException("Cannot use return-void with a non-void return type (" +
                returnType.getTypeDescriptor() + ")");
        }
        return true;
    }

    private boolean handleReturn(AnalyzedInstruction analyzedInstruction) {
        if (this.isInstanceConstructor()) {
            if (!checkConstructorReturn(analyzedInstruction)) {
                return false;
            }
        }

        SingleRegisterInstruction instruction = (SingleRegisterInstruction)analyzedInstruction.instruction;
        RegisterType returnRegisterType = analyzedInstruction.postRegisterMap[instruction.getRegisterA()];

        if (returnRegisterType.category == RegisterType.Category.Unknown) {
            return false;
        }

        checkRegister(returnRegisterType, Primitive32BitCategories);

        TypeIdItem returnType = encodedMethod.method.getPrototype().getReturnType();
        if (returnType.getTypeDescriptor().charAt(0) == 'V') {
            throw new ValidationException("Cannot use return with a void return type. Use return-void instead");
        }

        RegisterType registerType = RegisterType.getRegisterTypeForTypeIdItem(returnType);

        if (!Primitive32BitCategories.contains(registerType.category)) {
            //TODO: could add which return-* variation should be used instead
            throw new ValidationException("Cannot use return with return type " + returnType.getTypeDescriptor());
        }


        return true;
    }

    private boolean handleReturnWide(AnalyzedInstruction analyzedInstruction) {
        if (this.isInstanceConstructor()) {
            if (!checkConstructorReturn(analyzedInstruction)) {
                return false;
            }
        }

        SingleRegisterInstruction instruction = (SingleRegisterInstruction)analyzedInstruction.instruction;
        RegisterType returnType = getAndCheckWideSourcePair(analyzedInstruction, instruction.getRegisterA());

        if (returnType.category == RegisterType.Category.Unknown) {
            return false;
        }


        TypeIdItem returnTypeIdItem = encodedMethod.method.getPrototype().getReturnType();
        if (returnTypeIdItem.getTypeDescriptor().charAt(0) == 'V') {
            throw new ValidationException("Cannot use return-wide with a void return type. Use return-void instead");
        }

        returnType = RegisterType.getRegisterTypeForTypeIdItem(returnTypeIdItem);
        if (!WideLowCategories.contains(returnType.category)) {
            //TODO: could add which return-* variation should be used instead
            throw new ValidationException("Cannot use return-wide with return type " +
                    returnTypeIdItem.getTypeDescriptor());
        }

        return true;
    }

    private boolean handleReturnObject(AnalyzedInstruction analyzedInstruction) {
        if (this.isInstanceConstructor()) {
            if (!checkConstructorReturn(analyzedInstruction)) {
                return false;
            }
        }

        SingleRegisterInstruction instruction = (SingleRegisterInstruction)analyzedInstruction.instruction;
        int returnRegister = instruction.getRegisterA();
        RegisterType returnRegisterType = analyzedInstruction.postRegisterMap[returnRegister];

        if (returnRegisterType.category == RegisterType.Category.Unknown) {
            return false;
        }

        checkRegister(returnRegisterType, ReferenceCategories);


        TypeIdItem returnTypeIdItem = encodedMethod.method.getPrototype().getReturnType();
        if (returnTypeIdItem.getTypeDescriptor().charAt(0) == 'V') {
            throw new ValidationException("Cannot use return with a void return type. Use return-void instead");
        }

        RegisterType returnType = RegisterType.getRegisterTypeForTypeIdItem(returnTypeIdItem);

        if (!ReferenceCategories.contains(returnType.category)) {
            //TODO: could add which return-* variation should be used instead
            throw new ValidationException("Cannot use " + analyzedInstruction + " with return type " +
                    returnTypeIdItem.getTypeDescriptor());
        }

        if (returnType.type.isInterface()) {
            if (!returnRegisterType.type.implementsInterface(returnType.type)) {
                //TODO: how to handle warnings?
            }
        } else {
            if (!returnRegisterType.type.extendsClass(returnType.type)) {
                throw new ValidationException("The return value in register v" + Integer.toString(returnRegister) +
                        "(" + returnRegisterType.type.getClassType() + ") is not compatible with the method's return " +
                        "type (" + returnType.type.getClassType() + ")");
            }
        }

        return true;
    }

    private boolean handleConst(AnalyzedInstruction analyzedInstruction) {
        LiteralInstruction instruction = (LiteralInstruction)analyzedInstruction.instruction;

        RegisterType newDestinationRegisterType = RegisterType.getRegisterTypeForLiteral(instruction.getLiteral());

        //we assume that the literal value is a valid value for the given instruction type, because it's impossible
        //to store an invalid literal with the instruction. so we don't need to check the type of the literal
        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, newDestinationRegisterType);
        return true;
    }

    private boolean handleConstHigh16(AnalyzedInstruction analyzedInstruction) {
        LiteralInstruction instruction = (LiteralInstruction)analyzedInstruction.instruction;

        //TODO: test this
        long literalValue = instruction.getLiteral() << 16;
        RegisterType newDestinationRegisterType = RegisterType.getRegisterTypeForLiteral(literalValue);

        //we assume that the literal value is a valid value for the given instruction type, because it's impossible
        //to store an invalid literal with the instruction. so we don't need to check the type of the literal
        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, newDestinationRegisterType);
        return true;
    }

    private boolean handleWideConst(AnalyzedInstruction analyzedInstruction) {
        setWideDestinationRegisterTypeAndPropagateChanges(analyzedInstruction,
                RegisterType.getRegisterType(RegisterType.Category.LongLo, null));
        return true;
    }

    private boolean handleConstString(AnalyzedInstruction analyzedInstruction) {
        ClassPath.ClassDef stringClassDef = ClassPath.getClassDef("Ljava/lang/String;");
        RegisterType stringType = RegisterType.getRegisterType(RegisterType.Category.Reference, stringClassDef);
        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, stringType);
        return true;
    }

    private boolean handleConstClass(AnalyzedInstruction analyzedInstruction) {
        ClassPath.ClassDef classClassDef = ClassPath.getClassDef("Ljava/lang/Class;");
        RegisterType classType = RegisterType.getRegisterType(RegisterType.Category.Reference, classClassDef);

        InstructionWithReference instruction = (InstructionWithReference)analyzedInstruction.instruction;
        Item item = instruction.getReferencedItem();
        assert item.getItemType() == ItemType.TYPE_TYPE_ID_ITEM;

        //make sure the referenced class is resolvable
        //TODO: need to check class access
        ClassPath.ClassDef classDef = ClassPath.getClassDef((TypeIdItem)item);
        return false;
    }

    private boolean handleMonitor(AnalyzedInstruction analyzedInstruction) {
        SingleRegisterInstruction instruction = (SingleRegisterInstruction)analyzedInstruction;

        RegisterType registerType = analyzedInstruction.postRegisterMap[instruction.getRegisterA()];
        assert registerType != null;
        if (registerType.category == RegisterType.Category.Unknown) {
            return false;
        }

        checkRegister(registerType, ReferenceCategories);
        return true;
    }

    private boolean handleCheckCast(AnalyzedInstruction analyzedInstruction) {
        {
            //ensure the "source" register is a reference type
            SingleRegisterInstruction instruction = (SingleRegisterInstruction)analyzedInstruction.instruction;

            RegisterType registerType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterA());
            assert registerType != null;
            if (registerType.category == RegisterType.Category.Unknown) {
                return false;
            }

            checkRegister(registerType, ReferenceCategories);
        }

        {
            //resolve and verify the class that we're casting to
            InstructionWithReference instruction = (InstructionWithReference)analyzedInstruction.instruction;

            Item item = instruction.getReferencedItem();
            assert item.getItemType() == ItemType.TYPE_TYPE_ID_ITEM;

            //TODO: need to check class access
            RegisterType newDestinationRegisterType = RegisterType.getRegisterTypeForTypeIdItem((TypeIdItem)item);
            try {
                checkRegister(newDestinationRegisterType, ReferenceCategories);
            } catch (ValidationException ex) {
                //TODO: verify that dalvik allows a non-reference type..
                //TODO: print a warning, but don't re-throw the exception. dalvik allows a non-reference type during validation (but throws an exception at runtime)
            }

            setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, newDestinationRegisterType);
            return true;
        }
    }

    private boolean handleInstanceOf(AnalyzedInstruction analyzedInstruction) {
        {
            //ensure the register that is being checks is a reference type
            TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction;

            RegisterType registerType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterB());
            assert registerType != null;
            if (registerType.category == RegisterType.Category.Unknown) {
                return false;
            }

            checkRegister(registerType, ReferenceCategories);
        }

        {
            //resolve and verify the class that we're checking against
            InstructionWithReference instruction = (InstructionWithReference)analyzedInstruction.instruction;

            Item item = instruction.getReferencedItem();
            assert  item.getItemType() == ItemType.TYPE_TYPE_ID_ITEM;
            RegisterType registerType = RegisterType.getRegisterTypeForTypeIdItem((TypeIdItem)item);
            checkRegister(registerType, ReferenceCategories);

            //TODO: is it valid to use an array type?

            //TODO: could probably do an even more sophisticated check, where we check the possible register types against the specified type. In some cases, we could determine that it always fails, and print a warning to that effect.
            setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction,
                    RegisterType.getRegisterType(RegisterType.Category.Boolean, null));
            return true;
        }
    }

    private boolean handleArrayLength(AnalyzedInstruction analyzedInstruction) {
        TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction;

        int arrayRegisterNumber = instruction.getRegisterB();
        RegisterType arrayRegisterType = analyzedInstruction.getPreInstructionRegisterType(arrayRegisterNumber);
        assert arrayRegisterType != null;
        if (arrayRegisterType.category == RegisterType.Category.Unknown) {
            return false;
        }

        assert arrayRegisterType.type instanceof ClassPath.ArrayClassDef;

        checkRegister(arrayRegisterType, ReferenceCategories);
        if (arrayRegisterType.type != null) {
            if (arrayRegisterType.type.getClassType().charAt(0) != '[') {
                throw new ValidationException("Cannot use array-length with non-array type " +
                        arrayRegisterType.type.getClassType());
            }
        }

        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction,
                RegisterType.getRegisterType(RegisterType.Category.Integer, null));
        return true;
    }

    private boolean handleNewInstance(AnalyzedInstruction analyzedInstruction) {
        InstructionWithReference instruction = (InstructionWithReference)analyzedInstruction.instruction;

        Item item = instruction.getReferencedItem();
        assert item.getItemType() == ItemType.TYPE_TYPE_ID_ITEM;

        //TODO: need to check class access
        RegisterType classType = RegisterType.getRegisterTypeForTypeIdItem((TypeIdItem)item);
        checkRegister(classType, ReferenceCategories);
        if (((TypeIdItem)item).getTypeDescriptor().charAt(0) == '[') {
            throw new ValidationException("Cannot use array type \"" + ((TypeIdItem)item).getTypeDescriptor() +
                    "\" with new-instance. Use new-array instead.");
        }

        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction,
                RegisterType.getRegisterType(RegisterType.Category.UninitRef, classType.type));
        return true;
    }

    private boolean handleNewArray(AnalyzedInstruction analyzedInstruction) {
        {
            TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;

            int sizeRegister = instruction.getRegisterB();
            RegisterType registerType = analyzedInstruction.getPreInstructionRegisterType(sizeRegister);
            assert registerType != null;

            if (registerType.category == RegisterType.Category.Unknown) {
                return false;
            }

            checkRegister(registerType, Primitive32BitCategories);
        }

        InstructionWithReference instruction = (InstructionWithReference)analyzedInstruction.instruction;

        Item item = instruction.getReferencedItem();
        assert item.getItemType() == ItemType.TYPE_TYPE_ID_ITEM;

        RegisterType arrayType = RegisterType.getRegisterTypeForTypeIdItem((TypeIdItem)item);
        assert arrayType.type instanceof ClassPath.ArrayClassDef;

        checkRegister(arrayType, ReferenceCategories);
        if (arrayType.type.getClassType().charAt(0) != '[') {
            throw new ValidationException("Cannot use non-array type \"" + arrayType.type.getClassType() +
                    "\" with new-array. Use new-instance instead.");
        }

        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, arrayType);
        return true;
    }

    private static interface RegisterIterator {
        int getRegister();
        boolean moveNext();
    }

    private boolean handleFilledNewArrayCommon(AnalyzedInstruction analyzedInstruction,
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

            if (elementType.category == RegisterType.Category.Unknown) {
                return false;
            }

            if (!elementType.canBeAssignedTo(arrayImmediateElementType)) {
                throw new ValidationException("Register v" + Integer.toString(register) + " is of type " +
                        elementType.toString() + " and is incompatible with the array type " +
                        arrayType.type.getClassType());
            }
        } while (registerIterator.moveNext());

        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, arrayType);
        return true;
    }

    private boolean handleFilledNewArray(AnalyzedInstruction analyzedInstruction) {
        FiveRegisterInstruction instruction = (FiveRegisterInstruction)analyzedInstruction.instruction;
        final int registerCount = instruction.getRegCount();
        final int[] registers = new int[]{instruction.getRegisterD(), instruction.getRegisterE(),
                                          instruction.getRegisterF(), instruction.getRegisterG(),
                                          instruction.getRegisterA()};

        return handleFilledNewArrayCommon(analyzedInstruction,
                new RegisterIterator() {
                    private int currentRegister = 0;
                    public int getRegister() {
                        return registers[currentRegister];
                    }

                    public boolean moveNext() {
                        currentRegister++;
                        if (currentRegister >= registerCount) {
                            return false;
                        }
                        return true;
                    }
                });
    }

    private boolean handleFilledNewArrayRange(AnalyzedInstruction analyzedInstruction) {
        final RegisterRangeInstruction instruction = (RegisterRangeInstruction)analyzedInstruction.instruction;

        //instruction.getStartRegister() and instruction.getRegCount() both return an int value, but are actually
        //unsigned 16 bit values, so we don't have to worry about overflowing an int when adding them together
        if (instruction.getStartRegister() + instruction.getRegCount() >= 1<<16) {
            throw new ValidationException(String.format("Invalid register range {v%d .. v%d}. The ending register " +
                    "is larger than the largest allowed register of v65535.",
                    instruction.getStartRegister(),
                    instruction.getStartRegister() + instruction.getRegCount() - 1));
        }

        return handleFilledNewArrayCommon(analyzedInstruction,
                new RegisterIterator() {
                    private int currentRegister = 0;
                    private final int startRegister = instruction.getStartRegister();
                    private final int registerCount = instruction.getRegCount();

                    public int getRegister() {
                        return startRegister + currentRegister;
                    }

                    public boolean moveNext() {
                        currentRegister++;
                        if (currentRegister >= registerCount) {
                            return false;
                        }
                        return true;
                    }
                });
    }

    private boolean handleFillArrayData(AnalyzedInstruction analyzedInstruction) {
        SingleRegisterInstruction instruction = (SingleRegisterInstruction)analyzedInstruction.instruction;

        int register = instruction.getRegisterA();
        RegisterType registerType = analyzedInstruction.getPreInstructionRegisterType(register);
        assert registerType != null;

        if (registerType.category == RegisterType.Category.Unknown ||
            registerType.category == RegisterType.Category.Null) {
            return false;
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

        return true;
    }

    private boolean handleThrow(AnalyzedInstruction analyzedInstruction) {
        int register = ((SingleRegisterInstruction)analyzedInstruction.instruction).getRegisterA();

        RegisterType registerType = analyzedInstruction.getPreInstructionRegisterType(register);
        assert registerType != null;

        if (registerType.category == RegisterType.Category.Unknown) {
            return false;
        }

        if (registerType.category == RegisterType.Category.Null) {
            return true;
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

        return true;
    }

    private boolean handleSwitch(AnalyzedInstruction analyzedInstruction, Format expectedSwitchDataFormat) {
        int register = ((SingleRegisterInstruction)analyzedInstruction.instruction).getRegisterA();
        int switchCodeAddressOffset = ((OffsetInstruction)analyzedInstruction.instruction).getTargetAddressOffset();

        RegisterType registerType = analyzedInstruction.getPreInstructionRegisterType(register);
        assert registerType != null;

        if (registerType.category == RegisterType.Category.Unknown) {
            return false;
        }

        checkRegister(registerType, Primitive32BitCategories);

        int switchDataCodeAddress = this.getInstructionAddress(analyzedInstruction) + switchCodeAddressOffset;
        AnalyzedInstruction switchDataAnalyzedInstruction = instructions.get(switchDataCodeAddress);

        if (switchDataAnalyzedInstruction == null ||
            switchDataAnalyzedInstruction.instruction.getFormat() != expectedSwitchDataFormat) {
            throw new ValidationException(String.format("There is no %s structure at code address 0x%x",
                    expectedSwitchDataFormat.name(), switchDataCodeAddress));
        }

        return true;
    }

    private boolean handleFloatCmp(AnalyzedInstruction analyzedInstruction) {
        ThreeRegisterInstruction instruction = (ThreeRegisterInstruction)analyzedInstruction.instruction;

        RegisterType registerType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterB());
        assert registerType != null;

        if (registerType.category == RegisterType.Category.Unknown) {
            return false;
        }
        checkRegister(registerType, Primitive32BitCategories);

        registerType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterC());
        assert registerType != null;

        if (registerType.category == RegisterType.Category.Unknown) {
            return false;
        }
        checkRegister(registerType, Primitive32BitCategories);

        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction,
                RegisterType.getRegisterType(RegisterType.Category.Byte, null));
        return true;
    }

    private boolean handleWideCmp(AnalyzedInstruction analyzedInstruction) {
        ThreeRegisterInstruction instruction = (ThreeRegisterInstruction)analyzedInstruction.instruction;

        RegisterType registerType = getAndCheckWideSourcePair(analyzedInstruction, instruction.getRegisterB());
        assert registerType != null;
        if (registerType.category == RegisterType.Category.Unknown) {
            return false;
        }

        registerType = getAndCheckWideSourcePair(analyzedInstruction, instruction.getRegisterC());
        assert registerType != null;
        if (registerType.category == RegisterType.Category.Unknown) {
            return false;
        }

        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction,
                RegisterType.getRegisterType(RegisterType.Category.Byte, null));
        return true;
    }

    private boolean handleIfEqNe(AnalyzedInstruction analyzedInstruction) {
        TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;

        RegisterType registerType1 = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterA());
        assert registerType1 != null;
        if (registerType1.category == RegisterType.Category.Unknown) {
            return false;
        }

        RegisterType registerType2 = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterB());
        assert registerType2 != null;
        if (registerType2.category == RegisterType.Category.Unknown) {
            return false;
        }

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

        return true;
    }

    private boolean handleIf(AnalyzedInstruction analyzedInstruction) {
        TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;

        RegisterType registerType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterA());
        assert registerType != null;

        if (registerType.category == RegisterType.Category.Unknown) {
            return false;
        }
        checkRegister(registerType, Primitive32BitCategories);

        registerType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterB());
        assert registerType != null;

        if (registerType.category == RegisterType.Category.Unknown) {
            return false;
        }
        checkRegister(registerType, Primitive32BitCategories);

        return true;
    }

    private boolean handleIfEqzNez(AnalyzedInstruction analyzedInstruction) {
        SingleRegisterInstruction instruction = (SingleRegisterInstruction)analyzedInstruction.instruction;

        RegisterType registerType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterA());
        assert registerType != null;
        if (registerType.category == RegisterType.Category.Unknown) {
            return false;
        }

        if (!ReferenceCategories.contains(registerType.category) &&
            !Primitive32BitCategories.contains(registerType.category)) {
            throw new ValidationException(String.format("%s cannot be used with register type %s. Expecting 32-bit " +
                    "primitive type or reference type.", analyzedInstruction.instruction.opcode));
        }

        return true;
    }

    private boolean handleIfz(AnalyzedInstruction analyzedInstruction) {
        SingleRegisterInstruction instruction = (SingleRegisterInstruction)analyzedInstruction.instruction;

        RegisterType registerType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterA());
        assert registerType != null;

        if (registerType.category == RegisterType.Category.Unknown) {
            return false;
        }
        checkRegister(registerType, Primitive32BitCategories);

        return true;
    }

    private boolean handle32BitPrimitiveAget(AnalyzedInstruction analyzedInstruction,
                                             RegisterType.Category instructionCategory) {
        ThreeRegisterInstruction instruction = (ThreeRegisterInstruction)analyzedInstruction.instruction;

        RegisterType indexRegisterType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterC());
        assert indexRegisterType != null;
        if (indexRegisterType.category == RegisterType.Category.Unknown) {
            return false;
        }
        checkRegister(indexRegisterType, Primitive32BitCategories);

        RegisterType arrayRegisterType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterB());
        assert arrayRegisterType != null;
        if (indexRegisterType.category == RegisterType.Category.Unknown) {
            return false;
        }

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
            if (checkArrayFieldAssignment(arrayBaseType.category, instructionCategory)) {
                throw new ValidationException(String.format("Cannot use %s with array type %s. Incorrect array type " +
                        "for the instruction.", analyzedInstruction.instruction.opcode.name,
                        arrayRegisterType.type.getClassType()));
            }
        }

        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction,
                RegisterType.getRegisterType(instructionCategory, null));

        return true;
    }

    private boolean handleAgetWide(AnalyzedInstruction analyzedInstruction) {
        ThreeRegisterInstruction instruction = (ThreeRegisterInstruction)analyzedInstruction.instruction;

        RegisterType indexRegisterType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterC());
        assert indexRegisterType != null;
        if (indexRegisterType.category == RegisterType.Category.Unknown) {
            return false;
        }
        checkRegister(indexRegisterType, Primitive32BitCategories);

        RegisterType arrayRegisterType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterB());
        assert arrayRegisterType != null;
        if (indexRegisterType.category == RegisterType.Category.Unknown) {
            return false;
        }

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
                setWideDestinationRegisterTypeAndPropagateChanges(analyzedInstruction,
                        RegisterType.getRegisterType(RegisterType.Category.LongLo, null));
            } else if (arrayBaseType == 'D') {
                setWideDestinationRegisterTypeAndPropagateChanges(analyzedInstruction,
                        RegisterType.getRegisterType(RegisterType.Category.DoubleLo, null));
            } else {
                throw new ValidationException(String.format("Cannot use aget-wide with array type %s. Incorrect " +
                        "array type for the instruction.", arrayRegisterType.type.getClassType()));
            }
        } else {
            setWideDestinationRegisterTypeAndPropagateChanges(analyzedInstruction,
                        RegisterType.getRegisterType(RegisterType.Category.LongLo, null));
        }

        return true;
    }

    private boolean handleAgetObject(AnalyzedInstruction analyzedInstruction) {
        ThreeRegisterInstruction instruction = (ThreeRegisterInstruction)analyzedInstruction.instruction;

        RegisterType indexRegisterType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterC());
        assert indexRegisterType != null;
        if (indexRegisterType.category == RegisterType.Category.Unknown) {
            return false;
        }
        checkRegister(indexRegisterType, Primitive32BitCategories);

        RegisterType arrayRegisterType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterB());
        assert arrayRegisterType != null;
        if (indexRegisterType.category == RegisterType.Category.Unknown) {
            return false;
        }

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

        return true;
    }

    private boolean handle32BitPrimitiveAput(AnalyzedInstruction analyzedInstruction,
                                             RegisterType.Category instructionCategory) {
        ThreeRegisterInstruction instruction = (ThreeRegisterInstruction)analyzedInstruction.instruction;

        RegisterType indexRegisterType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterC());
        assert indexRegisterType != null;
        if (indexRegisterType.category == RegisterType.Category.Unknown) {
            return false;
        }
        checkRegister(indexRegisterType, Primitive32BitCategories);


        RegisterType sourceRegisterType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterA());
        assert sourceRegisterType != null;
        if (sourceRegisterType.category == RegisterType.Category.Unknown) {
            return false;
        }
        RegisterType instructionRegisterType = RegisterType.getRegisterType(instructionCategory, null);
        if (!sourceRegisterType.canBeAssignedTo(instructionRegisterType)) {
            throw new ValidationException(String.format("Cannot use %s with source register type %s.",
                    analyzedInstruction.instruction.opcode.name, sourceRegisterType.toString()));
        }


        RegisterType arrayRegisterType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterB());
        assert arrayRegisterType != null;
        if (indexRegisterType.category == RegisterType.Category.Unknown) {
            return false;
        }

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
            if (checkArrayFieldAssignment(arrayBaseType.category, instructionCategory)) {
                throw new ValidationException(String.format("Cannot use %s with array type %s. Incorrect array type " +
                        "for the instruction.", analyzedInstruction.instruction.opcode.name,
                        arrayRegisterType.type.getClassType()));
            }
        }

        return true;
    }

    private boolean handleAputWide(AnalyzedInstruction analyzedInstruction) {
        ThreeRegisterInstruction instruction = (ThreeRegisterInstruction)analyzedInstruction.instruction;

        RegisterType indexRegisterType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterC());
        assert indexRegisterType != null;
        if (indexRegisterType.category == RegisterType.Category.Unknown) {
            return false;
        }
        checkRegister(indexRegisterType, Primitive32BitCategories);

        RegisterType sourceRegisterType = getAndCheckWideSourcePair(analyzedInstruction, instruction.getRegisterA());
        if (sourceRegisterType.category == RegisterType.Category.Unknown) {
            return false;
        }

        RegisterType arrayRegisterType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterB());
        assert arrayRegisterType != null;
        if (indexRegisterType.category == RegisterType.Category.Unknown) {
            return false;
        }

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

        return true;
    }

    private boolean handleAputObject(AnalyzedInstruction analyzedInstruction) {
        ThreeRegisterInstruction instruction = (ThreeRegisterInstruction)analyzedInstruction.instruction;

        RegisterType indexRegisterType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterC());
        assert indexRegisterType != null;
        if (indexRegisterType.category == RegisterType.Category.Unknown) {
            return false;
        }
        checkRegister(indexRegisterType, Primitive32BitCategories);

        RegisterType sourceRegisterType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterA());
        assert sourceRegisterType != null;
        if (sourceRegisterType.category == RegisterType.Category.Unknown) {
            return false;
        }

        RegisterType arrayRegisterType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterB());
        assert arrayRegisterType != null;
        if (indexRegisterType.category == RegisterType.Category.Unknown) {
            return false;
        }

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

        return true;
    }

    private boolean handle32BitPrimitiveIget(AnalyzedInstruction analyzedInstruction,
                                             RegisterType.Category instructionCategory) {
        TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;

        RegisterType objectRegisterType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterB());
        assert objectRegisterType != null;
        if (objectRegisterType.category == RegisterType.Category.Unknown) {
            return false;
        }
        checkRegister(objectRegisterType, ReferenceCategories);

        //TODO: check access
        //TODO: allow an uninitialized "this" reference, if the current method is an <init> method
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

        return true;
    }

    private boolean handleIgetWide(AnalyzedInstruction analyzedInstruction) {
        TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;

        RegisterType objectRegisterType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterB());
        assert objectRegisterType != null;
        if (objectRegisterType.category == RegisterType.Category.Unknown) {
            return false;
        }
        checkRegister(objectRegisterType, ReferenceCategories);

        getAndCheckWideSourcePair(analyzedInstruction, instruction.getRegisterB());

        //TODO: check access
        //TODO: allow an uninitialized "this" reference, if the current method is an <init> method
        Item referencedItem = ((InstructionWithReference)analyzedInstruction.instruction).getReferencedItem();
        assert referencedItem instanceof FieldIdItem;
        FieldIdItem field = (FieldIdItem)referencedItem;

        if (objectRegisterType.category != RegisterType.Category.Null &&
            !objectRegisterType.type.extendsClass(ClassPath.getClassDef(field.getContainingClass()))) {
            throw new ValidationException(String.format("Cannot access field %s through type %s",
                    field.getFieldString(), objectRegisterType.type.getClassType()));
        }

        RegisterType fieldType = RegisterType.getRegisterTypeForTypeIdItem(field.getFieldType());

        try {
            checkRegister(fieldType, WideLowCategories);
        } catch (ValidationException ex) {
            throw new ValidationException(String.format("Cannot use %s with field %s. Incorrect field type " +
                        "for the instruction.", analyzedInstruction.instruction.opcode.name,
                        field.getFieldString()));
        }

        setWideDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, fieldType);

        return true;
    }

    private boolean handleIgetObject(AnalyzedInstruction analyzedInstruction) {
        TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;

        RegisterType objectRegisterType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterB());
        assert objectRegisterType != null;
        if (objectRegisterType.category == RegisterType.Category.Unknown) {
            return false;
        }
        checkRegister(objectRegisterType, ReferenceCategories);

        //TODO: check access
        //TODO: allow an uninitialized "this" reference, if the current method is an <init> method
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

        return true;
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

    private static void checkRegister(RegisterType registerType, EnumSet validCategories) {
        if (!validCategories.contains(registerType.category)) {
            //TODO: add expected categories to error message
            throw new ValidationException("Invalid register type. Expecting one of: " + " but got \"" +
                    registerType.category + "\"");
        }
    }

    private static void checkWideDestinationPair(AnalyzedInstruction analyzedInstruction) {
        int register = analyzedInstruction.getDestinationRegister();

        if (register == (analyzedInstruction.postRegisterMap.length - 1)) {
            throw new ValidationException("v" + register + " is the last register and not a valid wide register " +
                    "pair.");
        }
    }

    private static RegisterType getAndCheckWideSourcePair(AnalyzedInstruction analyzedInstruction, int firstRegister) {
        assert firstRegister >= 0 && firstRegister < analyzedInstruction.postRegisterMap.length;

        if (firstRegister == analyzedInstruction.postRegisterMap.length - 1) {
            throw new ValidationException("v" + firstRegister + " is the last register and not a valid wide register " +
                    "pair.");
        }

        RegisterType registerType = analyzedInstruction.getPreInstructionRegisterType(firstRegister);
        assert registerType != null;
        if (registerType.category == RegisterType.Category.Unknown) {
            return registerType;
        }
        checkRegister(registerType, WideLowCategories);

        RegisterType secondRegisterType = analyzedInstruction.getPreInstructionRegisterType(firstRegister + 1);
        assert secondRegisterType != null;
        checkRegister(secondRegisterType, WideHighCategories);

        if ((       registerType.category == RegisterType.Category.LongLo &&
                    secondRegisterType.category == RegisterType.Category.DoubleHi)
            ||  (   registerType.category == RegisterType.Category.DoubleLo &&
                    secondRegisterType.category == RegisterType.Category.LongHi)) {
            assert false;
            throw new ValidationException("The first register in the wide register pair isn't the same type (long " +
                    "vs. double) as the second register in the pair");
        }

        return registerType;
    }
}
