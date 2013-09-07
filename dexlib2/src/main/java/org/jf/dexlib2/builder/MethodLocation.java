package org.jf.dexlib2.builder;

import com.google.common.collect.Lists;
import org.jf.dexlib2.builder.debug.*;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.reference.StringReference;
import org.jf.dexlib2.iface.reference.TypeReference;
import org.jf.dexlib2.writer.builder.BuilderStringReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class MethodLocation {
    @Nullable BuilderInstruction instruction;
    int codeAddress;
    int index;

    private List<Label> labels = Lists.newArrayList();
    List<BuilderDebugItem> debugItems = Lists.newArrayList();

    MethodLocation(@Nullable BuilderInstruction instruction, int codeAddress, int index) {
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

    void mergeInto(@Nonnull MethodLocation other) {
        for (Label label: labels) {
            label.location = other;
            other.labels.add(label);
        }

        // We need to keep the debug items in the same order. We add the other debug items to this list, then reassign
        // the list.
        for (BuilderDebugItem debugItem: debugItems) {
            debugItem.location = other;
        }
        debugItems.addAll(other.debugItems);
        other.debugItems = debugItems;

        for (int i=debugItems.size()-1; i>=0; i--) {
            BuilderDebugItem debugItem = debugItems.get(i);
            debugItem.location = other;
            other.debugItems.add(0, debugItem);
        }
        for (BuilderDebugItem debugItem: debugItems) {
            debugItem.location = other;
            other.debugItems.add(0, debugItem);
        }
    }

    @Nonnull
    public Set<Label> getLabels() {
        return new AbstractSet<Label>() {
            @Nonnull
            @Override public Iterator<Label> iterator() {
                final Iterator<Label> it = labels.iterator();

                return new Iterator<Label>() {
                    private @Nullable Label currentLabel = null;

                    @Override public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override public Label next() {
                        currentLabel = it.next();
                        return currentLabel;
                    }

                    @Override public void remove() {
                        if (currentLabel != null) {
                            currentLabel.location = null;
                        }
                        it.remove();
                    }
                };
            }

            @Override public int size() {
                return labels.size();
            }

            @Override public boolean add(@Nonnull Label label) {
                if (label.isPlaced()) {
                    throw new IllegalArgumentException("Cannot add a label that is already placed. You must remove " +
                            "it from its current location first.");
                }
                label.location = MethodLocation.this;
                labels.add(label);
                return true;
            }
        };
    }

    @Nonnull
    public Label addNewLabel() {
        Label label = new Label(this);
        labels.add(label);
        return label;
    }

    @Nonnull
    public Set<BuilderDebugItem> getDebugItems() {
        return new AbstractSet<BuilderDebugItem>() {
            @Nonnull
            @Override public Iterator<BuilderDebugItem> iterator() {
                final Iterator<BuilderDebugItem> it = debugItems.iterator();

                return new Iterator<BuilderDebugItem>() {
                    private @Nullable BuilderDebugItem currentDebugItem = null;

                    @Override public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override public BuilderDebugItem next() {
                        currentDebugItem = it.next();
                        return currentDebugItem;
                    }

                    @Override public void remove() {
                        if (currentDebugItem != null) {
                            currentDebugItem.location = null;
                        }
                        it.remove();
                    }
                };
            }

            @Override public int size() {
                return labels.size();
            }

            @Override public boolean add(@Nonnull BuilderDebugItem debugItem) {
                if (debugItem.location != null) {
                    throw new IllegalArgumentException("Cannot add a debug item that has already been added to a " +
                            "method. You must remove it from its current location first.");
                }
                debugItem.location = MethodLocation.this;
                debugItems.add(debugItem);
                return true;
            }
        };
    }

    public void addLineNumber(int lineNumber) {
        debugItems.add(new BuilderLineNumber(this, lineNumber));
    }

    public void addStartLocal(int registerNumber, @Nullable StringReference name, @Nullable TypeReference type,
                              @Nullable StringReference signature) {
        debugItems.add(new BuilderStartLocal(this, registerNumber, name, type, signature));
    }

    public void addEndLocal(int registerNumber) {
        debugItems.add(new BuilderEndLocal(this, registerNumber));
    }

    public void addRestartLocal(int registerNumber) {
        debugItems.add(new BuilderRestartLocal(this, registerNumber));
    }

    public void addPrologue() {
        debugItems.add(new BuilderPrologueEnd(this));
    }

    public void addEpilogue() {
        debugItems.add(new BuilderEpilogueBegin(this));
    }

    public void addSetSourceFile(@Nullable BuilderStringReference sourceFile) {
        debugItems.add(new BuilderSetSourceFile(this, sourceFile));
    }
}
