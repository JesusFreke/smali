/*
 * Copyright 2012, Google Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.dexlib2.base;

import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.AnnotationElement;

import javax.annotation.Nonnull;

public abstract class BaseAnnotation implements Annotation {
    @Override
    public int hashCode() {
        return hashCode(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o != null && o instanceof Annotation) {
            return equals(this, (Annotation)o);
        }
        return false;
    }

    public static int hashCode(@Nonnull Annotation annotation) {
        int hashCode = annotation.getVisibility();
        hashCode = hashCode*31 + annotation.getType().hashCode();
        for (AnnotationElement element: annotation.getElements()) {
            hashCode = hashCode*31 + element.hashCode();
        }
        return hashCode;
    }

    public static boolean equals(@Nonnull Annotation annotation1, @Nonnull Annotation annotation2) {
        return (annotation1.getVisibility() == annotation2.getVisibility()) &&
                annotation1.getType().equals(annotation2.getType()) &&
                annotation1.getElements().equals(annotation2.getElements());
    }
}
