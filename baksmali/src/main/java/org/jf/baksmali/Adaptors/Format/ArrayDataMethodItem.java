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

import org.jf.dexlib.Code.Format.ArrayDataPseudoInstruction;
import org.jf.dexlib.Util.ByteArray;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.antlr.stringtemplate.StringTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Arrays;

public class ArrayDataMethodItem extends InstructionFormatMethodItem<ArrayDataPseudoInstruction> {
    public ArrayDataMethodItem(int offset, StringTemplateGroup stg,  ArrayDataPseudoInstruction instruction) {
        super(offset, stg, instruction);
    }

    protected void setAttributes(StringTemplate template) {
        template.setAttribute("ElementWidth", instruction.getElementWidth());
        template.setAttribute("Values", getValues());
    }

    private List<ByteArray> getValues() {
        List<ByteArray> values = new ArrayList<ByteArray>();        
        Iterator<ArrayDataPseudoInstruction.ArrayElement> iterator = instruction.getElements();

        while (iterator.hasNext()) {
            ArrayDataPseudoInstruction.ArrayElement element = iterator.next();
            byte[] array = new byte[element.elementWidth];
            System.arraycopy(element.buffer, element.bufferIndex, array, 0, element.elementWidth);
            values.add(new ByteArray(array));
        }

        return values;
    }

    public static class ByteArray
    {
        public final byte[] ByteArray;
        public ByteArray(byte[] byteArray) {
            this.ByteArray = byteArray;
        }
    }
}
