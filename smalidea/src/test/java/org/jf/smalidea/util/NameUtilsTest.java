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

package org.jf.smalidea.util;

import junit.framework.Assert;
import org.junit.Test;

public class NameUtilsTest {

    @Test
    public void testConversions() {
        testConversion("boolean", "Z");
        testConversion("byte", "B");
        testConversion("char", "C");
        testConversion("short", "S");
        testConversion("int", "I");
        testConversion("long", "J");
        testConversion("float", "F");
        testConversion("double", "D");

        testConversion("blah", "Lblah;");
        testConversion("my.blah", "Lmy/blah;");

        testConversion("boolean[]", "[Z");
        testConversion("byte[]", "[B");
        testConversion("char[]", "[C");
        testConversion("short[]", "[S");
        testConversion("int[]", "[I");
        testConversion("long[]", "[J");
        testConversion("float[]", "[F");
        testConversion("double[]", "[D");

        testConversion("blah[]", "[Lblah;");
        testConversion("my.blah[]", "[Lmy/blah;");

        testConversion("boolean[][][][]", "[[[[Z");
        testConversion("byte[][][][]", "[[[[B");
        testConversion("char[][][][]", "[[[[C");
        testConversion("short[][][][]", "[[[[S");
        testConversion("int[][][][]", "[[[[I");
        testConversion("long[][][][]", "[[[[J");
        testConversion("float[][][][]", "[[[[F");
        testConversion("double[][][][]", "[[[[D");

        testConversion("blah[][][][]", "[[[[Lblah;");
        testConversion("my.blah[][][][]", "[[[[Lmy/blah;");
    }

    private static void testConversion(String javaType, String smaliType) {
        Assert.assertEquals(javaType, NameUtils.smaliToJavaType(smaliType));
        Assert.assertEquals(smaliType, NameUtils.javaToSmaliType(javaType));
    }

    public void testShortNameFromQualifiedName() {
        Assert.assertEquals("blah", NameUtils.shortNameFromQualifiedName("org.blah.blah"));
        Assert.assertEquals("blah", NameUtils.shortNameFromQualifiedName("blah"));
    }
}
