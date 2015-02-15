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

import com.google.common.collect.Lists;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jf.smalidea.psi.SmaliElementTypes;
import org.jf.smalidea.psi.stub.SmaliExtendsListStub;

import java.util.List;

public class SmaliExtendsList extends SmaliBaseReferenceList<SmaliExtendsListStub> {
    public SmaliExtendsList(@NotNull SmaliExtendsListStub stub) {
        super(stub, SmaliElementTypes.EXTENDS_LIST);
    }

    public SmaliExtendsList(@NotNull ASTNode node) {
        super(node);
    }

    @NotNull @Override public SmaliClassTypeElement[] getReferenceElements() {
        if (((SmaliClass)getParent()).isInterface()) {
            return getImplementsElements();
        } else {
            return getExtendsElement();
        }
    }

    @NotNull private SmaliClassTypeElement[] getImplementsElements() {
        SmaliClass smaliClass = getStubOrPsiParentOfType(SmaliClass.class);
        assert smaliClass != null;

        SmaliImplementsStatement[] implementsStatements = smaliClass.getImplementsStatements();
        if (implementsStatements.length > 0) {
            // all implemented interfaces go in the extends list for an interface
            List<SmaliClassTypeElement> types = Lists.newArrayList();

            for (SmaliImplementsStatement implementsStatement: implementsStatements) {
                SmaliClassTypeElement classReference = implementsStatement.getClassReference();
                if (classReference != null) {
                    types.add(classReference);
                }
            }
            return types.toArray(new SmaliClassTypeElement[types.size()]);
        }
        return new SmaliClassTypeElement[0];
    }

    @NotNull private SmaliClassTypeElement[] getExtendsElement() {
        SmaliClass smaliClass = getStubOrPsiParentOfType(SmaliClass.class);
        assert smaliClass != null;

        SmaliSuperStatement superStatement = smaliClass.getSuperStatement();
        if (superStatement != null) {
            SmaliClassTypeElement classReference = superStatement.getClassReference();
            if (classReference != null) {
                return new SmaliClassTypeElement[] { classReference };
            }
        }
        return new SmaliClassTypeElement[0];
    }

    @Override public Role getRole() {
        return Role.EXTENDS_LIST;
    }
}
