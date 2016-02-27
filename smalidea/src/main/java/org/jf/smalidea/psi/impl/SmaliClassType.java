/*
 * Copyright 2014, Google Inc.
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

package org.jf.smalidea.psi.impl;

import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jf.smalidea.util.NameUtils;

public class SmaliClassType extends PsiClassType {
    private final PsiTypeElement element;

    public SmaliClassType(PsiTypeElement element) {
        this(element, LanguageLevel.JDK_1_5);
    }

    public SmaliClassType(PsiTypeElement element, LanguageLevel languageLevel) {
        super(languageLevel);
        this.element = element;
    }

    @Nullable
    @Override
    public PsiClass resolve() {
        PsiReference reference = element.getReference();
        if (reference == null) {
            return null;
        }
        PsiElement resolved = reference.resolve();
        if (resolved instanceof PsiClass) {
            return (PsiClass)resolved;
        }
        return null;
    }

    @Override
    public String getClassName() {
        PsiClass resolved = resolve();
        if (resolved != null) {
            return NameUtils.shortNameFromQualifiedName(resolved.getQualifiedName());
        }
        return NameUtils.shortNameFromQualifiedName(element.getText());
    }

    @NotNull
    @Override
    public PsiType[] getParameters() {
        // TODO: (generics) implement this
        return PsiType.EMPTY_ARRAY;
    }

    @NotNull
    @Override
    public ClassResolveResult resolveGenerics() {
        // TODO: (generics) implement this
        return new ClassResolveResult() {
            @Override
            public PsiClass getElement() {
                return resolve();
            }

            @Override
            public PsiSubstitutor getSubstitutor() {
                return PsiSubstitutor.EMPTY;
            }

            @Override
            public boolean isPackagePrefixPackageReference() {
                return false;
            }

            @Override
            public boolean isAccessible() {
                return true;
            }

            @Override
            public boolean isStaticsScopeCorrect() {
                return true;
            }

            @Override
            public PsiElement getCurrentFileResolveScope() {
                return null;
            }

            @Override
            public boolean isValidResult() {
                return true;
            }
        };
    }

    @NotNull
    @Override
    public SmaliClassType rawType() {
        // TODO: (generics) implement this
        return this;
    }

    @Override
    @NotNull
    public String getPresentableText() {
        return getCanonicalText();
    }

    @Override
    @NotNull
    public String getCanonicalText() {
        PsiClass psiClass = resolve();
        if (psiClass != null) {
            String qualifiedName = psiClass.getQualifiedName();
            if (qualifiedName != null) {
                return qualifiedName;
            }
        }
        return NameUtils.smaliToJavaType(element.getText());
    }

    @Override
    @NotNull
    public String getInternalCanonicalText() {
        return getCanonicalText();
    }

    @Override
    public boolean isValid() {
        return element.isValid();
    }

    @Override
    public boolean equalsToText(@NonNls String text) {
        return text.equals(getCanonicalText());
    }

    @NotNull
    @Override
    public GlobalSearchScope getResolveScope() {
        return element.getResolveScope();
    }

    @NotNull
    @Override
    public LanguageLevel getLanguageLevel() {
        return myLanguageLevel;
    }

    @NotNull
    @Override
    public PsiClassType setLanguageLevel(@NotNull LanguageLevel languageLevel) {
        return new SmaliClassType(element, languageLevel);
    }
}
