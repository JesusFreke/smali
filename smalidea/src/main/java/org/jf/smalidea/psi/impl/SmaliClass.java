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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.intellij.debugger.SourcePosition;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.PsiModifier.ModifierConstant;
import com.intellij.psi.impl.InheritanceImplUtil;
import com.intellij.psi.impl.PsiClassImplUtil;
import com.intellij.psi.impl.PsiImplUtil;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.IncorrectOperationException;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jf.smalidea.SmaliIcons;
import org.jf.smalidea.psi.SmaliElementTypes;
import org.jf.smalidea.psi.iface.SmaliModifierListOwner;
import org.jf.smalidea.psi.leaf.SmaliClassDescriptor;
import org.jf.smalidea.psi.stub.SmaliClassStub;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.Collection;
import java.util.List;

public class SmaliClass extends SmaliStubBasedPsiElement<SmaliClassStub> implements PsiClass, SmaliModifierListOwner {
    public SmaliClass(@NotNull SmaliClassStub stub) {
        super(stub, SmaliElementTypes.CLASS);
    }

    public SmaliClass(@NotNull ASTNode node) {
        super(node);
    }

    @Nonnull
    @Override
    public String getName() {
        String name = getQualifiedName();
        if (name == null) {
            return "";
        }
        int lastDot = name.lastIndexOf('.');
        if (lastDot < 0) {
            return name;
        }
        return name.substring(lastDot+1);
    }

    @Nullable @Override public String getQualifiedName() {
        SmaliClassStatement classStatement = getStubOrPsiChild(SmaliElementTypes.CLASS_STATEMENT);
        if (classStatement == null) {
            return null;
        }
        return classStatement.getQualifiedName();
    }

    @NotNull public String getPackageName() {
        String name = getQualifiedName();
        if (name == null) {
            return "";
        }
        int lastDot = name.lastIndexOf('.');
        if (lastDot < 0) {
            return "";
        }
        return name.substring(0, lastDot);
    }

    @Override public boolean hasTypeParameters() {
        // TODO: implement generics
        return false;
    }

    @Override public boolean isInterface() {
        return hasModifierProperty("interface");
    }

    @Override public boolean isAnnotationType() {
        return hasModifierProperty("annotation");
    }

    @Override public boolean isEnum() {
        return hasModifierProperty("enum");
    }

    @Nullable public SmaliSuperStatement getSuperStatement() {
        return findChildByClass(SmaliSuperStatement.class);
    }

    @NotNull @Override public SmaliExtendsList getExtendsList() {
        return getRequiredStubOrPsiChild(SmaliElementTypes.EXTENDS_LIST);
    }

    @NotNull public SmaliImplementsStatement[] getImplementsStatements() {
        return findChildrenByClass(SmaliImplementsStatement.class);
    }

    @NotNull @Override public SmaliImplementsList getImplementsList() {
        return getRequiredStubOrPsiChild(SmaliElementTypes.IMPLEMENTS_LIST);
    }

    @NotNull @Override public SmaliClassType[] getExtendsListTypes() {
        return getExtendsList().getReferencedTypes();
    }

    @NotNull @Override public SmaliClassType[] getImplementsListTypes() {
        return getImplementsList().getReferencedTypes();
    }

    @Nullable @Override public PsiClass getSuperClass() {
        return PsiClassImplUtil.getSuperClass(this);
    }

    @Override public PsiClass[] getInterfaces() {
        return PsiClassImplUtil.getInterfaces(this);
    }

    @NotNull @Override public PsiClass[] getSupers() {
        return PsiClassImplUtil.getSupers(this);
    }

    @NotNull @Override public PsiClassType[] getSuperTypes() {
        return PsiClassImplUtil.getSuperTypes(this);
    }

    @NotNull @Override public SmaliField[] getFields() {
        SmaliField[] fields = getStubOrPsiChildren(SmaliElementTypes.FIELD, new SmaliField[0]);
        List<SmaliField> filteredFields = null;
        for (int i=fields.length-1; i>=0; i--) {
            SmaliField field = fields[i];
            if (field.getName() == null) {
                if (filteredFields == null) {
                    filteredFields = Lists.newArrayList(fields);
                }
                filteredFields.remove(i);
            }
        }
        if (filteredFields != null) {
            return filteredFields.toArray(new SmaliField[filteredFields.size()]);
        }
        return fields;
    }

    @NotNull @Override public SmaliMethod[] getMethods() {
        return getStubOrPsiChildren(SmaliElementTypes.METHOD, new SmaliMethod[0]);
    }

    @NotNull @Override public PsiMethod[] getConstructors() {
        return PsiImplUtil.getConstructors(this);
    }

    @NotNull @Override public PsiClass[] getInnerClasses() {
        return new PsiClass[0];
    }

    @NotNull @Override public PsiClassInitializer[] getInitializers() {
        // TODO: do we need to return the <clinit> method here?
        return new PsiClassInitializer[0];
    }

    @NotNull @Override public PsiField[] getAllFields() {
        return PsiClassImplUtil.getAllFields(this);
    }

    @NotNull @Override public PsiMethod[] getAllMethods() {
        return PsiClassImplUtil.getAllMethods(this);
    }

    @NotNull @Override public PsiClass[] getAllInnerClasses() {
        return new PsiClass[0];
    }

    @Nullable @Override public PsiField findFieldByName(@NonNls String name, boolean checkBases) {
        return PsiClassImplUtil.findFieldByName(this, name, checkBases);
    }

    @Nullable @Override public PsiMethod findMethodBySignature(PsiMethod patternMethod, boolean checkBases) {
        return PsiClassImplUtil.findMethodBySignature(this, patternMethod, checkBases);
    }

    @NotNull @Override public PsiMethod[] findMethodsBySignature(PsiMethod patternMethod, boolean checkBases) {
        return PsiClassImplUtil.findMethodsBySignature(this, patternMethod, checkBases);
    }

    @NotNull @Override public PsiMethod[] findMethodsByName(@NonNls String name, boolean checkBases) {
        return PsiClassImplUtil.findMethodsByName(this, name, checkBases);
    }

    @NotNull @Override
    public List<Pair<PsiMethod, PsiSubstitutor>> findMethodsAndTheirSubstitutorsByName(@NonNls String name, boolean checkBases) {
        return PsiClassImplUtil.findMethodsAndTheirSubstitutorsByName(this, name, checkBases);
    }

    @NotNull @Override public List<Pair<PsiMethod, PsiSubstitutor>> getAllMethodsAndTheirSubstitutors() {
        return PsiClassImplUtil.getAllWithSubstitutorsByMap(this, PsiClassImplUtil.MemberType.METHOD);
    }

    @Nullable @Override public PsiClass findInnerClassByName(@NonNls String name, boolean checkBases) {
        return null;
    }

    @Nullable @Override public PsiElement getLBrace() {
        return null;
    }

    @Nullable @Override public PsiElement getRBrace() {
        return null;
    }

    @Nullable public SmaliClassStatement getClassStatement() {
        return getStubOrPsiChild(SmaliElementTypes.CLASS_STATEMENT);
    }

    @Nullable @Override public SmaliClassDescriptor getNameIdentifier() {
        SmaliClassStatement classStatement = getClassStatement();
        if (classStatement == null) {
            return null;
        }
        return classStatement.getNameIdentifier();
    }

    @Override public PsiElement getScope() {
        return null;
    }

    @Override public boolean isInheritor(@NotNull PsiClass baseClass, boolean checkDeep) {
        return InheritanceImplUtil.isInheritor(this, baseClass, checkDeep);
    }

    @Override public boolean isInheritorDeep(PsiClass baseClass, @Nullable PsiClass classToByPass) {
        return InheritanceImplUtil.isInheritorDeep(this, baseClass, classToByPass);
    }

    @Nullable @Override public PsiClass getContainingClass() {
        return null;
    }

    @NotNull @Override public Collection<HierarchicalMethodSignature> getVisibleSignatures() {
        return ImmutableList.of();
    }

    @Override public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
        SmaliClassStatement classStatement = getClassStatement();
        if (classStatement == null) {
            throw new IncorrectOperationException();
        }

        SmaliClassTypeElement classTypeElement = classStatement.getNameElement();
        if (classTypeElement == null) {
            throw new IncorrectOperationException();
        }

        String expectedPath = "/" + getName() + ".smali";

        VirtualFile virtualFile = this.getContainingFile().getVirtualFile();
        if (virtualFile != null) {
            String actualPath = virtualFile.getPath();
            if (actualPath.endsWith(expectedPath)) {
                getContainingFile().setName(name + ".smali");
            }
        }

        String packageName = this.getPackageName();
        String newName;
        if (packageName.length() > 0) {
            newName = packageName + "." + name;
        } else {
            newName = name;
        }
        classTypeElement.handleElementRename(newName);
        return this;
    }

    public void setPackageName(@NonNls @NotNull String packageName) {
        SmaliClassStatement classStatement = getClassStatement();
        if (classStatement == null) {
            throw new IncorrectOperationException();
        }

        SmaliClassTypeElement classTypeElement = classStatement.getNameElement();
        if (classTypeElement == null) {
            throw new IncorrectOperationException();
        }

        String newName;
        if (packageName.length() > 0) {
            newName = packageName + "." + getName();
        } else {
            newName = getName();
        }

        classTypeElement.handleElementRename(newName);
    }

    @Nullable @Override public PsiDocComment getDocComment() {
        return null;
    }

    @Override public boolean isDeprecated() {
        return false;
    }

    @Nullable @Override public PsiTypeParameterList getTypeParameterList() {
        return null;
    }

    @NotNull @Override public PsiTypeParameter[] getTypeParameters() {
        return new PsiTypeParameter[0];
    }

    @Nullable @Override public SmaliModifierList getModifierList() {
        SmaliClassStatement classStatement = getStubOrPsiChild(SmaliElementTypes.CLASS_STATEMENT);
        if (classStatement == null) {
            return null;
        }
        return classStatement.getModifierList();
    }

    @Override public boolean hasModifierProperty(@ModifierConstant @NonNls @NotNull String name) {
        SmaliModifierList smaliModifierList = getModifierList();
        return smaliModifierList != null && smaliModifierList.hasModifierProperty(name);
    }

    @NotNull @Override public SmaliAnnotation[] getAnnotations() {
        return getStubOrPsiChildren(SmaliElementTypes.ANNOTATION, new SmaliAnnotation[0]);
    }

    @NotNull @Override public SmaliAnnotation[] getApplicableAnnotations() {
        return getAnnotations();
    }

    @Nullable @Override public SmaliAnnotation findAnnotation(@NotNull @NonNls String qualifiedName) {
        for (SmaliAnnotation annotation: getAnnotations()) {
            if (qualifiedName.equals(annotation.getQualifiedName())) {
                return annotation;
            }
        }
        return null;
    }

    @NotNull @Override public SmaliAnnotation addAnnotation(@NotNull @NonNls String qualifiedName) {
        // TODO: implement this
        return null;
    }

    @Nullable public Location getLocationForSourcePosition(@Nonnull ReferenceType type,
                                                           @Nonnull SourcePosition position) {

        SmaliMethod[] smaliMethods = findChildrenByType(SmaliElementTypes.METHOD, SmaliMethod.class);

        for (SmaliMethod smaliMethod: smaliMethods) {
            //TODO: check the start line+end line of the method
            int offset = smaliMethod.getOffsetForLine(position.getLine());
            if (offset != -1) {
                List<Method> methods = type.methodsByName(smaliMethod.getName(),
                        smaliMethod.getMethodPrototype().getText());
                if (methods.size() > 0) {
                    return methods.get(0).locationOfCodeIndex(offset/2);
                }
            }
        }
        return null;
    }

    @Override
    public boolean processDeclarations(@NotNull PsiScopeProcessor processor, @NotNull ResolveState state,
                                       PsiElement lastParent, @NotNull PsiElement place) {
        return PsiClassImplUtil.processDeclarationsInClass(this, processor, state, null, lastParent, place,
                PsiUtil.getLanguageLevel(place), false);
    }

    @Nullable @Override protected Icon getElementIcon(@IconFlags int flags) {
        return SmaliIcons.SmaliIcon;
    }
}