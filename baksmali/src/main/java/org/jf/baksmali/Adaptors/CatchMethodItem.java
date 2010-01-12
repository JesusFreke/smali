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

import org.jf.dexlib.TypeIdItem;
import org.jf.baksmali.Adaptors.Reference.TypeReference;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.antlr.stringtemplate.StringTemplate;

public class CatchMethodItem extends MethodItem {
    private final StringTemplateGroup stg;
    private final TypeIdItem exceptionType;

    private final LabelMethodItem tryStartLabel;
    private final LabelMethodItem tryEndLabel;
    private final LabelMethodItem handlerLabel;

    public CatchMethodItem(MethodDefinition.LabelCache labelCache, int codeAddress, StringTemplateGroup stg,
                           TypeIdItem exceptionType, int startAddress, int endAddress, int handlerAddress) {
        super(codeAddress);
        this.stg = stg;
        this.exceptionType = exceptionType;

        tryStartLabel = labelCache.internLabel(new LabelMethodItem(startAddress, stg, "try_start_"));
        tryStartLabel.setUncommented();
        //use the address from the last covered instruction, but make the label
        //name refer to the address of the next instruction
        tryEndLabel = labelCache.internLabel(new EndTryLabelMethodItem(codeAddress, stg, endAddress));
        tryEndLabel.setUncommented();

        if (exceptionType == null) {
            handlerLabel = labelCache.internLabel(new LabelMethodItem(handlerAddress, stg, "catchall_"));
        } else {
            handlerLabel = labelCache.internLabel(new LabelMethodItem(handlerAddress, stg, "catch_"));
        }
        handlerLabel.setUncommented();
    }

    public LabelMethodItem getTryStartLabel() {
        return tryStartLabel;
    }

    public LabelMethodItem getTryEndLabel() {
        return tryEndLabel;
    }

    public LabelMethodItem getHandlerLabel() {
        return handlerLabel;
    }

    public int getSortOrder() {
        //sort after instruction and end_try label
        return 102;
    }

    protected String getTemplateName() {
        return "Catch";
    }

    @Override
    public String toString() {
        StringTemplate template = stg.getInstanceOf(getTemplateName());
        if (exceptionType != null) {
            template.setAttribute("ExceptionType", TypeReference.createTemplate(stg, exceptionType));
        }
        template.setAttribute("StartLabel", tryStartLabel);
        template.setAttribute("EndLabel", tryEndLabel);
        template.setAttribute("HandlerLabel", handlerLabel);
        return template.toString();
    }
}
