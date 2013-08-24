package org.jf.dexlib2.builder;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.reference.Reference;
import org.jf.dexlib2.iface.reference.StringReference;
import org.jf.dexlib2.iface.reference.TypeReference;
import org.jf.dexlib2.writer.builder.BuilderStringReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class MethodImplementationBuilder<ReferenceType extends Reference> {
    public MethodImplementationBuilder() {
    }

    public MethodImplementation buildMethodImplementation() {
        return null;
    }

    @Nonnull
    public LabelRef addLabel(@Nonnull String name) {
        return null;
    }

    @Nonnull
    public LabelRef getLabel(@Nonnull String name) {
        return null;
    }

    public void addCatch(String type, LabelRef from, LabelRef to, LabelRef using) {
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
                                  @Nonnull LabelRef label) {
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
                                  @Nonnull LabelRef label) {
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
                                  @Nonnull LabelRef label) {
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
                                  @Nonnull LabelRef labelRef) {
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
                                  @Nonnull LabelRef label) {
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
                                  @Nonnull LabelRef label) {
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

    public void addPackedSwitchPayload(int startKey, @Nullable List<? extends LabelRef> switchElements) {
    }

    public void addSparseSwitchPayload(@Nullable List<? extends SwitchLabelElement> switchElements) {
    }

    public void addArrayPayload(int elementWidth, @Nullable List<Number> arrayElements) {
    }
}
