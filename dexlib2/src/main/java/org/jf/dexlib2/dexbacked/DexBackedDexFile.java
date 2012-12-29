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

import org.jf.dexlib2.dexbacked.util.FixedSizeList;
import org.jf.dexlib2.dexbacked.util.FixedSizeSet;
import org.jf.dexlib2.iface.DexFile;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Set;

public class DexBackedDexFile implements DexFile {
    @Nonnull public final DexBuffer dexBuf;

    public DexBackedDexFile(@Nonnull DexBuffer dexBuf) {
        this.dexBuf = dexBuf;
    }

    @Nonnull
    @Override
    public Set<? extends DexBackedClassDef> getClasses() {
        final int classCount = dexBuf.getClassCount();

        return new FixedSizeSet<DexBackedClassDef>() {
            @Nonnull
            @Override
            public DexBackedClassDef readItem(int index) {
                int classOffset = dexBuf.getClassDefItemOffset(index);
                return new DexBackedClassDef(dexBuf, classOffset);
            }

            @Override
            public int size() {
                return classCount;
            }
        };
    }

    public int getChecksum() {
        return dexBuf.getChecksum();
    }

    public byte[] getSignature() {
        return dexBuf.getSignature();
    }

    public List<DexBackedMapItem> getMap() {
        final int mapOffset = dexBuf.getMapOffset();
        final int sectionCount = dexBuf.readSmallUint(mapOffset);

        return new FixedSizeList<DexBackedMapItem>() {
            @Override
            public DexBackedMapItem readItem(int index) {
                int mapItemOffset = mapOffset + 4 + index * DexBuffer.MAP_ITEM_SIZE;
                return new DexBackedMapItem(dexBuf, mapItemOffset);
            }

            @Override
            public int size() {
                return sectionCount;
            }
        };
    }
}
