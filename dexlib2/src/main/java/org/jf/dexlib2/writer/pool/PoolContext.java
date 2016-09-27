/*
 * Copyright 2013, Google Inc.
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

package org.jf.dexlib2.writer.pool;

import javax.annotation.Nonnull;

class PoolContext {
    @Nonnull final StringPool stringPool;
    @Nonnull final TypePool typePool;
    @Nonnull final FieldPool fieldPool;
    @Nonnull final TypeListPool typeListPool;
    @Nonnull final ProtoPool protoPool;
    @Nonnull final MethodPool methodPool;
    @Nonnull final AnnotationPool annotationPool;
    @Nonnull final AnnotationSetPool annotationSetPool;
    @Nonnull final ClassPool classPool;

    PoolContext() {
        stringPool = new StringPool();
        typePool = new TypePool(stringPool);
        fieldPool = new FieldPool(stringPool, typePool);
        typeListPool = new TypeListPool(typePool);
        protoPool = new ProtoPool(stringPool, typePool, typeListPool);
        methodPool = new MethodPool(stringPool, typePool, protoPool);
        annotationPool = new AnnotationPool(stringPool, typePool, fieldPool, methodPool);
        annotationSetPool = new AnnotationSetPool(annotationPool);
        classPool = new ClassPool(stringPool, typePool, fieldPool, methodPool, annotationSetPool, typeListPool);
    }
}
