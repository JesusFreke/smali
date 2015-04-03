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

package org.jf.smalidea;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jf.dexlib2.AccessFlags;
import org.jf.smalidea.psi.impl.SmaliAnnotation;
import org.jf.smalidea.psi.impl.SmaliClass;
import org.jf.smalidea.psi.impl.SmaliFile;
import org.jf.smalidea.psi.impl.SmaliModifierList;
import org.junit.Assert;

public class SmaliClassModifierListTest extends LightCodeInsightFixtureTestCase {
    public void testAllClassAccessFlags() {
        SmaliFile file = (SmaliFile)myFixture.addFileToProject("my/pkg/blah.smali",
                ".class public final interface abstract synthetic enum annotation Lmy/pkg/blah; " +
                ".super Ljava/lang/Object;");

        SmaliClass smaliClass = file.getPsiClass();
        SmaliModifierList modifierList = smaliClass.getModifierList();

        Assert.assertEquals(AccessFlags.PUBLIC.getValue() |
                        AccessFlags.FINAL.getValue() |
                        AccessFlags.INTERFACE.getValue() |
                        AccessFlags.ABSTRACT.getValue() |
                        AccessFlags.SYNTHETIC.getValue() |
                        AccessFlags.ENUM.getValue() |
                        AccessFlags.ANNOTATION.getValue(),
                modifierList.getAccessFlags());

        Assert.assertTrue(modifierList.hasModifierProperty("public"));
        Assert.assertTrue(modifierList.hasModifierProperty("final"));
        Assert.assertTrue(modifierList.hasModifierProperty("interface"));
        Assert.assertTrue(modifierList.hasModifierProperty("abstract"));
        Assert.assertTrue(modifierList.hasModifierProperty("synthetic"));
        Assert.assertTrue(modifierList.hasModifierProperty("enum"));
        Assert.assertTrue(modifierList.hasModifierProperty("annotation"));

        Assert.assertTrue(modifierList.hasExplicitModifier("public"));
        Assert.assertTrue(modifierList.hasExplicitModifier("final"));
        Assert.assertTrue(modifierList.hasExplicitModifier("interface"));
        Assert.assertTrue(modifierList.hasExplicitModifier("abstract"));
        Assert.assertTrue(modifierList.hasExplicitModifier("synthetic"));
        Assert.assertTrue(modifierList.hasExplicitModifier("enum"));
        Assert.assertTrue(modifierList.hasExplicitModifier("annotation"));
    }

    public void testNoClassAccessFlags() {
        SmaliFile file = (SmaliFile)myFixture.addFileToProject("my/pkg/blah.smali",
                ".class Lmy/pkg/blah; " +
                ".super Ljava/lang/Object;");

        SmaliClass smaliClass = file.getPsiClass();
        SmaliModifierList modifierList = smaliClass.getModifierList();

        Assert.assertEquals(0, modifierList.getAccessFlags());

        Assert.assertFalse(modifierList.hasModifierProperty("public"));
        Assert.assertFalse(modifierList.hasModifierProperty("final"));
        Assert.assertFalse(modifierList.hasModifierProperty("interface"));
        Assert.assertFalse(modifierList.hasModifierProperty("abstract"));
        Assert.assertFalse(modifierList.hasModifierProperty("synthetic"));
        Assert.assertFalse(modifierList.hasModifierProperty("enum"));
        Assert.assertFalse(modifierList.hasModifierProperty("annotation"));

        Assert.assertFalse(modifierList.hasExplicitModifier("public"));
        Assert.assertFalse(modifierList.hasExplicitModifier("final"));
        Assert.assertFalse(modifierList.hasExplicitModifier("interface"));
        Assert.assertFalse(modifierList.hasExplicitModifier("abstract"));
        Assert.assertFalse(modifierList.hasExplicitModifier("synthetic"));
        Assert.assertFalse(modifierList.hasExplicitModifier("enum"));
        Assert.assertFalse(modifierList.hasExplicitModifier("annotation"));
    }

    public void testAddClassAccessFlag() {
        final SmaliFile file = (SmaliFile)myFixture.addFileToProject("my/pkg/blah.smali",
                ".class public Lmy/pkg/blah;\n" +
                ".super Ljava/lang/Object;");
        myFixture.configureFromExistingVirtualFile(file.getVirtualFile());

        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override public void run() {
                file.getPsiClass().getModifierList().setModifierProperty("final", true);
            }
        });

        myFixture.checkResult(
                ".class public final Lmy/pkg/blah;\n" +
                ".super Ljava/lang/Object;");
    }

    public void testRemoveClassAccessFlag() {
        final SmaliFile file = (SmaliFile)myFixture.addFileToProject("my/pkg/blah.smali",
                ".class public final Lmy/pkg/blah;\n" +
                ".super Ljava/lang/Object;");
        myFixture.configureFromExistingVirtualFile(file.getVirtualFile());

        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override public void run() {
                file.getPsiClass().getModifierList().setModifierProperty("final", false);
            }
        });

        myFixture.checkResult(
                ".class public Lmy/pkg/blah;\n" +
                ".super Ljava/lang/Object;");
    }

    public void testBasicAnnotation() {
        final SmaliFile file = (SmaliFile)myFixture.addFileToProject("my/pkg/blah.smali",
                ".class public final Lmy/pkg/blah;\n" +
                ".super Ljava/lang/Object;\n" +
                ".annotation Lmy/pkg/anno; .end annotation");

        SmaliClass smaliClass = file.getPsiClass();
        SmaliModifierList modifierList = smaliClass.getModifierList();

        SmaliAnnotation[] annotations = modifierList.getAnnotations();
        Assert.assertEquals(1, annotations.length);

        Assert.assertEquals("my.pkg.anno", annotations[0].getQualifiedName());

        SmaliAnnotation[] applicableAnnotations = modifierList.getApplicableAnnotations();
        Assert.assertEquals(1, applicableAnnotations.length);
        Assert.assertEquals(annotations[0], applicableAnnotations[0]);
    }

    public void testNoAnnotation() {
        final SmaliFile file = (SmaliFile)myFixture.addFileToProject("my/pkg/blah.smali",
                ".class public final Lmy/pkg/blah;\n" +
                ".super Ljava/lang/Object;");

        SmaliClass smaliClass = file.getPsiClass();
        SmaliModifierList modifierList = smaliClass.getModifierList();

        // Ensures that the parent of the modifier list is a PsiModifierListOwner
        // e.g. for code like JavaSuppressionUtil.getInspectionIdsSuppressedInAnnotation,
        // which assumes the parent is a PsiModifierListOwner
        Assert.assertTrue(modifierList.getParent() instanceof PsiModifierListOwner);

        Assert.assertEquals(0, modifierList.getAnnotations().length);
        Assert.assertEquals(0, modifierList.getApplicableAnnotations().length);
    }

    public void testFindAnnotation() {
        final SmaliFile file = (SmaliFile)myFixture.addFileToProject("my/pkg/blah.smali",
                ".class public final Lmy/pkg/blah;\n" +
                ".annotation Lanno; .end annotation\n" +
                ".super Ljava/lang/Object;\n" +
                ".annotation Lmy/pkg/anno; .end annotation\n" +
                ".annotation Lmy/pkg/anno2; .end annotation\n" +
                ".annotation Lmy/pkg/anno3; .end annotation\n");

        SmaliClass smaliClass = file.getPsiClass();
        SmaliModifierList modifierList = smaliClass.getModifierList();

        SmaliAnnotation smaliAnnotation = modifierList.findAnnotation("my.pkg.anno2");
        Assert.assertNotNull(smaliAnnotation);
        Assert.assertEquals("my.pkg.anno2", smaliAnnotation.getQualifiedName());
    }

    // TODO: test modifierList.addAnnotation once implemented
}
