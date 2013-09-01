package org.jf.dexlib2.builder.debug;

import org.jf.dexlib2.DebugItemType;
import org.jf.dexlib2.builder.BuilderDebugItem;
import org.jf.dexlib2.builder.MethodLocation;
import org.jf.dexlib2.iface.debug.PrologueEnd;

import javax.annotation.Nonnull;

public class BuilderPrologueEnd extends BuilderDebugItem implements PrologueEnd {
    public BuilderPrologueEnd(@Nonnull MethodLocation location) {
        super(location);
    }

    @Override public int getDebugItemType() { return DebugItemType.PROLOGUE_END; }
}
