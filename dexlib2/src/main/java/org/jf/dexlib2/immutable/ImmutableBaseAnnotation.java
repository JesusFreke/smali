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

package org.jf.dexlib2.immutable;

import com.google.common.collect.ImmutableList;
import org.jf.dexlib2.iface.AnnotationElement;
import org.jf.dexlib2.iface.BaseAnnotation;
import org.jf.util.ImmutableListUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ImmutableBaseAnnotation implements BaseAnnotation {
    @Nonnull public final String type;
    @Nonnull public final ImmutableList<? extends ImmutableAnnotationElement> elements;

    public ImmutableBaseAnnotation(@Nonnull String type,
                                   @Nullable List<? extends AnnotationElement> elements) {
        this.type = type;
        this.elements = ImmutableAnnotationElement.immutableListOf(elements);
    }

    public ImmutableBaseAnnotation(@Nonnull String type,
                                   @Nullable ImmutableList<? extends ImmutableAnnotationElement> elements) {
        this.type = type;
        this.elements = ImmutableListUtils.nullToEmptyList(elements);
    }

    public static ImmutableBaseAnnotation of(BaseAnnotation baseAnnotation) {
        if (baseAnnotation instanceof ImmutableBaseAnnotation) {
            return (ImmutableBaseAnnotation)baseAnnotation;
        }
        return new ImmutableBaseAnnotation(
                baseAnnotation.getType(),
                baseAnnotation.getElements());
    }

    @Nonnull @Override public String getType() { return type; }
    @Nonnull @Override public ImmutableList<? extends ImmutableAnnotationElement> getElements() { return elements; }
}
