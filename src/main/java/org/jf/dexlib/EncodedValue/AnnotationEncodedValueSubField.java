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

package org.jf.dexlib.EncodedValue;

import org.jf.dexlib.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class AnnotationEncodedValueSubField extends CompositeField<AnnotationEncodedValueSubField>
    implements EncodedValueSubField<AnnotationEncodedValueSubField> {

    private final ArrayList<AnnotationElement> annotationElementList = new ArrayList<AnnotationElement>();

    private final IndexedItemReference<TypeIdItem> annotationType;
    private final ListSizeField annotationCount;
    private final FieldListField<AnnotationElement> annotationElements;

    public AnnotationEncodedValueSubField(final DexFile dexFile) {
        super("encoded_annotation");
        fields = new Field[] {
                annotationType = new IndexedItemReference<TypeIdItem>(dexFile.TypeIdsSection,
                        new Leb128Field(null), "type_idx"),
                annotationCount = new ListSizeField(annotationElementList, new Leb128Field("size")),
                annotationElements = new FieldListField<AnnotationElement>(annotationElementList, "elements") {
                    protected AnnotationElement make() {
                        return new AnnotationElement(dexFile);
                    }
                }
        };
    }

    public AnnotationEncodedValueSubField(final DexFile dexFile, TypeIdItem annotationType,
                                          List<AnnotationElement> annotationElements) {
        this(dexFile);
        this.annotationType.setReference(annotationType);
        this.annotationElementList.addAll(annotationElements);
    }

    public void setInitialValueArg(byte valueArg) {
        //valueArg is ignored for annotations
    }

    public byte getValueArg() {
        return 0;
    }

    public int place(int offset) {
        Collections.sort(annotationElementList);
        return super.place(offset);
    }

    public ValueType getValueType() {
        return ValueType.VALUE_ANNOTATION;
    }
}
