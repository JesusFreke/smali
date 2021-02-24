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

import org.jf.dexlib2.iface.reference.*;
import org.jf.dexlib2.iface.value.EncodedValue;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.Writer;

import static org.mockito.Mockito.mock;

public class DexFormatterTest {

    @Test
    public void testGetMethodReference() throws IOException {
        TestDexFormatter formatter = new TestDexFormatter();
        Assert.assertEquals(
                "method descriptor",
                formatter.getMethodDescriptor(mock(MethodReference.class)));
    }

    @Test
    public void testGetShortMethodReference() throws IOException {
        TestDexFormatter formatter = new TestDexFormatter();
        Assert.assertEquals(
                "short method descriptor",
                formatter.getShortMethodDescriptor(mock(MethodReference.class)));
    }

    @Test
    public void testGetMethodProtoReference() throws IOException {
        TestDexFormatter formatter = new TestDexFormatter();
        Assert.assertEquals(
                "method proto descriptor",
                formatter.getMethodProtoDescriptor(mock(MethodProtoReference.class)));
    }

    @Test
    public void testGetFieldReference() throws IOException {
        TestDexFormatter formatter = new TestDexFormatter();
        Assert.assertEquals(
                "field descriptor",
                formatter.getFieldDescriptor(mock(FieldReference.class)));
    }

    @Test
    public void testGetShortFieldReference() throws IOException {
        TestDexFormatter formatter = new TestDexFormatter();
        Assert.assertEquals(
                "short field descriptor",
                formatter.getShortFieldDescriptor(mock(FieldReference.class)));
    }

    @Test
    public void testGetMethodHandle() throws IOException {
        TestDexFormatter formatter = new TestDexFormatter();
        Assert.assertEquals(
                "method handle",
                formatter.getMethodHandle(mock(MethodHandleReference.class)));
    }

    @Test
    public void testGetCallSite() throws IOException {
        TestDexFormatter formatter = new TestDexFormatter();
        Assert.assertEquals(
                "call site",
                formatter.getCallSite(mock(CallSiteReference.class)));
    }

    @Test
    public void testGetType() throws IOException {
        TestDexFormatter formatter = new TestDexFormatter();
        Assert.assertEquals(
                "type",
                formatter.getType("mock type"));
    }

    @Test
    public void testGetQuotedString() throws IOException {
        TestDexFormatter formatter = new TestDexFormatter();
        Assert.assertEquals(
                "quoted string",
                formatter.getQuotedString("mock string"));
    }

    @Test
    public void testGetEncodedValue() throws IOException {
        TestDexFormatter formatter = new TestDexFormatter();
        Assert.assertEquals(
                "encoded value",
                formatter.getEncodedValue(mock(EncodedValue.class)));
    }

    @Test
    public void testReference() throws IOException {
        TestDexFormatter formatter = new TestDexFormatter();
        Assert.assertEquals(
                "reference",
                formatter.getReference(mock(Reference.class)));
    }

    private static class TestDexFormatter extends DexFormatter {

        @Override public DexFormattedWriter getWriter(Writer writer) {
            return new DexFormattedWriter(writer) {
                @Override public void writeMethodDescriptor(MethodReference methodReference) throws IOException {
                    writer.write("method descriptor");
                }

                @Override public void writeShortMethodDescriptor(MethodReference methodReference) throws IOException {
                    writer.write("short method descriptor");
                }

                @Override
                public void writeMethodProtoDescriptor(MethodProtoReference protoReference) throws IOException {
                    writer.write("method proto descriptor");
                }

                @Override public void writeFieldDescriptor(FieldReference fieldReference) throws IOException {
                    writer.write("field descriptor");
                }

                @Override public void writeShortFieldDescriptor(FieldReference fieldReference) throws IOException {
                    writer.write("short field descriptor");
                }

                @Override
                public void writeMethodHandle(MethodHandleReference methodHandleReference) throws IOException {
                    writer.write("method handle");
                }

                @Override public void writeCallSite(CallSiteReference callSiteReference) throws IOException {
                    writer.write("call site");
                }

                @Override public void writeType(CharSequence type) throws IOException {
                    writer.write("type");
                }

                @Override public void writeQuotedString(CharSequence string) throws IOException {
                    writer.write("quoted string");
                }

                @Override public void writeEncodedValue(EncodedValue encodedValue) throws IOException {
                    writer.write("encoded value");
                }

                @Override public void writeReference(Reference reference) throws IOException {
                    writer.write("reference");
                }
            };
        }
    }
}
