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

package org.jf.dexlib2.iface;

import org.jf.dexlib2.iface.reference.TypeReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * This class represents a class definition.
 *
 * It also acts as a TypeReference to itself. Any equality/comparison is based on its identity as a TypeReference,
 * and shouldn't take into account anything other than the type of this class.
 */
public interface ClassDef extends TypeReference {
    /**
     * Gets the class type.
     *
     * This will be a type descriptor per the dex file specification.
     *
     * @return The class type
     */
    @Override @Nonnull String getType();

    /**
     * Gets the access flags for this class.
     *
     * This will be a combination of the AccessFlags.* flags that are marked as compatible for use with a class.
     *
     * @return The access flags for this class
     */
    int getAccessFlags();

    /**
     * Gets the superclass of this class.
     *
     * This will only be null if this is the base java.lang.Object class.
     *
     * @return The superclass of this class
     */
    @Nullable String getSuperclass();

    /**
     * Gets a set of the interfaces that this class implements.
     *
     * @return A set of the interfaces that this class implements
     */
    @Nonnull Set<String> getInterfaces();

    /**
     * Gets the name of the primary source file that this class is defined in, if available.
     *
     * This will be the default source file associated with all methods defined in this class. This can be overridden
     * for sections of an individual method with the SetSourceFile debug item.
     *
     * @return The name of the primary source file for this class, or null if not available
     */
    @Nullable String getSourceFile();

    /**
     * Gets a set of the annotations that are applied to this class.
     *
     * The annotations in the returned set are guaranteed to have unique types.
     *
     * @return A set of the annotations that are applied to this class
     */
    @Nonnull Set<? extends Annotation> getAnnotations();

    /**
     * Gets a set of the fields that are defined by this class.
     *
     * TODO: uniqueness?
     *
     * @return A set of the fields that are defined by this class
     */
    @Nonnull Set<? extends Field> getFields();

    /**
     * Gets a set of the methods that are defined by this class.
     *
     * TODO: uniqueness?
     *
     * @return A set of the methods that are defined by this class.
     */
    @Nonnull Set<? extends Method> getMethods();
}
