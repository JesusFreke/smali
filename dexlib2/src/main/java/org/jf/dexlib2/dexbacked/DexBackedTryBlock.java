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

package org.jf.dexlib2.dexbacked;

import org.jf.dexlib2.DexFile;
import org.jf.dexlib2.DexFileReader;
import org.jf.dexlib2.iface.ExceptionHandler;
import org.jf.dexlib2.iface.TryBlock;
import org.jf.dexlib2.dexbacked.util.VariableSizeList;

import javax.annotation.Nonnull;
import java.util.List;

public class DexBackedTryBlock implements TryBlock {
    public final DexFile dexFile;

    public final int startIndex;
    public final int instructionCount;

    private final int exceptionHandlersOffset;

    public DexBackedTryBlock(DexFile dexFile,
                             int offset,
                             int handlersStartOffset) {
        this.dexFile = dexFile;
        this.startIndex = dexFile.readSmallUint(offset);
        this.instructionCount = dexFile.readUshort(offset+4);
        this.exceptionHandlersOffset = handlersStartOffset + dexFile.readUshort(6);
    }

    @Override public int getStartIndex() { return startIndex; }
    @Override public int getInstructionCount() { return instructionCount; }

    @Nonnull
    @Override
    public List<? extends ExceptionHandler> getExceptionHandlers() {
        DexFileReader reader = dexFile.readerAt(exceptionHandlersOffset);
        final int encodedSize = reader.readSleb128();

        if (encodedSize > 0) {
            //no catch-all
            return new VariableSizeList<ExceptionHandler>(dexFile, reader.getOffset()) {
                @Override
                protected ExceptionHandler readItem(DexFileReader dexFileReader, int index) {
                    return new DexBackedExceptionHandler(dexFileReader);
                }
                @Override public int size() { return encodedSize; }
            };
        } else {
            //with catch-all
            final int sizeWithCatchAll = (-1 * encodedSize) + 1;
            return new VariableSizeList<ExceptionHandler>(dexFile, reader.getOffset()) {
                @Override
                protected ExceptionHandler readItem(DexFileReader dexFileReader, int index) {
                    if (index == sizeWithCatchAll-1) {
                        return new DexBackedCatchAllExceptionHandler(dexFileReader);
                    } else {
                        return new DexBackedExceptionHandler(dexFileReader);
                    }
                }
                @Override public int size() { return sizeWithCatchAll; }
            };
        }
    }
}
