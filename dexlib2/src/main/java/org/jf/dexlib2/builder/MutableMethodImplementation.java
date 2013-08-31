package org.jf.dexlib2.builder;

import com.google.common.collect.Lists;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.reference.Reference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MutableMethodImplementation<ReferenceType extends Reference> {
    final ArrayList<MethodLocation> instructionList = Lists.newArrayList(new MethodLocation(null, 0, 0));

    public MutableMethodImplementation() {
    }

    public MethodImplementation buildMethodImplementation() {
        return null;
    }

    public List<MethodLocation> getInstruction() {
        return Collections.unmodifiableList(instructionList);
    }
}
