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

package org.jf.baksmali.Adaptors.Format;

import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.jf.baksmali.Adaptors.Reference.Reference;
import org.jf.dexlib.Code.Format.Instruction35c;
import org.jf.dexlib.CodeItem;

public class Instruction35cMethodItem extends InstructionFormatMethodItem<Instruction35c> {
    public Instruction35cMethodItem(CodeItem codeItem, int offset, StringTemplateGroup stg,
                                    Instruction35c instruction) {
        super(codeItem, offset, stg, instruction);
    }

    protected void setAttributes(StringTemplate template) {
        template.setAttribute("Reference", Reference.createReference(template.getGroup(),
                instruction.getReferencedItem()));
        setRegistersAttribute(template);
    }

    private void setRegistersAttribute(StringTemplate template) {
        switch (instruction.getRegCount()) {
            case 1:
                template.setAttribute("Registers", formatRegister(instruction.getRegisterD()));
                return;
            case 2:
                template.setAttribute("Registers", formatRegister(instruction.getRegisterD()));
                template.setAttribute("Registers", formatRegister(instruction.getRegisterE()));
                return;
            case 3:
                template.setAttribute("Registers", formatRegister(instruction.getRegisterD()));
                template.setAttribute("Registers", formatRegister(instruction.getRegisterE()));
                template.setAttribute("Registers", formatRegister(instruction.getRegisterF()));
                return;
            case 4:
                template.setAttribute("Registers", formatRegister(instruction.getRegisterD()));
                template.setAttribute("Registers", formatRegister(instruction.getRegisterE()));
                template.setAttribute("Registers", formatRegister(instruction.getRegisterF()));
                template.setAttribute("Registers", formatRegister(instruction.getRegisterG()));
                return;
            case 5:
                template.setAttribute("Registers", formatRegister(instruction.getRegisterD()));
                template.setAttribute("Registers", formatRegister(instruction.getRegisterE()));
                template.setAttribute("Registers", formatRegister(instruction.getRegisterF()));
                template.setAttribute("Registers", formatRegister(instruction.getRegisterG()));
                template.setAttribute("Registers", formatRegister(instruction.getRegisterA()));
        }
    }
}
