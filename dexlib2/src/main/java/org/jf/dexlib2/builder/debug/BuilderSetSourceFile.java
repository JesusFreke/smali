package org.jf.dexlib2.builder.debug;

import org.jf.dexlib2.DebugItemType;
import org.jf.dexlib2.builder.BuilderDebugItem;
import org.jf.dexlib2.builder.MethodLocation;
import org.jf.dexlib2.iface.debug.SetSourceFile;
import org.jf.dexlib2.iface.reference.StringReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BuilderSetSourceFile extends BuilderDebugItem implements SetSourceFile {
    @Nullable
    private final StringReference sourceFile;

    public BuilderSetSourceFile(@Nonnull MethodLocation location,
                                @Nullable StringReference sourceFile) {
        super(location);
        this.sourceFile = sourceFile;
    }

    @Override public int getDebugItemType() { return DebugItemType.SET_SOURCE_FILE; }

    @Nullable @Override public String getSourceFile() {
        return sourceFile==null?null:sourceFile.getString();
    }

    @Nullable @Override public StringReference getSourceFileReference() {
        return sourceFile;
    }
}
