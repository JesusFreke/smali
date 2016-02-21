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

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Maps;
import com.intellij.debugger.SourcePosition;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.*;
import com.intellij.psi.PsiModifier.ModifierConstant;
import com.intellij.psi.impl.PsiImplUtil;
import com.intellij.psi.impl.PsiSuperMethodImplUtil;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.util.MethodSignature;
import com.intellij.psi.util.MethodSignatureBackedByPsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jf.dexlib2.analysis.AnalysisException;
import org.jf.dexlib2.analysis.ClassPath;
import org.jf.dexlib2.analysis.MethodAnalyzer;
import org.jf.smalidea.dexlib.SmalideaMethod;
import org.jf.smalidea.dexlib.analysis.SmalideaClassProvider;
import org.jf.smalidea.psi.SmaliElementTypes;
import org.jf.smalidea.psi.iface.SmaliModifierListOwner;
import org.jf.smalidea.psi.stub.SmaliMethodStub;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SmaliMethod extends SmaliStubBasedPsiElement<SmaliMethodStub>
        implements PsiMethod, SmaliModifierListOwner, PsiAnnotationMethod {
    public SmaliMethod(@NotNull SmaliMethodStub stub) {
        super(stub, SmaliElementTypes.METHOD);
    }

    public SmaliMethod(@NotNull ASTNode node) {
        super(node);
    }

    @NotNull @Override public String getName() {
        SmaliMethodStub stub = getStub();
        String name = null;
        if (stub != null) {
            name = stub.getName();
        } else {
            SmaliMemberName nameIdentifier = getNameIdentifier();
            if (nameIdentifier != null) {
                name = nameIdentifier.getText();
            }
        }
        if (name == null || name.isEmpty()) {
            name = "<unnamed>";
        }
        return name;
    }

    @Override public boolean hasTypeParameters() {
        // TODO: (generics) implement this
        return false;
    }

    @NotNull
    public SmaliMethodPrototype getMethodPrototype() {
        return getRequiredStubOrPsiChild(SmaliElementTypes.METHOD_PROTOTYPE);
    }

    @Nullable @Override public PsiType getReturnType() {
        if (isConstructor()) return null;
        return getMethodPrototype().getReturnType();
    }

    @Nullable @Override public PsiTypeElement getReturnTypeElement() {
        if (isConstructor()) return null;
        return getMethodPrototype().getReturnTypeElement();
    }

    @NotNull @Override public SmaliMethodParamList getParameterList() {
        return getMethodPrototype().getParameterList();
    }

    @NotNull @Override public SmaliThrowsList getThrowsList() {
        return getRequiredStubOrPsiChild(SmaliElementTypes.THROWS_LIST);
    }

    @Nullable @Override public PsiCodeBlock getBody() {
        // not applicable
        return null;
    }

    @NotNull public List<SmaliInstruction> getInstructions() {
        return findChildrenByType(SmaliElementTypes.INSTRUCTION);
    }

    @NotNull public List<SmaliCatchStatement> getCatchStatements() {
        return Arrays.asList(findChildrenByClass(SmaliCatchStatement.class));
    }

    @Nullable public SourcePosition getSourcePositionForCodeOffset(int offset) {
        for (SmaliInstruction instruction: getInstructions()) {
            if (instruction.getOffset() >= offset) {
                return SourcePosition.createFromElement(instruction);
            }
        }
        return null;
    }

    public int getOffsetForLine(int line) {
        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(getProject());
        final Document document = documentManager.getDocument(getContainingFile());
        if (document == null) {
            return -1;
        }

        for (final SmaliInstruction instruction: getInstructions()) {
            int curLine = document.getLineNumber(instruction.getTextOffset());
            if (curLine >= line) {
                return instruction.getOffset();
            }
        }
        return -1;
    }

    public int getRegisterCount() {
        SmaliRegistersStatement registersStatement = findChildByClass(SmaliRegistersStatement.class);
        if (registersStatement == null) {
            return 0;
        }
        return registersStatement.getRegisterCount();
    }

    public int getParameterRegisterCount() {
        int parameterRegisterCount = getMethodPrototype().getParameterList().getParameterRegisterCount();
        if (!isStatic()) {
            parameterRegisterCount++;
        }
        return parameterRegisterCount;
    }

    @NotNull public SmaliParameterStatement[] getParameterStatements() {
        return findChildrenByClass(SmaliParameterStatement.class);
    }

    @Override public boolean isConstructor() {
        // TODO: should this return true for the class initializer?
        return hasModifierProperty("constructor") && !hasModifierProperty("static");
    }

    public boolean isStatic() {
        return hasModifierProperty("static");
    }

    @Override public boolean isVarArgs() {
        return hasModifierProperty("varargs");
    }

    @NotNull @Override public MethodSignature getSignature(@NotNull PsiSubstitutor substitutor) {
        return MethodSignatureBackedByPsiMethod.create(this, substitutor);
    }

    @Nullable @Override public SmaliMemberName getNameIdentifier() {
        return findChildByClass(SmaliMemberName.class);
    }

    @NotNull @Override public PsiMethod[] findSuperMethods() {
        return PsiSuperMethodImplUtil.findSuperMethods(this);
    }

    @NotNull @Override public PsiMethod[] findSuperMethods(boolean checkAccess) {
        return PsiSuperMethodImplUtil.findSuperMethods(this, checkAccess);
    }

    @NotNull @Override public PsiMethod[] findSuperMethods(PsiClass parentClass) {
        return PsiSuperMethodImplUtil.findSuperMethods(this, parentClass);
    }

    @NotNull @Override
    public List<MethodSignatureBackedByPsiMethod> findSuperMethodSignaturesIncludingStatic(boolean checkAccess) {
        return PsiSuperMethodImplUtil.findSuperMethodSignaturesIncludingStatic(this, checkAccess);
    }

    @Nullable @Override public PsiMethod findDeepestSuperMethod() {
        return PsiSuperMethodImplUtil.findDeepestSuperMethod(this);
    }

    @NotNull @Override public PsiMethod[] findDeepestSuperMethods() {
        return PsiSuperMethodImplUtil.findDeepestSuperMethods(this);
    }

    @NotNull @Override public SmaliModifierList getModifierList() {
        return getRequiredStubOrPsiChild(SmaliElementTypes.MODIFIER_LIST);
    }

    @Override public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
        SmaliMemberName smaliMemberName = getNameIdentifier();
        if (smaliMemberName == null) {
            throw new IncorrectOperationException();
        }
        smaliMemberName.setName(name);
        return this;
    }

    @NotNull @Override public HierarchicalMethodSignature getHierarchicalMethodSignature() {
        return PsiSuperMethodImplUtil.getHierarchicalMethodSignature(this);
    }

    @Nullable @Override public PsiDocComment getDocComment() {
        // not applicable
        return null;
    }

    @Override public boolean isDeprecated() {
        return PsiImplUtil.isDeprecatedByAnnotation(this);
    }

    @Nullable @Override public PsiTypeParameterList getTypeParameterList() {
        // TODO: (generics) implement this
        return null;
    }

    @NotNull @Override public PsiTypeParameter[] getTypeParameters() {
        // TODO: (generics) implement this
        return new PsiTypeParameter[0];
    }

    @Nullable @Override public SmaliClass getContainingClass() {
        PsiElement parent = getStubOrPsiParent();
        if (parent instanceof SmaliClass) {
            return (SmaliClass) parent;
        }
        return null;
    }

    @Override public boolean hasModifierProperty(@ModifierConstant @NonNls @NotNull String name) {
        return getModifierList().hasModifierProperty(name);
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

    private final Supplier<Map<String, SmaliLabel>> labelMap = Suppliers.memoize(
            new Supplier<Map<String, SmaliLabel>>() {
                @Override public Map<String, SmaliLabel> get() {
                    Map<String, SmaliLabel> labelMap = Maps.newHashMap();
                    for (SmaliLabel label: findChildrenByClass(SmaliLabel.class)) {
                        if (!labelMap.containsKey(label.getText())) {
                            labelMap.put(label.getText(), label);
                        }
                    }
                    return labelMap;
                }
            });

    @Nullable public SmaliLabel getLabel(String name) {
        return labelMap.get().get(name);
    }

    private MethodAnalyzer methodAnalyzer = null;

    @Nullable
    public MethodAnalyzer getMethodAnalyzer() {
        if (methodAnalyzer == null) {
            if (!PsiTreeUtil.hasErrorElements(this)) {
                ClassPath classPath;
                try {
                    classPath = new ClassPath(
                            new SmalideaClassProvider(getProject(), getContainingFile().getVirtualFile()));
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }

                try {
                    methodAnalyzer = new MethodAnalyzer(classPath, new SmalideaMethod(SmaliMethod.this), null, false);
                } catch (AnalysisException ex) {
                    methodAnalyzer = null;
                }
            }
        }
        return methodAnalyzer;
    }

    @Override public void subtreeChanged() {
        super.subtreeChanged();
        methodAnalyzer = null;
    }

    @Override public int getTextOffset() {
        SmaliMemberName smaliMemberName = getNameIdentifier();
        if (smaliMemberName != null) {
            return smaliMemberName.getTextOffset();
        }
        return super.getTextOffset();
    }

    @Nullable @Override public PsiAnnotationMemberValue getDefaultValue() {
        SmaliClass containingClass = getContainingClass();
        if (containingClass == null || !containingClass.isAnnotationType()) {
            return null;
        }

        for (SmaliAnnotation annotation: containingClass.getAnnotations()) {
            String annotationType = annotation.getQualifiedName();
            if (annotationType == null) {
                continue;
            }
            if (annotationType.equals("dalvik.annotation.AnnotationDefault")) {
                PsiAnnotationMemberValue value = annotation.findAttributeValue("value");
                if (!(value instanceof SmaliAnnotation)) {
                    return null;
                }
                SmaliAnnotation valueSubAnnotation = (SmaliAnnotation)value;
                return valueSubAnnotation.findAttributeValue(getName());
            }
        }
        return null;
    }
}
