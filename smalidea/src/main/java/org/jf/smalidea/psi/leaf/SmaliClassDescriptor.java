package org.jf.smalidea.psi.leaf;

import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;
import org.jf.smalidea.SmaliTokens;

public class SmaliClassDescriptor extends LeafPsiElement implements PsiIdentifier {
    public SmaliClassDescriptor(CharSequence text) {
        super(SmaliTokens.CLASS_DESCRIPTOR, text);
    }

    @Override
    public IElementType getTokenType() {
        return SmaliTokens.CLASS_DESCRIPTOR;
    }
}
