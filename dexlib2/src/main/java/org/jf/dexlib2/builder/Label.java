package org.jf.dexlib2.builder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Label {
    @Nullable MethodLocation location;

    Label() {
    }

    Label(MethodLocation location) {
        this.location = location;
    }

    public int getCodeAddress() {
        return getLocation().getCodeAddress();
    }

    @Nonnull
    public MethodLocation getLocation() {
        if (location == null) {
            throw new IllegalStateException("Cannot get the location of a label that hasn't been placed yet.");
        }
        return location;
    }

    public boolean isPlaced() {
        return location != null;
    }
}
