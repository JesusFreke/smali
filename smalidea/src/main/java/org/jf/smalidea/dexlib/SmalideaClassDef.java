/*
 * Copyright 2016, Google Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 * Neither the name of Google Inc. nor the names of its
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

package org.jf.smalidea.dexlib;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.base.reference.BaseTypeReference;
import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.Method;
import org.jf.smalidea.util.NameUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class SmalideaClassDef extends BaseTypeReference implements ClassDef {
    private final PsiClass psiClass;

    public SmalideaClassDef(PsiClass psiClass) {
        this.psiClass = psiClass;
    }

    @Override public int getAccessFlags() {
        PsiModifierList modifierList = psiClass.getModifierList();
        int flags = 0;

        if (modifierList == null) {
            return flags;
        }

        if (modifierList.hasModifierProperty("public")) {
            flags |= AccessFlags.PUBLIC.getValue();
        }

        if (modifierList.hasModifierProperty("final")) {
            flags |= AccessFlags.FINAL.getValue();
        }

        if (modifierList.hasModifierProperty("abstract")) {
            flags |= AccessFlags.ABSTRACT.getValue();
        }

        if (psiClass.isInterface()) {
            flags |= AccessFlags.INTERFACE.getValue();
        }

        if (psiClass.isEnum()) {
            flags |= AccessFlags.ENUM.getValue();
        }

        if (psiClass.isAnnotationType()) {
            flags |= AccessFlags.ANNOTATION.getValue();
        }

        return flags;
    }

    @Nonnull @Override public String getType() {
        return NameUtils.javaToSmaliType(psiClass);
    }

    @Nullable @Override public String getSuperclass() {
        PsiClass superClass = psiClass.getSuperClass();
        if (superClass == null) {
            return null;
        }
        return NameUtils.javaToSmaliType(superClass);
    }

    @Nonnull @Override public List<String> getInterfaces() {
        List<String> interfaceList = Lists.newArrayList();
        PsiClass[] interfaces = psiClass.getInterfaces();
        if (interfaces == null) {
            return interfaceList;
        }

        for (PsiClass psiClass: interfaces) {
            interfaceList.add(NameUtils.javaToSmaliType(psiClass));
        }

        return interfaceList;
    }

    @Nullable @Override public String getSourceFile() {
        return null;
    }

    @Nonnull @Override public Set<? extends Annotation> getAnnotations() {
        return ImmutableSet.of();
    }

    @Nonnull @Override public Iterable<? extends Field> getStaticFields() {
        return Iterables.transform(
                Iterables.filter(Arrays.asList(psiClass.getFields()), new Predicate<PsiField>() {
                    @Override public boolean apply(PsiField psiField) {
                        PsiModifierList modifierList = psiField.getModifierList();
                        if (modifierList == null) {
                            return false;
                        }
                        return modifierList.hasModifierProperty("static");
                    }
                }),
                new Function<PsiField, Field>() {
                    @Nullable @Override public Field apply(@Nullable PsiField psiField) {
                        return new SmalideaField(psiField);
                    }
                });
    }

    @Nonnull @Override public Iterable<? extends Field> getInstanceFields() {
        return Iterables.transform(
                Iterables.filter(Arrays.asList(psiClass.getFields()), new Predicate<PsiField>() {
                    @Override public boolean apply(PsiField psiField) {
                        PsiModifierList modifierList = psiField.getModifierList();
                        if (modifierList == null) {
                            return true;
                        }
                        return !modifierList.hasModifierProperty("static");
                    }
                }),
                new Function<PsiField, Field>() {
                    @Nullable @Override public Field apply(@Nullable PsiField psiField) {
                        return new SmalideaField(psiField);
                    }
                });
    }

    @Nonnull @Override public Iterable<? extends Field> getFields() {
        return Iterables.concat(getStaticFields(), getInstanceFields());
    }

    @Nonnull @Override public Iterable<? extends Method> getDirectMethods() {
        return Iterables.transform(
                Iterables.filter(
                        Iterables.concat(
                            Arrays.asList(psiClass.getConstructors()),
                            Arrays.asList(psiClass.getMethods())),
                        new Predicate<PsiMethod>() {
                    @Override public boolean apply(PsiMethod psiMethod) {
                        PsiModifierList modifierList = psiMethod.getModifierList();
                        return modifierList.hasModifierProperty("static") ||
                                modifierList.hasModifierProperty("private") ||
                                modifierList.hasModifierProperty("constructor");
                    }
                }),
                new Function<PsiMethod, Method>() {
                    @Nullable @Override public Method apply(PsiMethod psiMethod) {
                        return new SmalideaMethod(psiMethod);
                    }
                });
    }

    @Nonnull @Override public Iterable<? extends Method> getVirtualMethods() {
        return Iterables.transform(
                Iterables.filter(Arrays.asList(psiClass.getMethods()), new Predicate<PsiMethod>() {
                    @Override public boolean apply(PsiMethod psiMethod) {
                        PsiModifierList modifierList = psiMethod.getModifierList();
                        return !modifierList.hasModifierProperty("static") &&
                                !modifierList.hasModifierProperty("private") &&
                                !modifierList.hasModifierProperty("constructor");
                    }
                }),
                new Function<PsiMethod, Method>() {
                    @Nullable @Override public Method apply(PsiMethod psiMethod) {
                        return new SmalideaMethod(psiMethod);
                    }
                });
    }

    @Nonnull @Override public Iterable<? extends Method> getMethods() {
        return Iterables.concat(getDirectMethods(), getVirtualMethods());
    }
}
