package org.jf.baksmali.Adaptors.Format;

import org.antlr.stringtemplate.StringTemplateGroup;
import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.CodeItem;

public class OdexInstructionMethodItem<T extends Instruction> extends InstructionMethodItem<T> {
    protected Instruction fixedInstruction = null;

    public OdexInstructionMethodItem(CodeItem codeItem, int codeAddress, StringTemplateGroup stg, T ins) {
        super(codeItem, codeAddress, stg, ins);
    }

    public Instruction getFixedInstruction() {
        return fixedInstruction;
    }

    public void setFixedInstruction(Instruction fixedInstruction) {
        this.fixedInstruction = fixedInstruction;
    }
}
