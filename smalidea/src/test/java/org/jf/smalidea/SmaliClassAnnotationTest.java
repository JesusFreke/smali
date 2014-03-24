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

import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import junit.framework.Assert;
import org.jf.smalidea.psi.impl.SmaliAnnotation;
import org.jf.smalidea.psi.impl.SmaliAnnotationParameterList;
import org.jf.smalidea.psi.impl.SmaliClass;
import org.jf.smalidea.psi.impl.SmaliFile;

public class SmaliClassAnnotationTest extends LightCodeInsightFixtureTestCase {
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

        Assert.assertEquals(2, smaliClass.getAnnotations().length);

        Assert.assertEquals("my.TestAnnotation", smaliClass.getAnnotations()[0].getQualifiedName());
        PsiJavaCodeReferenceElement annotationNameRef = smaliClass.getAnnotations()[0].getNameReferenceElement();
        Assert.assertNotNull(annotationNameRef);
        SmaliClass smaliAnnotationClass = (SmaliClass)annotationNameRef.resolve();
        Assert.assertNotNull(smaliAnnotationClass);
        Assert.assertEquals("my.TestAnnotation", smaliAnnotationClass.getQualifiedName());

        Assert.assertEquals("my.TestAnnotation2", smaliClass.getAnnotations()[1].getQualifiedName());
        annotationNameRef = smaliClass.getAnnotations()[1].getNameReferenceElement();
        Assert.assertNotNull(annotationNameRef);
        smaliAnnotationClass = (SmaliClass)annotationNameRef.resolve();
        Assert.assertNotNull(smaliAnnotationClass);
        Assert.assertEquals("my.TestAnnotation2", smaliAnnotationClass.getQualifiedName());

        SmaliAnnotation smaliAnnotation = smaliClass.findAnnotation("my.TestAnnotation");
        Assert.assertNotNull(smaliAnnotation);
        Assert.assertEquals("my.TestAnnotation", smaliAnnotation.getQualifiedName());
        SmaliClass owner = (SmaliClass)smaliAnnotation.getOwner();
        Assert.assertNotNull(owner);
        Assert.assertEquals("my.pkg.blah", owner.getQualifiedName());
        annotationNameRef = smaliAnnotation.getNameReferenceElement();
        Assert.assertNotNull(annotationNameRef);
        smaliAnnotationClass = (SmaliClass)annotationNameRef.resolve();
        Assert.assertNotNull(smaliAnnotationClass);
        Assert.assertEquals("my.TestAnnotation", smaliAnnotationClass.getQualifiedName());

        SmaliAnnotationParameterList parameterList = smaliAnnotation.getParameterList();
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

        smaliAnnotation = smaliClass.findAnnotation("my.TestAnnotation2");
        Assert.assertNotNull(smaliAnnotation);
        Assert.assertEquals("my.TestAnnotation2", smaliAnnotation.getQualifiedName());
        owner = (SmaliClass)smaliAnnotation.getOwner();
        Assert.assertNotNull(owner);
        Assert.assertEquals("my.pkg.blah", owner.getQualifiedName());
        annotationNameRef = smaliAnnotation.getNameReferenceElement();
        Assert.assertNotNull(annotationNameRef);
        smaliAnnotationClass = (SmaliClass)annotationNameRef.resolve();
        Assert.assertNotNull(smaliAnnotationClass);
        Assert.assertEquals("my.TestAnnotation2", smaliAnnotationClass.getQualifiedName());

        parameterList = smaliAnnotation.getParameterList();
        Assert.assertNotNull(parameterList);
        Assert.assertEquals(0, parameterList.getAttributes().length);
    }
}
