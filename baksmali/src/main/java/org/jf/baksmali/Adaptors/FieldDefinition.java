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
import org.jf.dexlib.EncodedValue.NullEncodedValue;
import org.jf.dexlib.AnnotationSetItem;
import org.jf.dexlib.AnnotationItem;
import org.jf.dexlib.Util.AccessFlags;
import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;

import java.util.ArrayList;
import java.util.List;

public class FieldDefinition {
    public static StringTemplate createTemplate(StringTemplateGroup stg, ClassDataItem.EncodedField encodedField,
                                                EncodedValue initialValue, AnnotationSetItem annotationSet,
                                                boolean setInStaticConstructor) {
        StringTemplate template = stg.getInstanceOf("field");

        String fieldTypeDescriptor = encodedField.field.getFieldType().getTypeDescriptor();

        template.setAttribute("AccessFlags", getAccessFlags(encodedField));
        template.setAttribute("FieldName", encodedField.field.getFieldName().getStringValue());
        template.setAttribute("FieldType", encodedField.field.getFieldType().getTypeDescriptor());
        template.setAttribute("Annotations", getAnnotations(stg, annotationSet));

        if (setInStaticConstructor &&
            encodedField.isStatic() &&
            (encodedField.accessFlags & AccessFlags.FINAL.getValue()) != 0 &&
            initialValue != null &&
            (
                //it's a primitive type, or it's an array/reference type and the initial value isn't null
                fieldTypeDescriptor.length() == 1 ||
                initialValue != NullEncodedValue.NullValue
            )) {

            template.setAttribute("Comments",
                    new String[]{"the value of this static final field might be set in the static constructor"});
        } else {
            template.setAttribute("Comments", null);
        }

        if (initialValue != null) {
            template.setAttribute("InitialValue", EncodedValueAdaptor.make(stg, initialValue));
        }

        return template;
    }

    public static StringTemplate createTemplate(StringTemplateGroup stg, ClassDataItem.EncodedField encodedField,
                                                AnnotationSetItem annotationSet) {
        return createTemplate(stg, encodedField, null, annotationSet, false);
    }

    private static List<String> getAccessFlags(ClassDataItem.EncodedField encodedField) {
        List<String> accessFlags = new ArrayList<String>();

        for (AccessFlags accessFlag: AccessFlags.getAccessFlagsForField(encodedField.accessFlags)) {
            accessFlags.add(accessFlag.toString());
        }

        return accessFlags;
    }

    private static List<StringTemplate> getAnnotations(StringTemplateGroup stg, AnnotationSetItem annotationSet) {
        if (annotationSet == null) {
            return null;
        }

        List<StringTemplate> annotationAdaptors = new ArrayList<StringTemplate>();

        for (AnnotationItem annotationItem: annotationSet.getAnnotations()) {
            annotationAdaptors.add(AnnotationAdaptor.makeTemplate(stg, annotationItem));
        }
        return annotationAdaptors;
    }
}
