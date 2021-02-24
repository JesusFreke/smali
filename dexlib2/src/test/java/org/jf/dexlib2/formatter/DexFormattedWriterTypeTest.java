/*
 * Copyright 2021, Google Inc.
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

package org.jf.dexlib2.formatter;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

public class DexFormattedWriterTypeTest {


    @Test
    public void testWriteType_unquoted() throws IOException {
        String[] typeStrings = new String[] {
                "Ljava/lang/Object;",
                "Z",
                "B",
                "S",
                "C",
                "I",
                "J",
                "F",
                "D",
                "V",
                "[D",
                "[[D",
                "[Ljava/lang/Object;",
                "[[Ljava/lang/Object;",
                "LC;"
        };

        for (String typeString: typeStrings) {
            Assert.assertEquals(typeString, performWriteType(typeString));
        }
    }

    @Test
    public void testWriteType_invalid() throws IOException {

        assertWriteTypeFails("L;");
        assertWriteTypeFails("H");
        assertWriteTypeFails("L/blah;");
        assertWriteTypeFails("La//b;");

        assertWriteTypeFails("La//b");
        assertWriteTypeFails("La//b ");

        assertWriteTypeFails("[");

        assertWriteTypeFails("[L");

        assertWriteTypeFails("[L ");
    }

    private void assertWriteTypeFails(String input) throws IOException {
        try {
            performWriteType(input);
            Assert.fail("Expected failure did not occur");
        } catch (IllegalArgumentException ex) {
            // expected exception
        }
    }

    private String performWriteType(String input) throws IOException {
        StringWriter stringWriter = new StringWriter();
        DexFormattedWriter writer = new DexFormattedWriter(stringWriter);
        writer.writeType(input);
        return stringWriter.toString();
    }
}
