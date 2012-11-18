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

import com.google.common.collect.ImmutableSet;
import org.jf.dexlib2.base.reference.BaseTypeReference;
import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.Method;
import org.jf.util.ImmutableConverter;
import org.jf.util.ImmutableUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

public class ImmutableClassDef extends BaseTypeReference implements ClassDef {
    @Nonnull protected final String type;
    protected final int accessFlags;
    @Nullable protected final String superclass;
    @Nonnull protected final ImmutableSet<String> interfaces;
    @Nullable protected final String sourceFile;
    @Nonnull protected final ImmutableSet<? extends ImmutableAnnotation> annotations;
    @Nonnull protected final ImmutableSet<? extends ImmutableField> fields;
    @Nonnull protected final ImmutableSet<? extends ImmutableMethod> methods;

    public ImmutableClassDef(@Nonnull String type,
                             int accessFlags,
                             @Nullable String superclass,
                             @Nullable Collection<String> interfaces,
                             @Nullable String sourceFile,
                             @Nullable Collection<? extends Annotation> annotations,
                             @Nullable Collection<? extends Field> fields,
                             @Nullable Collection<? extends Method> methods) {
        this.type = type;
        this.accessFlags = accessFlags;
        this.superclass = superclass;
        this.interfaces = interfaces==null ? ImmutableSet.<String>of() : ImmutableSet.copyOf(interfaces);
        this.sourceFile = sourceFile;
        this.annotations = ImmutableAnnotation.immutableSetOf(annotations);
        this.fields = ImmutableField.immutableSetOf(fields);
        this.methods = ImmutableMethod.immutableSetOf(methods);
    }

    public ImmutableClassDef(@Nonnull String type,
                             int accessFlags,
                             @Nullable String superclass,
                             @Nullable ImmutableSet<String> interfaces,
                             @Nullable String sourceFile,
                             @Nullable ImmutableSet<? extends ImmutableAnnotation> annotations,
                             @Nullable ImmutableSet<? extends ImmutableField> fields,
                             @Nullable ImmutableSet<? extends ImmutableMethod> methods) {
        this.type = type;
        this.accessFlags = accessFlags;
        this.superclass = superclass;
        this.interfaces = ImmutableUtils.nullToEmptySet(interfaces);
        this.sourceFile = sourceFile;
        this.annotations = ImmutableUtils.nullToEmptySet(annotations);
        this.fields = ImmutableUtils.nullToEmptySet(fields);
        this.methods = ImmutableUtils.nullToEmptySet(methods);
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
    @Nonnull @Override public ImmutableSet<String> getInterfaces() { return interfaces; }
    @Nullable @Override public String getSourceFile() { return sourceFile; }
    @Nonnull @Override public ImmutableSet<? extends ImmutableAnnotation> getAnnotations() { return annotations; }
    @Nonnull @Override public ImmutableSet<? extends ImmutableField> getFields() { return fields; }
    @Nonnull @Override public ImmutableSet<? extends ImmutableMethod> getMethods() { return methods; }

    @Nonnull
    public static ImmutableSet<ImmutableClassDef> immutableSetOf(@Nullable Iterable<? extends ClassDef> iterable) {
        return CONVERTER.toSet(iterable);
    }

    private static final ImmutableConverter<ImmutableClassDef, ClassDef> CONVERTER =
            new ImmutableConverter<ImmutableClassDef, ClassDef>() {
                @Override
                protected boolean isImmutable(@Nonnull ClassDef item) {
                    return item instanceof ImmutableClassDef;
                }

                @Nonnull
                @Override
                protected ImmutableClassDef makeImmutable(@Nonnull ClassDef item) {
                    return ImmutableClassDef.of(item);
                }
            };
}
