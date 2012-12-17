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

package org.jf.dexlib2.writer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jf.dexlib2.DebugItemType;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.MethodParameter;
import org.jf.dexlib2.iface.debug.*;
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DebugInfoPool {
    @Nonnull private final Map<Method, Integer> debugInfoOffsetMap = Maps.newHashMap();
    @Nonnull private final DexFile dexFile;

    public DebugInfoPool(@Nonnull DexFile dexFile) {
        this.dexFile = dexFile;
    }

    public boolean intern(@Nonnull Method method) {
        boolean hasDebugInfo = false;
        for (MethodParameter param: method.getParameters()) {
            String paramName = param.getName();
            if (paramName != null) {
                hasDebugInfo = true;
                dexFile.stringPool.intern(paramName);
            }
        }

        MethodImplementation methodImpl = method.getImplementation();
        if (methodImpl != null) {
            for (DebugItem debugItem: methodImpl.getDebugItems()) {
                hasDebugInfo = true;
                switch (debugItem.getDebugItemType()) {
                    case DebugItemType.START_LOCAL:
                        StartLocal startLocal = (StartLocal)debugItem;
                        dexFile.stringPool.internNullable(startLocal.getName());
                        dexFile.typePool.internNullable(startLocal.getType());
                        dexFile.stringPool.internNullable(startLocal.getSignature());
                        break;
                    case DebugItemType.SET_SOURCE_FILE:
                        dexFile.stringPool.internNullable(((SetSourceFile) debugItem).getSourceFile());
                        break;
                }
            }
        }

        if (hasDebugInfo) {
            debugInfoOffsetMap.put(method, 0);
        }
        return hasDebugInfo;
    }

    public int getOffset(@Nonnull Method method) {
        Integer offset = debugInfoOffsetMap.get(method);
        if (offset == null) {
            return 0;
        }
        return offset;
    }

    public void write(@Nonnull DexWriter writer) throws IOException {
        List<Method> methods = Lists.newArrayList(debugInfoOffsetMap.keySet());
        Collections.sort(methods);
        for (Method method: methods) {
            debugInfoOffsetMap.put(method, writer.getPosition());

            int startingLineNumber = 0;

            MethodImplementation methodImpl = method.getImplementation();
            List<DebugItem> debugItems = null;

            if (methodImpl != null) {
                debugItems = Lists.newArrayList(methodImpl.getDebugItems());
                for (DebugItem item: debugItems) {
                    if (item.getDebugItemType() == DebugItemType.LINE_NUMBER) {
                        startingLineNumber = ((LineNumber)item).getLineNumber();
                    }
                }
            }
            writer.writeUleb128(startingLineNumber);

            // TODO: do we need to write out all param names, even if the last n are null?
            List<? extends MethodParameter> parameters = method.getParameters();
            writer.writeUleb128(parameters.size());
            for (MethodParameter parameter: parameters) {
                writer.writeUleb128(dexFile.stringPool.getIndexNullable(parameter.getName()));
            }

            if (debugItems != null && debugItems.size() > 0) {
                DebugWriter debugWriter = new DebugWriter(dexFile, writer, startingLineNumber);
                for (DebugItem debugItem: debugItems) {
                    switch (debugItem.getDebugItemType()) {
                        case DebugItemType.START_LOCAL:
                            debugWriter.emitStartLocal((StartLocal)debugItem);
                            break;
                        case DebugItemType.END_LOCAL:
                            debugWriter.emitEndLocal((EndLocal)debugItem);
                            break;
                        case DebugItemType.RESTART_LOCAL:
                            debugWriter.emitRestartLocal((RestartLocal)debugItem);
                            break;
                        case DebugItemType.PROLOGUE_END:
                            debugWriter.emitPrologueEnd((PrologueEnd)debugItem);
                            break;
                        case DebugItemType.EPILOGUE_BEGIN:
                            debugWriter.emitEpilogueBegin((EpilogueBegin)debugItem);
                            break;
                        case DebugItemType.SET_SOURCE_FILE:
                            debugWriter.emitSetSourceFile((SetSourceFile)debugItem);
                            break;
                        case DebugItemType.LINE_NUMBER:
                            debugWriter.emitLineNumber((LineNumber)debugItem);
                            break;
                        default:
                            throw new ExceptionWithContext("Unexpected debug item type: %d",
                                    debugItem.getDebugItemType());
                    }
                }
            }
            // write an END_SEQUENCE opcode, to end the debug item
            writer.write(0);
        }
    }

    // TODO: add some validation here.
    private static class DebugWriter {

        @Nonnull private final DexFile dexFile;
        @Nonnull private final DexWriter writer;
        private int currentAddress = 0;
        private int currentLine;

        public DebugWriter(@Nonnull DexFile dexFile, @Nonnull DexWriter writer, int startLine) {
            this.dexFile = dexFile;
            this.writer = writer;
            this.currentLine = startLine;
        }

        private void emitAdvancePC(int address) throws IOException {
            int addressDelta = address-currentAddress;

            if (addressDelta > 0) {
                writer.write(1);
                writer.writeUleb128(addressDelta);
                currentAddress = address;
            }
        }

        private void emitAdvanceLine(int line) throws IOException {
            int lineDelta = line-currentAddress;
            if (lineDelta != 0) {
                writer.write(2);
                writer.writeSleb128(lineDelta);
                currentLine = line;
            }
        }

        public void emitLineNumber(@Nonnull LineNumber lineNumber) throws IOException {
            int lineDelta = lineNumber.getLineNumber() - currentLine;
            int addressDelta = lineNumber.getCodeAddress() - currentAddress;

            if (lineDelta < -4 || lineDelta > 10) {
                emitAdvanceLine(lineNumber.getLineNumber());
                lineDelta = 0;
            } else if ((lineDelta < 2 && addressDelta > 16) || (lineDelta > 1 && addressDelta > 15)) {
                emitAdvancePC(lineNumber.getCodeAddress());
                addressDelta = 0;
            }

            //TODO: need to handle the case when the line delta is larger than a signed int
            emitSpecialOpcode(lineDelta, addressDelta);
        }

        public void emitStartLocal(@Nonnull StartLocal startLocal) throws IOException {
            int nameIndex = dexFile.stringPool.getIndexNullable(startLocal.getName());
            int typeIndex = dexFile.typePool.getIndexNullable(startLocal.getType());
            int signatureIndex = dexFile.stringPool.getIndexNullable(startLocal.getName());
            emitAdvancePC(startLocal.getCodeAddress());
            if (signatureIndex == -1) {
                writer.write(3);
                writer.writeUleb128(startLocal.getRegister());
                writer.writeUleb128(nameIndex+1);
                writer.writeUleb128(typeIndex+1);
            } else {
                writer.write(4);
                writer.writeUleb128(startLocal.getRegister());
                writer.writeUleb128(nameIndex+1);
                writer.writeUleb128(typeIndex+1);
                writer.writeUleb128(signatureIndex+1);
            }
        }

        public void emitEndLocal(@Nonnull EndLocal endLocal) throws IOException {
            emitAdvancePC(endLocal.getCodeAddress());
            writer.write(5);
            writer.writeUleb128(endLocal.getRegister());
        }

        public void emitRestartLocal(@Nonnull RestartLocal restartLocal) throws IOException {
            emitAdvancePC(restartLocal.getCodeAddress());
            writer.write(6);
            writer.writeUleb128(restartLocal.getRegister());
        }

        public void emitPrologueEnd(@Nonnull PrologueEnd prologueEnd) throws IOException {
            emitAdvancePC(prologueEnd.getCodeAddress());
            writer.write(7);
        }

        public void emitEpilogueBegin(@Nonnull EpilogueBegin epilogueBegin) throws IOException {
            emitAdvancePC(epilogueBegin.getCodeAddress());
            writer.write(8);
        }

        public void emitSetSourceFile(@Nonnull SetSourceFile setSourceFile) throws IOException {
            emitAdvancePC(setSourceFile.getCodeAddress());
            writer.write(9);
            writer.write(dexFile.stringPool.getIndexNullable(setSourceFile.getSourceFile()));
        }

        private static final int LINE_BASE = -4;
        private static final int LINE_RANGE = 15;
        private static final int FIRST_SPECIAL = 0x0a;
        private void emitSpecialOpcode(int lineDelta, int addressDelta) throws IOException {
            writer.write((byte)(FIRST_SPECIAL + (addressDelta * LINE_RANGE) + (lineDelta - LINE_BASE)));
            currentLine += lineDelta;
            addressDelta += addressDelta;
        }
    }
}
