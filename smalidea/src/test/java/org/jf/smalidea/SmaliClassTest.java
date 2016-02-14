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

import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.impl.JavaAwareProjectJdkTableImpl;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.LanguageLevelModuleExtension;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElementFactory;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jf.smalidea.psi.impl.SmaliClass;
import org.jf.smalidea.psi.impl.SmaliFile;
import org.junit.Assert;

public class SmaliClassTest extends LightCodeInsightFixtureTestCase {
    public void testName() {
        SmaliFile file = (SmaliFile)myFixture.addFileToProject("my/pkg/blah.smali",
                ".class public Lmy/pkg/blah; .super Ljava/lang/Object;");

        SmaliClass smaliClass = file.getPsiClass();
        Assert.assertEquals("my.pkg.blah", smaliClass.getQualifiedName());
        Assert.assertEquals("my.pkg", smaliClass.getPackageName());
        Assert.assertEquals("blah", smaliClass.getName());
    }

    public void testEmptyPackageName() {
        SmaliFile file = (SmaliFile)myFixture.addFileToProject("my/pkg/blah.smali",
                ".class public Lblah; .super Ljava/lang/Object;");

        SmaliClass smaliClass = file.getPsiClass();
        Assert.assertEquals("blah", smaliClass.getQualifiedName());
        Assert.assertEquals("", smaliClass.getPackageName());
    }

    public void testGetSuperclass() {
        myFixture.addFileToProject("base.smali",
                ".class public interface Lbase; .super Ljava/lang/Object;");

        myFixture.addFileToProject("iface.smali",
                ".class public interface Liface; .super Ljava/lang/Object;");

        SmaliFile file = (SmaliFile)myFixture.addFileToProject("blah.smali",
                ".class public Lblah; .super Lbase; .implements Liface;");

        SmaliClass smaliClass = file.getPsiClass();
        Assert.assertEquals("blah", smaliClass.getQualifiedName());
        PsiClass superClass = smaliClass.getSuperClass();
        Assert.assertNotNull(superClass);
        Assert.assertEquals("base", smaliClass.getSuperClass().getQualifiedName());

        Assert.assertEquals(2, smaliClass.getSupers().length);
        Assert.assertEquals("base", smaliClass.getSupers()[0].getQualifiedName());
        Assert.assertEquals("iface", smaliClass.getSupers()[1].getQualifiedName());

        Assert.assertEquals(2, smaliClass.getSuperTypes().length);
        Assert.assertEquals("base", smaliClass.getSuperTypes()[0].getCanonicalText());
        Assert.assertEquals("iface", smaliClass.getSuperTypes()[1].getCanonicalText());

        Assert.assertEquals(1, smaliClass.getInterfaces().length);
        Assert.assertEquals("iface", smaliClass.getInterfaces()[0].getQualifiedName());
    }

    public void testGetSuperclassForInterface() {
        myFixture.addFileToProject("iface.smali",
                ".class public interface Liface; .super Ljava/lang/Object;");

        SmaliFile file = (SmaliFile)myFixture.addFileToProject("blah.smali",
                ".class public interface Lblah; .super Ljava/lang/Object; .implements Liface;");

        SmaliClass smaliClass = file.getPsiClass();
        Assert.assertEquals("blah", smaliClass.getQualifiedName());
        PsiClass superClass = smaliClass.getSuperClass();
        Assert.assertNotNull(superClass);
        Assert.assertEquals("java.lang.Object", smaliClass.getSuperClass().getQualifiedName());

        Assert.assertEquals(2, smaliClass.getSupers().length);
        Assert.assertEquals("java.lang.Object", smaliClass.getSupers()[0].getQualifiedName());
        Assert.assertEquals("iface", smaliClass.getSupers()[1].getQualifiedName());

        Assert.assertEquals(1, smaliClass.getSuperTypes().length);
        Assert.assertEquals("iface", smaliClass.getSuperTypes()[0].getCanonicalText());

        Assert.assertEquals(1, smaliClass.getInterfaces().length);
        Assert.assertEquals("iface", smaliClass.getInterfaces()[0].getQualifiedName());
    }

    public void testIsInheritor() {
        SmaliFile file = (SmaliFile)myFixture.addFileToProject("blah.smali",
                ".class public Lblah; .super Ljava/lang/Exception;");
        SmaliClass smaliClass = file.getPsiClass();
        Assert.assertEquals("blah", smaliClass.getQualifiedName());

        PsiElementFactory factory = JavaPsiFacade.getInstance(getProject()).getElementFactory();
        PsiClassType throwableType = factory.createTypeByFQClassName("java.lang.Throwable", file.getResolveScope());
        PsiClass throwableClass = throwableType.resolve();
        Assert.assertNotNull(throwableClass);

        PsiClassType exceptionType = factory.createTypeByFQClassName("java.lang.Exception", file.getResolveScope());
        PsiClass exceptionClass = exceptionType.resolve();
        Assert.assertNotNull(exceptionClass);

        PsiClassType objectType = factory.createTypeByFQClassName("java.lang.Object", file.getResolveScope());
        PsiClass objectClass = objectType.resolve();
        Assert.assertNotNull(objectClass);

        Assert.assertTrue(smaliClass.isInheritor(exceptionClass, true));
        Assert.assertTrue(smaliClass.isInheritor(throwableClass, true));
        Assert.assertTrue(smaliClass.isInheritor(objectClass, true));

        Assert.assertTrue(smaliClass.isInheritorDeep(exceptionClass, null));
        Assert.assertTrue(smaliClass.isInheritorDeep(throwableClass, null));
        Assert.assertTrue(smaliClass.isInheritorDeep(objectClass, null));

        Assert.assertTrue(smaliClass.isInheritor(exceptionClass, false));
        Assert.assertFalse(smaliClass.isInheritor(throwableClass, false));
        Assert.assertFalse(smaliClass.isInheritor(objectClass, false));
    }

    @NotNull @Override protected LightProjectDescriptor getProjectDescriptor() {
        return new DefaultLightProjectDescriptor() {
            public Sdk getSdk() {
                return JavaAwareProjectJdkTableImpl.getInstanceEx().getInternalJdk();
            }

            public void configureModule(Module module, ModifiableRootModel model, ContentEntry contentEntry) {
                model.getModuleExtension(LanguageLevelModuleExtension.class).setLanguageLevel(LanguageLevel.JDK_1_6);
            }
        };
    }
}
