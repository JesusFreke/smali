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

package org.jf.dexlib2.immutable;

import com.google.common.collect.ImmutableList;
import org.jf.dexlib2.base.reference.BaseTypeReference;
import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.Method;
import org.jf.util.ImmutableListConverter;
import org.jf.util.ImmutableListUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

public class ImmutableClassDef extends BaseTypeReference implements ClassDef {
    @Nonnull public final String type;
    public final int accessFlags;
    @Nullable public final String superclass;
    @Nonnull public final ImmutableList<String> interfaces;
    @Nullable public final String sourceFile;
    @Nonnull public final ImmutableList<? extends ImmutableAnnotation> annotations;
    @Nonnull public final ImmutableList<? extends ImmutableField> fields;
    @Nonnull public final ImmutableList<? extends ImmutableMethod> methods;

    public ImmutableClassDef(@Nonnull String type,
                             int accessFlags,
                             @Nullable String superclass,
                             @Nullable List<String> interfaces,
                             @Nullable String sourceFile,
                             @Nullable Collection<? extends Annotation> annotations,
                             @Nullable Collection<? extends Field> fields,
                             @Nullable Collection<? extends Method> methods) {
        this.type = type;
        this.accessFlags = accessFlags;
        this.superclass = superclass;
        this.interfaces = interfaces==null ? ImmutableList.<String>of() : ImmutableList.copyOf(interfaces);
        this.sourceFile = sourceFile;
        this.annotations = ImmutableAnnotation.immutableListOf(annotations);
        this.fields = ImmutableField.immutableListOf(fields);
        this.methods = ImmutableMethod.immutableListOf(methods);
    }

    public ImmutableClassDef(@Nonnull String type,
                             int accessFlags,
                             @Nullable String superclass,
                             @Nullable ImmutableList<String> interfaces,
                             @Nullable String sourceFile,
                             @Nullable ImmutableList<? extends ImmutableAnnotation> annotations,
                             @Nullable ImmutableList<? extends ImmutableField> fields,
                             @Nullable ImmutableList<? extends ImmutableMethod> methods) {
        this.type = type;
        this.accessFlags = accessFlags;
        this.superclass = superclass;
        this.interfaces = ImmutableListUtils.nullToEmptyList(interfaces);
        this.sourceFile = sourceFile;
        this.annotations = ImmutableListUtils.nullToEmptyList(annotations);
        this.fields = ImmutableListUtils.nullToEmptyList(fields);
        this.methods = ImmutableListUtils.nullToEmptyList(methods);
    }

    public static ImmutableClassDef of(ClassDef classDef) {
        if (classDef instanceof ImmutableClassDef) {
            return (ImmutableClassDef)classDef;
        }
        return new ImmutableClassDef(
                classDef.getType(),
                classDef.getAccessFlags(),
                classDef.getSuperclass(),
                classDef.getInterfaces(),
                classDef.getSourceFile(),
                classDef.getAnnotations(),
                classDef.getFields(),
                classDef.getMethods());
    }

    @Nonnull @Override public String getType() { return type; }
    @Override public int getAccessFlags() { return accessFlags; }
    @Nullable @Override public String getSuperclass() { return superclass; }
    @Nonnull @Override public ImmutableList<String> getInterfaces() { return interfaces; }
    @Nullable @Override public String getSourceFile() { return sourceFile; }
    @Nonnull @Override public ImmutableList<? extends ImmutableAnnotation> getAnnotations() { return annotations; }
    @Nonnull @Override public ImmutableList<? extends ImmutableField> getFields() { return fields; }
    @Nonnull @Override public ImmutableList<? extends ImmutableMethod> getMethods() { return methods; }

    @Nonnull
    public static ImmutableList<ImmutableClassDef> immutableListOf(@Nullable List<? extends ClassDef> list) {
        return CONVERTER.convert(list);
    }

    private static final ImmutableListConverter<ImmutableClassDef, ClassDef> CONVERTER =
            new ImmutableListConverter<ImmutableClassDef, ClassDef>() {
                @Override
                protected boolean isImmutable(ClassDef item) {
                    return item instanceof ImmutableClassDef;
                }

                @Override
                protected ImmutableClassDef makeImmutable(ClassDef item) {
                    return ImmutableClassDef.of(item);
                }
            };
}
