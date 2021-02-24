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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.jf.dexlib2.MethodHandleType;
import org.jf.dexlib2.iface.reference.CallSiteReference;
import org.jf.dexlib2.iface.reference.MethodHandleReference;
import org.jf.dexlib2.immutable.ImmutableAnnotationElement;
import org.jf.dexlib2.immutable.reference.*;
import org.jf.dexlib2.immutable.value.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

public class DexFormattedWriterTest {

    private StringWriter output;

    @Before
    public void setup() {
        output = new StringWriter();
    }

    @Test
    public void testWriteMethodDescriptor() throws IOException {
        DexFormattedWriter writer = new DexFormattedWriter(output);

        writer.writeMethodDescriptor(getMethodReference());

        Assert.assertEquals("Ldefining/class;->methodName(Lparam1;Lparam2;)Lreturn/type;", output.toString());
    }

    @Test
    public void testWriteShortMethodDescriptor() throws IOException {
        DexFormattedWriter writer = new DexFormattedWriter(output);

        writer.writeShortMethodDescriptor(getMethodReference());

        Assert.assertEquals("methodName(Lparam1;Lparam2;)Lreturn/type;", output.toString());
    }

    @Test
    public void testWriteMethodProtoDescriptor() throws IOException {
        DexFormattedWriter writer = new DexFormattedWriter(output);

        writer.writeMethodProtoDescriptor(getMethodProtoReference());

        Assert.assertEquals("(Lparam1;Lparam2;)Lreturn/type;", output.toString());
    }

    @Test
    public void testWriteFieldDescriptor() throws IOException {
        DexFormattedWriter writer = new DexFormattedWriter(output);

        writer.writeFieldDescriptor(getFieldReference());

        Assert.assertEquals("Ldefining/class;->fieldName:Lfield/type;", output.toString());
    }

    @Test
    public void testWriteShortFieldDescriptor() throws IOException {
        DexFormattedWriter writer = new DexFormattedWriter(output);

        writer.writeShortFieldDescriptor(getFieldReference());

        Assert.assertEquals("fieldName:Lfield/type;", output.toString());
    }

    @Test
    public void testWriteMethodHandle_fieldAccess() throws IOException {
        DexFormattedWriter writer = new DexFormattedWriter(output);

        writer.writeMethodHandle(getMethodHandleReferenceForField());

        Assert.assertEquals("instance-get@Ldefining/class;->fieldName:Lfield/type;", output.toString());
    }

    @Test
    public void testWriteMethodHandle_methodAccess() throws IOException {
        DexFormattedWriter writer = new DexFormattedWriter(output);

        writer.writeMethodHandle(getMethodHandleReferenceForMethod());

        Assert.assertEquals("invoke-instance@Ldefining/class;->methodName(Lparam1;Lparam2;)Lreturn/type;",
                output.toString());
    }

    @Test
    public void testWriteCallsite() throws IOException {
        DexFormattedWriter writer = new DexFormattedWriter(output);

        writer.writeCallSite(getCallSiteReference());

        Assert.assertEquals(
                "callsiteName(\"callSiteMethodName\", " +
                        "(Lparam1;Lparam2;)Lreturn/type;, Ldefining/class;->fieldName:Lfield/type;, " +
                        "Ldefining/class;->methodName(Lparam1;Lparam2;)Lreturn/type;)@" +
                        "Ldefining/class;->methodName(Lparam1;Lparam2;)Lreturn/type;",
                output.toString());
    }

    @Test
    public void testWriteEncodedValue_boolean_true() throws IOException {
        DexFormattedWriter writer = new DexFormattedWriter(output);

        writer.writeEncodedValue(ImmutableBooleanEncodedValue.TRUE_VALUE);

        Assert.assertEquals("true", output.toString());
    }

    @Test
    public void testWriteEncodedValue_boolean_false() throws IOException {
        DexFormattedWriter writer = new DexFormattedWriter(output);

        writer.writeEncodedValue(ImmutableBooleanEncodedValue.FALSE_VALUE);

        Assert.assertEquals("false", output.toString());
    }

    @Test
    public void testWriteEncodedValue_byte() throws IOException {
        DexFormattedWriter writer = new DexFormattedWriter(output);

        writer.writeEncodedValue(new ImmutableByteEncodedValue((byte)0x12));

        Assert.assertEquals("0x12", output.toString());
    }

    @Test
    public void testWriteEncodedValue_char() throws IOException {
        DexFormattedWriter writer = new DexFormattedWriter(output);

        writer.writeEncodedValue(new ImmutableCharEncodedValue('a'));

        Assert.assertEquals("0x61", output.toString());
    }

    @Test
    public void testWriteEncodedValue_short() throws IOException {
        DexFormattedWriter writer = new DexFormattedWriter(output);

        writer.writeEncodedValue(new ImmutableShortEncodedValue((short) 0x12));

        Assert.assertEquals("0x12", output.toString());
    }

    @Test
    public void testWriteEncodedValue_int() throws IOException {
        DexFormattedWriter writer = new DexFormattedWriter(output);

        writer.writeEncodedValue(new ImmutableIntEncodedValue(0x12));

        Assert.assertEquals("0x12", output.toString());
    }

    @Test
    public void testWriteEncodedValue_long() throws IOException {
        DexFormattedWriter writer = new DexFormattedWriter(output);

        writer.writeEncodedValue(new ImmutableLongEncodedValue(0x12));

        Assert.assertEquals("0x12", output.toString());
    }

    @Test
    public void testWriteEncodedValue_float() throws IOException {
        DexFormattedWriter writer = new DexFormattedWriter(output);

        writer.writeEncodedValue(new ImmutableFloatEncodedValue(12.34f));

        Assert.assertEquals("12.34", output.toString());
    }

    @Test
    public void testWriteEncodedValue_double() throws IOException {
        DexFormattedWriter writer = new DexFormattedWriter(output);

        writer.writeEncodedValue(new ImmutableDoubleEncodedValue(12.34));

        Assert.assertEquals("12.34", output.toString());
    }

    @Test
    public void testWriteEncodedValue_annotation() throws IOException {
        DexFormattedWriter writer = new DexFormattedWriter(output);

        writer.writeEncodedValue(new ImmutableAnnotationEncodedValue(
                "Lannotation/type;",
                ImmutableSet.of(
                        new ImmutableAnnotationElement("element1", new ImmutableFieldEncodedValue(getFieldReference())),
                        new ImmutableAnnotationElement("element2", new ImmutableMethodEncodedValue(getMethodReference()))
                )));

        Assert.assertEquals(
                "Annotation[Lannotation/type;, " +
                        "element1=Ldefining/class;->fieldName:Lfield/type;, " +
                        "element2=Ldefining/class;->methodName(Lparam1;Lparam2;)Lreturn/type;]",
                output.toString());
    }

    @Test
    public void testWriteEncodedValue_array() throws IOException {
        DexFormattedWriter writer = new DexFormattedWriter(output);

        writer.writeEncodedValue(new ImmutableArrayEncodedValue(ImmutableList.of(
                new ImmutableFieldEncodedValue(getFieldReference()),
                new ImmutableMethodEncodedValue(getMethodReference()))));

        Assert.assertEquals(
                "Array[Ldefining/class;->fieldName:Lfield/type;, " +
                        "Ldefining/class;->methodName(Lparam1;Lparam2;)Lreturn/type;]",
                output.toString());
    }

    @Test
    public void testWriteEncodedValue_string() throws IOException {
        DexFormattedWriter writer = new DexFormattedWriter(output);

        writer.writeEncodedValue(new ImmutableStringEncodedValue("string value\n"));

        Assert.assertEquals(
                "\"string value\\n\"",
                output.toString());
    }

    @Test
    public void testWriteEncodedValue_field() throws IOException {
        DexFormattedWriter writer = new DexFormattedWriter(output);

        writer.writeEncodedValue(new ImmutableFieldEncodedValue(getFieldReference()));

        Assert.assertEquals(
                "Ldefining/class;->fieldName:Lfield/type;",
                output.toString());
    }

    @Test
    public void testWriteEncodedValue_enum() throws IOException {
        DexFormattedWriter writer = new DexFormattedWriter(output);

        writer.writeEncodedValue(new ImmutableEnumEncodedValue(getFieldReference()));

        Assert.assertEquals(
                "Ldefining/class;->fieldName:Lfield/type;",
                output.toString());
    }

    @Test
    public void testWriteEncodedValue_method() throws IOException {
        DexFormattedWriter writer = new DexFormattedWriter(output);

        writer.writeEncodedValue(new ImmutableMethodEncodedValue(getMethodReference()));

        Assert.assertEquals(
                "Ldefining/class;->methodName(Lparam1;Lparam2;)Lreturn/type;",
                output.toString());
    }

    @Test
    public void testWriteEncodedValue_type() throws IOException {
        DexFormattedWriter writer = new DexFormattedWriter(output);

        writer.writeEncodedValue(new ImmutableTypeEncodedValue("Ltest/type;"));

        Assert.assertEquals(
                "Ltest/type;",
                output.toString());
    }

    @Test
    public void testWriteEncodedValue_methodType() throws IOException {
        DexFormattedWriter writer = new DexFormattedWriter(output);

        writer.writeEncodedValue(new ImmutableMethodTypeEncodedValue(getMethodProtoReference()));

        Assert.assertEquals(
                "(Lparam1;Lparam2;)Lreturn/type;",
                output.toString());
    }

    @Test
    public void testWriteEncodedValue_methodHandle() throws IOException {
        DexFormattedWriter writer = new DexFormattedWriter(output);

        writer.writeEncodedValue(new ImmutableMethodHandleEncodedValue(getMethodHandleReferenceForField()));

        Assert.assertEquals(
                "instance-get@Ldefining/class;->fieldName:Lfield/type;",
                output.toString());
    }

    @Test
    public void testWriteEncodedValue_null() throws IOException {
        DexFormattedWriter writer = new DexFormattedWriter(output);

        writer.writeEncodedValue(ImmutableNullEncodedValue.INSTANCE);

        Assert.assertEquals(
                "null",
                output.toString());
    }

    @Test
    public void testWriteReference_string() throws IOException {
        DexFormattedWriter writer = new DexFormattedWriter(output);

        writer.writeReference(new ImmutableStringReference("string value"));

        Assert.assertEquals(
                "\"string value\"",
                output.toString());
    }

    @Test
    public void testWriteReference_type() throws IOException {
        DexFormattedWriter writer = new DexFormattedWriter(output);

        writer.writeReference(new ImmutableTypeReference("Ltest/type;"));

        Assert.assertEquals(
                "Ltest/type;",
                output.toString());
    }

    @Test
    public void testWriteReference_field() throws IOException {
        DexFormattedWriter writer = new DexFormattedWriter(output);

        writer.writeReference(getFieldReference());

        Assert.assertEquals(
                "Ldefining/class;->fieldName:Lfield/type;",
                output.toString());
    }

    @Test
    public void testWriteReference_method() throws IOException {
        DexFormattedWriter writer = new DexFormattedWriter(output);

        writer.writeReference(getMethodReference());

        Assert.assertEquals(
                "Ldefining/class;->methodName(Lparam1;Lparam2;)Lreturn/type;",
                output.toString());
    }

    @Test
    public void testWriteReference_methodProto() throws IOException {
        DexFormattedWriter writer = new DexFormattedWriter(output);

        writer.writeReference(getMethodProtoReference());

        Assert.assertEquals(
                "(Lparam1;Lparam2;)Lreturn/type;",
                output.toString());
    }

    @Test
    public void testWriteReference_methodHandle() throws IOException {
        DexFormattedWriter writer = new DexFormattedWriter(output);

        writer.writeReference(getMethodHandleReferenceForMethod());

        Assert.assertEquals(
                "invoke-instance@Ldefining/class;->methodName(Lparam1;Lparam2;)Lreturn/type;",
                output.toString());
    }

    @Test
    public void testWriteReference_callSite() throws IOException {
        DexFormattedWriter writer = new DexFormattedWriter(output);

        writer.writeReference(getCallSiteReference());

        Assert.assertEquals(
                "callsiteName(\"callSiteMethodName\", " +
                        "(Lparam1;Lparam2;)Lreturn/type;, Ldefining/class;->fieldName:Lfield/type;, " +
                        "Ldefining/class;->methodName(Lparam1;Lparam2;)Lreturn/type;)@" +
                        "Ldefining/class;->methodName(Lparam1;Lparam2;)Lreturn/type;",
                output.toString());
    }

    private ImmutableMethodReference getMethodReference() {
        return new ImmutableMethodReference(
                "Ldefining/class;",
                "methodName",
                ImmutableList.of("Lparam1;", "Lparam2;"),
                "Lreturn/type;");
    }

    private ImmutableMethodProtoReference getMethodProtoReference() {
        return new ImmutableMethodProtoReference(
                ImmutableList.of("Lparam1;", "Lparam2;"),
                "Lreturn/type;");
    }

    private ImmutableFieldReference getFieldReference() {
        return new ImmutableFieldReference(
                "Ldefining/class;",
                "fieldName",
                "Lfield/type;");
    }

    private ImmutableMethodHandleReference getMethodHandleReferenceForField() {
        return new ImmutableMethodHandleReference(
                MethodHandleType.INSTANCE_GET,
                getFieldReference());
    }

    private MethodHandleReference getMethodHandleReferenceForMethod() {
        return new ImmutableMethodHandleReference(
                MethodHandleType.INVOKE_INSTANCE,
                getMethodReference());
    }

    private MethodHandleReference getInvokeStaticMethodHandleReferenceForMethod() {
        return new ImmutableMethodHandleReference(
                MethodHandleType.INVOKE_STATIC,
                getMethodReference());
    }

    private CallSiteReference getCallSiteReference() {
        return new ImmutableCallSiteReference(
                "callsiteName",
                getInvokeStaticMethodHandleReferenceForMethod(),
                "callSiteMethodName",
                getMethodProtoReference(),
                ImmutableList.of(
                        new ImmutableFieldEncodedValue(getFieldReference()),
                        new ImmutableMethodEncodedValue(getMethodReference())));
    }
}
