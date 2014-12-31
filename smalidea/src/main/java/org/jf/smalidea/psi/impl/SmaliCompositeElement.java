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
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.CompositePsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public abstract class SmaliCompositeElement extends CompositePsiElement {
    public SmaliCompositeElement(IElementType type) {
        super(type);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    protected List<ASTNode> findChildrenByType(IElementType elementType) {
        List<ASTNode> result = ImmutableList.of();
        ASTNode child = getNode().getFirstChildNode();
        while (child != null) {
            if (elementType == child.getElementType()) {
                if (result.size() == 0) {
                    result = new ArrayList<ASTNode>();
                }
                result.add((ASTNode)child.getPsi());
            }
            child = child.getTreeNext();
        }
        return result;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    protected <T> T[] findChildrenByClass(Class<T> aClass) {
        List<T> result = new ArrayList<T>();
        for (PsiElement cur = getFirstChild(); cur != null; cur = cur.getNextSibling()) {
            if (aClass.isInstance(cur)) result.add((T)cur);
        }
        return result.toArray((T[]) Array.newInstance(aClass, result.size()));
    }

    @Nullable
    @SuppressWarnings("unchecked")
    protected <T> T findChildByClass(Class<T> aClass) {
        for (PsiElement cur = getFirstChild(); cur != null; cur = cur.getNextSibling()) {
            if (aClass.isInstance(cur)) return (T)cur;
        }
        return null;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    protected <T> T findAncestorByClass(Class<T> aClass) {
        PsiElement parent = getParent();
        while (parent != null) {
            if (aClass.isInstance(parent)) {
                return (T)parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T findNextSiblingByClass(@NotNull Class<T> cls) {
        PsiElement prev = getNextSibling();
        while (true) {
            if (prev == null) {
                return null;
            } else if (cls.isInstance(prev)) {
                return (T)prev;
            }
            prev = prev.getNextSibling();
        }
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T findPrevSiblingByClass(@NotNull Class<T> cls) {
        PsiElement prev = getPrevSibling();
        while (true) {
            if (prev == null) {
                return null;
            } else if (cls.isInstance(prev)) {
                return (T)prev;
            }
            prev = prev.getPrevSibling();
        }
    }
}
