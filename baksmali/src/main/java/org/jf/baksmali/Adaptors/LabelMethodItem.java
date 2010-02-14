/*
 * [The "BSD licence"]
 * Copyright (c) 2009 Ben Gruver
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.baksmali.Adaptors;

import org.antlr.stringtemplate.StringTemplateGroup;
import org.antlr.stringtemplate.StringTemplate;
import org.jf.baksmali.baksmali;

public class LabelMethodItem extends MethodItem {
    private final StringTemplateGroup stg;
    private final String labelPrefix;
    private int labelSequence;
    private boolean isCommentedOut = true;

    public LabelMethodItem(int codeAddress, StringTemplateGroup stg, String labelPrefix) {
        super(codeAddress);
        this.stg = stg;
        this.labelPrefix = labelPrefix;
    }

    public double getSortOrder() {
        return 0;
    }

    public boolean isCommentedOut() {
        return isCommentedOut;
    }

    public void setUncommented() {
        this.isCommentedOut = false;
    }

    public int compareTo(MethodItem methodItem) {
        int result = super.compareTo(methodItem);

        if (result == 0) {
            if (methodItem instanceof LabelMethodItem) {
                result = labelPrefix.compareTo(((LabelMethodItem)methodItem).labelPrefix);
            }
        }
        return result;
    }

    public int hashCode() {
        //force it to call equals when two labels are at the same address
        return getCodeAddress();
    }

    public boolean equals(Object o) {
        if (!(o instanceof LabelMethodItem)) {
            return false;
        }
        return this.compareTo((MethodItem)o) == 0;
    }

    @Override
    public String toString() {
        StringTemplate template = stg.getInstanceOf("Label");
        template.setAttribute("Prefix", labelPrefix);
        if (baksmali.useSequentialLabels) {
            template.setAttribute("Suffix", Integer.toHexString(labelSequence));
        } else {
            template.setAttribute("Suffix", getLabelAddress());
        }
        return template.toString();
    }

    public String getLabelPrefix() {
        return labelPrefix;
    }

    public String getLabelAddress() {
        return Integer.toHexString(this.getCodeAddress());
    }

    public int getLabelSequence() {
        return labelSequence;
    }

    public void setLabelSequence(int labelSequence) {
        this.labelSequence = labelSequence;
    }
}
