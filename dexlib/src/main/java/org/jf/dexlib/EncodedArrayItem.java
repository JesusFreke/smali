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
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.dexlib;

import org.jf.dexlib.EncodedValue.ArrayEncodedValueSubField;
import org.jf.dexlib.EncodedValue.EncodedValue;
import org.jf.dexlib.util.AnnotatedOutput;
import org.jf.dexlib.util.Input;

import java.util.ArrayList;

public class EncodedArrayItem extends OffsettedItem<EncodedArrayItem> {
    private final ArrayEncodedValueSubField encodedArray;
    
    public EncodedArrayItem(DexFile dexFile, int offset) {
        super(offset);

        fields = new Field[] {
                encodedArray = new ArrayEncodedValueSubField(dexFile)
        };
    }

    public EncodedArrayItem(DexFile dexFile, ArrayList<EncodedValue> encodedValues) {
        super(0);

        fields = new Field[] {
                encodedArray = new ArrayEncodedValueSubField(dexFile, encodedValues)
        };
    }

    public void readFrom(Input in) {
        super.readFrom(in);
    }

    public int place(int index, int offset) {
        return super.place(index, offset);
    }

    public void writeTo(AnnotatedOutput out) {
        super.writeTo(out);
    }

    protected int getAlignment() {
        return 1;
    }

    public int getOffset() {
        return super.getOffset();
    }

    public void add(int index, EncodedValue value) {
        encodedArray.add(index, value);
    }

    public ItemType getItemType() {
        return ItemType.TYPE_ENCODED_ARRAY_ITEM;
    }

    public String getConciseIdentity() {
        return "encoded_array @0x" + Integer.toHexString(getOffset());
    }

    public ArrayEncodedValueSubField getEncodedArray() {
        return encodedArray;
    }
}
