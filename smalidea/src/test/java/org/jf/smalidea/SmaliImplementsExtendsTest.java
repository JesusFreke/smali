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

import com.intellij.psi.PsiClass;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jf.smalidea.psi.impl.SmaliClass;
import org.jf.smalidea.psi.impl.SmaliExtendsList;
import org.jf.smalidea.psi.impl.SmaliFile;
import org.jf.smalidea.psi.impl.SmaliImplementsList;
import org.junit.Assert;

public class SmaliImplementsExtendsTest extends LightCodeInsightFixtureTestCase {
    public void testNormalClass() {
        myFixture.addFileToProject("my/pkg/base.smali",
                ".class public Lmy/pkg/base; .super Ljava/lang/Object;");
        myFixture.addFileToProject("my/pkg/iface.smali",
                ".class public Lmy/pkg/iface; .super Ljava/lang/Object;");
        myFixture.addFileToProject("my/pkg/iface2.smali",
                ".class public Lmy/pkg/iface2; .super Ljava/lang/Object;");

        SmaliFile file = (SmaliFile)myFixture.addFileToProject("my/pkg/blah.smali",
                ".class public Lmy/pkg/blah; .implements Lmy/pkg/iface; .super Lmy/pkg/base; " +
                ".implements Lmy/pkg/iface2;");

        SmaliClass smaliClass = file.getPsiClass();
        SmaliExtendsList extendsList = smaliClass.getExtendsList();
        Assert.assertEquals(1, extendsList.getReferencedTypes().length);
        Assert.assertEquals("my.pkg.base", extendsList.getReferencedTypes()[0].getCanonicalText());
        Assert.assertEquals(1, extendsList.getReferenceNames().length);
        Assert.assertEquals("my.pkg.base", extendsList.getReferenceNames()[0]);
        Assert.assertEquals(1, smaliClass.getExtendsListTypes().length);
        Assert.assertEquals("my.pkg.base", smaliClass.getExtendsListTypes()[0].getCanonicalText());

        PsiClass resolvedSuper = extendsList.getReferencedTypes()[0].resolve();
        Assert.assertNotNull(resolvedSuper);
        Assert.assertEquals("my.pkg.base", resolvedSuper.getQualifiedName());

        SmaliImplementsList implementsList = smaliClass.getImplementsList();
        Assert.assertEquals(2, implementsList.getReferencedTypes().length);
        Assert.assertEquals("my.pkg.iface", implementsList.getReferencedTypes()[0].getCanonicalText());
        Assert.assertEquals("my.pkg.iface2", implementsList.getReferencedTypes()[1].getCanonicalText());
        Assert.assertEquals(2, implementsList.getReferenceNames().length);
        Assert.assertEquals("my.pkg.iface", implementsList.getReferenceNames()[0]);
        Assert.assertEquals("my.pkg.iface2", implementsList.getReferenceNames()[1]);
        Assert.assertEquals(2, smaliClass.getImplementsListTypes().length);
        Assert.assertEquals("my.pkg.iface", smaliClass.getImplementsListTypes()[0].getCanonicalText());
        Assert.assertEquals("my.pkg.iface2", smaliClass.getImplementsListTypes()[1].getCanonicalText());

        PsiClass resolvedInterface = implementsList.getReferencedTypes()[0].resolve();
        Assert.assertNotNull(resolvedInterface);
        Assert.assertEquals("my.pkg.iface", resolvedInterface.getQualifiedName());

        resolvedInterface = implementsList.getReferencedTypes()[1].resolve();
        Assert.assertNotNull(resolvedInterface);
        Assert.assertEquals("my.pkg.iface2", resolvedInterface.getQualifiedName());
    }

    public void testInterface() {
        myFixture.addFileToProject("my/pkg/iface.smali",
                ".class public Lmy/pkg/iface; .super Ljava/lang/Object;");
        myFixture.addFileToProject("my/pkg/iface2.smali",
                ".class public Lmy/pkg/iface2; .super Ljava/lang/Object;");

        SmaliFile file = (SmaliFile)myFixture.addFileToProject("my/pkg/blah.smali",
                ".class public interface Lmy/pkg/blah; .implements Lmy/pkg/iface; .super Ljava/lang/Object; " +
                        ".implements Lmy/pkg/iface2;");

        SmaliClass smaliClass = file.getPsiClass();
        SmaliExtendsList extendsList = smaliClass.getExtendsList();

        Assert.assertEquals(2, extendsList.getReferencedTypes().length);
        Assert.assertEquals("my.pkg.iface", extendsList.getReferencedTypes()[0].getCanonicalText());
        Assert.assertEquals("my.pkg.iface2", extendsList.getReferencedTypes()[1].getCanonicalText());
        Assert.assertEquals(2, extendsList.getReferenceNames().length);
        Assert.assertEquals("my.pkg.iface", extendsList.getReferenceNames()[0]);
        Assert.assertEquals("my.pkg.iface2", extendsList.getReferenceNames()[1]);
        Assert.assertEquals(2, smaliClass.getExtendsListTypes().length);
        Assert.assertEquals("my.pkg.iface", smaliClass.getExtendsListTypes()[0].getCanonicalText());
        Assert.assertEquals("my.pkg.iface2", smaliClass.getExtendsListTypes()[1].getCanonicalText());

        PsiClass resolvedInterface = extendsList.getReferencedTypes()[0].resolve();
        Assert.assertNotNull(resolvedInterface);
        Assert.assertEquals("my.pkg.iface", resolvedInterface.getQualifiedName());

        resolvedInterface = extendsList.getReferencedTypes()[1].resolve();
        Assert.assertNotNull(resolvedInterface);
        Assert.assertEquals("my.pkg.iface2", resolvedInterface.getQualifiedName());

        SmaliImplementsList implementsList = smaliClass.getImplementsList();
        Assert.assertEquals(0, implementsList.getReferencedTypes().length);
        Assert.assertEquals(0, implementsList.getReferenceNames().length);
        Assert.assertEquals(0, smaliClass.getImplementsListTypes().length);
    }
}
