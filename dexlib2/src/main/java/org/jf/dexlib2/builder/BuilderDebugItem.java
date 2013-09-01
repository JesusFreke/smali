package org.jf.dexlib2.builder;

import org.jf.dexlib2.DebugItemType;
import org.jf.dexlib2.iface.debug.*;
import org.jf.dexlib2.iface.reference.StringReference;
import org.jf.dexlib2.iface.reference.TypeReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class BuilderDebugItem implements DebugItem {
    @Nullable MethodLocation location;

    public BuilderDebugItem(@Nonnull MethodLocation location) {
        this.location = location;
    }

    @Override public int getCodeAddress() {
        if (location == null) {
            throw new IllegalStateException("Cannot get the address of a BuilderDebugItem that isn't associated with " +
                    "a method.");
        }
        return location.getCodeAddress();
    }

}
