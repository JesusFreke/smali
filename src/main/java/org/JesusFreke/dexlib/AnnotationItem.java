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

import org.JesusFreke.dexlib.EncodedValue.AnnotationEncodedValueSubField;
import org.JesusFreke.dexlib.ItemType;

public class AnnotationItem extends OffsettedItem<AnnotationItem> {
    private final Field[] fields;

    private final ByteField visibility;
    private final AnnotationEncodedValueSubField annotation;

    public AnnotationItem(DexFile dexFile, int offset) {
        super(offset);

        fields = new Field[] {
                visibility = new ByteField(),
                annotation = new AnnotationEncodedValueSubField(dexFile)
        };
    }

    protected int getAlignment() {
        return 1;
    }

    protected Field[] getFields() {
        return fields;
    }

    public ItemType getItemType() {
        return ItemType.TYPE_ANNOTATION_ITEM;
    }

    public void copyTo(DexFile dexFile, AnnotationItem copy) {
        visibility.copyTo(dexFile, copy.visibility);
        annotation.copyTo(dexFile, copy.annotation);
    }

    public int hashCode() {
        return visibility.hashCode() * 31 + annotation.hashCode();
    }

    public boolean equals(Object o) {
        if (!(o instanceof AnnotationItem)) {
            return false;
        }

        AnnotationItem other = (AnnotationItem)o;

        if (!visibility.equals(other.visibility))
            return false;
        return annotation.equals(other.annotation);
    }
}
