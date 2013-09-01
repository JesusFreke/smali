package org.jf.dexlib2.builder.debug;

import org.jf.dexlib2.DebugItemType;
import org.jf.dexlib2.builder.BuilderDebugItem;
import org.jf.dexlib2.builder.MethodLocation;
import org.jf.dexlib2.iface.debug.EpilogueBegin;

import javax.annotation.Nonnull;

public class BuilderEpilogueBegin extends BuilderDebugItem implements EpilogueBegin {
    public BuilderEpilogueBegin(@Nonnull MethodLocation location) {
        super(location);
    }

    @Override public int getDebugItemType() { return DebugItemType.EPILOGUE_BEGIN; }
}
