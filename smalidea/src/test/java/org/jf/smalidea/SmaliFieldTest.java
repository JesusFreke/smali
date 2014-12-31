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

import com.intellij.psi.PsiField;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiTypeElement;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jf.dexlib2.AccessFlags;
import org.jf.smalidea.psi.impl.SmaliClass;
import org.jf.smalidea.psi.impl.SmaliField;
import org.jf.smalidea.psi.impl.SmaliFile;
import org.jf.smalidea.psi.impl.SmaliModifierList;
import org.junit.Assert;

public class SmaliFieldTest extends LightCodeInsightFixtureTestCase {
    public void testBasicField() {
        SmaliFile file = (SmaliFile)myFixture.addFileToProject("my/pkg/blah.smali",
                ".class public Lmy/pkg/blah; .super Ljava/lang/Object;\n" +
                ".field public myField:I");

        SmaliClass smaliClass = file.getPsiClass();
        Assert.assertEquals("my.pkg.blah", smaliClass.getQualifiedName());

        SmaliField[] fields = smaliClass.getFields();
        Assert.assertEquals(1, fields.length);
        Assert.assertEquals("myField", fields[0].getName());
        Assert.assertTrue(fields[0].getType() instanceof PsiPrimitiveType);
        Assert.assertEquals("int", fields[0].getType().getCanonicalText());
        PsiTypeElement typeElement = fields[0].getTypeElement();
        Assert.assertNotNull("I", typeElement);
        Assert.assertEquals("I", typeElement.getText());

        SmaliModifierList modifierList = fields[0].getModifierList();
        Assert.assertNotNull(modifierList);
        Assert.assertEquals(AccessFlags.PUBLIC.getValue(), modifierList.getAccessFlags());
        Assert.assertTrue(modifierList.hasExplicitModifier("public"));
        Assert.assertTrue(modifierList.hasModifierProperty("public"));
        Assert.assertTrue(fields[0].hasModifierProperty("public"));

        PsiField[] psifields = smaliClass.getAllFields();
        Assert.assertEquals(1, psifields.length);
        Assert.assertEquals("myField", psifields[0].getName());

        PsiField field = smaliClass.findFieldByName("myField", true);
        Assert.assertNotNull(field);
        Assert.assertEquals("myField", field.getName());

        field = smaliClass.findFieldByName("nonExistantField", true);
        Assert.assertNull(field);
        field = smaliClass.findFieldByName("nonExistantField", false);
        Assert.assertNull(field);
    }

    public void testSmaliSuperField() {
        myFixture.addFileToProject("my/pkg/base.smali",
                ".class public Lmy/pkg/base; .super Ljava/lang/Object;\n" +
                ".field public baseField:I");

        SmaliFile file = (SmaliFile)myFixture.addFileToProject("my/pkg/blah.smali",
                ".class public Lmy/pkg/blah; .super Lmy/pkg/base;\n" +
                ".field public myField:I");

        SmaliClass smaliClass = file.getPsiClass();
        Assert.assertEquals("my.pkg.blah", smaliClass.getQualifiedName());

        PsiField[] fields = smaliClass.getFields();
        Assert.assertEquals(1, fields.length);
        Assert.assertEquals("myField", fields[0].getName());

        fields = smaliClass.getAllFields();
        Assert.assertEquals(2, fields.length);

        Assert.assertTrue(fields[0].getName().equals("myField") || fields[1].getName().equals("myField"));
        Assert.assertTrue(fields[0].getName().equals("baseField") || fields[1].getName().equals("baseField"));

        PsiField field = smaliClass.findFieldByName("myField", true);
        Assert.assertNotNull(field);
        Assert.assertEquals("myField", field.getName());

        field = smaliClass.findFieldByName("myField", false);
        Assert.assertNotNull(field);
        Assert.assertEquals("myField", field.getName());

        field = smaliClass.findFieldByName("baseField", false);
        Assert.assertNull(field);

        field = smaliClass.findFieldByName("baseField", true);
        Assert.assertNotNull(field);
        Assert.assertEquals("baseField", field.getName());

        field = smaliClass.findFieldByName("nonExistantField", true);
        Assert.assertNull(field);
        field = smaliClass.findFieldByName("nonExistantField", false);
        Assert.assertNull(field);
    }

    public void testJavaSuperField() {
        myFixture.addFileToProject("my/pkg/base.java",
                "package my.pkg; public class base { public int baseField; }");

        SmaliFile file = (SmaliFile)myFixture.addFileToProject("my/pkg/blah.smali",
                ".class public Lmy/pkg/blah; .super Lmy/pkg/base;\n" +
                        ".field public myField:I");

        SmaliClass smaliClass = file.getPsiClass();
        Assert.assertEquals("my.pkg.blah", smaliClass.getQualifiedName());

        PsiField[] fields = smaliClass.getFields();
        Assert.assertEquals(1, fields.length);
        Assert.assertEquals("myField", fields[0].getName());

        fields = smaliClass.getAllFields();
        Assert.assertEquals(2, fields.length);

        Assert.assertTrue(fields[0].getName().equals("myField") || fields[1].getName().equals("myField"));
        Assert.assertTrue(fields[0].getName().equals("baseField") || fields[1].getName().equals("baseField"));

        PsiField field = smaliClass.findFieldByName("myField", true);
        Assert.assertNotNull(field);
        Assert.assertEquals("myField", field.getName());

        field = smaliClass.findFieldByName("myField", false);
        Assert.assertNotNull(field);
        Assert.assertEquals("myField", field.getName());

        field = smaliClass.findFieldByName("baseField", false);
        Assert.assertNull(field);

        field = smaliClass.findFieldByName("baseField", true);
        Assert.assertNotNull(field);
        Assert.assertEquals("baseField", field.getName());

        field = smaliClass.findFieldByName("nonExistantField", true);
        Assert.assertNull(field);
        field = smaliClass.findFieldByName("nonExistantField", false);
        Assert.assertNull(field);
    }

    public void testMultipleField() {
        SmaliFile file = (SmaliFile)myFixture.addFileToProject("my/pkg/blah.smali",
                ".class public Lmy/pkg/blah; .super Ljava/lang/Object;\n" +
                ".field public myField:I\n" +
                ".field public myField2:Ljava/lang/String;\n" +
                ".field public myField3:[Ljava/lang/String;\n" +
                ".field public myField4:[[[Ljava/lang/String;\n");

        SmaliClass smaliClass = file.getPsiClass();
        Assert.assertEquals("my.pkg.blah", smaliClass.getQualifiedName());

        SmaliField[] fields = smaliClass.getFields();
        Assert.assertEquals(4, fields.length);
        Assert.assertEquals("myField", fields[0].getName());
        Assert.assertEquals("myField2", fields[1].getName());
        Assert.assertEquals("myField3", fields[2].getName());
        Assert.assertEquals("myField4", fields[3].getName());
        Assert.assertEquals("int", fields[0].getType().getCanonicalText());
        Assert.assertEquals("java.lang.String", fields[1].getType().getCanonicalText());
        Assert.assertEquals("java.lang.String[]", fields[2].getType().getCanonicalText());
        Assert.assertEquals("java.lang.String[][][]", fields[3].getType().getCanonicalText());

        PsiField field = smaliClass.findFieldByName("myField", true);
        Assert.assertNotNull(field);
        Assert.assertEquals("myField", field.getName());

        field = smaliClass.findFieldByName("myField2", true);
        Assert.assertNotNull(field);
        Assert.assertEquals("myField2", field.getName());

        field = smaliClass.findFieldByName("myField3", true);
        Assert.assertNotNull(field);
        Assert.assertEquals("myField3", field.getName());

        field = smaliClass.findFieldByName("myField4", true);
        Assert.assertNotNull(field);
        Assert.assertEquals("myField4", field.getName());

        field = smaliClass.findFieldByName("nonExistantField", true);
        Assert.assertNull(field);
        field = smaliClass.findFieldByName("nonExistantField", false);
        Assert.assertNull(field);
    }

    public void testFieldAnnotations() {
        SmaliFile file = (SmaliFile)myFixture.addFileToProject("my/pkg/blah.smali",
                ".class public Lmy/pkg/blah; .super Ljava/lang/Object;\n" +
                        ".field public myField:I");

        SmaliClass smaliClass = file.getPsiClass();
        Assert.assertEquals("my.pkg.blah", smaliClass.getQualifiedName());

        SmaliField[] fields = smaliClass.getFields();
        Assert.assertEquals(1, fields.length);
        Assert.assertEquals("myField", fields[0].getName());
        Assert.assertTrue(fields[0].getType() instanceof PsiPrimitiveType);
        Assert.assertEquals("int", fields[0].getType().getCanonicalText());
        PsiTypeElement typeElement = fields[0].getTypeElement();
        Assert.assertNotNull("I", typeElement);
        Assert.assertEquals("I", typeElement.getText());

        SmaliModifierList modifierList = fields[0].getModifierList();
        Assert.assertNotNull(modifierList);
        Assert.assertEquals(AccessFlags.PUBLIC.getValue(), modifierList.getAccessFlags());
        Assert.assertTrue(modifierList.hasExplicitModifier("public"));
        Assert.assertTrue(modifierList.hasModifierProperty("public"));
        Assert.assertTrue(fields[0].hasModifierProperty("public"));

        PsiField[] psifields = smaliClass.getAllFields();
        Assert.assertEquals(1, psifields.length);
        Assert.assertEquals("myField", psifields[0].getName());

        PsiField field = smaliClass.findFieldByName("myField", true);
        Assert.assertNotNull(field);
        Assert.assertEquals("myField", field.getName());

        field = smaliClass.findFieldByName("nonExistantField", true);
        Assert.assertNull(field);
        field = smaliClass.findFieldByName("nonExistantField", false);
        Assert.assertNull(field);
    }
}
