package org.jf.smalidea.psi.leaf;

import com.intellij.psi.impl.source.tree.LeafPsiElement;
import org.jf.smalidea.SmaliTokens;

public class SmaliSimpleName extends LeafPsiElement {
    public SmaliSimpleName(CharSequence text) {
        super(SmaliTokens.SIMPLE_NAME, text);
    }
}
