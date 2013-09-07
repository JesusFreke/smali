package org.jf.dexlib2.builder;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.iface.instruction.OffsetInstruction;

import javax.annotation.Nonnull;

public abstract class BuilderOffsetInstruction extends BuilderInstruction implements OffsetInstruction {
    @Nonnull
    protected final Label target;

    public BuilderOffsetInstruction(@Nonnull Opcode opcode,
                                    @Nonnull Label target) {
        super(opcode);
        this.target = target;
    }

    @Override public int getCodeOffset() {
        int codeOffset = internalGetCodeOffset();
        if ((this.getCodeUnits() == 1 && (codeOffset < Byte.MIN_VALUE || codeOffset > Byte.MAX_VALUE)) ||
            (this.getCodeUnits() == 2 && (codeOffset < Short.MIN_VALUE || codeOffset > Short.MAX_VALUE))) {
            throw new IllegalStateException("Target is out of range");
        }
        return codeOffset;
    }


    int internalGetCodeOffset() {
        return target.getCodeAddress() - this.getLocation().getCodeAddress();
    }

    @Nonnull
    public Label getTarget() {
        return target;
    }
}
