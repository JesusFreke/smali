/*
 * Copyright 2015, Google Inc.
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

package org.jf.baksmali;

import com.google.common.io.ByteStreams;
import junit.framework.Assert;
import org.antlr.runtime.RecognitionException;
import org.jf.baksmali.Adaptors.ClassDefinition;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.smali.SmaliTestUtils;
import org.jf.util.IndentingWriter;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BaksmaliTestUtils {

    private static String newline = System.getProperty("line.separator");

    public static void assertSmaliCompiledEquals(String source, String expected,
                                                 BaksmaliOptions options, boolean stripComments) throws IOException,
            RecognitionException {
        ClassDef classDef = SmaliTestUtils.compileSmali(source, options.apiLevel);

        // Remove unnecessary whitespace and optionally strip all comments from smali file
        String normalizedActual = getNormalizedSmali(classDef, options, stripComments);
        String normalizedExpected = normalizeSmali(expected, stripComments);

        // Assert that normalized strings are now equal
        Assert.assertEquals(normalizedExpected, normalizedActual);
    }

    public static void assertSmaliCompiledEquals(String source, String expected,
            BaksmaliOptions options) throws IOException, RecognitionException {
        assertSmaliCompiledEquals(source, expected, options, false);
    }

    public static void assertSmaliCompiledEquals(String source, String expected)
            throws IOException, RecognitionException {
        BaksmaliOptions options = new BaksmaliOptions();
        assertSmaliCompiledEquals(source, expected, options);
    }

    @Nonnull
    public static String normalizeSmali(@Nonnull String smaliText, boolean stripComments) {
        if (stripComments) {
            smaliText = stripComments(smaliText);
        }
        return normalizeWhitespace(smaliText);
    }

    @Nonnull
    public static String getNormalizedSmali(@Nonnull ClassDef classDef, @Nonnull BaksmaliOptions options,
                                            boolean stripComments)
            throws IOException {
        StringWriter stringWriter = new StringWriter();
        IndentingWriter writer = new IndentingWriter(stringWriter);
        ClassDefinition classDefinition = new ClassDefinition(options, classDef);
        classDefinition.writeTo(writer);
        writer.close();
        return normalizeSmali(stringWriter.toString(), stripComments);
    }

    @Nonnull
    public static byte[] readResourceBytesFully(@Nonnull String fileName) throws IOException {
        InputStream smaliStream = RoundtripTest.class.getClassLoader().
                getResourceAsStream(fileName);
        if (smaliStream == null) {
            org.junit.Assert.fail("Could not load " + fileName);
        }

        return ByteStreams.toByteArray(smaliStream);
    }

    @Nonnull
    public static String readResourceFully(@Nonnull String fileName) throws IOException {
        return readResourceFully(fileName, "UTF-8");
    }

    @Nonnull
    public static String readResourceFully(@Nonnull String fileName, @Nonnull String encoding)
            throws IOException {
        return new String(readResourceBytesFully(fileName), encoding);
    }

    @Nonnull
    public static String normalizeNewlines(@Nonnull String source) {
        return normalizeNewlines(source, newline);
    }

    @Nonnull
    public static String normalizeNewlines(@Nonnull String source, String newlineValue) {
        return source.replace("\r", "").replace("\n", newlineValue);
    }

    @Nonnull
    public static String normalizeWhitespace(@Nonnull String source) {
        // Go to native system new lines so that ^/$ work correctly
        source = normalizeNewlines(source);

        // Remove all suffix/prefix whitespace
        Pattern pattern = Pattern.compile("((^[ \t]+)|([ \t]+$))", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(source);
        source = matcher.replaceAll("");

        // Remove all empty lines
        Pattern pattern2 = Pattern.compile("^\r?\n?", Pattern.MULTILINE);
        Matcher matcher2 = pattern2.matcher(source);
        source = matcher2.replaceAll("");

        // Remove a trailing new line, if present
        Pattern pattern3 = Pattern.compile("\r?\n?$");
        Matcher matcher3 = pattern3.matcher(source);
        source = matcher3.replaceAll("");

        // Go back to unix-style \n newlines
        source = normalizeNewlines(source, "\n");
        return source;
    }

    @Nonnull
    public static String stripComments(@Nonnull String source) {
        Pattern pattern = Pattern.compile("#(.*)");
        Matcher matcher = pattern.matcher(source);
        return matcher.replaceAll("");
    }

    @Test
    public void testStripComments() {
        Assert.assertEquals("", stripComments("#world"));
        Assert.assertEquals("hello", stripComments("hello#world"));
        Assert.assertEquals("multi\nline", stripComments("multi#hello world\nline#world"));
    }

    @Test
    public void testNormalizeWhitespace() {
        Assert.assertEquals("", normalizeWhitespace(" "));
        Assert.assertEquals("hello", normalizeWhitespace("hello "));
        Assert.assertEquals("hello", normalizeWhitespace(" hello"));
        Assert.assertEquals("hello", normalizeWhitespace(" hello "));
        Assert.assertEquals("hello\nworld", normalizeWhitespace("hello \n \n world"));
    }
}
