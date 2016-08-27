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

import com.intellij.openapi.project.DumbServiceImpl;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.impl.JavaAwareProjectJdkTableImpl;
import com.intellij.psi.JavaResolveResult;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiReference;
import com.intellij.testFramework.ResolveTestCase;
import org.jf.smalidea.psi.impl.SmaliClass;
import org.jf.smalidea.psi.impl.SmaliClassTypeElement;
import org.junit.Assert;

public class ClassReferenceTest extends ResolveTestCase {
    /**
     * Test a reference to a java class from a smali class
     */
    public void testJavaReferenceFromSmali() throws Exception {
        SmaliClassTypeElement typeElement = (SmaliClassTypeElement)configureByFileText(
                ".class public Lblah; .super L<ref>java/lang/Object;", "blah.smali");

        Assert.assertNotNull(typeElement);
        Assert.assertEquals("Object", typeElement.getName());

        PsiClass psiClass = typeElement.resolve();
        Assert.assertNotNull(psiClass);
        Assert.assertEquals("java.lang.Object", psiClass.getQualifiedName());

        JavaResolveResult resolveResult = typeElement.advancedResolve(false);
        Assert.assertNotNull(resolveResult.getElement());
        Assert.assertEquals("java.lang.Object", ((PsiClass)resolveResult.getElement()).getQualifiedName());

        JavaResolveResult[] resolveResults = typeElement.multiResolve(false);
        Assert.assertEquals(1, resolveResults.length);
        Assert.assertNotNull(resolveResults[0].getElement());
        Assert.assertEquals("java.lang.Object", ((PsiClass)resolveResults[0].getElement()).getQualifiedName());
    }

    /**
     * Test a reference to a java class from a smali class, while in dumb mode
     */
    public void testJavaReferenceFromSmaliInDumbMode() throws Exception {
        SmaliClassTypeElement typeElement = (SmaliClassTypeElement)configureByFileText(
                ".class public Lblah; .super L<ref>java/lang/Object;", "blah.smali");

        Assert.assertNotNull(typeElement);
        Assert.assertEquals("Object", typeElement.getName());

        DumbServiceImpl.getInstance(getProject()).setDumb(true);

        PsiClass psiClass = typeElement.resolve();
        Assert.assertNull(psiClass);

        DumbServiceImpl.getInstance(getProject()).setDumb(false);
    }

    /**
     * Test a reference to a smali class from a smali class
     */
    public void testSmaliReferenceFromSmali() throws Exception {
        createFile("blarg.smali", ".class public Lblarg; .super Ljava/lang/Object;");

        SmaliClassTypeElement typeElement = (SmaliClassTypeElement)configureByFileText(
                ".class public Lblah; .super L<ref>blarg;", "blah.smali");

        Assert.assertEquals("blarg", typeElement.getName());

        SmaliClass smaliClass = (SmaliClass)typeElement.resolve();
        Assert.assertNotNull(smaliClass);
        Assert.assertEquals("blarg", smaliClass.getQualifiedName());

        JavaResolveResult resolveResult = typeElement.advancedResolve(false);
        Assert.assertNotNull(resolveResult.getElement());
        Assert.assertEquals("blarg", ((PsiClass)resolveResult.getElement()).getQualifiedName());

        JavaResolveResult[] resolveResults = typeElement.multiResolve(false);
        Assert.assertEquals(1, resolveResults.length);
        Assert.assertNotNull(resolveResults[0].getElement());
        Assert.assertEquals("blarg", ((PsiClass)resolveResults[0].getElement()).getQualifiedName());
    }

    /**
     * Test a reference to a smali class from a java class
     */
    public void testSmaliReferenceFromJava() throws Exception {
        createFile("blarg.smali", ".class public Lblarg; .super Ljava/lang/Object;");

        PsiReference reference = configureByFileText(
                "public class blah extends bla<ref>rg { }", "blah.java");

        SmaliClass smaliClass = (SmaliClass)reference.resolve();
        Assert.assertNotNull(smaliClass);
        Assert.assertEquals("blarg", smaliClass.getQualifiedName());
    }



    @Override
    protected Sdk getTestProjectJdk() {
        return JavaAwareProjectJdkTableImpl.getInstanceEx().getInternalJdk();
    }
}
