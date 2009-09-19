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

package org.jf.baksmali.Adaptors;

import org.jf.dexlib.EncodedValue.EncodedValue;
import org.jf.dexlib.*;
import org.jf.dexlib.Util.AccessFlags;
import org.jf.dexlib.Util.SparseArray;
import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;

import java.util.*;

public class ClassDefinition {
    private StringTemplateGroup stg;
    private ClassDefItem classDefItem;
    private ClassDataItem classDataItem;

    private SparseArray<AnnotationSetItem> methodAnnotationsMap;
    private SparseArray<AnnotationSetItem> fieldAnnotationsMap;
    private SparseArray<AnnotationSetRefList> parameterAnnotationsMap;

    public ClassDefinition(StringTemplateGroup stg, ClassDefItem classDefItem) {
        this.stg = stg;
        this.classDefItem = classDefItem;
        this.classDataItem = classDefItem.getClassData();
        buildAnnotationMaps();
    }

    public StringTemplate makeTemplate() {
        StringTemplate template = stg.getInstanceOf("smaliFile");

        template.setAttribute("AccessFlags", getAccessFlags());
        template.setAttribute("ClassType", classDefItem.getClassType().getTypeDescriptor());
        template.setAttribute("SuperType", getSuperType());
        template.setAttribute("SourceFile", getSourceFile());
        template.setAttribute("Interfaces", getInterfaces());
        template.setAttribute("Annotations", getAnnotations());
        template.setAttribute("StaticFields", getStaticFields());
        template.setAttribute("InstanceFields", getInstanceFields());
        template.setAttribute("DirectMethods", getDirectMethods());
        template.setAttribute("VirtualMethods", getVirtualMethods());

        return template;
    }

    private void buildAnnotationMaps() {
        AnnotationDirectoryItem annotationDirectory = classDefItem.getAnnotations();
        if (annotationDirectory == null) {
            return;
        }

        methodAnnotationsMap = new SparseArray<AnnotationSetItem>(annotationDirectory.getMethodAnnotationCount());
        annotationDirectory.iterateMethodAnnotations(new AnnotationDirectoryItem.MethodAnnotationIteratorDelegate() {
            public void processMethodAnnotations(MethodIdItem method, AnnotationSetItem methodAnnotations) {
                methodAnnotationsMap.put(method.getIndex(), methodAnnotations);
            }
        });

        fieldAnnotationsMap = new SparseArray<AnnotationSetItem>(annotationDirectory.getFieldAnnotationCount());
        annotationDirectory.iterateFieldAnnotations(new AnnotationDirectoryItem.FieldAnnotationIteratorDelegate() {
            public void processFieldAnnotations(FieldIdItem field, AnnotationSetItem fieldAnnotations) {
                fieldAnnotationsMap.put(field.getIndex(), fieldAnnotations);
            }
        });

        parameterAnnotationsMap = new SparseArray<AnnotationSetRefList>(
                annotationDirectory.getParameterAnnotationCount());
        annotationDirectory.iterateParameterAnnotations(
          new AnnotationDirectoryItem.ParameterAnnotationIteratorDelegate() {
            public void processParameterAnnotations(MethodIdItem method, AnnotationSetRefList parameterAnnotations) {
                parameterAnnotationsMap.put(method.getIndex(), parameterAnnotations);
            }
        });
    }

    private List<String> getAccessFlags() {
        List<String> accessFlags = new ArrayList<String>();

        for (AccessFlags accessFlag: AccessFlags.getAccessFlagsForClass(classDefItem.getAccessFlags())) {
            accessFlags.add(accessFlag.toString());
        }

        return accessFlags;
    }


    private String getSuperType() {
        TypeIdItem superClass = classDefItem.getSuperclass();
        if (superClass != null) {
            return superClass.getTypeDescriptor();
        }
        return null;
    }

    private String getSourceFile() {
        StringIdItem sourceFile = classDefItem.getSourceFile();

        if (sourceFile == null) {
            return null;
        }
        return classDefItem.getSourceFile().getStringValue();
    }

    private List<String> getInterfaces() {
        List<String> interfaces = new ArrayList<String>();

        TypeListItem interfaceList = classDefItem.getInterfaces();

        if (interfaceList != null) {
            for (TypeIdItem typeIdItem: interfaceList.getTypes()) {
                interfaces.add(typeIdItem.getTypeDescriptor());
            }
        }
        
        return interfaces;
    }

    private List<StringTemplate> getAnnotations() {
        AnnotationDirectoryItem annotationDirectory = classDefItem.getAnnotations();
        if (annotationDirectory == null) {
            return null;
        }

        AnnotationSetItem annotationSet = annotationDirectory.getClassAnnotations();
        if (annotationSet == null) {
            return null;
        }

        List<StringTemplate> annotations = new ArrayList<StringTemplate>();

        for (AnnotationItem annotationItem: annotationSet.getAnnotations()) {
            annotations.add(AnnotationAdaptor.makeTemplate(stg, annotationItem));
        }
        return annotations;
    }

    private List<StringTemplate> getStaticFields() {
        List<StringTemplate> staticFields = new ArrayList<StringTemplate>();

        if (classDataItem != null) {
            EncodedArrayItem encodedStaticInitializers = classDefItem.getStaticFieldInitializers();

            EncodedValue[] staticInitializers;
            if (encodedStaticInitializers != null) {
                staticInitializers = encodedStaticInitializers.getEncodedArray().values;
            } else {
                staticInitializers = new EncodedValue[0];
            }

            int i=0;
            for (ClassDataItem.EncodedField field: classDataItem.getStaticFields()) {
                EncodedValue encodedValue = null;
                if (i < staticInitializers.length) {
                    encodedValue = staticInitializers[i];
                }
                AnnotationSetItem annotationSet = fieldAnnotationsMap.get(field.field.getIndex());

                staticFields.add(FieldDefinition.createTemplate(stg, field, encodedValue, annotationSet));
                i++;
            }
        }
        return staticFields;
    }

    private List<StringTemplate> getInstanceFields() {
        List<StringTemplate> instanceFields = new ArrayList<StringTemplate>();

        if (classDataItem != null) {
            for (ClassDataItem.EncodedField field: classDataItem.getInstanceFields()) {
                AnnotationSetItem annotationSet = fieldAnnotationsMap.get(field.field.getIndex());
                instanceFields.add(FieldDefinition.createTemplate(stg, field, annotationSet));
            }
        }

        return instanceFields;       
    }

    private List<StringTemplate> getDirectMethods() {
        List<StringTemplate> directMethods = new ArrayList<StringTemplate>();

        if (classDataItem != null) {
            for (ClassDataItem.EncodedMethod method: classDataItem.getDirectMethods()) {
                AnnotationSetItem annotationSet = methodAnnotationsMap.get(method.method.getIndex());
                AnnotationSetRefList parameterAnnotationList = parameterAnnotationsMap.get(method.method.getIndex());
                directMethods.add(MethodDefinition.makeTemplate(stg, method, annotationSet, parameterAnnotationList));
            }
        }

        return directMethods;
    }

    private List<StringTemplate> getVirtualMethods() {
        List<StringTemplate> virtualMethods = new ArrayList<StringTemplate>();

        if (classDataItem != null) {
            for (ClassDataItem.EncodedMethod method: classDataItem.getVirtualMethods()) {
                AnnotationSetItem annotationSet = methodAnnotationsMap.get(method.method.getIndex());
                AnnotationSetRefList parameterAnnotationList = parameterAnnotationsMap.get(method.method.getIndex());
                virtualMethods.add(MethodDefinition.makeTemplate(stg, method, annotationSet, parameterAnnotationList));
            }
        }

        return virtualMethods;
    }
}
