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

import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jf.smalidea.psi.impl.SmaliClass;
import org.jf.smalidea.psi.impl.SmaliClassType;
import org.jf.smalidea.psi.impl.SmaliClassTypeElement;
import org.jf.smalidea.psi.impl.SmaliFile;
import org.junit.Assert;

public class SmaliClassTypeElementTest extends LightCodeInsightFixtureTestCase {
    public void testGetType() {
        myFixture.addFileToProject("my/blarg.smali",
                ".class public Lmy/blarg; " +
                ".super Ljava/lang/Object;");

        String text = ".class public Lmy/pkg/blah; " +
                      ".super Lmy/bl<ref>arg;";

        SmaliFile file = (SmaliFile)myFixture.addFileToProject("my/pkg/blah.smali",
                text.replace("<ref>", ""));

        SmaliClassTypeElement typeElement =
                (SmaliClassTypeElement)file.findReferenceAt(text.indexOf("<ref>"));
        Assert.assertNotNull(typeElement);
        SmaliClassType type = typeElement.getType();

        Assert.assertEquals("blarg", typeElement.getName());
        Assert.assertEquals("my.blarg", typeElement.getCanonicalText());
        Assert.assertEquals("blarg", type.getClassName());
        Assert.assertEquals("my.blarg", type.getCanonicalText());

        SmaliClass resolvedClass = (SmaliClass)typeElement.resolve();
        Assert.assertNotNull(resolvedClass);
        Assert.assertEquals("my.blarg", resolvedClass.getQualifiedName());

        resolvedClass = (SmaliClass)type.resolve();
        Assert.assertNotNull(resolvedClass);
        Assert.assertEquals("my.blarg", resolvedClass.getQualifiedName());
    }

    public void testSimpleInnerClass() {
        myFixture.addFileToProject("Outer.java", "" +
                "public class Outer {" +
                "   public static class Inner {" +
                "   }" +
                "}");

        String text = ".class public Lsmali; " +
                ".super LOuter$In<ref>ner;";

        SmaliFile file = (SmaliFile)myFixture.addFileToProject("smali.smali", text.replace("<ref>", ""));

        SmaliClassTypeElement typeElement =
                (SmaliClassTypeElement)file.findReferenceAt(text.indexOf("<ref>"));
        Assert.assertNotNull(typeElement);
        SmaliClassType type = typeElement.getType();

        Assert.assertEquals("Outer.Inner", typeElement.getQualifiedName());
        Assert.assertEquals("Outer.Inner", type.getCanonicalText());
    }

    public void testInnerClassWithPackage() {
        myFixture.addFileToProject("my/Outer.java", "" +
                "package my;" +
                "public class Outer {" +
                "   public static class Inner {" +
                "   }" +
                "}");

        String text = ".class public Lsmali; " +
                ".super Lmy/Outer$In<ref>ner;";

        SmaliFile file = (SmaliFile)myFixture.addFileToProject("smali.smali", text.replace("<ref>", ""));

        SmaliClassTypeElement typeElement =
                (SmaliClassTypeElement)file.findReferenceAt(text.indexOf("<ref>"));
        Assert.assertNotNull(typeElement);
        SmaliClassType type = typeElement.getType();

        Assert.assertEquals("my.Outer.Inner", typeElement.getQualifiedName());
        Assert.assertEquals("my.Outer.Inner", type.getCanonicalText());
    }

    public void testComplexInnerClass() {
        myFixture.addFileToProject("my/Outer$blah.java", "" +
                "package my;" +
                "public class Outer$blah {" +
                "   public static class Inner {" +
                "   }" +
                "   public static class Inner$blah {" +
                "   }" +
                "}");

        String text = ".class public Lsmali; " +
                ".super Lmy/Outer$blah$In<ref>ner$blah;";

        SmaliFile file = (SmaliFile)myFixture.addFileToProject("smali.smali", text.replace("<ref>", ""));

        SmaliClassTypeElement typeElement =
                (SmaliClassTypeElement)file.findReferenceAt(text.indexOf("<ref>"));
        Assert.assertNotNull(typeElement);
        SmaliClassType type = typeElement.getType();

        Assert.assertEquals("my.Outer$blah.Inner$blah", typeElement.getQualifiedName());
        Assert.assertEquals("my.Outer$blah.Inner$blah", type.getCanonicalText());

        text = ".class public Lsmali2; " +
                ".super Lmy/Outer$blah$In<ref>ner;";

        file = (SmaliFile)myFixture.addFileToProject("smali2.smali", text.replace("<ref>", ""));

        typeElement = (SmaliClassTypeElement)file.findReferenceAt(text.indexOf("<ref>"));
        Assert.assertNotNull(typeElement);
        type = typeElement.getType();

        Assert.assertEquals("my.Outer$blah.Inner", typeElement.getQualifiedName());
        Assert.assertEquals("my.Outer$blah.Inner", type.getCanonicalText());
    }

    public void testInnerClassTrailingDollar() {
        myFixture.addFileToProject("my/Outer$blah.java", "" +
                "package my;" +
                "public class Outer$ {" +
                "   public static class Inner$ {" +
                "   }" +
                "}");

        String text = ".class public Lsmali; " +
                ".super Lmy/Outer$$In<ref>ner$;";

        SmaliFile file = (SmaliFile)myFixture.addFileToProject("smali.smali", text.replace("<ref>", ""));

        SmaliClassTypeElement typeElement =
                (SmaliClassTypeElement)file.findReferenceAt(text.indexOf("<ref>"));
        Assert.assertNotNull(typeElement);
        SmaliClassType type = typeElement.getType();

        Assert.assertEquals("my.Outer$.Inner$", typeElement.getQualifiedName());
        Assert.assertEquals("my.Outer$.Inner$", type.getCanonicalText());
    }
}
