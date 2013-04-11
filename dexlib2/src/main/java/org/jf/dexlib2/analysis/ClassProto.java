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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.analysis.util.TypeProtoUtils;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

/**
 * A class "prototype". This contains things like the interfaces, the superclass, the vtable and the instance fields
 * and their offsets.
 */
public class ClassProto implements TypeProto {
    @Nonnull protected final ClassPath classPath;
    @Nonnull protected final String type;
    @Nullable protected ClassDef classDef;
    @Nullable protected Set<String> interfaces;
    protected boolean interfacesFullyResolved = true;

    public ClassProto(@Nonnull ClassPath classPath, @Nonnull String type) {
        if (type.charAt(0) != 'L') {
            throw new ExceptionWithContext("Cannot construct ClassProto for non reference type: %s", type);
        }
        this.classPath = classPath;
        this.type = type;
    }

    @Override public String toString() { return type; }
    @Nonnull @Override public ClassPath getClassPath() { return classPath; }
    @Nonnull @Override public String getType() { return type; }

    @Nonnull
    public ClassDef getClassDef() {
        if (classDef == null) {
            classDef = classPath.getClassDef(type);
        }
        return classDef;
    }

    /**
     * Returns true if this class is an interface.
     *
     * If this class is not defined, then this will throw an UnresolvedClassException
     *
     * @return True if this class is an interface
     */
    public boolean isInterface() {
        ClassDef classDef = getClassDef();
        return (classDef.getAccessFlags() & AccessFlags.INTERFACE.getValue()) != 0;
    }

    private void addInterfacesRecursively(@Nonnull ClassDef classDef) {
        assert interfaces != null;
        for (String iface: classDef.getInterfaces()) {
            interfaces.add(iface);
            addInterfacesRecursively(iface);
        }
    }

    private void addInterfacesRecursively(@Nonnull String cls) {
        ClassDef classDef;
        try {
            classDef = classPath.getClassDef(cls);
            addInterfacesRecursively(classDef);
        } catch (UnresolvedClassException ex) {
            interfacesFullyResolved = false;
        }
    }

    @Nonnull
    protected Set<String> getInterfaces() {
        if (interfaces != null) {
            return interfaces;
        }

        interfaces = Sets.newHashSet();

        try {
            ClassDef classDef = getClassDef();

            if (isInterface()) {
                interfaces.add(getType());
            }

            while (true) {
                addInterfacesRecursively(classDef);

                String superclass = classDef.getSuperclass();
                if (superclass != null) {
                    classDef = classPath.getClassDef(superclass);
                } else {
                    break;
                }
            }
        } catch (UnresolvedClassException ex) {
            interfacesFullyResolved = false;
        }

        return interfaces;
    }

    /**
     * Checks if this class implements the given interface.
     *
     * If the interfaces of this class cannot be fully resolved then this
     * method will either return true or throw an UnresolvedClassException
     *
     * @param iface The interface to check for
     * @return true if this class implements the given interface, otherwise false
     */
    @Override
    public boolean implementsInterface(@Nonnull String iface) {
        for (String implementIface: getInterfaces()) {
            if (implementIface.equals(iface)) {
                return true;
            }
        }
        if (!interfacesFullyResolved) {
            throw new UnresolvedClassException("Interfaces for class %s not fully resolved", getType());
        }
        return false;
    }

    @Nullable @Override
    public String getSuperclass() {
        return getClassDef().getSuperclass();
    }

    /**
     * This is a helper method for getCommonSuperclass
     *
     * It checks if this class is an interface, and if so, if other implements it.
     *
     * If this class is undefined, we go ahead and check if it is listed in other's interfaces. If not, we throw an
     * UndefinedClassException
     *
     * If the interfaces of other cannot be fully resolved, we check the interfaces that can be resolved. If not found,
     * we throw an UndefinedClassException
     *
     * @param other The class to check the interfaces of
     * @return true if this class is an interface (or is undefined) other implements this class
     *
     */
    private boolean checkInterface(@Nonnull ClassProto other) {
        boolean isResolved = true;
        boolean isInterface = true;
        try {
            isInterface = isInterface();
        } catch (UnresolvedClassException ex) {
            isResolved = false;
            // if we don't know if this class is an interface or not,
            // we can still try to call other.implementsInterface(this)
        }
        if (isInterface) {
            try {
                if (other.implementsInterface(getType())) {
                    return true;
                }
            } catch (UnresolvedClassException ex) {
                // There are 2 possibilities here, depending on whether we were able to resolve this class.
                // 1. If this class is resolved, then we know it is an interface class. The other class either
                //    isn't defined, or its interfaces couldn't be fully resolved.
                //    In this case, we throw an UnresolvedClassException
                // 2. If this class is not resolved, we had tried to call implementsInterface anyway. We don't
                //    know for sure if this class is an interface or not. We return false, and let processing
                //    continue in getCommonSuperclass
                if (isResolved) {
                    throw ex;
                }
            }
        }
        return false;
    }

    @Override @Nonnull
    public TypeProto getCommonSuperclass(@Nonnull TypeProto other) {
        // use the other type's more specific implementation
        if (!(other instanceof ClassProto)) {
            return other.getCommonSuperclass(this);
        }

        if (this == other || getType().equals(other.getType())) {
            return this;
        }

        if (this.getType().equals("Ljava/lang/Object;")) {
            return this;
        }

        if (other.getType().equals("Ljava/lang/Object;")) {
            return other;
        }

        boolean gotException = false;
        try {
            if (checkInterface((ClassProto)other)) {
                return this;
            }
        } catch (UnresolvedClassException ex) {
            gotException = true;
        }

        try {
            if (((ClassProto)other).checkInterface(this)) {
                return other;
            }
        } catch (UnresolvedClassException ex) {
            gotException = true;
        }
        if (gotException) {
            return classPath.getUnknownClass();
        }

        List<TypeProto> thisChain = Lists.<TypeProto>newArrayList(this);
        Iterables.addAll(thisChain, TypeProtoUtils.getSuperclassChain(this));

        List<TypeProto> otherChain = Lists.newArrayList(other);
        Iterables.addAll(otherChain, TypeProtoUtils.getSuperclassChain(other));

        // reverse them, so that the first entry is either Ljava/lang/Object; or Ujava/lang/Object;
        thisChain = Lists.reverse(thisChain);
        otherChain = Lists.reverse(otherChain);

        for (int i=Math.min(thisChain.size(), otherChain.size())-1; i>=0; i--) {
            TypeProto typeProto = thisChain.get(i);
            if (typeProto.getType().equals(otherChain.get(i).getType())) {
                return typeProto;
            }
        }

        return classPath.getUnknownClass();
    }

    @Override
    @Nullable
    public FieldReference getFieldByOffset(int fieldOffset) {
        // TODO: implement this
        return null;
    }

    @Override
    @Nullable
    public MethodReference getMethodByVtableIndex(int vtableIndex) {
        // TODO: implement this
        return null;
    }
}
