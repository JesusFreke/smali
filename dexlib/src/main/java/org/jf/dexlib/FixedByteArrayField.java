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

import org.jf.dexlib.util.Input;
import org.jf.dexlib.util.ByteArray;
import org.jf.dexlib.util.AnnotatedOutput;

public class FixedByteArrayField implements Field<FixedByteArrayField> {
    protected byte[] value;
    private final String fieldName; 

    public FixedByteArrayField(int size, String fieldName) {
        value = new byte[size];
        this.fieldName = fieldName;
    }

    public FixedByteArrayField(byte[] bytes, String fieldName) {
        this.value = bytes.clone();
        this.fieldName = fieldName;
    }

    public FixedByteArrayField(ByteArray byteArray, String fieldName) {
        value = new byte[byteArray.size()];
        byteArray.getBytes(value, 0);
        this.fieldName = fieldName;
    }

    public void writeTo(AnnotatedOutput out) {
        if (fieldName != null) {
            out.annotate(fieldName);
        }
        out.write(value);
    }

    public void readFrom(Input in) {
        in.read(value);
    }

    public int place(int offset) {
        return offset + value.length;
    }

    public void copyTo(DexFile dexFile, FixedByteArrayField copy) {
        copy.value = value.clone();
    }
}
