package org.jf.smalidea;

import com.intellij.lang.ASTFactory;
import com.intellij.psi.impl.source.tree.LeafElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.Nullable;
import org.jf.smalidea.psi.leaf.SmaliClassDescriptor;

public class SmaliASTFactory extends ASTFactory {

    @Nullable
    @Override
    public LeafElement createLeaf(IElementType type, CharSequence text) {
        if (type == SmaliTokens.CLASS_DESCRIPTOR) {
            return new SmaliClassDescriptor(text);
        }
        return super.createLeaf(type, text);
    }
}
