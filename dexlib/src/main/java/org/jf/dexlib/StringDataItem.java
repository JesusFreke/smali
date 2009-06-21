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

import org.jf.dexlib.Util.ByteArray;
import org.jf.dexlib.Util.Utf8Utils;

public class StringDataItem extends OffsettedItem<StringDataItem> implements Comparable<StringDataItem> {
    private String value = null;

    private final Leb128Field stringSize;
    private final NullTerminatedByteArrayField stringByteArray;

    public StringDataItem(int offset) {
        super(offset);

        fields = new Field[] {
                stringSize = new Leb128Field("string_length"),
                stringByteArray = new NullTerminatedByteArrayField("data")
        };
    }

    public StringDataItem(String value) {
        super(-1);

        this.value = value; 

        fields = new Field[] {
                stringSize = new Leb128Field(value.length(), "string_length"),
                stringByteArray = new NullTerminatedByteArrayField(Utf8Utils.stringToUtf8Bytes(value), "data")
        };
    }

    public ItemType getItemType() {
        return ItemType.TYPE_STRING_DATA_ITEM;
    }

    public String getStringValue() {
        if (value == null) {
            value = Utf8Utils.utf8BytesToString(new ByteArray(((NullTerminatedByteArrayField)fields[1]).value));
        }

        return value;
    }

    public String getConciseIdentity() {
        return "string_data_item: " + getStringValue();
    }

    public int compareTo(StringDataItem o) {
        return getStringValue().compareTo(o.getStringValue());
    }
}
