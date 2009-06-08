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

package org.jf.dexlib.util;

import org.jf.dexlib.EncodedValue.EncodedValueSubFieldFactory;
import org.jf.dexlib.EncodedValue.ValueType;
import org.jf.dexlib.EncodedValue.EncodedValueSubField;
import org.jf.dexlib.DexFile;

public class TypeUtils
{
    public static EncodedValueSubField makeDefaultValueForType(DexFile dexFile, String type) {
        switch (type.charAt(0)) {
            case 'Z':
                return EncodedValueSubFieldFactory.makeEncodedValueField(dexFile, ValueType.VALUE_BOOLEAN);
            case 'B':
                return EncodedValueSubFieldFactory.makeEncodedValueField(dexFile, ValueType.VALUE_BYTE);
            case 'S':
                return EncodedValueSubFieldFactory.makeEncodedValueField(dexFile, ValueType.VALUE_SHORT);
            case 'C':
                return EncodedValueSubFieldFactory.makeEncodedValueField(dexFile, ValueType.VALUE_CHAR);
            case 'I':
                return EncodedValueSubFieldFactory.makeEncodedValueField(dexFile, ValueType.VALUE_INT);
            case 'J':
                return EncodedValueSubFieldFactory.makeEncodedValueField(dexFile, ValueType.VALUE_LONG);
            case 'F':
                return EncodedValueSubFieldFactory.makeEncodedValueField(dexFile, ValueType.VALUE_FLOAT);
            case 'D':
                return EncodedValueSubFieldFactory.makeEncodedValueField(dexFile, ValueType.VALUE_DOUBLE);
            case 'L':
            case '[':
                return EncodedValueSubFieldFactory.makeEncodedValueField(dexFile, ValueType.VALUE_NULL);
        }
        return null;
    }
}
