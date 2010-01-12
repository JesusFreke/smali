package org.jf.baksmali.Adaptors.Format;

import org.antlr.stringtemplate.StringTemplateGroup;
import org.jf.baksmali.Adaptors.MethodDefinition;
import org.jf.dexlib.Code.Format.*;
import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.OffsetInstruction;
import org.jf.dexlib.CodeItem;

public class InstructionMethodItemFactory {
    private InstructionMethodItemFactory() {
    }

    public static InstructionMethodItem makeInstructionFormatMethodItem(MethodDefinition methodDefinition,
                                                                              CodeItem codeItem,
                                                                              int codeAddress,
                                                                              StringTemplateGroup stg,
                                                                              Instruction instruction) {

        if (instruction instanceof OffsetInstruction) {
            return new OffsetInstructionFormatMethodItem(methodDefinition.getLabelCache(), codeItem, codeAddress, stg,
                    instruction);
        }

        switch (instruction.getFormat()) {
            case ArrayData:
                return new ArrayDataMethodItem(codeItem, codeAddress, stg,
                        (ArrayDataPseudoInstruction)instruction);
            case PackedSwitchData:
                return new PackedSwitchMethodItem(methodDefinition, codeItem, codeAddress, stg,
                        (PackedSwitchDataPseudoInstruction)instruction);
            case SparseSwitchData:
                return new SparseSwitchMethodItem(methodDefinition, codeItem, codeAddress, stg,
                        (SparseSwitchDataPseudoInstruction)instruction);
            default:
                return new InstructionMethodItem(codeItem, codeAddress, stg, instruction);
        }
    }
}
