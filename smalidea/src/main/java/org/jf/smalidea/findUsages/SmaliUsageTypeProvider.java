/*
 * Copyright 2015, Google Inc.
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

package org.jf.smalidea.findUsages;

import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.usages.impl.rules.UsageType;
import com.intellij.usages.impl.rules.UsageTypeProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jf.dexlib2.Opcode;
import org.jf.smalidea.SmaliTokens;
import org.jf.smalidea.psi.impl.*;

import java.util.EnumSet;
import java.util.Set;

public class SmaliUsageTypeProvider implements UsageTypeProvider {

    static final UsageType CLASS_DECLARATION = new UsageType("Class declaration");
    static final UsageType VERIFICATION_ERROR = new UsageType("Usage in verification error");
    static final UsageType FIELD_TYPE_REFERENCE = new UsageType("Usage as field type in a field reference");
    static final UsageType FIELD_DECLARING_TYPE_REFERENCE = new UsageType("Usage as a declaring type in a field reference");
    static final UsageType METHOD_RETURN_TYPE_REFERENCE = new UsageType("Usage as return type in a method reference");
    static final UsageType METHOD_PARAM_REFERENCE = new UsageType("Usage as parameter in a method reference");
    static final UsageType METHOD_DECLARING_TYPE_REFERENCE = new UsageType("Usage as a declaring type in a method reference");
    static final UsageType LITERAL = new UsageType("Usage as a literal");

    @Nullable @Override public UsageType getUsageType(PsiElement element) {
        if (element instanceof PsiReference) {
            PsiElement referenced = ((PsiReference) element).resolve();
            if (referenced != null) {
                if (referenced instanceof PsiClass) {
                    return findClassUsageType(element);
                } else if (referenced instanceof PsiField) {
                    return findFieldUsageType(element);
                } else if (referenced instanceof PsiMethod) {
                    return findMethodUsageType(element);
                }
            }
        }
        return UsageType.UNCLASSIFIED;
    }

    private final Set<Opcode> newArrayInstructions = EnumSet.of(Opcode.FILLED_NEW_ARRAY, Opcode.NEW_ARRAY,
            Opcode.FILLED_NEW_ARRAY_RANGE);

    private final Set<Opcode> fieldReadInstructions = EnumSet.of(Opcode.IGET, Opcode.IGET_BOOLEAN, Opcode.IGET_BYTE,
            Opcode.IGET_CHAR, Opcode.IGET_OBJECT, Opcode.IGET_OBJECT_VOLATILE, Opcode.IGET_SHORT, Opcode.IGET_VOLATILE,
            Opcode.IGET_WIDE, Opcode.IGET_WIDE_VOLATILE, Opcode.SGET, Opcode.SGET_BOOLEAN, Opcode.SGET_BYTE,
            Opcode.SGET_CHAR, Opcode.SGET_OBJECT, Opcode.SGET_OBJECT_VOLATILE, Opcode.SGET_SHORT, Opcode.SGET_VOLATILE,
            Opcode.SGET_WIDE, Opcode.SGET_WIDE_VOLATILE);

    private final Set<Opcode> fieldWriteInstructions = EnumSet.of(Opcode.IPUT, Opcode.IPUT_BOOLEAN, Opcode.IPUT_BYTE,
            Opcode.IPUT_CHAR, Opcode.IPUT_OBJECT, Opcode.IPUT_OBJECT_VOLATILE, Opcode.IPUT_SHORT, Opcode.IPUT_VOLATILE,
            Opcode.IPUT_WIDE, Opcode.IPUT_WIDE_VOLATILE, Opcode.SPUT, Opcode.SPUT_BOOLEAN, Opcode.SPUT_BYTE,
            Opcode.SPUT_CHAR, Opcode.SPUT_OBJECT, Opcode.SPUT_OBJECT_VOLATILE, Opcode.SPUT_SHORT, Opcode.SPUT_VOLATILE,
            Opcode.SPUT_WIDE, Opcode.SPUT_WIDE_VOLATILE);

    @Nullable
    private UsageType findClassUsageType(@NotNull PsiElement element) {
        PsiElement originalElement = element;

        while (element != null) {
            if (element instanceof SmaliFieldReference) {
                PsiElement prev = originalElement.getPrevSibling();
                while (prev != null) {
                    // if the element is to the right of a colon, then it is the field type, otherwise it is
                    // the declaring class
                    if (prev.getNode().getElementType() == SmaliTokens.COLON) {
                        return FIELD_TYPE_REFERENCE;
                    }
                    prev = prev.getPrevSibling();
                }
                return FIELD_DECLARING_TYPE_REFERENCE;
            } else if (element instanceof SmaliMethodReferenceParamList) {
                return METHOD_PARAM_REFERENCE;
            } else if (element instanceof SmaliMethodReference) {
                PsiElement prev = originalElement.getPrevSibling();
                while (prev != null) {
                    IElementType elementType = prev.getNode().getElementType();
                    // if the element is to the right of a close paren, then it is the return type,
                    // otherwise it is the declaring class. Any parameter type will be taken care of by the previous
                    // "if" for SmaliMethodReferenceParamList
                    if (elementType == SmaliTokens.CLOSE_PAREN) {
                        return METHOD_RETURN_TYPE_REFERENCE;
                    }
                    prev = prev.getPrevSibling();
                }
                return METHOD_DECLARING_TYPE_REFERENCE;
            } else if (element instanceof SmaliInstruction) {
                Opcode opcode = ((SmaliInstruction) element).getOpcode();
                if (opcode == Opcode.INSTANCE_OF) {
                    return UsageType.CLASS_INSTANCE_OF;
                } else if (opcode == Opcode.CHECK_CAST) {
                    return UsageType.CLASS_CAST_TO;
                } else if (newArrayInstructions.contains(opcode)) {
                    return UsageType.CLASS_NEW_ARRAY;
                } else if (opcode == Opcode.NEW_INSTANCE) {
                    return UsageType.CLASS_NEW_OPERATOR;
                } else if (opcode == Opcode.CONST_CLASS) {
                    return UsageType.CLASS_CLASS_OBJECT_ACCESS;
                } else if (opcode == Opcode.THROW_VERIFICATION_ERROR) {
                    return VERIFICATION_ERROR;
                }
            } else if (element instanceof SmaliSuperStatement || element instanceof SmaliImplementsStatement) {
                return UsageType.CLASS_EXTENDS_IMPLEMENTS_LIST;
            } else if (element instanceof SmaliClassStatement) {
                return CLASS_DECLARATION;
            } else if (element instanceof SmaliMethodParamList) {
                return UsageType.CLASS_METHOD_PARAMETER_DECLARATION;
            } else if (element instanceof SmaliMethodPrototype) {
                return UsageType.CLASS_METHOD_RETURN_TYPE;
            } else if (element instanceof SmaliField) {
                return UsageType.CLASS_FIELD_DECLARATION;
            } else if (element instanceof SmaliCatchStatement) {
                return UsageType.CLASS_CATCH_CLAUSE_PARAMETER_DECLARATION;
            } else if (element instanceof SmaliLocalDebugStatement) {
                return UsageType.CLASS_LOCAL_VAR_DECLARATION;
            } else if (element instanceof SmaliAnnotation) {
                return UsageType.ANNOTATION;
            } else if (element instanceof SmaliLiteral) {
                return LITERAL;
            }
            element = element.getParent();
        }
        return UsageType.UNCLASSIFIED;
    }

    @Nullable
    private UsageType findFieldUsageType(@NotNull PsiElement element) {
        PsiElement originalElement = element;

        while (element != null) {
            element = element.getParent();

            if (element instanceof SmaliInstruction) {
                Opcode opcode = ((SmaliInstruction) element).getOpcode();
                if (fieldReadInstructions.contains(opcode)) {
                    return UsageType.READ;
                } else if (fieldWriteInstructions.contains(opcode)) {
                    return UsageType.WRITE;
                } else if (opcode == Opcode.THROW_VERIFICATION_ERROR) {
                    return VERIFICATION_ERROR;
                }
            } if (element instanceof SmaliLiteral) {
                return LITERAL;
            }
        }
        return UsageType.UNCLASSIFIED;
    }

    @Nullable
    private UsageType findMethodUsageType(@NotNull PsiElement element) {
        PsiElement originalElement = element;

        while (element != null) {
            element = element.getParent();

            if (element instanceof SmaliInstruction) {
                Opcode opcode = ((SmaliInstruction) element).getOpcode();
                if (opcode == Opcode.THROW_VERIFICATION_ERROR) {
                    return VERIFICATION_ERROR;
                }
            } if (element instanceof SmaliLiteral) {
                return LITERAL;
            }
        }
        return UsageType.UNCLASSIFIED;
    }
}
