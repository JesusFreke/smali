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

package org.jf.baksmali.Adaptors.EncodedValue;

import org.jf.dexlib.EncodedValue.EncodedValue;
import org.jf.dexlib.EncodedValue.AnnotationEncodedSubValue;
import org.jf.dexlib.StringIdItem;
import org.jf.baksmali.Adaptors.Reference.TypeReference;

import java.util.List;
import java.util.ArrayList;

public class AnnotationEncodedValueAdaptor extends EncodedValueAdaptor {
    private AnnotationEncodedSubValue encodedAnnotation;

    public AnnotationEncodedValueAdaptor(AnnotationEncodedSubValue encodedAnnotation) {
        this.encodedAnnotation = encodedAnnotation;
    }

    public String getFormat() {
        return "AnnotationEncodedValue";
    }

    public Object getValue() {
        return this;
    }

    public TypeReference getAnnotationType() {
        return new TypeReference(encodedAnnotation.annotationType);
    }

    public List<AnnotationElementAdaptor> getElements() {
        List<AnnotationElementAdaptor> elements = new ArrayList<AnnotationElementAdaptor>();

        for (int i=0; i<encodedAnnotation.names.length; i++) {
            elements.add(new AnnotationElementAdaptor(encodedAnnotation.names[i], encodedAnnotation.values[i]));
        }

        return elements;
    }


    public static class AnnotationElementAdaptor {
        private StringIdItem name;
        private EncodedValue value;

        public AnnotationElementAdaptor(StringIdItem name, EncodedValue value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name.getStringValue();
        }

        public EncodedValueAdaptor getValue() {
            return EncodedValueAdaptor.make(value);
        }
    }
}
