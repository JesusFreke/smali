package org.jf.dexlib2.builder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

class MethodItem {
    @Nullable
    MethodLocation location;

    @Nonnull
    public MethodLocation getLocation() {
        if (location == null) {
            throw new IllegalStateException("Cannot get the address of MethodItem that hasn't been added to a method.");
        }
        return location;
    }

    public boolean isPlaced() {
        return location != null;
    }
}
