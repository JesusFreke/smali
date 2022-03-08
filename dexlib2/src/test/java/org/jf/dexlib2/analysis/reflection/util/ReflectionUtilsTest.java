/*
 * Copyright 2013, Google Inc.
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

package org.jf.dexlib2.analysis.reflection.util;

import org.junit.Assert;
import org.junit.Test;


public class ReflectionUtilsTest {

    @Test
    public void testDexToJavaName() {

        Assert.assertEquals("some.class.name", ReflectionUtils.dexToJavaName("Lsome/class/name;"));

        Assert.assertEquals("boolean", ReflectionUtils.dexToJavaName("Z"));
        Assert.assertEquals("int", ReflectionUtils.dexToJavaName("I"));
        Assert.assertEquals("long", ReflectionUtils.dexToJavaName("J"));
        Assert.assertEquals("double", ReflectionUtils.dexToJavaName("D"));
        Assert.assertEquals("void", ReflectionUtils.dexToJavaName("V"));
        Assert.assertEquals("float", ReflectionUtils.dexToJavaName("F"));
        Assert.assertEquals("char", ReflectionUtils.dexToJavaName("C"));
        Assert.assertEquals("short", ReflectionUtils.dexToJavaName("S"));
        Assert.assertEquals("byte", ReflectionUtils.dexToJavaName("B"));

        Assert.assertEquals("[Lsome.class.name;", ReflectionUtils.dexToJavaName("[Lsome/class/name;"));
        Assert.assertEquals("[[Lsome.class.name;", ReflectionUtils.dexToJavaName("[[Lsome/class/name;"));
        Assert.assertEquals("[I", ReflectionUtils.dexToJavaName("[I"));
        Assert.assertEquals("[[I", ReflectionUtils.dexToJavaName("[[I"));

    }

    @Test
    public void testJavaToDexToName() {

        Assert.assertEquals("Lsome/class/name;", ReflectionUtils.javaToDexName("some.class.name"));

        Assert.assertEquals("Z", ReflectionUtils.javaToDexName("boolean"));
        Assert.assertEquals("I", ReflectionUtils.javaToDexName("int"));
        Assert.assertEquals("J", ReflectionUtils.javaToDexName("long"));
        Assert.assertEquals("D", ReflectionUtils.javaToDexName("double"));
        Assert.assertEquals("V", ReflectionUtils.javaToDexName("void"));
        Assert.assertEquals("F", ReflectionUtils.javaToDexName("float"));
        Assert.assertEquals("C", ReflectionUtils.javaToDexName("char"));
        Assert.assertEquals("S", ReflectionUtils.javaToDexName("short"));
        Assert.assertEquals("B", ReflectionUtils.javaToDexName("byte"));

        Assert.assertEquals("[Lsome/class/name;", ReflectionUtils.javaToDexName("[Lsome.class.name;"));
        Assert.assertEquals("[[Lsome/class/name;", ReflectionUtils.javaToDexName("[[Lsome.class.name;"));
        Assert.assertEquals("[I", ReflectionUtils.javaToDexName("[I"));
        Assert.assertEquals("[[I", ReflectionUtils.javaToDexName("[[I"));

    }
}
