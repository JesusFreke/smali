package org.jf.dexlib2.builder;

import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.reference.StringReference;
import org.jf.dexlib2.iface.reference.TypeReference;
import org.jf.dexlib2.writer.builder.BuilderStringReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;

public class MethodImplementationBuilder {
    // Contains all named labels - both placed and unplaced
    private final HashMap<String, Label> labels = new HashMap<String, Label>();

    @Nonnull
    private final MutableMethodImplementation impl;

    private MethodLocation currentLocation;

    public MethodImplementationBuilder() {
        this.impl = new MutableMethodImplementation();
        this.currentLocation = impl.instructionList.get(0);
    }

    public MethodImplementation buildMethodImplementation() {
        return impl.buildMethodImplementation();
    }

    /**
     * Adds a new named label at the current location.
     *
     * Any previous unplaced references to a label of this name will now refer to this label/location
     *
     * @param name The name of the label to add
     * @return A LabelRef representing the label
     */
    @Nonnull
    public Label addLabel(@Nonnull String name) {
        Label label = labels.get(name);

        if (label != null) {
            if (label.isPlaced()) {
                throw new IllegalArgumentException("There is already a label with that name.");
            } else {
                currentLocation.getLabels().add(label);
            }
        } else {
            label = currentLocation.addNewLabel();
            labels.put(name, label);
        }

        return label;
    }

    /**
     * Get a reference to a label with the given name.
     *
     * If a label with that name has not been added yet, a new one is created, but is left
     * in an unplaced state. It is assumed that addLabel(name) will be called at a later
     * point to define the location of the label.
     *
     * @param name The name of the label to get
     * @return A LabelRef representing the label
     */
    @Nonnull
    public Label getLabel(@Nonnull String name) {
        Label label = labels.get(name);
        if (label == null) {
            label = new Label();
            labels.put(name, label);
        }
        return label;
    }

    public void addCatch(@Nullable TypeReference type, @Nonnull Label from,
                         @Nonnull Label to, @Nonnull Label handler) {
        impl.addCatch(type, from, to, handler);
    }

    public void addCatch(@Nullable String type, @Nonnull Label from, @Nonnull Label to,
                         @Nonnull Label handler) {
        impl.addCatch(type, from, to, handler);
    }

    public void addCatch(@Nonnull Label from, @Nonnull Label to, @Nonnull Label handler) {
        impl.addCatch(from, to, handler);
    }

    public void addLineNumber(int lineNumber) {
        currentLocation.addLineNumber(lineNumber);
    }

    public void addStartLocal(int registerNumber, @Nullable StringReference name, @Nullable TypeReference type,
                              @Nullable StringReference signature) {
        currentLocation.addStartLocal(registerNumber, name, type, signature);
    }

    public void addEndLocal(int registerNumber) {
        currentLocation.addEndLocal(registerNumber);
    }

    public void addRestartLocal(int registerNumber) {
        currentLocation.addRestartLocal(registerNumber);
    }

    public void addPrologue() {
        currentLocation.addPrologue();
    }

    public void addEpilogue() {
        currentLocation.addEpilogue();
    }

    public void addSetSourceFile(@Nullable BuilderStringReference sourceFile) {
        currentLocation.addSetSourceFile(sourceFile);
    }

    public void addInstruction(@Nullable BuilderInstruction instruction) {
        impl.addInstruction(instruction);
        currentLocation = impl.instructionList.get(impl.instructionList.size()-1);
    }
}
