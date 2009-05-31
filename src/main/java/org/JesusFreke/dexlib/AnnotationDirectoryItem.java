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

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

//TODO: fix field names in dex-format.html and submit
public class AnnotationDirectoryItem extends OffsettedItem<AnnotationDirectoryItem> {
    private final ArrayList<FieldAnnotation> fieldAnnotationList = new ArrayList<FieldAnnotation>();
    private final ArrayList<MethodAnnotation> methodAnnotationList = new ArrayList<MethodAnnotation>();
    private final ArrayList<ParameterAnnotation> parameterAnnotationList = new ArrayList<ParameterAnnotation>();

    private final OffsettedItemReference<AnnotationSetItem> classAnnotationsReferenceField;
    private final ListSizeField annotatedFieldsCountField;
    private final ListSizeField annotatedMethodsCountField;
    private final ListSizeField annotatedParametersCountField;
    private final FieldListField<FieldAnnotation> fieldAnnotationListField;
    private final FieldListField<MethodAnnotation> methodAnnotationListField;
    private final FieldListField<ParameterAnnotation> parameterAnnotationListField;
                                             
    public AnnotationDirectoryItem(final DexFile dexFile, int offset) {
        super(offset);

        fields = new Field[] {
                classAnnotationsReferenceField = new OffsettedItemReference<AnnotationSetItem>(
                        dexFile.AnnotationSetsSection, new IntegerField(null), "class_annotations_off"),
                annotatedFieldsCountField = new ListSizeField(fieldAnnotationList, new IntegerField("fields_size")),
                annotatedMethodsCountField = new ListSizeField(methodAnnotationList,
                        new IntegerField("annotated_methods_size")),
                annotatedParametersCountField = new ListSizeField(parameterAnnotationList,
                        new IntegerField("annotated_parameters_size")),
                fieldAnnotationListField = new FieldListField<FieldAnnotation>(fieldAnnotationList,
                        "field_annotations") {
                    protected FieldAnnotation make() {
                        return new FieldAnnotation(dexFile);
                    }
                },
                methodAnnotationListField = new FieldListField<MethodAnnotation>(methodAnnotationList,
                        "method_annotations") {
                    protected MethodAnnotation make() {
                        return new MethodAnnotation(dexFile);
                    }
                },
                parameterAnnotationListField = new FieldListField<ParameterAnnotation>(parameterAnnotationList,
                        "parameter_annotations") {
                    protected ParameterAnnotation make() {
                        return new ParameterAnnotation(dexFile);
                    }
                }
        };
    }
    
    public AnnotationDirectoryItem(final DexFile dexFile,
                                   AnnotationSetItem classAnnotations,
                                   List<FieldAnnotation> fieldAnnotations,
                                   List<MethodAnnotation> methodAnnotations,
                                   List<ParameterAnnotation> parameterAnnotations) {
        this(dexFile, -1);

        classAnnotationsReferenceField.setReference(classAnnotations);

        if (fieldAnnotationListField != null) {
            fieldAnnotationList.addAll(fieldAnnotations);
        }

        if (methodAnnotationListField != null) {
            methodAnnotationList.addAll(methodAnnotations);
        }

        if (parameterAnnotations != null) {
            parameterAnnotationList.addAll(parameterAnnotations);
        }
    }

    public int place(int index, int offset)
    {
        Collections.sort(fieldAnnotationList);
        Collections.sort(methodAnnotationList);
        Collections.sort(parameterAnnotationList);
        return super.place(index, offset);
    }

    protected int getAlignment() {
        return 4;
    }

    public ItemType getItemType() {
        return ItemType.TYPE_ANNOTATIONS_DIRECTORY_ITEM;
    }

    public String getConciseIdentity() {
        return "annotation_directory_item @0x" + Integer.toHexString(getOffset());
    }

    public static class FieldAnnotation extends CompositeField<FieldAnnotation>
            implements Comparable<FieldAnnotation> {
        private final IndexedItemReference<FieldIdItem> fieldReferenceField;
        private final OffsettedItemReference<AnnotationSetItem> annotationSetReferenceField;

        public FieldAnnotation(DexFile dexFile) {
            super("field_annotation");
            fields = new Field[] {
                    fieldReferenceField = new IndexedItemReference<FieldIdItem>(dexFile.FieldIdsSection,
                            new IntegerField(null), "field_idx"),
                    annotationSetReferenceField = new OffsettedItemReference<AnnotationSetItem>(
                            dexFile.AnnotationSetsSection, new IntegerField(null), "annotations_off")
            };
        }

        public FieldAnnotation(DexFile dexFile, FieldIdItem field, AnnotationSetItem annotationSet) {
            this(dexFile);
            this.fieldReferenceField.setReference(field);
            this.annotationSetReferenceField.setReference(annotationSet);
        }

        public int compareTo(FieldAnnotation o) {
            return ((Integer) fieldReferenceField.getReference().getIndex()).compareTo(
                    o.fieldReferenceField.getReference().getIndex());
        }
    }

    public static class MethodAnnotation extends CompositeField<MethodAnnotation>
            implements Comparable<MethodAnnotation> {
        private final IndexedItemReference<MethodIdItem> method;
        private final OffsettedItemReference<AnnotationSetItem> annotationSet;

        public MethodAnnotation(DexFile dexFile) {
            super("method_annotation");
            fields = new Field[] {
                    method = new IndexedItemReference<MethodIdItem>(dexFile.MethodIdsSection,
                            new IntegerField(null), "method_idx"),
                    annotationSet = new OffsettedItemReference<AnnotationSetItem>(dexFile.AnnotationSetsSection,
                            new IntegerField(null), "annotations_off")
            };
        }

        public MethodAnnotation(DexFile dexFile, MethodIdItem method, AnnotationSetItem annotationSet) {
            this(dexFile);
            this.method.setReference(method);
            this.annotationSet.setReference(annotationSet);
        }

        public int compareTo(MethodAnnotation o) {
            return ((Integer)method.getReference().getIndex()).compareTo(o.method.getReference().getIndex());
        }
    }

    public static class ParameterAnnotation extends CompositeField<ParameterAnnotation>
            implements Comparable<ParameterAnnotation> {
        private final IndexedItemReference<MethodIdItem> method;
        private final OffsettedItemReference<AnnotationSetRefList> parameterAnnotations;
        
        public ParameterAnnotation(DexFile dexFile) {
            super("parameter_annotation");              
            fields = new Field[] {
                    method = new IndexedItemReference<MethodIdItem>(dexFile.MethodIdsSection,
                            new IntegerField(null), "method_idx"),
                    parameterAnnotations = new OffsettedItemReference<AnnotationSetRefList>(
                            dexFile.AnnotationSetRefListsSection, new IntegerField(null), "annotations_off")
            };
        }

        public ParameterAnnotation(DexFile dexFile, MethodIdItem method, AnnotationSetRefList parameterAnnotations) {
            this(dexFile);
            this.method.setReference(method);
            this.parameterAnnotations.setReference(parameterAnnotations);
        }

        public int compareTo(ParameterAnnotation o) {
            return ((Integer)method.getReference().getIndex()).compareTo(o.method.getReference().getIndex());
        }
    }
}
