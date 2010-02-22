/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver (JesusFreke)
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

package org.jf.baksmali.Adaptors.EncodedValue;

import org.jf.dexlib.EncodedValue.EncodedValue;
import org.jf.dexlib.EncodedValue.AnnotationEncodedSubValue;
import org.jf.dexlib.StringIdItem;
import org.jf.baksmali.Adaptors.Reference.TypeReference;
import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;

import java.util.List;
import java.util.ArrayList;

public abstract class AnnotationEncodedValueAdaptor {

    public static StringTemplate createTemplate(StringTemplateGroup stg, AnnotationEncodedSubValue encodedAnnotation) {
        StringTemplate template = stg.getInstanceOf("AnnotationEncodedValue");
        template.setAttribute("AnnotationType", TypeReference.createTemplate(stg, encodedAnnotation.annotationType));
        template.setAttribute("Elements", getElements(stg, encodedAnnotation));
        return template;
    }

    public static void setAttributesForAnnotation(StringTemplate template,
                                                  AnnotationEncodedSubValue encodedAnnotation) {
        template.setAttribute("AnnotationType", TypeReference.createTemplate(template.getGroup(),
                encodedAnnotation.annotationType));
        template.setAttribute("Elements", getElements(template.getGroup(), encodedAnnotation));
    }

    private static List<String> getElements(StringTemplateGroup stg,
                                                              AnnotationEncodedSubValue encodedAnnotation) {
        List<String> elements = new ArrayList<String>();

        for (int i=0; i<encodedAnnotation.names.length; i++) {
            elements.add(AnnotationElementAdaptor.toString(stg, encodedAnnotation.names[i], encodedAnnotation.values[i]));
        }

        return elements;
    }


    private static class AnnotationElementAdaptor {
        public static String toString(StringTemplateGroup stg, StringIdItem name, EncodedValue value) {
            StringTemplate template = stg.getInstanceOf("AnnotationElement");
            template.setAttribute("Name", name);
            template.setAttribute("Value", EncodedValueAdaptor.create(stg, value));
            return template.toString();
        }
    }
}
