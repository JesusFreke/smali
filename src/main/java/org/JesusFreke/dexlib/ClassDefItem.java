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
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.JesusFreke.dexlib;

import org.JesusFreke.dexlib.ItemType;
import org.JesusFreke.dexlib.EncodedValue.EncodedValue;
import org.JesusFreke.dexlib.EncodedValue.EncodedValueSubField;
import org.JesusFreke.dexlib.util.TypeUtils;

import java.util.HashMap;
import java.util.ArrayList;

public class ClassDefItem extends IndexedItem<ClassDefItem> {
    private final Field[] fields;

    private final IndexedItemReference<TypeIdItem> classType;
    private final IntegerField accessFlags;
    private final IndexedItemReference<TypeIdItem> superclassType;
    private final OffsettedItemReference<TypeListItem> classInterfacesList;
    private final IndexedItemReference<StringIdItem> sourceFile;
    private final OffsettedItemReference<AnnotationDirectoryItem> classAnnotations;
    private final OffsettedItemReference<ClassDataItem> classData;
    private final OffsettedItemReference<EncodedArrayItem> staticFieldInitialValues;

    private ArrayList<EncodedValue> staticFieldInitialValuesList;

    private final DexFile dexFile;

    public ClassDefItem(DexFile dexFile, int index) {
        super(index);

        this.dexFile = dexFile;

        fields = new Field[] {
                classType = new IndexedItemReference<TypeIdItem>(dexFile.TypeIdsSection, new IntegerField()),
                accessFlags = new IntegerField(),
                superclassType = new IndexedItemReference<TypeIdItem>(dexFile.TypeIdsSection, new IntegerField()),
                classInterfacesList = new OffsettedItemReference<TypeListItem>(dexFile.TypeListsSection, new IntegerField()),
                sourceFile = new IndexedItemReference<StringIdItem>(dexFile.StringIdsSection, new IntegerField()),
                classAnnotations = new OffsettedItemReference<AnnotationDirectoryItem>(dexFile.AnnotationDirectoriesSection, new IntegerField()),
                classData = new OffsettedItemReference<ClassDataItem>(dexFile.ClassDataSection, new IntegerField()),
                staticFieldInitialValues = new OffsettedItemReference<EncodedArrayItem>(dexFile.EncodedArraysSection, new IntegerField())
        };
    }

    public ClassDefItem(DexFile dexFile,
                        TypeIdItem classType,
                        int accessFlags,
                        TypeIdItem superType,
                        TypeListItem implementsList,
                        StringIdItem source,
                        ClassDataItem classDataItem) {
        super(-1);

        this.dexFile = dexFile;

        fields = new Field[] {
                this.classType = new IndexedItemReference<TypeIdItem>(dexFile, classType, new IntegerField()),
                this.accessFlags = new IntegerField(accessFlags),
                superclassType = new IndexedItemReference<TypeIdItem>(dexFile, superType, new IntegerField()),
                classInterfacesList = new OffsettedItemReference<TypeListItem>(dexFile, implementsList, new IntegerField()),
                sourceFile = new IndexedItemReference<StringIdItem>(dexFile, source, new IntegerField()),
                classAnnotations = new OffsettedItemReference<AnnotationDirectoryItem>(dexFile.AnnotationDirectoriesSection, new IntegerField()),
                classData = new OffsettedItemReference<ClassDataItem>(dexFile, classDataItem, new IntegerField()),
                staticFieldInitialValues = new OffsettedItemReference<EncodedArrayItem>(dexFile.EncodedArraysSection, new IntegerField())
        };
    }

    public TypeIdItem getSuperclass() {
        return superclassType.getReference();
    }

    public TypeIdItem getClassType() {
        return classType.getReference();
    }

    protected int getAlignment() {
        return 4;
    }

    protected Field[] getFields() {
        return fields;
    }

    public ItemType getItemType() {
        return ItemType.TYPE_CLASS_DEF_ITEM;
    }

    public String getClassName() {
        return classType.getReference().toString();
    }

    public String toString() {
        return getClassName();
    }

    public int hashCode() {
        return classType.getReference().hashCode(); 
    }

    public boolean equals(Object o) {
        if (!(o instanceof ClassDefItem)) {
            return false;
        }
        ClassDefItem other = (ClassDefItem)o;
        return classType.equals(other.classType);
    }

    public int compareTo(ClassDefItem o) {
        //sorting is implemented in SortClassDefItemSection, so this class doesn't
        //need an implementation of compareTo
        return 0;
    }

    public void addField(ClassDataItem.EncodedField encodedField, EncodedValue initialValue) {
        //fields are added in ClassDefItem instead of ClassDataItem because we need to grab
        //the static initializers for StaticFieldInitialValues
        if (!encodedField.isStatic() && initialValue != null) {
            throw new RuntimeException("Initial values are only allowed for static fields.");
        }

        ClassDataItem classDataItem = this.classData.getReference();

        int fieldIndex = classDataItem.addField(encodedField);
        if (initialValue != null) {
            if (staticFieldInitialValuesList == null) {
                staticFieldInitialValuesList = new ArrayList<EncodedValue>();

                EncodedArrayItem encodedArrayItem = new EncodedArrayItem(dexFile, staticFieldInitialValuesList);
                staticFieldInitialValues.setReference(encodedArrayItem);
            }

            //All static fields before this one must have an initial value. Add any default values as needed
            for (int i=staticFieldInitialValuesList.size(); i < fieldIndex; i++) {
                ClassDataItem.EncodedField staticField = classDataItem.getStaticFieldAtIndex(i);
                EncodedValueSubField subField = TypeUtils.makeDefaultValueForType(dexFile, staticField.getField().getFieldType().toString());
                EncodedValue encodedValue = new EncodedValue(dexFile, subField);
                staticFieldInitialValuesList.add(i, encodedValue);
            }

            staticFieldInitialValuesList.add(fieldIndex, initialValue);
        }
    }

    public void setAnnotations(AnnotationDirectoryItem annotations) {
        this.classAnnotations.setReference(annotations);
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
        private final HashMap<TypeIdItem, ClassDefItem> classDefsByType = new HashMap<TypeIdItem, ClassDefItem>();

        private int currentIndex = 0;
        private int currentOffset;

        public ClassDefPlacer(IndexedSection<ClassDefItem> section) {
            this.section = section;

            for (ClassDefItem classDefItem: section.items) {
                TypeIdItem typeIdItem = classDefItem.classType.getReference();
                classDefsByType.put(typeIdItem, classDefItem);
            }
        }

        public int placeSection(int offset) {
            currentOffset = offset;
            for (ClassDefItem classDefItem: section.items) {
                placeClass(classDefItem);
            }

            for (ClassDefItem classDefItem: classDefsByType.values()) {
                section.items.set(classDefItem.getIndex(), classDefItem);
            }

            return currentOffset;
        }

        private void placeClass(ClassDefItem classDefItem) {
            if (!classDefItem.isPlaced()) {
                TypeIdItem superType = classDefItem.superclassType.getReference();
                ClassDefItem superClassDefItem = classDefsByType.get(superType);

                if (superClassDefItem != null) {
                    placeClass(superClassDefItem);
                }

                TypeListItem interfaces = classDefItem.classInterfacesList.getReference();

                if (interfaces != null) {
                    for (TypeIdItem interfaceType: interfaces.getTypes()) {
                        ClassDefItem interfaceClass = classDefsByType.get(interfaceType);
                        if (interfaceClass != null) {
                            placeClass(interfaceClass);
                        }
                    }
                }

                currentOffset = classDefItem.place(currentIndex++, currentOffset);
            }
        }

    }
}
