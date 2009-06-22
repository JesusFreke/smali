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

package org.jf.dexlib;

import org.jf.dexlib.EncodedValue.EncodedValue;
import org.jf.dexlib.EncodedValue.EncodedValueSubField;
import org.jf.dexlib.Util.TypeUtils;
import org.jf.dexlib.Util.Input;

import java.util.*;

public class ClassDefItem extends IndexedItem<ClassDefItem> {
    private final IndexedItemReference<TypeIdItem> classTypeReferenceField;
    private final IntegerField accessFlagsField;
    private final IndexedItemReference<TypeIdItem> superclassTypeReferenceField;
    private final OffsettedItemReference<TypeListItem> classInterfacesListReferenceField;
    private final IndexedItemReference<StringIdItem> sourceFileReferenceField;
    private final OffsettedItemReference<AnnotationDirectoryItem> classAnnotationsReferenceField;
    private final OffsettedItemReference<ClassDataItem> classDataReferenceField;
    private final OffsettedItemReference<EncodedArrayItem> staticFieldInitialValuesReferenceField;

    private ArrayList<EncodedValue> staticFieldInitialValuesList;

    private final DexFile dexFile;

    public ClassDefItem(DexFile dexFile, int index) {
        super(index);

        this.dexFile = dexFile;

        fields = new Field[] {
                classTypeReferenceField = new IndexedItemReference<TypeIdItem>(dexFile.TypeIdsSection,
                        new IntegerField(null), "class_idx"),
                //TODO: add annotated output showing the flags
                accessFlagsField = new IntegerField("access_flags:"),
                superclassTypeReferenceField = new IndexedItemReference<TypeIdItem>(dexFile.TypeIdsSection,
                        new IntegerField(null), "superclass_idx"),
                classInterfacesListReferenceField = new OffsettedItemReference<TypeListItem>(dexFile.TypeListsSection,
                        new IntegerField(null), "interfaces_off"),
                sourceFileReferenceField = new IndexedItemReference<StringIdItem>(dexFile.StringIdsSection,
                        new IntegerField(null), "source_file_off"),
                classAnnotationsReferenceField = new OffsettedItemReference<AnnotationDirectoryItem>(
                    dexFile.AnnotationDirectoriesSection, new IntegerField(null), "annotations_off"),
                classDataReferenceField = new OffsettedItemReference<ClassDataItem>(dexFile.ClassDataSection,
                        new IntegerField(null), "class_data_off"),
                staticFieldInitialValuesReferenceField = new OffsettedItemReference<EncodedArrayItem>(
                        dexFile.EncodedArraysSection, new IntegerField(null), "static_values_off")
        };
    }

    public ClassDefItem(DexFile dexFile,
                        TypeIdItem classType,
                        int accessFlags,
                        TypeIdItem superType,
                        TypeListItem implementsList,
                        StringIdItem source,
                        ClassDataItem classDataItem) {
        this(dexFile, -1);

        classTypeReferenceField.setReference(classType);
        accessFlagsField.cacheValue(accessFlags);
        superclassTypeReferenceField.setReference(superType);
        classInterfacesListReferenceField.setReference(implementsList);
        sourceFileReferenceField.setReference(source);        
        classDataReferenceField.setReference(classDataItem);

        if (classDataItem != null) {
            classDataItem.setParent(this);
        }
    }

    public TypeIdItem getSuperclass() {
        return superclassTypeReferenceField.getReference();
    }

    public TypeIdItem getClassType() {
        return classTypeReferenceField.getReference();
    }

    protected int getAlignment() {
        return 4;
    }

    public ItemType getItemType() {
        return ItemType.TYPE_CLASS_DEF_ITEM;
    }

    public String getClassName() {
        return classTypeReferenceField.getReference().getTypeDescriptor();
    }

    public String getSourceFile() {
        StringIdItem stringIdItem = sourceFileReferenceField.getReference();
        if (stringIdItem == null) {
            return null;
        }
        return stringIdItem.getStringValue();
    }

    public int getAccessFlags() {
        return accessFlagsField.getCachedValue();
    }

    public List<TypeIdItem> getInterfaces() {
        TypeListItem interfaceList = classInterfacesListReferenceField.getReference();
        if (interfaceList == null) {
            return null;
        }

        return interfaceList.getTypes();
    }

    public ClassDataItem getClassData() {
        return classDataReferenceField.getReference();
    }

    public AnnotationDirectoryItem getAnnotationDirectory() {
        return classAnnotationsReferenceField.getReference();
    }

    public String getConciseIdentity() {
        return "class_def_item: " + getClassName();
    }

    public int hashCode() {
        return classTypeReferenceField.getReference().hashCode();
    }

    public boolean equals(Object o) {
        if (!(o instanceof ClassDefItem)) {
            return false;
        }
        ClassDefItem other = (ClassDefItem)o;
        return classTypeReferenceField.equals(other.classTypeReferenceField);
    }

    public EncodedArrayItem getStaticInitializers() {
        return staticFieldInitialValuesReferenceField.getReference();        
    }

    public int compareTo(ClassDefItem o) {
        //The actual sorting for this class is implemented in SortClassDefItemSection.
        //This method is just used for sorting the associated ClassDataItem items, so
        //we can just do the comparison based on the offsets of the items
        return ((Integer)this.offset).compareTo(o.offset);
    }

    public void addField(ClassDataItem.EncodedField encodedField, EncodedValue initialValue) {
        //fields are added in ClassDefItem instead of ClassDataItem because we need to grab
        //the static initializers for StaticFieldInitialValues
        if (!encodedField.isStatic() && initialValue != null) {
            throw new RuntimeException("Initial values are only allowed for static fields.");
        }

        ClassDataItem classDataItem = this.classDataReferenceField.getReference();

        int fieldIndex = classDataItem.addField(encodedField);
        if (initialValue != null) {
            if (staticFieldInitialValuesList == null) {
                staticFieldInitialValuesList = new ArrayList<EncodedValue>();

                EncodedArrayItem encodedArrayItem = new EncodedArrayItem(dexFile, staticFieldInitialValuesList);
                staticFieldInitialValuesReferenceField.setReference(encodedArrayItem);
            }

            //All static fields before this one must have an initial value. Add any default values as needed
            for (int i=staticFieldInitialValuesList.size(); i < fieldIndex; i++) {
                ClassDataItem.EncodedField staticField = classDataItem.getStaticFields().get(i);
                EncodedValueSubField subField = TypeUtils.makeDefaultValueForType(dexFile,
                        staticField.getField().getFieldType().getTypeDescriptor());
                EncodedValue encodedValue = new EncodedValue(dexFile, subField);
                staticFieldInitialValuesList.add(i, encodedValue);
            }

            staticFieldInitialValuesList.add(fieldIndex, initialValue);
        } else if (staticFieldInitialValuesList != null && encodedField.isStatic() && fieldIndex < staticFieldInitialValuesList.size()) {
            EncodedValueSubField subField = TypeUtils.makeDefaultValueForType(dexFile,
                    encodedField.getField().getFieldType().getTypeDescriptor());
            EncodedValue encodedValue = new EncodedValue(dexFile, subField);
            staticFieldInitialValuesList.add(fieldIndex, encodedValue);
        }
    }

    public void setAnnotations(AnnotationDirectoryItem annotations) {
        this.classAnnotationsReferenceField.setReference(annotations);
        annotations.setParent(this);
    }

    public void setClassDataItem(ClassDataItem classDataItem) {
        this.classDataReferenceField.setReference(classDataItem);
        if (classDataItem != null) {
            classDataItem.setParent(this);
        }
    }

    public void readFrom(Input in, int index) {
        super.readFrom(in, index);

        ClassDataItem classDataItem = classDataReferenceField.getReference();
        if (classDataItem != null) {
            classDataItem.setParent(this);
        }

        AnnotationDirectoryItem annotationDirectoryItem = classAnnotationsReferenceField.getReference();
        if (annotationDirectoryItem != null) {
            annotationDirectoryItem.setParent(this);
        }
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
                TypeIdItem typeIdItem = classDefItem.classTypeReferenceField.getReference();
                classDefsByType.put(typeIdItem, classDefItem);
            }
        }

        public int placeSection(int offset) {
            currentOffset = offset;

            //presort the list, to guarantee a unique ordering
            Collections.sort(section.items, new Comparator<ClassDefItem>() {
                public int compare(ClassDefItem classDefItem, ClassDefItem classDefItem1) {
                    return classDefItem.getClassType().compareTo(classDefItem1.getClassType());
                }
            });

            for (ClassDefItem classDefItem: section.items) {
                classDefItem.offset = -1;
            }

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
                TypeIdItem superType = classDefItem.superclassTypeReferenceField.getReference();
                ClassDefItem superClassDefItem = classDefsByType.get(superType);

                if (superClassDefItem != null) {
                    placeClass(superClassDefItem);
                }

                TypeListItem interfaces = classDefItem.classInterfacesListReferenceField.getReference();

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
