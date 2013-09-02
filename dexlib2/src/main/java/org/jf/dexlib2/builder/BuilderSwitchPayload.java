package org.jf.dexlib2.builder;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.iface.instruction.SwitchPayload;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class BuilderSwitchPayload extends BuilderInstruction implements SwitchPayload {
    @Nullable
    MethodLocation referrer;

    protected BuilderSwitchPayload(@Nonnull Opcode opcode) {
        super(opcode);
    }

    @Nonnull
    public MethodLocation getReferrer() {
        if (referrer == null) {
            throw new IllegalStateException("The referrer has not been set yet");
        }
        return referrer;
    }
}
