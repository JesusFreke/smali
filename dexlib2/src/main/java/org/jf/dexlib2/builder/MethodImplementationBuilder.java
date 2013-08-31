package org.jf.dexlib2.builder;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.iface.reference.Reference;
import org.jf.dexlib2.iface.reference.StringReference;
import org.jf.dexlib2.iface.reference.TypeReference;
import org.jf.dexlib2.writer.builder.BuilderStringReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;

public class MethodImplementationBuilder<ReferenceType extends Reference> {
    // Contains all named labels - both placed and unplaced
    private final HashMap<String, LabelMethodItem> labels = new HashMap<String, LabelMethodItem>();

    @Nonnull
    private final MutableMethodImplementation<ReferenceType> impl;

    // the current instruction index
    private int currentIndex;

    public MethodImplementationBuilder(@Nonnull MutableMethodImplementation<ReferenceType> impl) {
        this.impl = impl;
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
    public LabelMethodItem addLabel(@Nonnull String name) {
        MethodLocation location = impl.instructionList.get(currentIndex);

        LabelMethodItem label = labels.get(name);

        if (label != null) {
            if (label.isPlaced()) {
                throw new IllegalArgumentException("There is already a label with that name.");
            } else {
                location.addLabel(label);
            }
        } else {
            label = location.addNewLabel();
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
    public LabelMethodItem getLabel(@Nonnull String name) {
        LabelMethodItem label = labels.get(name);
        if (label == null) {
            label = new LabelMethodItem();
            labels.put(name, label);
        }
        return label;
    }

    public void addCatch(@Nullable TypeReference type, @Nonnull LabelMethodItem from,
                         @Nonnull LabelMethodItem to, @Nonnull LabelMethodItem handler) {
    }

    public void addCatch(@Nullable String type, @Nonnull LabelMethodItem from, @Nonnull LabelMethodItem to,
                         @Nonnull LabelMethodItem handler) {
    }

    public void addCatch(@Nonnull LabelMethodItem from, @Nonnull LabelMethodItem to, @Nonnull LabelMethodItem handler) {
    }

    public void addLineNumber(int lineNumber) {
    }

    public void addStartLocal(int registerNumber, @Nullable StringReference name, @Nullable TypeReference type,
                              @Nullable StringReference signature) {
    }

    public void addEndLocal(int registerNumber) {
    }

    public void addRestartLocal(int registerNumber) {
    }

    public void addPrologue() {
    }

    public void addEpilogue() {
    }

    public void addSetSourceFile(@Nullable BuilderStringReference sourceFile) {
    }

    public void addInstruction10t(@Nonnull Opcode opcode,
                                  @Nonnull LabelMethodItem label) {
    }

    public void addInstruction10x(@Nonnull Opcode opcode) {
    }

    public void addInstruction11n(@Nonnull Opcode opcode,
                                  int registerA,
                                  int literal) {
    }

    public void addInstruction11x(@Nonnull Opcode opcode,
                                  int registerA) {
    }

    public void addInstruction12x(@Nonnull Opcode opcode,
                                  int registerA,
                                  int registerB) {
    }

    public void addInstruction20bc(@Nonnull Opcode opcode,
                                   int verificationError,
                                   @Nonnull ReferenceType reference) {
    }

    public void addInstruction20t(@Nonnull Opcode opcode,
                                  @Nonnull LabelMethodItem label) {
    }

    public void addInstruction21c(@Nonnull Opcode opcode,
                                  int registerA,
                                  @Nonnull ReferenceType reference) {
    }

    public void addInstruction21ih(@Nonnull Opcode opcode,
                                   int registerA,
                                   int literal) {
    }

    public void addInstruction21lh(@Nonnull Opcode opcode,
                                   int registerA,
                                   long literal) {
    }

    public void addInstruction21s(@Nonnull Opcode opcode,
                                  int registerA,
                                  int literal) {
    }

    public void addInstruction21t(@Nonnull Opcode opcode,
                                  int registerA,
                                  @Nonnull LabelMethodItem label) {
    }

    public void addInstruction22b(@Nonnull Opcode opcode,
                                  int registerA,
                                  int registerB,
                                  int literal) {
    }

    public void addInstruction22c(@Nonnull Opcode opcode,
                                  int registerA,
                                  int registerB,
                                  @Nonnull ReferenceType reference) {
    }

    public void addInstruction22s(@Nonnull Opcode opcode,
                                  int registerA,
                                  int registerB,
                                  int literal) {
    }

    public void addInstruction22t(@Nonnull Opcode opcode,
                                  int registerA,
                                  int registerB,
                                  @Nonnull LabelMethodItem labelMethodItem) {
    }

    public void addInstruction22x(@Nonnull Opcode opcode,
                                  int registerA,
                                  int registerB) {
    }

    public void addInstruction23x(@Nonnull Opcode opcode,
                                  int registerA,
                                  int registerB,
                                  int registerC) {
    }

    public void addInstruction30t(@Nonnull Opcode opcode,
                                  @Nonnull LabelMethodItem label) {
    }

    public void addInstruction31c(@Nonnull Opcode opcode,
                                  int registerA,
                                  @Nonnull ReferenceType reference) {
    }

    public void addInstruction31i(@Nonnull Opcode opcode,
                                  int registerA,
                                  int literal) {
    }

    public void addInstruction31t(@Nonnull Opcode opcode,
                                  int registerA,
                                  @Nonnull LabelMethodItem label) {
    }

    public void addInstruction32x(@Nonnull Opcode opcode,
                                  int registerA,
                                  int registerB) {
    }

    public void addInstruction35c(@Nonnull Opcode opcode,
                                  int registerCount,
                                  int registerC,
                                  int registerD,
                                  int registerE,
                                  int registerF,
                                  int registerG,
                                  @Nonnull ReferenceType reference) {
    }

    public void addInstruction3rc(@Nonnull Opcode opcode,
                                  int startRegister,
                                  int registerCount,
                                  @Nonnull ReferenceType reference) {
    }

    public void addInstruction51l(@Nonnull Opcode opcode,
                                  int registerA,
                                  long literal) {
    }

    public void addPackedSwitchPayload(int startKey, @Nullable List<? extends LabelMethodItem> switchElements) {
    }

    public void addSparseSwitchPayload(@Nullable List<? extends SwitchLabelElement> switchElements) {
    }

    public void addArrayPayload(int elementWidth, @Nullable List<Number> arrayElements) {
    }
}
