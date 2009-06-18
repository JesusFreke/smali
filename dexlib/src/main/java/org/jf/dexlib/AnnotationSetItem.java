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

import java.util.ArrayList;
import java.util.List;

public class AnnotationSetItem extends OffsettedItem<AnnotationSetItem> {
    private final ArrayList<OffsettedItemReference<AnnotationItem>> annotationReferences =
            new ArrayList<OffsettedItemReference<AnnotationItem>>();

    private final ListSizeField annotationCountField;
    private final FieldListField<OffsettedItemReference<AnnotationItem>> annotationsListField;

    public AnnotationSetItem(final DexFile dexFile, int offset) {
        super(offset);

        fields = new Field[] {
                annotationCountField = new ListSizeField(annotationReferences, new IntegerField("size")),
                annotationsListField = new FieldListField<OffsettedItemReference<AnnotationItem>>(
                        annotationReferences, "annotation") {
                    protected OffsettedItemReference<AnnotationItem> make() {
                        return new OffsettedItemReference<AnnotationItem>(dexFile.AnnotationsSection,
                                new IntegerField(null), "annotation_off");
                    }
                }
        };
    }

    public AnnotationSetItem(final DexFile dexFile, List<AnnotationItem> annotations) {
        this(dexFile, -1);

        for (AnnotationItem annotationItem: annotations) {
            OffsettedItemReference<AnnotationItem> annotationReference = annotationsListField.make();
            annotationReference.setReference(annotationItem);
            this.annotationReferences.add(annotationReference);
        }
    }

    protected int getAlignment() {
        return 4;
    }

    public ItemType getItemType() {
        return ItemType.TYPE_ANNOTATION_SET_ITEM;
    }

    public String getConciseIdentity() {
        return "annotation_set_item @0x" + Integer.toHexString(getOffset());
    }
}
