/*
 * Copyright 2012, Google Inc.
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

package org.jf.dexlib2.dexbacked.util;

import org.jf.dexlib2.DebugItemType;
import org.jf.dexlib2.dexbacked.DexBuffer;
import org.jf.dexlib2.dexbacked.DexReader;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.MethodParameter;
import org.jf.dexlib2.iface.debug.DebugItem;
import org.jf.dexlib2.iface.debug.EndLocal;
import org.jf.dexlib2.iface.debug.LocalInfo;
import org.jf.dexlib2.immutable.debug.*;
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

public class DebugItemList extends VariableSizeListWithContext<DebugItem> {
    @Nonnull public final DexBuffer dexBuf;
    private final int debugInfoOffset;
    @Nonnull private final Method method;
    @Nonnull private final MethodImplementation methodImpl;

    public DebugItemList(@Nonnull DexBuffer dexBuf,
                         int debugInfoOffset,
                         @Nonnull Method method) {
        this.dexBuf = dexBuf;
        this.debugInfoOffset = debugInfoOffset;
        this.method = method;
        MethodImplementation methodImpl = method.getImplementation();
        if (methodImpl == null) {
            throw new ExceptionWithContext("Creating a DebugItemList for a method with no implementation. WTF?");
        }
        this.methodImpl = methodImpl;
    }

    private static final LocalInfo EMPTY_LOCAL_INFO = new LocalInfo() {
        @Nullable @Override public String getName() { return null; }
        @Nullable @Override public String getType() { return null; }
        @Nullable @Override public String getSignature() { return null; }
    };

    @Nonnull
    @Override
    public Iterator listIterator() {
        DexReader initialReader = dexBuf.readerAt(debugInfoOffset);
        // TODO: this unsigned value could legitimally be > MAX_INT
        final int lineNumberStart = initialReader.readSmallUleb128();
        int registerCount = methodImpl.getRegisterCount();

        //TODO: does dalvik allow references to invalid registers?
        final LocalInfo[] locals = new LocalInfo[registerCount];
        Arrays.fill(locals, EMPTY_LOCAL_INFO);

        List<? extends MethodParameter> parameters = method.getParameters();

        //TODO: need to add parameter info to MethodParameter. Is there some way we could use the same reader for that?
        int debugParametersSize = initialReader.readSmallUleb128();
        if (debugParametersSize > parameters.size()) {
            //TODO: make sure that dalvik doesn't allow this
            throw new ExceptionWithContext("DebugInfoItem has more parameters than the method itself does. WTF?");
        }
        for (int i=0; i<parameters.size(); i++) {
            // TODO: look for a signature annotation on the... method? parameter?, and get the parameter signature
            final MethodParameter methodParameter = parameters.get(i);
            final String parameterName = dexBuf.getOptionalString(initialReader.readSmallUleb128() - 1);

            locals[i] = new LocalInfo() {
                @Nullable @Override public String getName() { return parameterName; }
                @Nullable @Override public String getType() { return methodParameter.getType(); }
                @Nullable @Override public String getSignature() { return null; }
            };
        }

        if (parameters.size() < registerCount) {
            // we need to push the parameter locals back to their appropriate register
            int localIndex = registerCount-1;
            for (int i=parameters.size()-1; i>-1; i--) {
                LocalInfo currentLocal = locals[i];
                locals[localIndex] = currentLocal;
                locals[i] = EMPTY_LOCAL_INFO;
                String type = currentLocal.getType();
                localIndex--;
                if (type != null && (type.equals("J") || type.equals("D"))) {
                    localIndex--;
                }
            }
        }

        return new Iterator(dexBuf, initialReader.getOffset()) {
            private boolean finished = false;
            private int codeAddress = 0;
            private int lineNumber = lineNumberStart;

            @Nonnull
            @Override
            protected DebugItem readItem(@Nonnull DexReader reader, int index) {
                if (finished) {
                    throw new NoSuchElementException();
                }
                while (true) {
                    int next = reader.readUbyte();
                    switch (next) {
                        case DebugItemType.END_SEQUENCE: {
                            finished = true;
                            throw new NoSuchElementException();
                        }
                        case DebugItemType.ADVANCE_PC: {
                            int addressDiff = reader.readSmallUleb128();
                            codeAddress += addressDiff;
                            continue;
                        }
                        case DebugItemType.ADVANCE_LINE: {
                            int lineDiff = reader.readSleb128();
                            lineNumber += lineDiff;
                            continue;
                        }
                        case DebugItemType.START_LOCAL: {
                            int register = reader.readSmallUleb128();
                            String name = dexBuf.getOptionalString(reader.readSmallUleb128() - 1);
                            String type = dexBuf.getOptionalType(reader.readSmallUleb128() - 1);
                            ImmutableStartLocal startLocal =
                                    new ImmutableStartLocal(codeAddress, register, name, type, null);
                            locals[register] = startLocal;
                            return startLocal;
                        }
                        case DebugItemType.START_LOCAL_EXTENDED: {
                            int register = reader.readSmallUleb128();
                            String name = dexBuf.getOptionalString(reader.readSmallUleb128() - 1);
                            String type = dexBuf.getOptionalType(reader.readSmallUleb128() - 1);
                            String signature = dexBuf.getOptionalString(reader.readSmallUleb128() - 1);
                            ImmutableStartLocal startLocal =
                                    new ImmutableStartLocal(codeAddress, register, name, type, signature);
                            locals[register] = startLocal;
                            return startLocal;
                        }
                        case DebugItemType.END_LOCAL: {
                            int register = reader.readSmallUleb128();
                            LocalInfo localInfo = locals[register];
                            boolean replaceLocalInTable = true;
                            if (localInfo instanceof EndLocal) {
                                localInfo = EMPTY_LOCAL_INFO;
                                // don't replace the local info in locals. The new EndLocal won't have any info at all,
                                // and we dont want to wipe out what's there, so that it is available for a subsequent
                                // RestartLocal
                                replaceLocalInTable = false;
                            }
                            ImmutableEndLocal endLocal =
                                    new ImmutableEndLocal(codeAddress, register, localInfo.getName(),
                                            localInfo.getType(), localInfo.getSignature());
                            if (replaceLocalInTable) {
                                locals[register] = endLocal;
                            }
                            return endLocal;
                        }
                        case DebugItemType.RESTART_LOCAL: {
                            int register = reader.readSmallUleb128();
                            LocalInfo localInfo = locals[register];
                            ImmutableRestartLocal restartLocal =
                                    new ImmutableRestartLocal(codeAddress, register, localInfo.getName(),
                                            localInfo.getType(), localInfo.getSignature());
                            locals[register] = restartLocal;
                            return restartLocal;
                        }
                        case DebugItemType.PROLOGUE_END: {
                            return new ImmutablePrologueEnd(codeAddress);
                        }
                        case DebugItemType.EPILOGUE_BEGIN: {
                            return new ImmutableEpilogueBegin(codeAddress);
                        }
                        case DebugItemType.SET_SOURCE_FILE: {
                            String sourceFile = dexBuf.getOptionalString(reader.readSmallUleb128() - 1);
                            return new ImmutableSetSourceFile(codeAddress, sourceFile);
                        }
                        default: {
                            int base = ((next & 0xFF) - 0x0A);
                            codeAddress += base / 15;
                            lineNumber += (base % 15) - 4;
                            return new ImmutableLineNumber(codeAddress, lineNumber);
                        }
                    }
                }
            }

            @Override
            protected void skipItem(@Nonnull DexReader reader, int index) {
                super.skipItem(reader, index);
            }

            @Override
            protected void checkBounds(int index) {
                // skip the bounds check here. We'll throw NoSuchElementException directly from readItem
            }
        };
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException();
    }
}
