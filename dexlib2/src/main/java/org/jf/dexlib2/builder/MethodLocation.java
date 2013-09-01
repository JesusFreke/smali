package org.jf.dexlib2.builder;

import com.google.common.collect.Lists;
import org.jf.dexlib2.iface.instruction.Instruction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class MethodLocation {
    @Nullable Instruction instruction;
    int codeAddress;
    int index;

    private List<Label> labels = Lists.newArrayList();

    MethodLocation(@Nullable Instruction instruction,
    int codeAddress, int index) {
        this.instruction = instruction;
        this.codeAddress = codeAddress;
        this.index = index;
    }

    @Nullable
    public Instruction getInstruction() {
        return instruction;
    }

    public int getCodeAddress() {
        return codeAddress;
    }

    public int getIndex() {
        return index;
    }

    @Nonnull
    public Collection<Label> getLabels() {
        return Collections.unmodifiableCollection(labels);
    }

    public void addLabel(@Nonnull Label label) {
        if (label.isPlaced()) {
            label.getLocation().removeLabel(label);
        }
        label.location = this;
        labels.add(label);
    }

    @Nonnull
    public Label addNewLabel() {
        Label label = new Label(this);
        labels.add(label);
        return label;
    }

    public void removeLabel(@Nonnull Label label) {
        for (int i=0; i<labels.size(); i++) {
            labels.remove(label);
        }
        label.location = null;
    }
}
