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

import org.jf.dexlib.EncodedValue.AnnotationEncodedValueSubField;

public class AnnotationItem extends OffsettedItem<AnnotationItem> {
    private final ByteField visibilityField;
    private final AnnotationEncodedValueSubField annotationField;

    public AnnotationItem(DexFile dexFile, int offset) {
        super(offset);

        fields = new Field[] {
                visibilityField = new ByteField("visibility"),
                annotationField = new AnnotationEncodedValueSubField(dexFile)
        };
    }

    public AnnotationItem(DexFile dexFile, AnnotationVisibility visibility,
                          AnnotationEncodedValueSubField annotation) {
        super(-1);

        fields = new Field[] {
                this.visibilityField = new ByteField(visibility.value, "visibility"),
                this.annotationField = annotation
        };
    }

    public ItemType getItemType() {
        return ItemType.TYPE_ANNOTATION_ITEM;
    }

    public String getConciseIdentity() {
        return "annotation_item @0x" + Integer.toHexString(getOffset());
    }

    public Visibility getVisibility() {
        return Visibility.get((byte)visibilityField.getCachedValue());
    }

    public AnnotationEncodedValueSubField getEncodedAnnotation() {
        return annotationField;
    }

    public int compareTo(AnnotationItem annotationItem) {
        int comp = ((Integer)visibilityField.getCachedValue()).compareTo(annotationItem.visibilityField.getCachedValue());
        if (comp == 0) {
            comp = annotationField.compareTo(annotationItem.annotationField);
        }
        return comp;
    }

    public enum Visibility {
        build(0x00),
        runtime(0x01),
        system(0x02);

        public final byte value;

        private Visibility(int value) {
            this.value = (byte)value;
        }

        public static Visibility get(byte value) {
            switch (value) {
                case 0x00:
                    return build;
                case 0x01:
                    return runtime;
                case 0x02:
                    return system;
            }
            return null;
        }
    }
}
