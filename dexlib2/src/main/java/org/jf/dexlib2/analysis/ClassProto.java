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

import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Ints;
import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.analysis.util.TypeProtoUtils;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.immutable.ImmutableMethod;
import org.jf.dexlib2.util.MethodUtil;
import org.jf.util.AlignmentUtils;
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
    private static final byte REFERENCE = 0;
    private static final byte WIDE = 1;
    private static final byte OTHER = 2;

    @Nonnull protected final ClassPath classPath;
    @Nonnull protected final String type;

    protected boolean vtableFullyResolved = true;
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
        return classDefSupplier.get();
    }


    @Nonnull private final Supplier<ClassDef> classDefSupplier = Suppliers.memoize(new Supplier<ClassDef>() {
        @Override public ClassDef get() {
            return classPath.getClassDef(type);
        }
    });

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

    /**
     * Returns the set of interfaces that this class implements as a Map<String, ClassDef>.
     *
     * The ClassDef value will be present only for the interfaces that this class directly implements (including any
     * interfaces transitively implemented), but not for any interfaces that are only implemented by a superclass of
     * this class
     *
     * For any interfaces that are only implemented by a superclass (or the class itself, if the class is an interface),
     * the value will be null.
     *
     * If any interface couldn't be resolved, then the interfacesFullyResolved field will be set to false upon return.
     *
     * @return the set of interfaces that this class implements as a Map<String, ClassDef>.
     */
    @Nonnull
    protected LinkedHashMap<String, ClassDef> getInterfaces() {
        return interfacesSupplier.get();
    }

    @Nonnull
    private final Supplier<LinkedHashMap<String, ClassDef>> interfacesSupplier =
            Suppliers.memoize(new Supplier<LinkedHashMap<String, ClassDef>>() {
                @Override public LinkedHashMap<String, ClassDef> get() {
                    LinkedHashMap<String, ClassDef> interfaces = Maps.newLinkedHashMap();

                    try {
                        for (String interfaceType: getClassDef().getInterfaces()) {
                            if (!interfaces.containsKey(interfaceType)) {
                                ClassDef interfaceDef;
                                try {
                                    interfaceDef = classPath.getClassDef(interfaceType);
                                    interfaces.put(interfaceType, interfaceDef);
                                } catch (UnresolvedClassException ex) {
                                    interfaces.put(interfaceType, null);
                                    interfacesFullyResolved = false;
                                }

                                ClassProto interfaceProto = (ClassProto) classPath.getClass(interfaceType);
                                for (String superInterface: interfaceProto.getInterfaces().keySet()) {
                                    if (!interfaces.containsKey(superInterface)) {
                                        interfaces.put(superInterface, interfaceProto.getInterfaces().get(superInterface));
                                    }
                                }
                                if (!interfaceProto.interfacesFullyResolved) {
                                    interfacesFullyResolved = false;
                                }
                            }
                        }
                    } catch (UnresolvedClassException ex) {
                        interfacesFullyResolved = false;
                    }

                    // now add self and super class interfaces, required for common super class lookup
                    // we don't really need ClassDef's for that, so let's just use null

                    if (isInterface() && !interfaces.containsKey(getType())) {
                        interfaces.put(getType(), null);
                    }

                    try {
                        String superclass = getSuperclass();
                        if (superclass != null) {
                            ClassProto superclassProto = (ClassProto) classPath.getClass(superclass);
                            for (String superclassInterface: superclassProto.getInterfaces().keySet()) {
                                if (!interfaces.containsKey(superclassInterface)) {
                                    interfaces.put(superclassInterface, null);
                                }
                            }
                            if (!superclassProto.interfacesFullyResolved) {
                                interfacesFullyResolved = false;
                            }
                        }
                    } catch (UnresolvedClassException ex) {
                        interfacesFullyResolved = false;
                    }

                    return interfaces;
                }
            });

    /**
     * Gets the interfaces directly implemented by this class, or the interfaces they transitively implement.
     *
     * This does not include any interfaces that are only implemented by a superclass
     *
     * @return An iterables of ClassDefs representing the directly or transitively implemented interfaces
     * @throws UnresolvedClassException if interfaces could not be fully resolved
     */
    @Nonnull
    protected Iterable<ClassDef> getDirectInterfaces() {
        Iterable<ClassDef> directInterfaces =
                FluentIterable.from(getInterfaces().values()).filter(Predicates.notNull());

        if (!interfacesFullyResolved) {
            throw new UnresolvedClassException("Interfaces for class %s not fully resolved", getType());
        }

        return directInterfaces;
    }

    /**
     * Checks if this class implements the given interface.
     *
     * If the interfaces of this class cannot be fully resolved then this
     * method will either return true or throw an UnresolvedClassException
     *
     * @param iface The interface to check for
     * @return true if this class implements the given interface, otherwise false
     * @throws UnresolvedClassException if the interfaces for this class could not be fully resolved, and the interface
     * is not one of the interfaces that were successfully resolved
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
    public Method getMethodByVtableIndex(int vtableIndex) {
        List<Method> vtable = getVtable();
        if (vtableIndex < 0 || vtableIndex >= vtable.size()) {
            return null;
        }

        return vtable.get(vtableIndex);
    }

    public int findMethodIndexInVtable(@Nonnull MethodReference method) {
        List<Method> vtable = getVtable();
        for (int i=0; i<vtable.size(); i++) {
            Method candidate = vtable.get(i);
            if (MethodUtil.methodSignaturesMatch(candidate, method)) {
                if (!classPath.shouldCheckPackagePrivateAccess() ||
                        AnalyzedMethodUtil.canAccess(this, candidate, true, false, false)) {
                    return i;
                }
            }
        }
        return -1;
    }

    @Nonnull public SparseArray<FieldReference> getInstanceFields() {
        if (classPath.isArt()) {
            return artInstanceFieldsSupplier.get();
        } else {
            return dalvikInstanceFieldsSupplier.get();
        }
    }

    @Nonnull private final Supplier<SparseArray<FieldReference>> dalvikInstanceFieldsSupplier =
            Suppliers.memoize(new Supplier<SparseArray<FieldReference>>() {
                @Override public SparseArray<FieldReference> get() {
                    //This is a bit of an "involved" operation. We need to follow the same algorithm that dalvik uses to
                    //arrange fields, so that we end up with the same field offsets (which is needed for deodexing).
                    //See mydroid/dalvik/vm/oo/Class.c - computeFieldOffsets()

                    ArrayList<Field> fields = getSortedInstanceFields(getClassDef());
                    final int fieldCount = fields.size();
                    //the "type" for each field in fields. 0=reference,1=wide,2=other
                    byte[] fieldTypes = new byte[fields.size()];
                    for (int i=0; i<fieldCount; i++) {
                        fieldTypes[i] = getFieldType(fields.get(i));
                    }

                    //The first operation is to move all of the reference fields to the front. To do this, find the first
                    //non-reference field, then find the last reference field, swap them and repeat
                    int back = fields.size() - 1;
                    int front;
                    for (front = 0; front<fieldCount; front++) {
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
                    if (front < fieldCount && (front % 2) != fieldIndexMod) {
                        if (fieldTypes[front] == WIDE) {
                            //we need to swap in a 32-bit field, so the wide fields will be correctly aligned
                            back = fieldCount - 1;
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
                    back = fieldCount - 1;
                    for (; front<fieldCount; front++) {
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

                    SparseArray<FieldReference> superFields;
                    if (superclass != null) {
                        superFields = superclass.getInstanceFields();
                    } else {
                        superFields = new SparseArray<FieldReference>();
                    }
                    int superFieldCount = superFields.size();

                    //now the fields are in the correct order. Add them to the SparseArray and lookup, and calculate the offsets
                    int totalFieldCount = superFieldCount + fieldCount;
                    SparseArray<FieldReference> instanceFields = new SparseArray<FieldReference>(totalFieldCount);

                    int fieldOffset;

                    if (superclass != null && superFieldCount > 0) {
                        for (int i=0; i<superFieldCount; i++) {
                            instanceFields.append(superFields.keyAt(i), superFields.valueAt(i));
                        }

                        fieldOffset = instanceFields.keyAt(superFieldCount-1);

                        FieldReference lastSuperField = superFields.valueAt(superFieldCount-1);
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
                    for (int i=0; i<fieldCount; i++) {
                        FieldReference field = fields.get(i);

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

                @Nonnull
                private ArrayList<Field> getSortedInstanceFields(@Nonnull ClassDef classDef) {
                    ArrayList<Field> fields = Lists.newArrayList(classDef.getInstanceFields());
                    Collections.sort(fields);
                    return fields;
                }

                private void swap(byte[] fieldTypes, List<Field> fields, int position1, int position2) {
                    byte tempType = fieldTypes[position1];
                    fieldTypes[position1] = fieldTypes[position2];
                    fieldTypes[position2] = tempType;

                    Field tempField = fields.set(position1, fields.get(position2));
                    fields.set(position2, tempField);
                }
            });

    private static abstract class FieldGap implements Comparable<FieldGap> {
        public final int offset;
        public final int size;

        public static FieldGap newFieldGap(int offset, int size, int oatVersion) {
            if (oatVersion >= 67) {
                return new FieldGap(offset, size) {
                    @Override public int compareTo(FieldGap o) {
                        int result = Ints.compare(o.size, size);
                        if (result != 0) {
                            return result;
                        }
                        return Ints.compare(offset, o.offset);
                    }
                };
            } else {
                return new FieldGap(offset, size) {
                    @Override public int compareTo(FieldGap o) {
                        int result = Ints.compare(size, o.size);
                        if (result != 0) {
                            return result;
                        }
                        return Ints.compare(o.offset, offset);
                    }
                };
            }
        }

        private FieldGap(int offset, int size) {
            this.offset = offset;
            this.size = size;
        }
    }

    @Nonnull private final Supplier<SparseArray<FieldReference>> artInstanceFieldsSupplier =
            Suppliers.memoize(new Supplier<SparseArray<FieldReference>>() {

                @Override public SparseArray<FieldReference> get() {
                    // We need to follow the same algorithm that art uses to arrange fields, so that we end up with the
                    // same field offsets, which is needed for deodexing.
                    // See LinkFields() in art/runtime/class_linker.cc

                    PriorityQueue<FieldGap> gaps = new PriorityQueue<FieldGap>();

                    SparseArray<FieldReference> linkedFields = new SparseArray<FieldReference>();
                    ArrayList<Field> fields = getSortedInstanceFields(getClassDef());

                    int fieldOffset = 0;
                    String superclassType = getSuperclass();
                    if (superclassType != null) {
                        // TODO: what to do if superclass doesn't exist?
                        ClassProto superclass = (ClassProto) classPath.getClass(superclassType);
                        SparseArray<FieldReference> superFields = superclass.getInstanceFields();
                        FieldReference field = null;
                        int lastOffset = 0;
                        for (int i=0; i<superFields.size(); i++) {
                            int offset = superFields.keyAt(i);
                            field = superFields.valueAt(i);
                            linkedFields.put(offset, field);
                            lastOffset = offset;
                        }
                        if (field != null) {
                            fieldOffset = lastOffset + getFieldSize(field);
                        }
                    }

                    for (Field field: fields) {
                        int fieldSize = getFieldSize(field);

                        if (!AlignmentUtils.isAligned(fieldOffset, fieldSize)) {
                            int oldOffset = fieldOffset;
                            fieldOffset = AlignmentUtils.alignOffset(fieldOffset, fieldSize);
                            addFieldGap(oldOffset, fieldOffset, gaps);
                        }

                        FieldGap gap = gaps.peek();
                        if (gap != null && gap.size >= fieldSize) {
                            gaps.poll();
                            linkedFields.put(gap.offset, field);
                            if (gap.size > fieldSize) {
                                addFieldGap(gap.offset + fieldSize, gap.offset + gap.size, gaps);
                            }
                        } else {
                            linkedFields.append(fieldOffset, field);
                            fieldOffset += fieldSize;
                        }
                    }

                    return linkedFields;
                }

                private void addFieldGap(int gapStart, int gapEnd, @Nonnull PriorityQueue<FieldGap> gaps) {
                    int offset = gapStart;

                    while (offset < gapEnd) {
                        int remaining = gapEnd - offset;

                        if ((remaining >= 4) && (offset % 4 == 0)) {
                            gaps.add(FieldGap.newFieldGap(offset, 4, classPath.oatVersion));
                            offset += 4;
                        } else if (remaining >= 2 && (offset % 2 == 0)) {
                            gaps.add(FieldGap.newFieldGap(offset, 2, classPath.oatVersion));
                            offset += 2;
                        } else {
                            gaps.add(FieldGap.newFieldGap(offset, 1, classPath.oatVersion));
                            offset += 1;
                        }
                    }
                }

                @Nonnull
                private ArrayList<Field> getSortedInstanceFields(@Nonnull ClassDef classDef) {
                    ArrayList<Field> fields = Lists.newArrayList(classDef.getInstanceFields());
                    Collections.sort(fields, new Comparator<Field>() {
                        @Override public int compare(Field field1, Field field2) {
                            int result = Ints.compare(getFieldSortOrder(field1), getFieldSortOrder(field2));
                            if (result != 0) {
                                return result;
                            }

                            result = field1.getName().compareTo(field2.getName());
                            if (result != 0) {
                                return result;
                            }
                            return field1.getType().compareTo(field2.getType());
                        }
                    });
                    return fields;
                }

                private int getFieldSortOrder(@Nonnull FieldReference field) {
                    // The sort order is based on type size (except references are first), and then based on the
                    // enum value of the primitive type for types of equal size. See: Primitive::Type enum
                    // in art/runtime/primitive.h
                    switch (field.getType().charAt(0)) {
                        /* reference */
                        case '[':
                        case 'L':
                            return 0;
                        /* 64 bit */
                        case 'J':
                            return 1;
                        case 'D':
                            return 2;
                        /* 32 bit */
                        case 'I':
                            return 3;
                        case 'F':
                            return 4;
                        /* 16 bit */
                        case 'C':
                            return 5;
                        case 'S':
                            return 6;
                        /* 8 bit */
                        case 'Z':
                            return 7;
                        case 'B':
                            return 8;
                    }
                    throw new ExceptionWithContext("Invalid field type: %s", field.getType());
                }

                private int getFieldSize(@Nonnull FieldReference field) {
                    return getTypeSize(field.getType().charAt(0));
                }
            });

    private int getNextFieldOffset() {
        SparseArray<FieldReference> instanceFields = getInstanceFields();
        if (instanceFields.size() == 0) {
            return classPath.isArt() ? 0 : 8;
        }

        int lastItemIndex = instanceFields.size()-1;
        int fieldOffset = instanceFields.keyAt(lastItemIndex);
        FieldReference lastField = instanceFields.valueAt(lastItemIndex);

        if (classPath.isArt()) {
            return fieldOffset + getTypeSize(lastField.getType().charAt(0));
        } else {
            switch (lastField.getType().charAt(0)) {
                case 'J':
                case 'D':
                    return fieldOffset + 8;
                default:
                    return fieldOffset + 4;
            }
        }
    }

    private static int getTypeSize(char type) {
        switch (type) {
            case 'J':
            case 'D':
                return 8;
            case '[':
            case 'L':
            case 'I':
            case 'F':
                return 4;
            case 'C':
            case 'S':
                return 2;
            case 'B':
            case 'Z':
                return 1;
        }
        throw new ExceptionWithContext("Invalid type: %s", type);
    }

    @Nonnull public List<Method> getVtable() {
        return vtableSupplier.get();
    }

    //TODO: check the case when we have a package private method that overrides an interface method
    @Nonnull private final Supplier<List<Method>> vtableSupplier = Suppliers.memoize(new Supplier<List<Method>>() {
        @Override public List<Method> get() {
            List<Method> vtable = Lists.newArrayList();

            //copy the virtual methods from the superclass
            String superclassType;
            try {
                superclassType = getSuperclass();
            } catch (UnresolvedClassException ex) {
                vtable.addAll(((ClassProto)classPath.getClass("Ljava/lang/Object;")).getVtable());
                vtableFullyResolved = false;
                return vtable;
            }

            if (superclassType != null) {
                ClassProto superclass = (ClassProto) classPath.getClass(superclassType);
                vtable.addAll(superclass.getVtable());

                // if the superclass's vtable wasn't fully resolved, then we can't know where the new methods added by this
                // class should start, so we just propagate what we can from the parent and hope for the best.
                if (!superclass.vtableFullyResolved) {
                    vtableFullyResolved = false;
                    return vtable;
                }
            }

            //iterate over the virtual methods in the current class, and only add them when we don't already have the
            //method (i.e. if it was implemented by the superclass)
            if (!isInterface()) {
                addToVtable(getClassDef().getVirtualMethods(), vtable, true);

                // assume that interface method is implemented in the current class, when adding it to vtable
                // otherwise it looks like that method is invoked on an interface, which fails Dalvik's optimization checks
                for (ClassDef interfaceDef: getDirectInterfaces()) {
                    List<Method> interfaceMethods = Lists.newArrayList();
                    for (Method interfaceMethod: interfaceDef.getVirtualMethods()) {
                        ImmutableMethod method = new ImmutableMethod(
                                type,
                                interfaceMethod.getName(),
                                interfaceMethod.getParameters(),
                                interfaceMethod.getReturnType(),
                                interfaceMethod.getAccessFlags(),
                                interfaceMethod.getAnnotations(),
                                interfaceMethod.getImplementation());
                        interfaceMethods.add(method);
                    }
                    addToVtable(interfaceMethods, vtable, false);
                }
            }
            return vtable;
        }

        private void addToVtable(@Nonnull Iterable<? extends Method> localMethods,
                                 @Nonnull List<Method> vtable, boolean replaceExisting) {
            List<? extends Method> methods = Lists.newArrayList(localMethods);
            Collections.sort(methods);

            outer: for (Method virtualMethod: methods) {
                for (int i=0; i<vtable.size(); i++) {
                    Method superMethod = vtable.get(i);
                    if (MethodUtil.methodSignaturesMatch(superMethod, virtualMethod)) {
                        if (!classPath.shouldCheckPackagePrivateAccess() ||
                                AnalyzedMethodUtil.canAccess(ClassProto.this, superMethod, true, false, false)) {
                            if (replaceExisting) {
                                vtable.set(i, virtualMethod);
                            }
                            continue outer;
                        }
                    }
                }
                // we didn't find an equivalent method, so add it as a new entry
                vtable.add(virtualMethod);
            }
        }
    });

    private static byte getFieldType(@Nonnull FieldReference field) {
        switch (field.getType().charAt(0)) {
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
}
