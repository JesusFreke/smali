/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver (JesusFreke)
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
import org.jf.baksmali.Adaptors.MethodItem;
import org.jf.baksmali.Adaptors.Reference.Reference;
import org.jf.baksmali.Adaptors.RegisterFormatter;
import org.jf.dexlib.Code.*;
import org.jf.dexlib.CodeItem;

import java.util.LinkedList;

public class InstructionMethodItem<T extends Instruction> extends MethodItem {
    protected final CodeItem codeItem;
    protected final StringTemplateGroup stg;
    protected final T instruction;

    public InstructionMethodItem(CodeItem codeItem, int codeAddress, StringTemplateGroup stg, T instruction) {
        super(codeAddress);
        this.codeItem = codeItem;
        this.stg = stg;
        this.instruction = instruction;
    }

    public double getSortOrder() {
        //instructions should appear after everything except an "end try" label and .catch directive
        return 100;
    }

    protected String formatRegister(int register) {
        return RegisterFormatter.formatRegister(codeItem, register);
    }

    @Override
    public String toString() {
        StringTemplate template = stg.getInstanceOf(instruction.getFormat().name());
        template.setAttribute("Opcode", instruction.opcode.name);
        setAttributes(template);
        return template.toString();
    }

    protected void setAttributes(StringTemplate template) {
        if (instruction instanceof LiteralInstruction) {
            setLiteralAttributes((LiteralInstruction)instruction, template);
        }

        if (instruction instanceof SingleRegisterInstruction) {
            setSingleRegisterAttributes((SingleRegisterInstruction)instruction, template);
        }

        if (instruction instanceof FiveRegisterInstruction) {
            setFiveRegisterAttributes((FiveRegisterInstruction)instruction, template);
        }

        if (instruction instanceof RegisterRangeInstruction) {
            setRegisterRangeAttributes((RegisterRangeInstruction)instruction, template);
        }

        if (instruction instanceof InstructionWithReference) {
            setInstructionWithReferenceAttributes((InstructionWithReference)instruction, template);
        }

        if (instruction instanceof OdexedInvokeVirtual) {
            setOdexedInvokeVirtualAttributes((OdexedInvokeVirtual)instruction, template);
        }

        if (instruction instanceof OdexedFieldAccess) {
            setOdexedFieldAccessAttributes((OdexedFieldAccess)instruction, template);
        }
    }

    private void setLiteralAttributes(LiteralInstruction instruction, StringTemplate template) {
        long literal = instruction.getLiteral();
            //TODO: do we really need to check and cast it to an int?
            if (literal <= Integer.MAX_VALUE && literal >= Integer.MIN_VALUE) {
                template.setAttribute("Literal", (int)literal);
            } else {
                template.setAttribute("Literal", literal);
            }
    }

    private void setSingleRegisterAttributes(SingleRegisterInstruction instruction, StringTemplate template) {
        template.setAttribute("RegisterA", formatRegister(instruction.getRegisterA()));

        if (instruction instanceof TwoRegisterInstruction) {
            setTwoRegisterAttributes((TwoRegisterInstruction)instruction, template);
        }
    }

    private void setTwoRegisterAttributes(TwoRegisterInstruction instruction, StringTemplate template) {
        template.setAttribute("RegisterB", formatRegister(instruction.getRegisterB()));

        if (instruction instanceof ThreeRegisterInstruction) {
            setThreeRegisterAttributes((ThreeRegisterInstruction)instruction, template);
        }
    }

    private void setThreeRegisterAttributes(ThreeRegisterInstruction instruction, StringTemplate template) {
        template.setAttribute("RegisterC", formatRegister(instruction.getRegisterC()));
    }

    private void setFiveRegisterAttributes(FiveRegisterInstruction instruction, StringTemplate template) {
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

    private void setRegisterRangeAttributes(RegisterRangeInstruction instruction, StringTemplate template) {
        String[] registers = RegisterFormatter.formatFormat3rcRegisters(codeItem, instruction.getStartRegister(),
                instruction.getStartRegister() + instruction.getRegCount() - 1);

        template.setAttribute("StartRegister", registers[0]);
        template.setAttribute("LastRegister", registers[1]);
    }

    private void setInstructionWithReferenceAttributes(InstructionWithReference instruction, StringTemplate template) {
        template.setAttribute("Reference", Reference.createReference(template.getGroup(),
                instruction.getReferencedItem()));
    }

    private void setOdexedInvokeVirtualAttributes(OdexedInvokeVirtual instruction, StringTemplate template) {
        template.setAttribute("MethodIndex", instruction.getMethodIndex());
    }

    private void setOdexedFieldAccessAttributes(OdexedFieldAccess instruction, StringTemplate template) {
        template.setAttribute("FieldOffset", instruction.getFieldOffset());
    }
}
