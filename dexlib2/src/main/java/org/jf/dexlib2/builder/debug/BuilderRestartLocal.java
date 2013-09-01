package org.jf.dexlib2.builder.debug;

import org.jf.dexlib2.DebugItemType;
import org.jf.dexlib2.builder.BuilderDebugItem;
import org.jf.dexlib2.builder.MethodLocation;
import org.jf.dexlib2.iface.debug.RestartLocal;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BuilderRestartLocal extends BuilderDebugItem implements RestartLocal {
    private final int register;

    public BuilderRestartLocal(@Nonnull MethodLocation location,
                               int register) {
        super(location);
        this.register = register;
    }

    @Override public int getRegister() { return register; }
    @Nullable @Override public String getName() { return null; }
    @Nullable @Override public String getType() { return null; }
    @Nullable @Override public String getSignature() { return null; }

    @Override public int getDebugItemType() { return DebugItemType.RESTART_LOCAL; }
}
