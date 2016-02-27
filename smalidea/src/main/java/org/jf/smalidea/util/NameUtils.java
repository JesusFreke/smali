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

package org.jf.smalidea.util;

import com.google.common.collect.ImmutableMap;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.ResolveScopeManager;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class NameUtils {
    private static final Map<String, String> javaToSmaliPrimitiveTypes = ImmutableMap.<String, String>builder()
            .put("boolean", "Z")
            .put("byte", "B")
            .put("char", "C")
            .put("short", "S")
            .put("int", "I")
            .put("long", "J")
            .put("float", "F")
            .put("double", "D")
            .build();

    @NotNull
    public static String javaToSmaliType(@NotNull PsiType psiType) {
        if (psiType instanceof PsiClassType) {
            PsiClass psiClass = ((PsiClassType)psiType).resolve();
            if (psiClass != null) {
                return javaToSmaliType(psiClass);
            }
        }
        return javaToSmaliType(psiType.getCanonicalText());
    }

    @NotNull
    public static String javaToSmaliType(@NotNull PsiClass psiClass) {
        String qualifiedName = psiClass.getQualifiedName();
        if (qualifiedName == null) {
            throw new IllegalArgumentException("This method does not support anonymous classes");
        }
        PsiClass parent = psiClass.getContainingClass();
        if (parent != null) {
            int offset = qualifiedName.lastIndexOf('.');
            String parentName = qualifiedName.substring(0, offset);
            assert parentName.equals(parent.getQualifiedName());
            String className = qualifiedName.substring(offset+1, qualifiedName.length());
            assert className.equals(psiClass.getName());
            return javaToSmaliType(parentName + '$' + className);
        } else {
            return javaToSmaliType(psiClass.getQualifiedName());
        }
    }

    @NotNull
    public static String javaToSmaliType(@NotNull String javaType) {
        if (javaType.charAt(javaType.length()-1) == ']') {
            int dimensions = 0;
            int firstArrayChar = -1;
            for (int i=0; i<javaType.length(); i++) {
                if (javaType.charAt(i) == '[') {
                    if (firstArrayChar == -1) {
                        firstArrayChar = i;
                    }
                    dimensions++;
                }
            }
            if (dimensions > 0) {
                StringBuilder sb = new StringBuilder(firstArrayChar + 2 + dimensions);
                for (int i=0; i<dimensions; i++) {
                    sb.append('[');
                }
                convertSimpleJavaToSmaliType(javaType.substring(0, firstArrayChar), sb);
                return sb.toString();
            }
        }

        return simpleJavaToSmaliType(javaType);
    }

    private static void convertSimpleJavaToSmaliType(@NotNull String javaType, @NotNull StringBuilder dest) {
        String smaliType = javaToSmaliPrimitiveTypes.get(javaType);
        if (smaliType != null) {
            dest.append(smaliType);
        } else {
            dest.append('L');
            for (int i=0; i<javaType.length(); i++) {
                char c = javaType.charAt(i);
                if (c == '.') {
                    dest.append('/');
                } else {
                    dest.append(c);
                }
            }
            dest.append(';');
        }
    }

    public static PsiClass resolveSmaliType(@NotNull Project project, @NotNull GlobalSearchScope scope,
                                            @NotNull String smaliType) {
        JavaPsiFacade facade = JavaPsiFacade.getInstance(project);

        String javaType = NameUtils.smaliToJavaType(smaliType);

        PsiClass psiClass = facade.findClass(javaType, scope);
        if (psiClass != null) {
            return psiClass;
        }

        int offset = javaType.lastIndexOf('.');
        if (offset < 0) {
            offset = 0;
        }
        // find the first $ after the last .
        offset = javaType.indexOf('$', offset+1);
        if (offset < 0) {
            return null;
        }

        while (offset > 0 && offset < javaType.length()-1) {
            String left = javaType.substring(0, offset);
            psiClass = facade.findClass(left, scope);
            if (psiClass != null) {
                psiClass = findInnerClass(psiClass, javaType.substring(offset+1, javaType.length()), facade, scope);
                if (psiClass != null) {
                    return psiClass;
                }
            }
            offset = javaType.indexOf('$', offset+1);
        }
        return null;
    }

    @Nullable
    public static PsiClass resolveSmaliType(@NotNull PsiElement element, @NotNull String smaliType) {
        GlobalSearchScope scope = ResolveScopeManager.getElementResolveScope(element);
        return resolveSmaliType(element.getProject(), scope, smaliType);
    }

    @Nullable
    public static PsiClass findInnerClass(@NotNull PsiClass outerClass, String innerText, JavaPsiFacade facade,
                                          GlobalSearchScope scope) {
        int offset = innerText.indexOf('$');
        if (offset < 0) {
            offset = innerText.length();
        }

        while (offset > 0 && offset <= innerText.length()) {
            String left = innerText.substring(0, offset);
            String nextInner = outerClass.getQualifiedName() + "." + left;
            PsiClass psiClass = facade.findClass(nextInner, scope);
            if (psiClass != null) {
                if (offset < innerText.length()) {
                    psiClass = findInnerClass(psiClass, innerText.substring(offset+1, innerText.length()), facade,
                            scope);
                    if (psiClass != null) {
                        return psiClass;
                    }
                } else {
                    return psiClass;
                }
            }
            if (offset >= innerText.length()) {
                break;
            }
            offset = innerText.indexOf('$', offset+1);
            if (offset < 0) {
                offset = innerText.length();
            }
        }
        return null;
    }

    private static String simpleJavaToSmaliType(@NotNull String simpleJavaType) {
        StringBuilder sb = new StringBuilder(simpleJavaType.length() + 2);
        convertSimpleJavaToSmaliType(simpleJavaType, sb);
        sb.trimToSize();
        return sb.toString();
    }

    @NotNull
    public static String smaliToJavaType(@NotNull String smaliType) {
        if (smaliType.charAt(0) == '[') {
            return convertSmaliArrayToJava(smaliType);
        } else {
            StringBuilder sb = new StringBuilder(smaliType.length());
            convertAndAppendNonArraySmaliTypeToJava(smaliType, sb);
            return sb.toString();
        }
    }

    @NotNull
    public static String resolveSmaliToJavaType(@NotNull Project project, @NotNull GlobalSearchScope scope,
                                                @NotNull String smaliType) {
        // First, try to resolve the type and get its qualified name, so that we can make sure
        // to use the correct name for inner classes
        PsiClass resolvedType = resolveSmaliType(project, scope, smaliType);
        if (resolvedType != null) {
            String qualifiedName = resolvedType.getQualifiedName();
            if (qualifiedName != null) {
                return qualifiedName;
            }
        }

        // if we can't find it, just do a textual conversion of the name
        return smaliToJavaType(smaliType);
    }

    @NotNull
    public static String resolveSmaliToJavaType(@NotNull PsiElement element, @NotNull String smaliType) {
        return resolveSmaliToJavaType(element.getProject(), element.getResolveScope(), smaliType);
    }

    @NotNull
    public static PsiType resolveSmaliToPsiType(@NotNull PsiElement element, @NotNull String smaliType) {
        PsiClass resolvedType = resolveSmaliType(element, smaliType);
        if (resolvedType != null) {
            PsiElementFactory factory = JavaPsiFacade.getInstance(element.getProject()).getElementFactory();
            return factory.createType(resolvedType);
        }

        String javaType = NameUtils.smaliToJavaType(smaliType);
        PsiElementFactory factory = JavaPsiFacade.getInstance(element.getProject()).getElementFactory();
        return factory.createTypeFromText(javaType, element);
    }

    @NotNull
    private static String convertSmaliArrayToJava(@NotNull String smaliType) {
        int dimensions=0;
        while (smaliType.charAt(dimensions) == '[') {
            dimensions++;
        }

        StringBuilder sb = new StringBuilder(smaliType.length() + dimensions);
        convertAndAppendNonArraySmaliTypeToJava(smaliType.substring(dimensions), sb);
        for (int i=0; i<dimensions; i++) {
            sb.append("[]");
        }
        return sb.toString();
    }

    private static void convertAndAppendNonArraySmaliTypeToJava(@NotNull String smaliType, @NotNull StringBuilder dest) {
        switch (smaliType.charAt(0)) {
            case 'Z':
                dest.append("boolean");
                return;
            case 'B':
                dest.append("byte");
                return;
            case 'C':
                dest.append("char");
                return;
            case 'S':
                dest.append("short");
                return;
            case 'I':
                dest.append("int");
                return;
            case 'J':
                dest.append("long");
                return;
            case 'F':
                dest.append("float");
                return;
            case 'D':
                dest.append("double");
            case 'L':
                for (int i=1; i<smaliType.length()-1; i++) {
                    char c = smaliType.charAt(i);
                    if (c == '/') {
                        dest.append('.');
                    } else {
                        dest.append(c);
                    }
                }
                return;
            case 'V':
                dest.append("void");
                return;
            case 'U':
                if (smaliType.equals("Ujava/lang/Object;")) {
                    dest.append("java.lang.Object");
                    return;
                }
                // fall through
            default:
                throw new RuntimeException("Invalid smali type: " + smaliType);
        }
    }

    @Nullable
    public static String shortNameFromQualifiedName(@Nullable String qualifiedName) {
        if (qualifiedName == null) {
            return null;
        }

        int index = qualifiedName.lastIndexOf('.');
        if (index == -1) {
            return qualifiedName;
        }
        return qualifiedName.substring(index+1);
    }
}
