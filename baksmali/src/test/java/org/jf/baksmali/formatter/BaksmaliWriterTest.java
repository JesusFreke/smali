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

package org.jf.baksmali.formatter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.jf.dexlib2.MethodHandleType;
import org.jf.dexlib2.iface.reference.MethodHandleReference;
import org.jf.dexlib2.immutable.ImmutableAnnotationElement;
import org.jf.dexlib2.immutable.reference.*;
import org.jf.dexlib2.immutable.value.*;
import org.jf.util.IndentingWriter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

public class BaksmaliWriterTest {

    private StringWriter stringWriter;
    private IndentingWriter output;

    @Before
    public void setup() {
        stringWriter = new StringWriter();
        output = new IndentingWriter(stringWriter);
    }

    @Test
    public void testWriteMethodDescriptor_withSpaces() throws IOException {
        BaksmaliWriter writer =
                new BaksmaliWriter(output);

        writer.writeMethodDescriptor(getMethodReferenceWithSpaces());

        Assert.assertEquals(
                "Ldefining/class/`with spaces`;->`methodName with spaces`(L`param with spaces 1`;L`param with spaces 2`;)" +
                        "Lreturn/type/`with spaces`;",
                stringWriter.toString());
    }

    @Test
    public void testWriteShortMethodDescriptor_withSpaces() throws IOException {
        BaksmaliWriter writer =
                new BaksmaliWriter(output);

        writer.writeShortMethodDescriptor(getMethodReferenceWithSpaces());

        Assert.assertEquals(
                "`methodName with spaces`(L`param with spaces 1`;L`param with spaces 2`;)" +
                        "Lreturn/type/`with spaces`;",
                stringWriter.toString());
    }

    @Test
    public void testWriteMethodProtoDescriptor_withSpaces() throws IOException {
        BaksmaliWriter writer =
                new BaksmaliWriter(output);

        writer.writeMethodProtoDescriptor(getMethodProtoReferenceWithSpaces());

        Assert.assertEquals(
                "(L`param with spaces 1`;L`param with spaces 2`;)Lreturn/type/`with spaces`;",
                stringWriter.toString());
    }

    @Test
    public void testWriteFieldDescriptor_withSpaces() throws IOException {
        BaksmaliWriter writer =
                new BaksmaliWriter(output);

        writer.writeFieldDescriptor(getFieldReferenceWithSpaces());

        Assert.assertEquals("Ldefining/class/`with spaces`;->`fieldName with spaces`:Lfield/`type with spaces`;",
                stringWriter.toString());
    }

    @Test
    public void testWriteShortFieldDescriptor_withSpaces() throws IOException {
        BaksmaliWriter writer =
                new BaksmaliWriter(output);

        writer.writeShortFieldDescriptor(getFieldReferenceWithSpaces());

        Assert.assertEquals("`fieldName with spaces`:Lfield/`type with spaces`;",
                stringWriter.toString());
    }

    @Test
    public void testWriteMethodHandle_fieldAccess_withSpaces() throws IOException {
        BaksmaliWriter writer =
                new BaksmaliWriter(output);

        writer.writeMethodHandle(getMethodHandleReferenceForFieldWithSpaces());

        Assert.assertEquals("instance-get@Ldefining/class/`with spaces`;->`fieldName with spaces`:" +
                "Lfield/`type with spaces`;", stringWriter.toString());
    }

    @Test
    public void testWriteMethodHandle_methodAccess_withSpaces() throws IOException {
        BaksmaliWriter writer =
                new BaksmaliWriter(output);

        writer.writeMethodHandle(getMethodHandleReferenceForMethodWithSpaces());

        Assert.assertEquals("invoke-instance@Ldefining/class/`with spaces`;->`methodName with spaces`(" +
                        "L`param with spaces 1`;L`param with spaces 2`;)Lreturn/type/`with spaces`;",
                stringWriter.toString());
    }

    @Test
    public void testWriteCallsite_withSpaces() throws IOException {
        BaksmaliWriter writer =
                new BaksmaliWriter(output);

        writer.writeCallSite(new ImmutableCallSiteReference(
                "callsiteName with spaces",
                getInvokeStaticMethodHandleReferenceForMethodWithSpaces(),
                "callSiteMethodName with spaces",
                getMethodProtoReferenceWithSpaces(),
                ImmutableList.of(
                        new ImmutableFieldEncodedValue(getFieldReferenceWithSpaces()),
                        new ImmutableMethodEncodedValue(getMethodReferenceWithSpaces()))));

        Assert.assertEquals(
                "`callsiteName with spaces`(\"callSiteMethodName with spaces\", " +
                        "(L`param with spaces 1`;L`param with spaces 2`;)Lreturn/type/`with spaces`;, " +
                        "Ldefining/class/`with spaces`;->`fieldName with spaces`:Lfield/`type with spaces`;, " +
                        "Ldefining/class/`with spaces`;->`methodName with spaces`(" +
                        "L`param with spaces 1`;L`param with spaces 2`;)Lreturn/type/`with spaces`;)@" +
                        "Ldefining/class/`with spaces`;->`methodName with spaces`(" +
                        "L`param with spaces 1`;L`param with spaces 2`;)Lreturn/type/`with spaces`;",
                stringWriter.toString());
    }

    @Test
    public void testWriteEncodedValue_annotation_withSpaces() throws IOException {
        BaksmaliWriter writer =
                new BaksmaliWriter(output);

        writer.writeEncodedValue(new ImmutableAnnotationEncodedValue(
                "Lannotation/type with spaces;",
                ImmutableSet.of(
                        new ImmutableAnnotationElement("element with spaces 1",
                                new ImmutableFieldEncodedValue(getFieldReferenceWithSpaces())),
                        new ImmutableAnnotationElement("element with spaces 2",
                                new ImmutableMethodEncodedValue(getMethodReferenceWithSpaces()))
                )));

        Assert.assertEquals(
                ".subannotation Lannotation/`type with spaces`;\n" +
                        "    `element with spaces 1` = Ldefining/class/`with spaces`;->`fieldName with spaces`:Lfield/`type with spaces`;\n" +
                        "    `element with spaces 2` = Ldefining/class/`with spaces`;->`methodName with spaces`(" +
                        "L`param with spaces 1`;L`param with spaces 2`;)Lreturn/type/`with spaces`;\n" +
                        ".end subannotation",
                stringWriter.toString());
    }

    @Test
    public void testWriteEncodedValue_array_withSpaces() throws IOException {
        BaksmaliWriter writer =
                new BaksmaliWriter(output);

        writer.writeEncodedValue(new ImmutableArrayEncodedValue(ImmutableList.of(
                new ImmutableFieldEncodedValue(getFieldReferenceWithSpaces()),
                new ImmutableMethodEncodedValue(getMethodReferenceWithSpaces()))));

        Assert.assertEquals(
                "{\n" +
                        "    Ldefining/class/`with spaces`;->`fieldName with spaces`:Lfield/`type with spaces`;,\n" +
                        "    Ldefining/class/`with spaces`;->`methodName with spaces`(L`param with spaces 1`;L`param with spaces 2`;)Lreturn/type/`with spaces`;\n" +
                        "}",
                stringWriter.toString());
    }

    @Test
    public void testWriteEncodedValue_field_withSpaces() throws IOException {
        BaksmaliWriter writer =
                new BaksmaliWriter(output);

        writer.writeEncodedValue(new ImmutableFieldEncodedValue(getFieldReferenceWithSpaces()));

        Assert.assertEquals(
                "Ldefining/class/`with spaces`;->`fieldName with spaces`:Lfield/`type with spaces`;",
                stringWriter.toString());
    }

    @Test
    public void testWriteEncodedValue_enum_withSpaces() throws IOException {
        BaksmaliWriter writer =
                new BaksmaliWriter(output);

        writer.writeEncodedValue(new ImmutableEnumEncodedValue(getFieldReferenceWithSpaces()));

        Assert.assertEquals(
                ".enum Ldefining/class/`with spaces`;->`fieldName with spaces`:Lfield/`type with spaces`;",
                stringWriter.toString());
    }

    @Test
    public void testWriteEncodedValue_method_withSpaces() throws IOException {
        BaksmaliWriter writer =
                new BaksmaliWriter(output);

        writer.writeEncodedValue(new ImmutableMethodEncodedValue(getMethodReferenceWithSpaces()));

        Assert.assertEquals(
                "Ldefining/class/`with spaces`;->`methodName with spaces`(" +
                        "L`param with spaces 1`;L`param with spaces 2`;)Lreturn/type/`with spaces`;",
                stringWriter.toString());
    }

    @Test
    public void testWriteEncodedValue_type_withSpaces() throws IOException {
        BaksmaliWriter writer =
                new BaksmaliWriter(output);

        writer.writeEncodedValue(new ImmutableTypeEncodedValue("Ltest/type with spaces;"));

        Assert.assertEquals(
                "Ltest/`type with spaces`;",
                stringWriter.toString());
    }

    @Test
    public void testWriteEncodedValue_methodType_withSpaces() throws IOException {
        BaksmaliWriter writer =
                new BaksmaliWriter(output);

        writer.writeEncodedValue(new ImmutableMethodTypeEncodedValue(getMethodProtoReferenceWithSpaces()));

        Assert.assertEquals(
                "(L`param with spaces 1`;L`param with spaces 2`;)Lreturn/type/`with spaces`;",
                stringWriter.toString());
    }

    @Test
    public void testWriteEncodedValue_methodHandle_withSpaces() throws IOException {
        BaksmaliWriter writer =
                new BaksmaliWriter(output);

        writer.writeEncodedValue(
                new ImmutableMethodHandleEncodedValue(getMethodHandleReferenceForMethodWithSpaces()));

        Assert.assertEquals(
                "invoke-instance@Ldefining/class/`with spaces`;->`methodName with spaces`(" +
                        "L`param with spaces 1`;L`param with spaces 2`;)Lreturn/type/`with spaces`;",
                stringWriter.toString());
    }

    private ImmutableMethodReference getMethodReferenceWithSpaces() {
        return new ImmutableMethodReference(
                "Ldefining/class/with spaces;",
                "methodName with spaces",
                ImmutableList.of("Lparam with spaces 1;", "Lparam with spaces 2;"),
                "Lreturn/type/with spaces;");
    }

    private ImmutableMethodProtoReference getMethodProtoReferenceWithSpaces() {
        return new ImmutableMethodProtoReference(
                ImmutableList.of("Lparam with spaces 1;", "Lparam with spaces 2;"),
                "Lreturn/type/with spaces;");
    }

    private ImmutableFieldReference getFieldReferenceWithSpaces() {
        return new ImmutableFieldReference(
                "Ldefining/class/with spaces;",
                "fieldName with spaces",
                "Lfield/type with spaces;");
    }

    private MethodHandleReference getMethodHandleReferenceForFieldWithSpaces() {
        return new ImmutableMethodHandleReference(
                MethodHandleType.INSTANCE_GET,
                getFieldReferenceWithSpaces());
    }


    private ImmutableMethodHandleReference getMethodHandleReferenceForMethodWithSpaces() {
        return new ImmutableMethodHandleReference(
                MethodHandleType.INVOKE_INSTANCE,
                getMethodReferenceWithSpaces());
    }

    private MethodHandleReference getInvokeStaticMethodHandleReferenceForMethodWithSpaces() {
        return new ImmutableMethodHandleReference(
                MethodHandleType.INVOKE_STATIC,
                getMethodReferenceWithSpaces());
    }
}
