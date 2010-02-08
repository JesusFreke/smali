package org.jf.baksmali.Adaptors;

import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;

public class CommentMethodItem extends MethodItem {
    private final StringTemplate template;
    private final int sortOrder;

    public CommentMethodItem(StringTemplateGroup stg, String comment, int codeAddress, int sortOrder) {
        super(codeAddress);
        template = stg.getInstanceOf("Comment");
        template.setAttribute("Comment", comment);
        this.sortOrder = sortOrder;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    @Override
    public String toString() {
        return template.toString();
    }
}
