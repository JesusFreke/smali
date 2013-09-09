package org.jf.dexlib2.builder;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.instruction.BuilderInstruction10x;
import org.jf.dexlib2.builder.instruction.BuilderInstruction20t;
import org.jf.dexlib2.builder.instruction.BuilderInstruction30t;
import org.jf.dexlib2.iface.ExceptionHandler;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.TryBlock;
import org.jf.dexlib2.iface.debug.DebugItem;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.reference.TypeReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class MutableMethodImplementation implements MethodImplementation {
    private final int registerCount;
    final ArrayList<MethodLocation> instructionList = Lists.newArrayList(new MethodLocation(null, 0, 0));
    private final ArrayList<BuilderTryBlock> tryBlocks = Lists.newArrayList();
    private boolean fixInstructions = true;

    public MutableMethodImplementation(@Nonnull MethodImplementation methodImplementation) {
        throw new UnsupportedOperationException("not implemented yet.");
    }

    public MutableMethodImplementation(int registerCount) {
        this.registerCount = registerCount;
    }

    @Override public int getRegisterCount() {
        return registerCount;
    }

    @Nonnull
    public List<Instruction> getInstructions() {
        if (fixInstructions) {
            fixInstructions();
        }

        return new AbstractList<Instruction>() {
            @Override public Instruction get(int i) {
                if (i >= size()) {
                    throw new IndexOutOfBoundsException();
                }
                if (fixInstructions) {
                    fixInstructions();
                }
                return instructionList.get(i).instruction;
            }

            @Override public int size() {
                if (fixInstructions) {
                    fixInstructions();
                }
                // don't include the last MethodLocation, which always has a null instruction
                return instructionList.size() - 1;
            }
        };
    }

    @Nonnull @Override public List<? extends TryBlock<? extends ExceptionHandler>> getTryBlocks() {
        if (fixInstructions) {
            fixInstructions();
        }
        return Collections.unmodifiableList(tryBlocks);
    }

    @Nonnull @Override public Iterable<? extends DebugItem> getDebugItems() {
        if (fixInstructions) {
            fixInstructions();
        }
        return Iterables.concat(
                Iterables.transform(instructionList, new Function<MethodLocation, Iterable<? extends DebugItem>>() {
                    @Nullable @Override public Iterable<? extends DebugItem> apply(@Nullable MethodLocation input) {
                        assert input != null;
                        if (fixInstructions) {
                            throw new IllegalStateException("This iterator was invalidated by a change to" +
                                    " this MutableMethodImplementation.");
                        }
                        return input.getDebugItems();
                    }
                }));
    }

    public void addCatch(@Nullable TypeReference type, @Nonnull Label from,
                         @Nonnull Label to, @Nonnull Label handler) {
        tryBlocks.add(new BuilderTryBlock(from, to, type, handler));
    }

    public void addCatch(@Nullable String type, @Nonnull Label from, @Nonnull Label to,
                         @Nonnull Label handler) {
        tryBlocks.add(new BuilderTryBlock(from, to, type, handler));
    }

    public void addCatch(@Nonnull Label from, @Nonnull Label to, @Nonnull Label handler) {
        tryBlocks.add(new BuilderTryBlock(from, to, handler));
    }

    public void addInstruction(int index, BuilderInstruction instruction) {
        // the end check here is intentially >= rather than >, because the list always includes an "empty"
        // (null instruction) MethodLocation at the end. To add an instruction to the end of the list, the user would
        // provide the index of this empty item, which would be size() - 1.
        if (index >= instructionList.size()) {
            throw new IndexOutOfBoundsException();
        }

        if (index == instructionList.size() - 1) {
            addInstruction(instruction);
            return;
        }
        int codeAddress = instructionList.get(index).getCodeAddress();

        instructionList.add(index, new MethodLocation(instruction, codeAddress, index));
        codeAddress += instruction.getCodeUnits();

        for (int i=index+1; i<instructionList.size(); i++) {
            MethodLocation location = instructionList.get(i);
            location.index++;
            location.codeAddress = codeAddress;
            if (location.instruction != null) {
                codeAddress += location.instruction.getCodeUnits();
            } else {
                // only the last MethodLocation should have a null instruction
                assert i == instructionList.size()-1;
            }
        }

        this.fixInstructions = true;
    }

    public void addInstruction(@Nonnull BuilderInstruction instruction) {
        MethodLocation last = instructionList.get(instructionList.size()-1);
        last.instruction = instruction;
        instruction.location = last;

        int nextCodeAddress = last.codeAddress + instruction.getCodeUnits();
        instructionList.add(new MethodLocation(null, nextCodeAddress, instructionList.size()));

        this.fixInstructions = true;
    }

    public void replaceInstruction(int index, @Nonnull BuilderInstruction replacementInstruction) {
        if (index >= instructionList.size() - 1) {
            throw new IndexOutOfBoundsException();
        }

        MethodLocation replaceLocation = instructionList.get(index);
        replacementInstruction.location = replaceLocation;
        BuilderInstruction old = replaceLocation.instruction;
        assert old != null;
        old.location = null;
        replaceLocation.instruction = replacementInstruction;

        // TODO: factor out index/address fix up loop
        int codeAddress = replaceLocation.codeAddress + replaceLocation.instruction.getCodeUnits();
        for (int i=index+1; i<instructionList.size(); i++) {
            MethodLocation location = instructionList.get(i);
            location.codeAddress = codeAddress;

            Instruction instruction = location.getInstruction();
            if (instruction != null) {
                codeAddress += instruction.getCodeUnits();
            } else {
                assert i == instructionList.size() - 1;
            }
        }

        this.fixInstructions = true;
    }

    public void removeInstruction(int index) {
        if (index >= instructionList.size() - 1) {
            throw new IndexOutOfBoundsException();
        }

        MethodLocation toRemove = instructionList.get(index);
        toRemove.instruction = null;
        MethodLocation next = instructionList.get(index+1);
        toRemove.mergeInto(next);

        instructionList.remove(index);
        int codeAddress = toRemove.codeAddress;
        for (int i=index; i<instructionList.size(); i++) {
            MethodLocation location = instructionList.get(i);
            location.index = i;
            location.codeAddress = codeAddress;

            Instruction instruction = location.getInstruction();
            if (instruction != null) {
                codeAddress += instruction.getCodeUnits();
            } else {
                assert i == instructionList.size() - 1;
            }
        }

        this.fixInstructions = true;
    }

    public void swapInstructions(int index1, int index2) {
        if (index1 >= instructionList.size() - 1 || index2 >= instructionList.size() - 1) {
            throw new IndexOutOfBoundsException();
        }
        MethodLocation first = instructionList.get(index1);
        MethodLocation second = instructionList.get(index2);

        // only the last MethodLocation may have a null instruction
        assert first.instruction != null;
        assert second.instruction != null;

        first.instruction.location = second;
        second.instruction.location = first;

        {
            BuilderInstruction tmp = second.instruction;
            second.instruction = first.instruction;
            first.instruction = tmp;
        }

        if (index2 < index1) {
            int tmp = index2;
            index2 = index1;
            index1 = tmp;
        }

        int codeAddress = first.codeAddress + first.instruction.getCodeUnits();
        for (int i=index1+1; i<=index2; i++) {
            MethodLocation location = instructionList.get(i);
            location.codeAddress = codeAddress;

            Instruction instruction = location.instruction;
            assert instruction != null;
            codeAddress += location.instruction.getCodeUnits();
        }

        this.fixInstructions = true;
    }

    @Nullable
    private BuilderInstruction getFirstNonNop(int startIndex) {

        for (int i=startIndex; i<instructionList.size()-1; i++) {
            BuilderInstruction instruction = instructionList.get(i).instruction;
            assert instruction != null;
            if (instruction.getOpcode() != Opcode.NOP) {
                return instruction;
            }
        }
        return null;
    }

    private void fixInstructions() {
        HashSet<MethodLocation> payloadLocations = Sets.newHashSet();

        for (MethodLocation location: instructionList) {
            BuilderInstruction instruction = location.instruction;
            if (instruction != null) {
                switch (instruction.getOpcode()) {
                    case SPARSE_SWITCH:
                    case PACKED_SWITCH: {
                        MethodLocation targetLocation =
                                ((BuilderOffsetInstruction)instruction).getTarget().getLocation();
                        BuilderInstruction targetInstruction = targetLocation.instruction;
                        if (targetInstruction == null) {
                            throw new IllegalStateException(String.format("Switch instruction at address/index " +
                                    "0x%x/%d points to the end of the method.", location.codeAddress, location.index));
                        }

                        if (targetInstruction.getOpcode() == Opcode.NOP) {
                            targetInstruction = getFirstNonNop(targetLocation.index+1);
                        }
                        if (targetInstruction == null || !(targetInstruction instanceof BuilderSwitchPayload)) {
                            throw new IllegalStateException(String.format("Switch instruction at address/index " +
                                    "0x%x/%d does not refer to a payload instruction.",
                                    location.codeAddress, location.index));
                        }
                        if ((instruction.opcode == Opcode.PACKED_SWITCH &&
                                targetInstruction.getOpcode() != Opcode.PACKED_SWITCH_PAYLOAD) ||
                            (instruction.opcode == Opcode.SPARSE_SWITCH &&
                                targetInstruction.getOpcode() != Opcode.SPARSE_SWITCH_PAYLOAD)) {
                            throw new IllegalStateException(String.format("Switch instruction at address/index " +
                                    "0x%x/%d refers to the wrong type of payload instruction.",
                                    location.codeAddress, location.index));
                        }

                        if (!payloadLocations.add(targetLocation)) {
                            throw new IllegalStateException("Multiple switch instructions refer to the same payload. " +
                                    "This is not currently supported. Please file a bug :)");
                        }

                        ((BuilderSwitchPayload)targetInstruction).referrer = location;
                        break;
                    }
                }
            }
        }

        boolean madeChanges;
        do {
            madeChanges = false;

            for (int index=0; index<instructionList.size(); index++) {
                MethodLocation location = instructionList.get(index);
                BuilderInstruction instruction = location.instruction;
                if (instruction != null) {
                    switch (instruction.getOpcode()) {
                        case GOTO: {
                            int offset = ((BuilderOffsetInstruction)instruction).internalGetCodeOffset();
                            if (offset < Byte.MIN_VALUE || offset > Byte.MAX_VALUE) {
                                BuilderOffsetInstruction replacement;
                                if (offset < Short.MIN_VALUE || offset > Short.MAX_VALUE) {
                                    replacement = new BuilderInstruction30t(Opcode.GOTO_32,
                                            ((BuilderOffsetInstruction)instruction).getTarget());
                                } else {
                                    replacement = new BuilderInstruction20t(Opcode.GOTO_16,
                                            ((BuilderOffsetInstruction)instruction).getTarget());
                                }
                                replaceInstruction(location.index, replacement);
                                madeChanges = true;
                            }
                            break;
                        }
                        case GOTO_16: {
                            int offset = ((BuilderOffsetInstruction)instruction).internalGetCodeOffset();
                            if (offset < Short.MIN_VALUE || offset > Short.MAX_VALUE) {
                                BuilderOffsetInstruction replacement =  new BuilderInstruction30t(Opcode.GOTO_32,
                                            ((BuilderOffsetInstruction)instruction).getTarget());
                                replaceInstruction(location.index, replacement);
                                madeChanges = true;
                            }
                            break;
                        }
                        case SPARSE_SWITCH_PAYLOAD:
                        case PACKED_SWITCH_PAYLOAD:
                        case ARRAY_PAYLOAD: {
                            if ((location.codeAddress & 0x01) != 0) {
                                int previousIndex = location.index - 1;
                                MethodLocation previousLocation = instructionList.get(previousIndex);
                                Instruction previousInstruction = previousLocation.instruction;
                                assert previousInstruction != null;
                                if (previousInstruction.getOpcode() == Opcode.NOP) {
                                    removeInstruction(previousIndex);
                                    index--;
                                } else {
                                    addInstruction(location.index, new BuilderInstruction10x(Opcode.NOP));
                                    index++;
                                }
                                madeChanges = true;
                            }
                            break;
                        }
                    }
                }
            }
        } while (madeChanges);

        fixInstructions = false;
    }
}
