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

package org.jf.dexlib2.analysis;

import com.google.common.collect.Lists;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * A class "prototype". This contains things like the interfaces, the superclass, the vtable and the instance fields
 * and their offsets.
 */
public class ClassProto {
    @Nonnull public final ClassPath classPath;
    @Nonnull public final String type;
    @Nullable private ClassDef classDef;

    public ClassProto(@Nonnull ClassPath classPath, @Nonnull String type) {
        this.classPath = classPath;
        this.type = type;
    }

    @Nonnull
    public ClassDef getClassDef() {
        if (classDef == null) {
            classDef = classPath.getClassDef(type);
        }
        return classDef;
    }

    @Nonnull
    public String getType() {
        return type;
    }

    public boolean isInterface() {
        // TODO: implement
        return false;
    }

    public boolean implementsInterface(String iface) {
        // TODO: implement
        return false;
    }

    /**
     * Get the chain of superclasses of this class. The first element will be the immediate superclass followed by
     * it's superclass, etc. up to java.lang.Object.
     *
     * Returns an empty iterable if called on java.lang.Object.
     *
     * @return An iterable containing the superclasses of this class.
     * @throws UnresolvedClassException if any class in the chain can't be resolved
     */
    @Nonnull
    public Iterable<String> getSuperclassChain() {
        final ClassDef topClassDef = this.getClassDef();

        return new Iterable<String>() {
            private ClassDef classDef = topClassDef;

            @Override public Iterator<String> iterator() {
                return new Iterator<String>() {
                    @Override public boolean hasNext() {
                        return classDef == null || classDef.getSuperclass() == null;
                    }

                    @Override public String next() {
                        if (classDef == null) {
                            throw new NoSuchElementException();
                        }

                        String next = classDef.getSuperclass();
                        if (next == null) {
                            throw new NoSuchElementException();
                        }

                        classDef = classPath.getClassDef(next);
                        return next;
                    }

                    @Override public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    @Nonnull public ClassProto getCommonSuperclass(@Nonnull ClassProto other) {
        if (this == other || getType().equals(other.getType())) {
            return this;
        }

        if (isInterface()) {
            if (other.implementsInterface(getType())) {
                return this;
            }
            return classPath.getClass("Ljava/lang/Object;");
        }

        if (other.isInterface()) {
            if (implementsInterface(other.getType())) {
                return other;
            }
            return classPath.getClass("Ljava/lang/Object;");
        }

        boolean thisResolved = true;
        boolean otherResolved = true;
        List<String> thisChain = Lists.newArrayList(getType());
        List<String> otherChain = Lists.newArrayList(other.getType());

        // grab as much of the superclass chain as we can for both types,
        // and keep track of whether we were able to get all of it
        try {
            for (String type: getSuperclassChain()) {
                thisChain.add(type);
            }
        } catch (UnresolvedClassException ex) {
            thisResolved = false;
        }

        try {
            for (String type: other.getSuperclassChain()) {
                otherChain.add(type);
            }
        } catch (UnresolvedClassException ex) {
            otherResolved = false;
        }

        // if both were resolved, then we start looking backwards from the end of the shorter chain, until
        // we find a pair of entries in the chains that match
        if (thisResolved && otherResolved) {
            for (int i=Math.min(thisChain.size(), otherChain.size()); i>=0; i--) {
                String type = thisChain.get(i);
                if (type.equals(otherChain.get(i))) {
                    return classPath.getClass(type);
                }
            }
            // "This should never happen"
            throw new ExceptionWithContext("Wasn't able to find a common superclass for %s and %s", this.getType(),
                    other.getType());
        }

        // we weren't able to fully resolve both classes. Let's see if we can find a common superclass in what we
        // were able to resolve
        for (String thisType: thisChain) {
            for (String otherType: otherChain) {
                if (thisType.equals(otherType)) {
                    return classPath.getClass(thisType);
                }
            }
        }

        // Nope. We'll throw an UnresolvedClassException. The caller can catch the exception and use java.lang.Object
        // as the superclass, if it is appropriate to do so
        if (!thisResolved) {
            if (!otherResolved) {
                throw new UnresolvedClassException(
                        "Could not fully resolve %s or %s while getting their common superclass",
                        getType(), other.getType());
            } else {
                throw new UnresolvedClassException(
                        "Could not fully resolve %s while getting common superclass with %s",
                        getType(), other.getType());
            }
        }
        throw new UnresolvedClassException(
                "Could not fully resolve %s while getting common superclass with %s", other.getType(), getType());
    }
}
