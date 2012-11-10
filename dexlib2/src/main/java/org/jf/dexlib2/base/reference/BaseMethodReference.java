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

package org.jf.dexlib2.base.reference;

import org.jf.dexlib2.iface.reference.BasicMethodParameter;
import org.jf.dexlib2.iface.reference.MethodReference;

import javax.annotation.Nonnull;
import java.util.List;

public abstract class BaseMethodReference implements MethodReference {
    @Nonnull public abstract String getContainingClass();
    @Nonnull public abstract String getName();
    @Nonnull public abstract List<? extends BasicMethodParameter> getParameters();
    @Nonnull
    public abstract String getReturnType();

    @Override
    public int hashCode() {
        return hashCode(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o != null && o instanceof MethodReference) {
            return equals(this, (MethodReference)o);
        }
        return false;
    }

    public static int hashCode(@Nonnull MethodReference methodRef) {
        int hashCode = methodRef.getContainingClass().hashCode();
        hashCode = hashCode*31 + methodRef.getName().hashCode();
        hashCode = hashCode*31 + methodRef.getReturnType().hashCode();
        for (BasicMethodParameter param: methodRef.getParameters()) {
            hashCode = hashCode*31 + param.hashCode();
        }
        return hashCode;
    }

    public static boolean equals(@Nonnull MethodReference methodRef1, @Nonnull MethodReference methodRef2) {
        return methodRef1.getContainingClass().equals(methodRef2.getContainingClass()) &&
               methodRef1.getName().equals(methodRef2.getName()) &&
               methodRef1.getReturnType().equals(methodRef2.getReturnType()) &&
               methodRef1.getParameters().equals(methodRef2.getParameters());
    }
}
