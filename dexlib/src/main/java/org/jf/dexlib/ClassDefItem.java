/*
 * [The "BSD licence"]
 * Copyright (c) 2009 Ben Gruver
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.dexlib;

import org.jf.dexlib.Util.Input;
import org.jf.dexlib.Util.AnnotatedOutput;

import java.util.*;

public class ClassDefItem extends Item<ClassDefItem> {
    private TypeIdItem classType;
    private int accessFlags;
    private TypeIdItem superType;
    private TypeListItem implementedInterfaces;
    private StringIdItem sourceFile;
    private AnnotationDirectoryItem annotations;
    private ClassDataItem classData;
    private EncodedArrayItem staticFieldInitializers;

    /**
     * Creates a new uninitialized <code>ClassDefItem</code>
     * @param dexFile The <code>DexFile</code> that this item belongs to
     */
    protected ClassDefItem(DexFile dexFile) {
        super(dexFile);
    }

    /**
     * Creates a new <code>ClassDefItem</code> with the given values
     * @param dexFile The <code>DexFile</code> that this item belongs to
     * @param classType The type of this class
     * @param accessFlags The access flags of this class
     * @param superType The superclass of this class, or null if none (only valid for java.lang.Object)
     * @param implementedInterfaces A list of the interfaces that this class implements, or null if none
     * @param sourceFile The main source file that this class is defined in, or null if not available
     * @param annotations The annotations for this class and its fields, methods and method parameters, or null if none
     * @param classData The <code>ClassDataItem</code> containing the method and field definitions for this class
     * @param staticFieldInitializers The initial values for this class's static fields, or null if none
     */
    private ClassDefItem(DexFile dexFile, TypeIdItem classType, int accessFlags, TypeIdItem superType,
                         TypeListItem implementedInterfaces, StringIdItem sourceFile,
                         AnnotationDirectoryItem annotations, ClassDataItem classData,
                         EncodedArrayItem staticFieldInitializers) {
        super(dexFile);
        this.classType = classType;
        this.accessFlags = accessFlags;
        this.superType = superType;
        this.implementedInterfaces = implementedInterfaces;
        this.sourceFile = sourceFile;
        this.annotations = annotations;
        this.classData = classData;
        this.staticFieldInitializers = staticFieldInitializers;

        if (classData != null) {
            classData.setParent(this);
        }
        if (annotations != null) {
            annotations.setParent(this);
        }
    }

    /**
     * Returns a <code>ClassDefItem</code> for the given values, and that has been interned into the given
     * <code>DexFile</code>
     * @param dexFile The <code>DexFile</code> that this item belongs to
     * @param classType The type of this class
     * @param accessFlags The access flags of this class
     * @param superType The superclass of this class, or null if none (only valid for java.lang.Object)
     * @param implementedInterfaces A list of the interfaces that this class implements, or null if none
     * @param sourceFile The main source file that this class is defined in, or null if not available
     * @param annotations The annotations for this class and its fields, methods and method parameters, or null if none
     * @param classData The <code>ClassDataItem</code> containing the method and field definitions for this class
     * @param staticFieldInitializers The initial values for this class's static fields, or null if none
     * @return a <code>ClassDefItem</code> for the given values, and that has been interned into the given
     * <code>DexFile</code>
     */
    public static ClassDefItem getInternedClassDefItem(DexFile dexFile, TypeIdItem classType, int accessFlags,
                         TypeIdItem superType, TypeListItem implementedInterfaces, StringIdItem sourceFile,
                         AnnotationDirectoryItem annotations, ClassDataItem classData,
                         EncodedArrayItem staticFieldInitializers) {
        ClassDefItem classDefItem = new ClassDefItem(dexFile, classType, accessFlags, superType, implementedInterfaces,
                sourceFile, annotations, classData, staticFieldInitializers);
        return dexFile.ClassDefsSection.intern(classDefItem);
    }

    /** {@inheritDoc} */
    protected void readItem(Input in, ReadContext readContext) {
        classType = dexFile.TypeIdsSection.getItemByIndex(in.readInt());
        accessFlags = in.readInt();
        superType = dexFile.TypeIdsSection.getItemByIndex(in.readInt());
        implementedInterfaces = (TypeListItem)readContext.getOffsettedItemByOffset(ItemType.TYPE_TYPE_LIST,
                in.readInt());
        sourceFile = dexFile.StringIdsSection.getItemByIndex(in.readInt());
        annotations = (AnnotationDirectoryItem)readContext.getOffsettedItemByOffset(
                ItemType.TYPE_ANNOTATIONS_DIRECTORY_ITEM, in.readInt());        
        classData = (ClassDataItem)readContext.getOffsettedItemByOffset(ItemType.TYPE_CLASS_DATA_ITEM, in.readInt());
        staticFieldInitializers = (EncodedArrayItem)readContext.getOffsettedItemByOffset(
                ItemType.TYPE_ENCODED_ARRAY_ITEM, in.readInt());

        if (classData != null) {
            classData.setParent(this);
        }
        if (annotations != null) {
            annotations.setParent(this);
        }
    }

    /** {@inheritDoc} */
    protected int placeItem(int offset) {
        return offset + 32;
    }

    /** {@inheritDoc} */
    protected void writeItem(AnnotatedOutput out) {
        if (out.annotates()) {
            out.annotate(4, "class_idx");
            out.annotate(4, "access_flags");
            out.annotate(4, "superclass_idx");
            out.annotate(4, "interfaces_off");
            out.annotate(4, "source_file_idx");
            out.annotate(4, "annotations_off");
            out.annotate(4, "class_data_off");
            out.annotate(4, "static_values_off");
        }
        out.writeInt(classType.getIndex());
        out.writeInt(accessFlags);
        out.writeInt(superType==null?-1:superType.getIndex());
        out.writeInt(implementedInterfaces==null?0:implementedInterfaces.getOffset());
        out.writeInt(sourceFile==null?-1:sourceFile.getIndex());
        out.writeInt(annotations==null?0:annotations.getOffset());
        out.writeInt(classData==null?0:classData.getOffset());
        out.writeInt(staticFieldInitializers==null?0:staticFieldInitializers.getOffset());
    }

    /** {@inheritDoc} */
    public ItemType getItemType() {
        return ItemType.TYPE_CLASS_DEF_ITEM;
    }

    /** {@inheritDoc} */
    public String getConciseIdentity() {
        return "class_def_item: " + classType.getTypeDescriptor();
    }

    /** {@inheritDoc} */
    public int compareTo(ClassDefItem o) {
        //The actual sorting for this class is implemented in SortClassDefItemSection.
        //This method is just used for sorting the associated ClassDataItem items, so
        //we can just do the comparison based on the offsets of the items
        return this.getOffset() - o.getOffset();
    }

    public TypeIdItem getClassType() {
        return classType;
    }

    public int getAccessFlags() {
        return accessFlags;
    }

    public TypeIdItem getSuperclass() {
        return superType;
    }

    public TypeListItem getInterfaces() {
        return implementedInterfaces;
    }

    public StringIdItem getSourceFile() {
        return sourceFile;
    }

    public AnnotationDirectoryItem getAnnotations() {
        return annotations;
    }

    public ClassDataItem getClassData() {
        return classData;
    }

    public EncodedArrayItem getStaticFieldInitializers() {
        return staticFieldInitializers;
    }

    public static int placeClassDefItems(IndexedSection<ClassDefItem> section, int offset) {
        ClassDefPlacer cdp = new ClassDefPlacer(section);
        return cdp.placeSection(offset);
    }

    /**
     * This class places the items within a ClassDefItem section, such that superclasses and interfaces are
     * placed before sub/implementing classes 
     */
    private static class ClassDefPlacer {
        private final IndexedSection<ClassDefItem> section;
        private final HashMap<TypeIdItem, ClassDefItem> unplacedClassDefsByType =
                new HashMap<TypeIdItem, ClassDefItem>();

        private int currentIndex = 0;
        private int currentOffset;

        public ClassDefPlacer(IndexedSection<ClassDefItem> section) {
            this.section = section;

            for (ClassDefItem classDefItem: section.items) {
                TypeIdItem typeIdItem = classDefItem.classType;
                unplacedClassDefsByType.put(typeIdItem, classDefItem);
            }
        }

        public int placeSection(int offset) {
            currentOffset = offset;

            if (section.DexFile.getSortAllItems()) {
                //presort the list, to guarantee a unique ordering
                Collections.sort(section.items, new Comparator<ClassDefItem>() {
                    public int compare(ClassDefItem a, ClassDefItem b) {
                        return a.getClassType().compareTo(b.getClassType());
                    }
                });
            }

            for (ClassDefItem classDefItem: section.items) {
                placeClass(classDefItem);
            }

            for (ClassDefItem classDefItem: unplacedClassDefsByType.values()) {
                section.items.set(classDefItem.getIndex(), classDefItem);
            }

            return currentOffset;
        }

        private void placeClass(ClassDefItem classDefItem) {
            if (classDefItem.getOffset() == -1) {
                TypeIdItem superType = classDefItem.superType;
                ClassDefItem superClassDefItem = unplacedClassDefsByType.get(superType);

                if (superClassDefItem != null) {
                    placeClass(superClassDefItem);
                }

                TypeListItem interfaces = classDefItem.implementedInterfaces;

                if (interfaces != null) {
                    for (TypeIdItem interfaceType: interfaces.getTypes()) {
                        ClassDefItem interfaceClass = unplacedClassDefsByType.get(interfaceType);
                        if (interfaceClass != null) {
                            placeClass(interfaceClass);
                        }
                    }
                }

                currentOffset = classDefItem.placeAt(currentIndex++, currentOffset);
                unplacedClassDefsByType.remove(classDefItem.classType);
            }
        }

    }
}
