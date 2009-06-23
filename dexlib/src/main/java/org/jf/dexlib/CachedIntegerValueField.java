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

import org.jf.dexlib.Util.AnnotatedOutput;
import org.jf.dexlib.Util.Output;

public abstract class CachedIntegerValueField<T extends CachedIntegerValueField>
        implements Field<T> {
    
    private final String fieldName;
    protected int value;

    protected CachedIntegerValueField(String fieldName) {
        this.fieldName = fieldName;
    }

    protected CachedIntegerValueField(int value, String fieldName) {
        this(fieldName);
        this.value = value;
    }

    public void copyTo(DexFile dexFile, T copy) {
        copy.value = value;
    }

    public int hashCode() {
        return value;
    }

    protected abstract void writeValue(Output out);

    public void writeTo(AnnotatedOutput out) {
        if (fieldName != null) {
            out.annotate(fieldName + ": 0x" + Integer.toHexString(getCachedValue()));
        }
        writeValue(out);
    }

    public boolean equals(Object o) {
        return (this.getClass() == o.getClass()) &&
               (getCachedValue() == ((CachedIntegerValueField)o).getCachedValue());
    }

    /**
     * This method returns the integer value that has been cached. This
     * value is either the value that the field was constructed with, the
     * value that was read via <code>readFrom</code>, or the value that was
     * cached when <code>place</code> was called
     * @return the cached value
     */
    public int getCachedValue() {
        return value;
    }

    public void cacheValue(int value) {
        this.value = value;
    }
}
