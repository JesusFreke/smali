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

package org.JesusFreke.dexlib;

import org.JesusFreke.dexlib.util.Input;
import org.JesusFreke.dexlib.util.AnnotatedOutput;

public class OffsettedItemReference<T extends OffsettedItem<T>> extends
        ItemReference<T,OffsettedItemReference<T>> {
    
    public OffsettedItemReference(DexFile dexFile, T item, CachedIntegerValueField underlyingField,
                                  String fieldName) {
        super(dexFile, item, underlyingField, fieldName);
    }
    
    public OffsettedItemReference(OffsettedSection<T> section, CachedIntegerValueField underlyingField,
                                  String fieldName) {
        super(section, underlyingField, fieldName);
    }


    public OffsettedSection<T> getSection() {
        return (OffsettedSection<T>)super.getSection();
    }

    protected int getReferenceValue() {
        T item = getReference();

        if (item == null) {
            return 0;
        } else {
            return item.getOffset();
        }
    }

    protected T getReferencedItem(int referenceValue) {
        if (referenceValue == 0) {
            return null;
        }
        return getSection().getByOffset(referenceValue);
    }
}
