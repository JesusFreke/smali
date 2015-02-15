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
import org.jf.smalidea.psi.stub.SmaliImplementsListStub;

import java.util.List;

public class SmaliImplementsList extends SmaliBaseReferenceList<SmaliImplementsListStub> {
    public SmaliImplementsList(@NotNull SmaliImplementsListStub stub) {
        super(stub, SmaliElementTypes.IMPLEMENTS_LIST);
    }

    public SmaliImplementsList(@NotNull ASTNode node) {
        super(node);
    }

    @NotNull @Override public SmaliClassTypeElement[] getReferenceElements() {
        if (!((SmaliClass)getParent()).isInterface()) {
            return getImplementsElements();
        }
        return new SmaliClassTypeElement[0];
    }

    @NotNull private SmaliClassTypeElement[] getImplementsElements() {
        SmaliClass smaliClass = (SmaliClass)getStubOrPsiParent();
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

    @Override public Role getRole() {
        return Role.IMPLEMENTS_LIST;
    }
}
