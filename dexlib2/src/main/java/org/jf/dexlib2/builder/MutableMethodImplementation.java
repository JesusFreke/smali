package org.jf.dexlib2.builder;

import com.google.common.collect.Lists;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.reference.Reference;
import org.jf.dexlib2.iface.reference.TypeReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MutableMethodImplementation<ReferenceType extends Reference> {
    final ArrayList<MethodLocation> instructionList = Lists.newArrayList(new MethodLocation(null, 0, 0));
    private final ArrayList<BuilderTryBlock> tryBlocks = Lists.newArrayList();

    public MutableMethodImplementation() {
    }

    public MethodImplementation buildMethodImplementation() {
        return null;
    }

    public List<MethodLocation> getInstruction() {
        return Collections.unmodifiableList(instructionList);
    }

    public void addCatch(@Nullable TypeReference type, @Nonnull Label from,
                         @Nonnull Label to, @Nonnull Label handler) {
        tryBlocks.add(new BuilderTryBlock(from, to, type, handler));
    }

    public void addCatch(@Nullable String type, @Nonnull Label from, @Nonnull Label to,
                         @Nonnull Label handler) {
        tryBlocks.add(new BuilderTryBlock(from, to, type, handler));
    }

    public void addCatch(@Nonnull Label from, @Nonnull Label to, @Nonnull Label handler) {
        tryBlocks.add(new BuilderTryBlock(from, to, handler));
    }
}
