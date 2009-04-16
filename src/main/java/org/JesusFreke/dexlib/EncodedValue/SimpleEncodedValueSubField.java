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

package org.JesusFreke.dexlib.EncodedValue;

import org.JesusFreke.dexlib.DexFile;

public abstract class SimpleEncodedValueSubField<V, T extends SimpleEncodedValueSubField<V,T>> implements EncodedValueSubField<T>
{
    protected V value;
    protected byte valueArg = 0;

    public V getValue() {
        return value;
    }

    public void setInitialValueArg(byte valueArg) {
        this.valueArg = valueArg;
    }

    public void copyTo(DexFile dexFile, T copy) {
        copy.value = value;
    }

    public int hashCode() {
        if (value == null) {
            return 0;
        }
        return value.hashCode();
    }

    public boolean equals(Object o) {
        if (!(o instanceof SimpleEncodedValueSubField)) {
            return false;
        }

        SimpleEncodedValueSubField other = (SimpleEncodedValueSubField)o;
        if (value == null) {
            return other.value == null;
        }
        return value.equals(other.value);
    }
}
