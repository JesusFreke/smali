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
import org.jf.dexlib.StringIdItem;
import org.jf.dexlib.CodeItem;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.antlr.stringtemplate.StringTemplate;

public class LocalDebugMethodItem extends DebugMethodItem {
    private final String register;
    private final String name;
    private final String type;
    private final String signature;

    public LocalDebugMethodItem(CodeItem codeItem, int codeAddress, StringTemplateGroup stg, String templateName,
                                double sortOrder, int register, StringIdItem name, TypeIdItem type,
                                StringIdItem signature) {
        super(codeAddress, stg, templateName, sortOrder);
        this.register = RegisterFormatter.formatRegister(codeItem, register);
        this.name = name==null?null:name.getStringValue();
        this.type = type==null?null:type.getTypeDescriptor();
        this.signature = signature==null?null:signature.getStringValue();
    }

    @Override
    protected void setAttributes(StringTemplate template) {
        template.setAttribute("Register", register);
        template.setAttribute("Name", name);
        template.setAttribute("Type", type);
        template.setAttribute("Signature", signature);
    }
}
