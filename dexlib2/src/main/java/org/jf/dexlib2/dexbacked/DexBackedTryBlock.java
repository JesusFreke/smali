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

import org.jf.dexlib2.dexbacked.util.VariableSizeCollection;
import org.jf.dexlib2.iface.ExceptionHandler;
import org.jf.dexlib2.iface.TryBlock;

import javax.annotation.Nonnull;
import java.util.Collection;

public class DexBackedTryBlock implements TryBlock {
    @Nonnull public final DexBuffer dexBuf;
    private final int tryItemOffset;
    private final int handlersStartOffset;

    private static final int START_ADDRESS_OFFSET = 0;
    private static final int CODE_UNIT_COUNT_OFFSET = 4;
    private static final int HANDLER_OFFSET_OFFSET = 6;

    public DexBackedTryBlock(@Nonnull DexBuffer dexBuf,
                             int tryItemOffset,
                             int handlersStartOffset) {
        this.dexBuf = dexBuf;
        this.tryItemOffset = tryItemOffset;
        this.handlersStartOffset = handlersStartOffset;
    }

    @Override public int getStartCodeOffset() { return dexBuf.readSmallUint(tryItemOffset + START_ADDRESS_OFFSET); }
    @Override public int getCodeUnitCount() { return dexBuf.readUshort(tryItemOffset + CODE_UNIT_COUNT_OFFSET); }

    @Nonnull
    @Override
    public Collection<? extends ExceptionHandler> getExceptionHandlers() {
        DexReader reader =
                dexBuf.readerAt(handlersStartOffset + dexBuf.readUshort(tryItemOffset + HANDLER_OFFSET_OFFSET));
        final int encodedSize = reader.readSleb128();

        if (encodedSize > 0) {
            //no catch-all
            return new VariableSizeCollection<ExceptionHandler>(dexBuf, reader.getOffset(), encodedSize) {
                @Nonnull
                @Override
                protected DexBackedExceptionHandler readNextItem(@Nonnull DexReader reader, int index) {
                    return new DexBackedExceptionHandler(reader);
                }
            };
        } else {
            //with catch-all
            final int sizeWithCatchAll = (-1 * encodedSize) + 1;
            return new VariableSizeCollection<ExceptionHandler>(dexBuf, reader.getOffset(), sizeWithCatchAll) {
                @Nonnull
                @Override
                protected ExceptionHandler readNextItem(@Nonnull DexReader dexReader, int index) {
                    if (index == sizeWithCatchAll-1) {
                        return new DexBackedCatchAllExceptionHandler(dexReader);
                    } else {
                        return new DexBackedExceptionHandler(dexReader);
                    }
                }
            };
        }
    }
}
