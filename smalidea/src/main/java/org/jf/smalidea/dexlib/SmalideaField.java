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

import com.google.common.collect.ImmutableSet;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiModifierList;
import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.base.reference.BaseFieldReference;
import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.value.EncodedValue;
import org.jf.smalidea.psi.impl.SmaliField;
import org.jf.smalidea.util.NameUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

public class SmalideaField extends BaseFieldReference implements Field {
    private final PsiField psiField;

    public SmalideaField(PsiField psiField) {
        this.psiField = psiField;
    }

    @Override public int getAccessFlags() {
        if (psiField instanceof SmaliField) {
            return ((SmaliField)psiField).getModifierList().getAccessFlags();
        } else {
            int flags = 0;
            PsiModifierList modifierList = psiField.getModifierList();
            if (modifierList == null) {
                return flags;
            }
            if (modifierList.hasModifierProperty("public")) {
                flags |= AccessFlags.PUBLIC.getValue();
            } else if (modifierList.hasModifierProperty("protected")) {
                flags |= AccessFlags.PROTECTED.getValue();
            } else if (modifierList.hasModifierProperty("private")) {
                flags |= AccessFlags.PRIVATE.getValue();
            }

            if (modifierList.hasModifierProperty("static")) {
                flags |= AccessFlags.STATIC.getValue();
            }

            if (modifierList.hasModifierProperty("final")) {
                flags |= AccessFlags.FINAL.getValue();
            }

            if (modifierList.hasModifierProperty("volatile")) {
                flags |= AccessFlags.VOLATILE.getValue();
            }
            // TODO: how do we tell if it's an enum?

            return flags;
        }
    }

    @Nonnull @Override public String getDefiningClass() {
        PsiClass containingClass = psiField.getContainingClass();
        if (containingClass == null) {
            throw new RuntimeException("I don't know what to do here... Is this even possible?");
        }
        return NameUtils.javaToSmaliType(containingClass);
    }

    @Nonnull @Override public String getName() {
        return psiField.getNameIdentifier().getText();
    }

    @Nonnull @Override public String getType() {
        return NameUtils.javaToSmaliType(psiField.getType());
    }

    @Nullable @Override public EncodedValue getInitialValue() {
        // TODO: implement this. Not needed for method analysis
        return null;
    }

    @Nonnull @Override public Set<? extends Annotation> getAnnotations() {
        // TODO: implement this. Not needed for method analysis
        return ImmutableSet.of();
    }
}
