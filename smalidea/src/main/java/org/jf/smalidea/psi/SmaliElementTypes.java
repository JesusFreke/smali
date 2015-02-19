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

package org.jf.smalidea.psi;

import org.jf.smalidea.psi.impl.*;
import org.jf.smalidea.psi.stub.element.*;

public class SmaliElementTypes {
    public static final SmaliFileElementType FILE = SmaliFileElementType.INSTANCE;
    public static final SmaliClassElementType CLASS = SmaliClassElementType.INSTANCE;
    public static final SmaliFieldElementType FIELD = SmaliFieldElementType.INSTANCE;
    public static final SmaliMethodElementType METHOD = SmaliMethodElementType.INSTANCE;
    public static final SmaliClassStatementElementType CLASS_STATEMENT = SmaliClassStatementElementType.INSTANCE;
    public static final SmaliMethodPrototypeElementType METHOD_PROTOTYPE = SmaliMethodPrototypeElementType.INSTANCE;
    public static final SmaliMethodParamListElementType METHOD_PARAM_LIST = SmaliMethodParamListElementType.INSTANCE;
    public static final SmaliMethodParameterElementType METHOD_PARAMETER = SmaliMethodParameterElementType.INSTANCE;
    public static final SmaliAnnotationElementType ANNOTATION = SmaliAnnotationElementType.INSTANCE;
    public static final SmaliModifierListElementType MODIFIER_LIST = SmaliModifierListElementType.INSTANCE;
    public static final SmaliExtendsListElementType EXTENDS_LIST = SmaliExtendsListElementType.INSTANCE;
    public static final SmaliImplementsListElementType IMPLEMENTS_LIST = SmaliImplementsListElementType.INSTANCE;
    public static final SmaliThrowsListElementType THROWS_LIST = SmaliThrowsListElementType.INSTANCE;

    public static final SmaliCompositeElementType LITERAL =
            new SmaliCompositeElementType("LITERAL", SmaliLiteral.FACTORY);
    public static final SmaliCompositeElementType SUPER_STATEMENT =
            new SmaliCompositeElementType("SUPER_STATEMENT", SmaliSuperStatement.FACTORY);
    public static final SmaliCompositeElementType IMPLEMENTS_STATEMENT =
            new SmaliCompositeElementType("IMPLEMENTS_STATEMENT", SmaliImplementsStatement.FACTORY);
    public static final SmaliCompositeElementType SOURCE_STATEMENT =
            new SmaliCompositeElementType("SOURCE_STATEMENT", SmaliSourceStatement.FACTORY);
    public static final SmaliCompositeElementType REGISTERS_STATEMENT =
            new SmaliCompositeElementType("REGISTERS_STATEMENT", SmaliRegistersStatement.FACTORY);
    public static final SmaliCompositeElementType REGISTER_REFERENCE =
            new SmaliCompositeElementType("REGISTER_REFERENCE", SmaliRegisterReference.FACTORY);
    public static final SmaliCompositeElementType MEMBER_NAME =
            new SmaliCompositeElementType("MEMBER_NAME", SmaliMemberName.FACTORY);
    public static final SmaliCompositeElementType LOCAL_NAME =
            new SmaliCompositeElementType("LOCAL_NAME", SmaliLocalName.FACTORY);
    public static final SmaliCompositeElementType PARAMETER_STATEMENT =
            new SmaliCompositeElementType("PARAMETER_STATEMENT", SmaliParameterStatement.FACTORY);
    public static final SmaliCompositeElementType FIELD_INITIALIZER =
            new SmaliCompositeElementType("FIELD_INITIALIZER", SmaliFieldInitializer.FACTORY);
    public static final SmaliCompositeElementType INSTRUCTION =
            new SmaliCompositeElementType("INSTRUCTION", SmaliInstruction.FACTORY);
    public static final SmaliCompositeElementType ANNOTATION_PARAMETER_LIST =
            new SmaliCompositeElementType("ANNOTATION_PARAMETER_LIST", SmaliAnnotationParameterList.FACTORY);
    public static final SmaliCompositeElementType ANNOTATION_ELEMENT =
            new SmaliCompositeElementType("ANNOTATION_ELEMENT", SmaliAnnotationElement.FACTORY);
    public static final SmaliCompositeElementType ANNOTATION_ELEMENT_NAME =
            new SmaliCompositeElementType("ANNOTATION_ELEMENT_NAME", SmaliAnnotationElementName.FACTORY);
    public static final SmaliCompositeElementType FIELD_REFERENCE =
            new SmaliCompositeElementType("FIELD_REFERENCE", SmaliFieldReference.FACTORY);
    public static final SmaliCompositeElementType METHOD_REFERENCE =
            new SmaliCompositeElementType("METHOD_REFERENCE", SmaliMethodReference.FACTORY);
    public static final SmaliCompositeElementType METHOD_REFERENCE_PARAM_LIST =
            new SmaliCompositeElementType("METHOD_REFERENCE_PARAM_LIST", SmaliMethodReferenceParamList.FACTORY);
    public static final SmaliCompositeElementType LABEL =
            new SmaliCompositeElementType("LABEL", SmaliLabel.FACTORY);
    public static final SmaliCompositeElementType LABEL_REFERENCE =
            new SmaliCompositeElementType("LABEL_REFERENCE", SmaliLabelReference.FACTORY);
    public static final SmaliCompositeElementType LINE_DEBUG_STATEMENT =
            new SmaliCompositeElementType("LINE_DEBUG_STATEMENT", SmaliLineDebugStatement.FACTORY);
    public static final SmaliCompositeElementType LOCAL_DEBUG_STATEMENT =
            new SmaliCompositeElementType("LOCAL_DEBUG_STATEMENT", SmaliLocalDebugStatement.FACTORY);
    public static final SmaliCompositeElementType END_LOCAL_DEBUG_STATEMENT =
            new SmaliCompositeElementType("END_LOCAL_DEBUG_STATEMENT", SmaliEndLocalDebugStatement.FACTORY);
    public static final SmaliCompositeElementType RESTART_LOCAL_DEBUG_STATEMENT =
            new SmaliCompositeElementType("RESTART_LOCAL_DEBUG_STATEMENT", SmaliRestartLocalDebugStatement.FACTORY);
    public static final SmaliCompositeElementType PROLOGUE_DEBUG_STATEMENT =
            new SmaliCompositeElementType("PROLOGUE_DEBUG_STATEMENT", SmaliPrologueDebugStatement.FACTORY);
    public static final SmaliCompositeElementType EPILOGUE_DEBUG_STATEMENT =
            new SmaliCompositeElementType("EPILOGUE_DEBUG_STATEMENT", SmaliEpilogueDebugStatement.FACTORY);
    public static final SmaliCompositeElementType SOURCE_DEBUG_STATEMENT =
            new SmaliCompositeElementType("SOURCE_DEBUG_STATEMENT", SmaliSourceDebugStatement.FACTORY);
    public static final SmaliCompositeElementType PRIMITIVE_TYPE =
            new SmaliCompositeElementType("PRIMITIVE_TYPE", SmaliPrimitiveTypeElement.FACTORY);
    public static final SmaliCompositeElementType CLASS_TYPE =
            new SmaliCompositeElementType("CLASS_TYPE", SmaliClassTypeElement.FACTORY);
    public static final SmaliCompositeElementType ARRAY_TYPE =
            new SmaliCompositeElementType("ARRAY_TYPE", SmaliArrayTypeElement.FACTORY);
    public static final SmaliCompositeElementType VOID_TYPE =
            new SmaliCompositeElementType("VOID_TYPE", SmaliVoidTypeElement.FACTORY);
    public static final SmaliCompositeElementType CATCH_STATEMENT =
            new SmaliCompositeElementType("CATCH_STATEMENT", SmaliCatchStatement.FACTORY);
    public static final SmaliCompositeElementType CATCH_ALL_STATEMENT =
            new SmaliCompositeElementType("CATCH_ALL_STATEMENT", SmaliCatchAllStatement.FACTORY);
    public static final SmaliCompositeElementType PACKED_SWITCH_ELEMENT =
            new SmaliCompositeElementType("PACKED_SWITCH_ELEMENT", SmaliPackedSwitchElement.FACTORY);
    public static final SmaliCompositeElementType SPARSE_SWITCH_ELEMENT =
            new SmaliCompositeElementType("SPARSE_SWITCH_ELEMENT", SmaliSparseSwitchElement.FACTORY);
    public static final SmaliCompositeElementType ARRAY_DATA_ELEMENT =
            new SmaliCompositeElementType("ARRAY_DATA_ELEMENT", SmaliArrayDataElement.FACTORY);
}
