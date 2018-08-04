/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.baksmali.Adaptors;

import org.jf.baksmali.Adaptors.EncodedValue.EncodedValueAdaptor;
import org.jf.dexlib2.MethodHandleType;
import org.jf.dexlib2.ReferenceType;
import org.jf.dexlib2.iface.reference.*;
import org.jf.dexlib2.iface.value.EncodedValue;
import org.jf.dexlib2.util.ReferenceUtil;
import org.jf.util.IndentingWriter;
import org.jf.util.StringUtils;

import java.io.IOException;

public class ReferenceFormatter {
    public static void writeStringReference(IndentingWriter writer, String item) throws IOException {
        writer.write('"');
        StringUtils.writeEscapedString(writer, item);
        writer.write('"');
    }

    public static void writeCallSiteReference(IndentingWriter writer, CallSiteReference callSite) throws IOException {
        writer.write(callSite.getName());
        writer.write('(');
        writer.write('"');
        StringUtils.writeEscapedString(writer, callSite.getMethodName());
        writer.write("\", ");
        writeReference(writer, ReferenceType.METHOD_PROTO, callSite.getMethodProto());

        for (EncodedValue encodedValue : callSite.getExtraArguments()) {
            writer.write(", ");
            EncodedValueAdaptor.writeTo(writer, encodedValue, null);
        }
        writer.write(")@");
        MethodHandleReference methodHandle = callSite.getMethodHandle();
        if (methodHandle.getMethodHandleType() != MethodHandleType.INVOKE_STATIC) {
            throw new IllegalArgumentException("The linker method handle for a call site must be of type invoke-static");
        }
        writeReference(writer, ReferenceType.METHOD, callSite.getMethodHandle().getMemberReference());
    }

    public static void writeReference(IndentingWriter writer, int referenceType,
                                      Reference reference) throws IOException {
        switch (referenceType) {
            case ReferenceType.STRING:
                writeStringReference(writer, ((StringReference)reference).getString());
                return;
            case ReferenceType.TYPE:
                writer.write(((TypeReference)reference).getType());
                return;
            case ReferenceType.METHOD:
                ReferenceUtil.writeMethodDescriptor(writer, (MethodReference)reference);
                return;
            case ReferenceType.FIELD:
                ReferenceUtil.writeFieldDescriptor(writer, (FieldReference)reference);
                return;
            case ReferenceType.METHOD_PROTO:
                ReferenceUtil.writeMethodProtoDescriptor(writer, (MethodProtoReference)reference);
                return;
            case ReferenceType.METHOD_HANDLE:
                ReferenceUtil.writeMethodHandle(writer, (MethodHandleReference)reference);
                return;
            case ReferenceType.CALL_SITE:
                // We can't use ReferenceUtil.writeCallSite here, because it doesn't write encoded values out in the
                // exact format we need here.
                writeCallSiteReference(writer, (CallSiteReference)reference);
                return;
            default:
                throw new IllegalStateException("Unknown reference type");
        }
    }
}
