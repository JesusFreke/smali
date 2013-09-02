package org.jf.dexlib2.builder;

import com.google.common.collect.Lists;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.reference.TypeReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MutableMethodImplementation {
    final ArrayList<MethodLocation> instructionList = Lists.newArrayList(new MethodLocation(null, 0, 0));
    private final ArrayList<BuilderTryBlock> tryBlocks = Lists.newArrayList();

    public MutableMethodImplementation() {
    }

    public MethodImplementation buildMethodImplementation() {
        return null;
    }

    public List<MethodLocation> getInstructions() {
        return Collections.unmodifiableList(instructionList);
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
                assert index == instructionList.size()-1;
            }
        }
    }

    public void addInstruction(BuilderInstruction instruction) {
        MethodLocation last = instructionList.get(instructionList.size()-1);
        last.instruction = instruction;

        int nextCodeAddress = last.codeAddress + instruction.getCodeUnits();
        instructionList.add(new MethodLocation(null, nextCodeAddress, instructionList.size()));
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
            MethodLocation location = instructionList.get(index);
            location.index = i;
            location.codeAddress = codeAddress;

            Instruction instruction = location.getInstruction();
            if (instruction != null) {
                codeAddress += instruction.getCodeUnits();
            } else {
                assert i == instructionList.size() - 1;
            }
        }
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
    }
}
