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

import org.jf.baksmali.Adaptors.LabelMethodItem;
import org.jf.baksmali.Adaptors.MethodDefinition;
import org.jf.baksmali.IndentingWriter;
import org.jf.baksmali.Renderers.IntegerRenderer;
import org.jf.dexlib.Code.Format.PackedSwitchDataPseudoInstruction;
import org.jf.dexlib.CodeItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PackedSwitchMethodItem extends InstructionMethodItem<PackedSwitchDataPseudoInstruction>
        implements Iterable<LabelMethodItem> {
    private final List<LabelMethodItem> labels;

    public PackedSwitchMethodItem(MethodDefinition methodDefinition, CodeItem codeItem, int codeAddress,
                                  PackedSwitchDataPseudoInstruction instruction) {
        super(codeItem, codeAddress, instruction);

        int baseCodeAddress = methodDefinition.getPackedSwitchBaseAddress(codeAddress);

        labels = new ArrayList<LabelMethodItem>();
        Iterator<PackedSwitchDataPseudoInstruction.PackedSwitchTarget> iterator = instruction.iterateKeysAndTargets();
        while (iterator.hasNext()) {
            PackedSwitchDataPseudoInstruction.PackedSwitchTarget target = iterator.next();
            LabelMethodItem label = new LabelMethodItem(baseCodeAddress + target.targetAddressOffset, "pswitch_");
            label = methodDefinition.getLabelCache().internLabel(label);
            labels.add(label);
        }
    }

    @Override
    public boolean writeTo(IndentingWriter writer) throws IOException {
        writer.write(".packed-switch ");
        IntegerRenderer.writeTo(writer, instruction.getFirstKey());
        writer.indent(4);
        writer.write('\n');
        for (LabelMethodItem label: labels) {
            label.writeTo(writer);
            writer.write('\n');
        }
        writer.deindent(4);
        writer.write(".end packed-switch");
        return true;
    }

    public Iterator<LabelMethodItem> iterator() {
        return labels.iterator();
    }
}
