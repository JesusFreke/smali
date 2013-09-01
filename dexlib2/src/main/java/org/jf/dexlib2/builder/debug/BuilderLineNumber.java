package org.jf.dexlib2.builder.debug;

import org.jf.dexlib2.DebugItemType;
import org.jf.dexlib2.builder.BuilderDebugItem;
import org.jf.dexlib2.builder.MethodLocation;
import org.jf.dexlib2.iface.debug.LineNumber;

import javax.annotation.Nonnull;

public class BuilderLineNumber extends BuilderDebugItem implements LineNumber {
    private final int lineNumber;

    public BuilderLineNumber(@Nonnull MethodLocation location,
                             int lineNumber) {
        super(location);
        this.lineNumber = lineNumber;
    }

    @Override public int getLineNumber() { return lineNumber; }

    @Override public int getDebugItemType() { return DebugItemType.LINE_NUMBER; }
}
