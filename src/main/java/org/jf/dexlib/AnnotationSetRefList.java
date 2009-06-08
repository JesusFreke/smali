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

import org.jf.dexlib.ItemType;

import java.util.ArrayList;
import java.util.List;

public class AnnotationSetRefList extends OffsettedItem<AnnotationSetRefList> {
    private final ArrayList<OffsettedItemReference<AnnotationSetItem>> annotationSetReferences =
            new ArrayList<OffsettedItemReference<AnnotationSetItem>>();

    private final ListSizeField annotationSetCountField;
    private final FieldListField<OffsettedItemReference<AnnotationSetItem>> annotationSetsListField;

    public AnnotationSetRefList(final DexFile dexFile, int offset) {
        super(offset);

        fields = new Field[] {
                annotationSetCountField = new ListSizeField(annotationSetReferences, new IntegerField("size")),
                annotationSetsListField = new FieldListField<OffsettedItemReference<AnnotationSetItem>>(
                        annotationSetReferences, "list") {
                    protected OffsettedItemReference<AnnotationSetItem> make() {
                        return new OffsettedItemReference<AnnotationSetItem>(dexFile.AnnotationSetsSection,
                                new IntegerField(null), "annotation_set_ref_item");
                    }
                }
        };
    }

    public AnnotationSetRefList(final DexFile dexFile, List<AnnotationSetItem> annotationSets) {
        this(dexFile, -1);

        for (AnnotationSetItem annotationSet: annotationSets) {
            OffsettedItemReference<AnnotationSetItem> annotationSetReference = annotationSetsListField.make();
            annotationSetReference.setReference(annotationSet);
            this.annotationSetReferences.add(annotationSetReference);
        }
    }

    protected int getAlignment() {
        return 4;
    }

    public ItemType getItemType() {
        return ItemType.TYPE_ANNOTATION_SET_REF_LIST;
    }

    public String getConciseIdentity() {
        return "annotation_set_item @0x" + Integer.toHexString(getOffset());
    }
}
