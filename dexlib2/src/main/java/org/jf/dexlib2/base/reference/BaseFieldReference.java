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

import org.jf.dexlib2.iface.reference.FieldReference;

import javax.annotation.Nonnull;

public abstract class BaseFieldReference implements FieldReference {
    @Nonnull public abstract String getContainingClass();
    @Nonnull public abstract String getName();
    @Nonnull public abstract String getType();

    @Override
    public int hashCode() {
        return hashCode(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o != null && o instanceof FieldReference) {
            return equals(this, (FieldReference)o);
        }
        return false;
    }

    public static int hashCode(@Nonnull FieldReference fieldRef) {
        int hashCode = fieldRef.getContainingClass().hashCode();
        hashCode = hashCode*31 + fieldRef.getName().hashCode();
        return hashCode*31 + fieldRef.getType().hashCode();
    }

    public static boolean equals(@Nonnull FieldReference fieldRef1, @Nonnull FieldReference fieldRef2) {
        return fieldRef1.getContainingClass().equals(fieldRef2.getContainingClass()) &&
               fieldRef1.getName().equals(fieldRef2.getName()) &&
               fieldRef1.getType().equals(fieldRef2.getType());
    }
}
