package org.jf.dexlib2.builder;

import javax.annotation.Nonnull;

public class SwitchLabelElement {
    public final int key;
    @Nonnull public final Label target;

    public SwitchLabelElement(int key, @Nonnull Label target) {
        this.key = key;
        this.target = target;
    }
}
