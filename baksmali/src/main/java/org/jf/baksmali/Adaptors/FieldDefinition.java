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
 * INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.baksmali.Adaptors;

import org.jf.baksmali.Adaptors.EncodedValue.EncodedValueAdaptor;
import org.jf.dexlib.ClassDataItem;
import org.jf.dexlib.EncodedValue.EncodedValue;
import org.jf.dexlib.FieldIdItem;
import org.jf.dexlib.AnnotationSetItem;
import org.jf.dexlib.AnnotationItem;
import org.jf.dexlib.Util.AccessFlags;

import java.util.ArrayList;
import java.util.List;

public class FieldDefinition {
    private ClassDataItem.EncodedField encodedField;
    private FieldIdItem fieldIdItem;
    private EncodedValue initialValue;
    private AnnotationSetItem annotationSet;

    public FieldDefinition(ClassDataItem.EncodedField encodedField, AnnotationSetItem annotationSet) {
        this(encodedField, null, annotationSet);
    }

    public FieldDefinition(ClassDataItem.EncodedField encodedField, EncodedValue initialValue,
                           AnnotationSetItem annotationSet) {
        this.encodedField = encodedField;
        this.fieldIdItem = encodedField.getField();
        this.initialValue = initialValue;
        this.annotationSet = annotationSet;
    }

    private List<String> accessFlags = null;
    public List<String> getAccessFlags() {
        if (accessFlags == null) {
            accessFlags = new ArrayList<String>();

            for (AccessFlags accessFlag: AccessFlags.getAccessFlagsForField(encodedField.getAccessFlags())) {
                accessFlags.add(accessFlag.toString());
            }
        }
        return accessFlags;
    }

    private String fieldName = null;
    public String getFieldName() {
        if (fieldName == null) {
            fieldName = fieldIdItem.getFieldName().getStringValue();
        }
        return fieldName;
    }

    private String fieldType = null;
    public String getFieldType() {
        if (fieldType == null) {
            fieldType = fieldIdItem.getFieldType().getTypeDescriptor();
        }
        return fieldType;
    }

    private EncodedValueAdaptor encodedValueAdaptor = null;
    public EncodedValueAdaptor getInitialValue() {
        if (encodedValueAdaptor == null && initialValue != null) {
            encodedValueAdaptor = EncodedValueAdaptor.make(initialValue);
        }
        return encodedValueAdaptor;
    }

    public List<AnnotationAdaptor> getAnnotations() {
        if (annotationSet == null) {
            return null;
        }

        List<AnnotationAdaptor> annotationAdaptors = new ArrayList<AnnotationAdaptor>();

        for (AnnotationItem annotationItem: annotationSet.getAnnotationItems()) {
            annotationAdaptors.add(new AnnotationAdaptor(annotationItem));
        }
        return annotationAdaptors;
    }
}
