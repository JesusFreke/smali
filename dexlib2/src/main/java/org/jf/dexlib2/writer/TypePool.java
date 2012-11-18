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
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TypePool {
    private final static int TYPE_ID_ITEM_SIZE = 4;
    @Nonnull private final Map<String, Integer> internedTypeIdItems = Maps.newHashMap();
    @Nonnull private final DexFile dexFile;

    public TypePool(@Nonnull DexFile dexFile) {
        this.dexFile = dexFile;
    }

    public void intern(@Nonnull CharSequence type) {
        Integer prev = internedTypeIdItems.put(type.toString(), 0);
        if (prev == null) {
            dexFile.stringPool.intern(type);
        }
    }

    public void internNullable(@Nullable CharSequence type) {
        if (type != null) {
            intern(type);
        }
    }

    public int getIndex(@Nonnull CharSequence type) {
        Integer index = internedTypeIdItems.get(type.toString());
        if (index == null) {
            throw new ExceptionWithContext("Type not found.: %s", type);
        }
        return index;
    }

    public int getIndexNullable(@Nullable CharSequence type) {
        if (type == null) {
            return -1;
        }
        return getIndex(type);
    }

    public int getIndexedSectionSize() {
        return internedTypeIdItems.size() * TYPE_ID_ITEM_SIZE;
    }

    public void write(@Nonnull DexWriter writer) throws IOException {
        List<String> types = Lists.newArrayList(internedTypeIdItems.keySet());
        Collections.sort(types);

        int index = 0;
        for (String type: types) {
            internedTypeIdItems.put(type, index++);
            int stringIndex = dexFile.stringPool.getIndex(type);
            writer.writeInt(stringIndex);
        }
    }
}
