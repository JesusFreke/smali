package org.jf.dexlib2.builder.instruction;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.BuilderInstruction;
import org.jf.dexlib2.builder.Label;
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
        return target.getCodeAddress() - this.getLocation().getCodeAddress();
    }
}
