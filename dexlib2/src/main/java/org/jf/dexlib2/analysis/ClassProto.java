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
import com.google.common.collect.Maps;
import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.analysis.util.TypeProtoUtils;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.util.FieldUtil;
import org.jf.util.ExceptionWithContext;
import org.jf.util.SparseArray;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * A class "prototype". This contains things like the interfaces, the superclass, the vtable and the instance fields
 * and their offsets.
 */
public class ClassProto implements TypeProto {
    @Nonnull protected final ClassPath classPath;
    @Nonnull protected final String type;
    @Nullable protected ClassDef classDef;
    @Nullable protected LinkedHashMap<String, ClassDef> interfaces;
    @Nullable protected Method[] vtable;
    @Nullable protected SparseArray<FieldReference> instanceFields;
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

    @Nonnull
    Method[] getVtable() {
        if (vtable == null) {
            vtable = loadVtable();
        }
        return vtable;
    }

    @Nonnull
    SparseArray<FieldReference> getInstanceFields() {
        if (instanceFields == null) {
            instanceFields = loadFields();
        }
        return instanceFields;
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

    @Nonnull
    protected LinkedHashMap<String, ClassDef> getInterfaces() {
        if (interfaces != null) {
            return interfaces;
        }

        interfaces = Maps.newLinkedHashMap();

        try {
            for (String interfaceType: getClassDef().getInterfaces()) {
                if (!interfaces.containsKey(interfaceType)) {
                    ClassDef interfaceDef;
                    try {
                        interfaceDef = classPath.getClassDef(interfaceType);
                        interfaces.put(interfaceType, interfaceDef);
                    } catch (UnresolvedClassException ex) {
                        interfacesFullyResolved = false;
                    }

                    ClassProto interfaceProto = (ClassProto) classPath.getClass(interfaceType);
                    for (ClassDef superInterface: interfaceProto.getInterfaces().values()) {
                        if (!interfaces.containsKey(superInterface.getType())) {
                            interfaces.put(superInterface.getType(), superInterface);
                        }
                    }
                }
            }
        } catch (UnresolvedClassException ex) {
            interfacesFullyResolved = false;
        }

        if (!interfacesFullyResolved) {
            throw new UnresolvedClassException("Interfaces for class %s not fully resolved", getType());
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
        if (getInterfaces().containsKey(iface)) {
            return true;
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
        if (getInstanceFields().size() == 0) {
            return null;
        }
        return getInstanceFields().get(fieldOffset);
    }

    @Override
    @Nullable
    public MethodReference getMethodByVtableIndex(int vtableIndex) {
        if (vtableIndex < 0 || vtableIndex >= getVtable().length) {
            return null;
        }
        return getVtable()[vtableIndex];
    }

    @Nonnull
    private SparseArray<FieldReference> loadFields() {
        //This is a bit of an "involved" operation. We need to follow the same algorithm that dalvik uses to
        //arrange fields, so that we end up with the same field offsets (which is needed for deodexing).
        //See mydroid/dalvik/vm/oo/Class.c - computeFieldOffsets()

        final byte REFERENCE = 0;
        final byte WIDE = 1;
        final byte OTHER = 2;

        ArrayList<Field> loadedFields = getInstanceFields(getClassDef());
        Field[] fields = new Field[loadedFields.size()];
        //the "type" for each field in fields. 0=reference,1=wide,2=other
        byte[] fieldTypes = new byte[fields.length];
        for (int i=0;i<fields.length;i++) {
            fields[i] = loadedFields.get(i);
            fieldTypes[i] = getFieldType(fields[i].getType());
        }

        //The first operation is to move all of the reference fields to the front. To do this, find the first
        //non-reference field, then find the last reference field, swap them and repeat
        int back = fields.length - 1;
        int front;
        for (front = 0; front<fields.length; front++) {
            if (fieldTypes[front] != REFERENCE) {
                while (back > front) {
                    if (fieldTypes[back] == REFERENCE) {
                        swap(fieldTypes, fields, front, back--);
                        break;
                    }
                    back--;
                }
            }

            if (fieldTypes[front] != REFERENCE) {
                break;
            }
        }

        int startFieldOffset = 8;
        String superclassType = getSuperclass();
        ClassProto superclass = null;
        if (superclassType != null) {
            superclass = (ClassProto) classPath.getClass(superclassType);
            if (superclass != null) {
                startFieldOffset = superclass.getNextFieldOffset();
            }
        }

        int fieldIndexMod;
        if ((startFieldOffset % 8) == 0) {
            fieldIndexMod = 0;
        } else {
            fieldIndexMod = 1;
        }

        //next, we need to group all the wide fields after the reference fields. But the wide fields have to be
        //8-byte aligned. If we're on an odd field index, we need to insert a 32-bit field. If the next field
        //is already a 32-bit field, use that. Otherwise, find the first 32-bit field from the end and swap it in.
        //If there are no 32-bit fields, do nothing for now. We'll add padding when calculating the field offsets
        if (front < fields.length && (front % 2) != fieldIndexMod) {
            if (fieldTypes[front] == WIDE) {
                //we need to swap in a 32-bit field, so the wide fields will be correctly aligned
                back = fields.length - 1;
                while (back > front) {
                    if (fieldTypes[back] == OTHER) {
                        swap(fieldTypes, fields, front++, back);
                        break;
                    }
                    back--;
                }
            } else {
                //there's already a 32-bit field here that we can use
                front++;
            }
        }

        //do the swap thing for wide fields
        back = fields.length - 1;
        for (; front<fields.length; front++) {
            if (fieldTypes[front] != WIDE) {
                while (back > front) {
                    if (fieldTypes[back] == WIDE) {
                        swap(fieldTypes, fields, front, back--);
                        break;
                    }
                    back--;
                }
            }

            if (fieldTypes[front] != WIDE) {
                break;
            }
        }

        int superFieldCount = 0;
        if (superclass != null) {
            superFieldCount = superclass.instanceFields.size();
        }

        //now the fields are in the correct order. Add them to the SparseArray and lookup, and calculate the offsets
        int totalFieldCount = superFieldCount + fields.length;
        SparseArray<FieldReference> instanceFields = new SparseArray<FieldReference>(totalFieldCount);

        int fieldOffset;

        if (superclass != null && superFieldCount > 0) {
            for (int i=0; i<superFieldCount; i++) {
                instanceFields.append(superclass.instanceFields.keyAt(i), superclass.instanceFields.valueAt(i));
            }

            fieldOffset = instanceFields.keyAt(superFieldCount-1);

            FieldReference lastSuperField = superclass.instanceFields.valueAt(superFieldCount-1);
            char fieldType = lastSuperField.getType().charAt(0);
            if (fieldType == 'J' || fieldType == 'D') {
                fieldOffset += 8;
            } else {
                fieldOffset += 4;
            }
        } else {
            //the field values start at 8 bytes into the DataObject dalvik structure
            fieldOffset = 8;
        }

        boolean gotDouble = false;
        for (int i=0; i<fields.length; i++) {
            FieldReference field = fields[i];

            //add padding to align the wide fields, if needed
            if (fieldTypes[i] == WIDE && !gotDouble) {
                if (!gotDouble) {
                    if (fieldOffset % 8 != 0) {
                        assert fieldOffset % 8 == 4;
                        fieldOffset += 4;
                    }
                    gotDouble = true;
                }
            }

            instanceFields.append(fieldOffset, field);
            if (fieldTypes[i] == WIDE) {
                fieldOffset += 8;
            } else {
                fieldOffset += 4;
            }
        }

        return instanceFields;
    }

    private ArrayList<Field> getInstanceFields(ClassDef classDef) {
        ArrayList<Field> instanceFields = Lists.newArrayList();
        for (Field field: classDef.getFields()) {
            if (!FieldUtil.isStatic(field)) {
                instanceFields.add(field);
            }
        }
        return instanceFields;
    }

    private byte getFieldType(String fieldType) {
        switch (fieldType.charAt(0)) {
            case '[':
            case 'L':
                return 0; //REFERENCE
            case 'J':
            case 'D':
                return 1; //WIDE
            default:
                return 2; //OTHER
        }
    }

    private void swap(byte[] fieldTypes, FieldReference[] fields, int position1, int position2) {
        byte tempType = fieldTypes[position1];
        fieldTypes[position1] = fieldTypes[position2];
        fieldTypes[position2] = tempType;

        FieldReference tempField = fields[position1];
        fields[position1] = fields[position2];
        fields[position2] = tempField;
    }

    private int getNextFieldOffset() {
        SparseArray<FieldReference> instanceFields = getInstanceFields();
        if (instanceFields.size() == 0) {
            return 8;
        }

        int lastItemIndex = instanceFields.size()-1;
        int fieldOffset = instanceFields.keyAt(lastItemIndex);
        FieldReference lastField = instanceFields.valueAt(lastItemIndex);

        switch (lastField.getType().charAt(0)) {
            case 'J':
            case 'D':
                return fieldOffset + 8;
            default:
                return fieldOffset + 4;
        }
    }

    //TODO: check the case when we have a package private method that overrides an interface method
    @Nonnull
    private Method[] loadVtable() {
        //TODO: it might be useful to keep track of which class's implementation is used for each virtual method. In other words, associate the implementing class type with each vtable entry
        List<Method> virtualMethodList = Lists.newLinkedList();

        //copy the virtual methods from the superclass
        String superclassType = getSuperclass();
        if (superclassType != null) {
            ClassProto superclass = (ClassProto) classPath.getClass(superclassType);
            for (int i=0; i<superclass.getVtable().length; i++) {
                virtualMethodList.add(superclass.getVtable()[i]);
            }
        }

        //iterate over the virtual methods in the current class, and only add them when we don't already have the
        //method (i.e. if it was implemented by the superclass)
        if (!isInterface()) {
            addToVtable(getClassDef().getVirtualMethods(), virtualMethodList);

            for (ClassDef interfaceDef: getInterfaces().values()) {
                addToVtable(interfaceDef.getVirtualMethods(), virtualMethodList);
            }
        }

        Method[] vtable = new Method[virtualMethodList.size()];
        for (int i=0; i<virtualMethodList.size(); i++) {
            vtable[i] = virtualMethodList.get(i);
        }

        return vtable;
    }

    private void addToVtable(@Nonnull Iterable<? extends Method> localMethods, @Nonnull List<Method> vtable) {
        List<? extends Method> methods = Lists.newArrayList(localMethods);
        Collections.sort(methods);

        for (Method virtualMethod: methods) {
            boolean found = false;
            for (int i=0; i<vtable.size(); i++) {
                Method superMethod = vtable.get(i);
                if (methodSignaturesMatch(superMethod, virtualMethod)) {
                    if (classPath.getApi() < 17 || canAccess(superMethod)) {
                        found = true;
                        vtable.set(i, virtualMethod);
                        break;
                    }
                }
            }
            if (!found) {
                vtable.add(virtualMethod);
            }
        }
    }

    private boolean methodSignaturesMatch(Method a, Method b) {
        return (a.getName().equals(b.getName())
                && a.getReturnType().equals(b.getReturnType())
                && a.getParameters().equals(b.getParameters()));
    }

    private boolean canAccess(Method virtualMethod) {
        if (!methodIsPackagePrivate(virtualMethod.getAccessFlags())) {
            return true;
        }

        String otherPackage = getPackage(virtualMethod.getDefiningClass());
        String ourPackage = getPackage(getClassDef().getType());
        return otherPackage.equals(ourPackage);
    }

    private String getPackage(String classType) {
        int lastSlash = classType.lastIndexOf('/');
        if (lastSlash < 0) {
            return "";
        }
        return classType.substring(1, lastSlash);
    }

    private static boolean methodIsPackagePrivate(int accessFlags) {
        return (accessFlags & (AccessFlags.PRIVATE.getValue() |
                AccessFlags.PROTECTED.getValue() |
                AccessFlags.PUBLIC.getValue())) == 0;
    }
}
