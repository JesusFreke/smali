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

import com.intellij.psi.*;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jf.smalidea.psi.impl.SmaliClass;
import org.jf.smalidea.psi.impl.SmaliFile;
import org.jf.smalidea.psi.impl.SmaliLiteral;
import org.jf.smalidea.psi.impl.SmaliMethod;
import org.junit.Assert;

public class SmaliAnnotationTest extends LightCodeInsightFixtureTestCase {
    // TODO: test default values

    public void testClassAnnotation() {
        myFixture.addFileToProject("my/TestAnnotation.smali",
                ".class public interface abstract annotation Lmy/TestAnnotation;\n" +
                ".super Ljava/lang/Object;\n" +
                ".implements Ljava/lang/annotation/Annotation;\n" +
                "\n" +
                ".method public abstract testBooleanValue()Z\n" +
                ".end method\n" +
                "\n" +
                ".method public abstract testStringArrayValue()[Ljava/lang/String;\n" +
                ".end method\n" +
                "\n" +
                ".method public abstract testStringValue()Ljava/lang/String;\n" +
                ".end method");

        myFixture.addFileToProject("my/TestAnnotation2.smali",
                ".class public interface abstract annotation Lmy/TestAnnotation2;\n" +
                ".super Ljava/lang/Object;\n" +
                ".implements Ljava/lang/annotation/Annotation;\n");

        SmaliFile file = (SmaliFile)myFixture.addFileToProject("my/pkg/blah.smali",
                ".class public Lmy/pkg/blah; .super Ljava/lang/Object;\n" +
                "\n" +
                ".annotation runtime Lmy/TestAnnotation;\n" +
                "    testBooleanValue = true\n" +
                "    testStringValue = \"blah\"\n" +
                "    testStringArrayValue = {\n" +
                "        \"blah1\",\n" +
                "        \"blah2\"\n" +
                "    }\n" +
                ".end annotation\n" +
                "\n" +
                ".annotation runtime Lmy/TestAnnotation2;\n" +
                ".end annotation");

        SmaliClass smaliClass = file.getPsiClass();
        Assert.assertEquals("my.pkg.blah", smaliClass.getQualifiedName());

        doTest(smaliClass);
    }

    public void testFieldAnnotation() {
        myFixture.addFileToProject("my/TestAnnotation.smali",
                ".class public interface abstract annotation Lmy/TestAnnotation;\n" +
                ".super Ljava/lang/Object;\n" +
                ".implements Ljava/lang/annotation/Annotation;\n" +
                "\n" +
                ".method public abstract testBooleanValue()Z\n" +
                ".end method\n" +
                "\n" +
                ".method public abstract testStringArrayValue()[Ljava/lang/String;\n" +
                ".end method\n" +
                "\n" +
                ".method public abstract testStringValue()Ljava/lang/String;\n" +
                ".end method");

        myFixture.addFileToProject("my/TestAnnotation2.smali",
                ".class public interface abstract annotation Lmy/TestAnnotation2;\n" +
                ".super Ljava/lang/Object;\n" +
                ".implements Ljava/lang/annotation/Annotation;\n");

        SmaliFile file = (SmaliFile)myFixture.addFileToProject("my/pkg/blah.smali",
                ".class public Lmy/pkg/blah; .super Ljava/lang/Object;\n" +
                "\n" +
                ".field public myField:I\n" +
                "    .annotation runtime Lmy/TestAnnotation;\n" +
                "        testBooleanValue = true\n" +
                "        testStringValue = \"blah\"\n" +
                "        testStringArrayValue = {\n" +
                "            \"blah1\",\n" +
                "            \"blah2\"\n" +
                "        }\n" +
                "    .end annotation\n" +
                "    .annotation runtime Lmy/TestAnnotation2;\n" +
                "    .end annotation\n" +
                ".end field");

        SmaliClass smaliClass = file.getPsiClass();
        Assert.assertEquals("my.pkg.blah", smaliClass.getQualifiedName());

        PsiField field = smaliClass.findFieldByName("myField", false);
        doTest((PsiAnnotationOwner)field);
    }

    public void testMethodAnnotation() {
        myFixture.addFileToProject("my/TestAnnotation.smali",
                ".class public interface abstract annotation Lmy/TestAnnotation;\n" +
                ".super Ljava/lang/Object;\n" +
                ".implements Ljava/lang/annotation/Annotation;\n" +
                "\n" +
                ".method public abstract testBooleanValue()Z\n" +
                ".end method\n" +
                "\n" +
                ".method public abstract testStringArrayValue()[Ljava/lang/String;\n" +
                ".end method\n" +
                "\n" +
                ".method public abstract testStringValue()Ljava/lang/String;\n" +
                ".end method");

        myFixture.addFileToProject("my/TestAnnotation2.smali",
                ".class public interface abstract annotation Lmy/TestAnnotation2;\n" +
                ".super Ljava/lang/Object;\n" +
                ".implements Ljava/lang/annotation/Annotation;\n");

        SmaliFile file = (SmaliFile)myFixture.addFileToProject("my/pkg/blah.smali",
                ".class public Lmy/pkg/blah; .super Ljava/lang/Object;\n" +
                "\n" +
                ".method public myMethod()V\n" +
                "    .annotation runtime Lmy/TestAnnotation;\n" +
                "        testBooleanValue = true\n" +
                "        testStringValue = \"blah\"\n" +
                "        testStringArrayValue = {\n" +
                "            \"blah1\",\n" +
                "            \"blah2\"\n" +
                "        }\n" +
                "    .end annotation\n" +
                "    .annotation runtime Lmy/TestAnnotation2;\n" +
                "    .end annotation\n" +
                ".end method");

        SmaliClass smaliClass = file.getPsiClass();
        Assert.assertEquals("my.pkg.blah", smaliClass.getQualifiedName());

        SmaliMethod method = smaliClass.getMethods()[0];
        doTest(method);
    }

    public void doTest(PsiAnnotationOwner annotationOwner) {
        Assert.assertEquals(2, annotationOwner.getAnnotations().length);

        Assert.assertEquals("my.TestAnnotation", annotationOwner.getAnnotations()[0].getQualifiedName());
        PsiJavaCodeReferenceElement annotationNameRef = annotationOwner.getAnnotations()[0].getNameReferenceElement();
        Assert.assertNotNull(annotationNameRef);
        SmaliClass smaliAnnotationClass = (SmaliClass)annotationNameRef.resolve();
        Assert.assertNotNull(smaliAnnotationClass);
        Assert.assertEquals("my.TestAnnotation", smaliAnnotationClass.getQualifiedName());

        Assert.assertEquals("my.TestAnnotation2", annotationOwner.getAnnotations()[1].getQualifiedName());
        annotationNameRef = annotationOwner.getAnnotations()[1].getNameReferenceElement();
        Assert.assertNotNull(annotationNameRef);
        smaliAnnotationClass = (SmaliClass)annotationNameRef.resolve();
        Assert.assertNotNull(smaliAnnotationClass);
        Assert.assertEquals("my.TestAnnotation2", smaliAnnotationClass.getQualifiedName());

        PsiAnnotation smaliAnnotation = annotationOwner.findAnnotation("my.TestAnnotation");
        Assert.assertNotNull(smaliAnnotation);
        Assert.assertEquals("my.TestAnnotation", smaliAnnotation.getQualifiedName());
        PsiAnnotationOwner owner = smaliAnnotation.getOwner();
        Assert.assertNotNull(owner);
        Assert.assertSame(annotationOwner, owner);
        annotationNameRef = smaliAnnotation.getNameReferenceElement();
        Assert.assertNotNull(annotationNameRef);
        smaliAnnotationClass = (SmaliClass)annotationNameRef.resolve();
        Assert.assertNotNull(smaliAnnotationClass);
        Assert.assertEquals("my.TestAnnotation", smaliAnnotationClass.getQualifiedName());

        PsiAnnotationParameterList parameterList = smaliAnnotation.getParameterList();
        Assert.assertNotNull(parameterList);
        Assert.assertEquals(3, parameterList.getAttributes().length);
        Assert.assertEquals("testBooleanValue", parameterList.getAttributes()[0].getName());
        PsiAnnotationMemberValue value = parameterList.getAttributes()[0].getValue();
        Assert.assertNotNull(value);
        // TODO: test the values rather than the text
        Assert.assertEquals("true", value.getText());
        Assert.assertEquals("testStringValue", parameterList.getAttributes()[1].getName());
        value = parameterList.getAttributes()[1].getValue();
        Assert.assertNotNull(value);
        Assert.assertEquals("\"blah\"", value.getText());
        Assert.assertEquals("testStringArrayValue", parameterList.getAttributes()[2].getName());
        value = parameterList.getAttributes()[2].getValue();
        Assert.assertNotNull(value);
        // TODO: test the individual values, once the array literal stuff is implemented

        value = smaliAnnotation.findAttributeValue("testBooleanValue");
        Assert.assertNotNull(value);
        Assert.assertEquals("true", value.getText());

        value = smaliAnnotation.findAttributeValue("testStringValue");
        Assert.assertNotNull(value);
        Assert.assertEquals("\"blah\"", value.getText());

        value = smaliAnnotation.findAttributeValue("testStringArrayValue");
        Assert.assertNotNull(value);

        // TODO: test findAttributeValue vs findDeclaredAttributeValue for default values

        smaliAnnotation = annotationOwner.findAnnotation("my.TestAnnotation2");
        Assert.assertNotNull(smaliAnnotation);
        Assert.assertEquals("my.TestAnnotation2", smaliAnnotation.getQualifiedName());
        owner = smaliAnnotation.getOwner();
        Assert.assertNotNull(owner);
        Assert.assertSame(annotationOwner, owner);
        annotationNameRef = smaliAnnotation.getNameReferenceElement();
        Assert.assertNotNull(annotationNameRef);
        smaliAnnotationClass = (SmaliClass)annotationNameRef.resolve();
        Assert.assertNotNull(smaliAnnotationClass);
        Assert.assertEquals("my.TestAnnotation2", smaliAnnotationClass.getQualifiedName());

        parameterList = smaliAnnotation.getParameterList();
        Assert.assertNotNull(parameterList);
        Assert.assertEquals(0, parameterList.getAttributes().length);
    }

    public void testDefaultValue() {
        SmaliFile file = (SmaliFile)myFixture.addFileToProject("AnnotationWithDefaultValue.smali", "" +
                ".class public abstract interface annotation LAnnotationWithValues;\n" +
                ".super Ljava/lang/Object;\n" +
                ".implements Ljava/lang/annotation/Annotation;\n" +
                "\n" +
                ".method public abstract intValue()I\n" +
                ".end method\n" +
                "\n" +
                ".annotation system Ldalvik/annotation/AnnotationDefault;\n" +
                "    value = .subannotation LAnnotationWithValues;\n" +
                "                intValue = 4\n" +
                "            .end subannotation\n" +
                ".end annotation\n" +
                "\n");

        SmaliClass smaliClass = file.getPsiClass();
        Assert.assertNotNull(smaliClass);
        SmaliMethod method = smaliClass.getMethods()[0];
        Assert.assertEquals("intValue", method.getName());

        PsiAnnotationMemberValue defaultValue = method.getDefaultValue();
        Assert.assertTrue(defaultValue instanceof SmaliLiteral);
        Assert.assertEquals(4, ((SmaliLiteral)defaultValue).getIntegralValue());
    }
}
