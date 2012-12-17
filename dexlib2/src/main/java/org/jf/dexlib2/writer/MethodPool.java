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
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.util.ReferenceUtil;
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class MethodPool {
    public final static int METHOD_ID_ITEM_SIZE = 0x08;

    @Nonnull private final Map<MethodReference, Integer> internedMethodIdItems = Maps.newHashMap();
    @Nonnull private final DexFile dexFile;
    private int sectionOffset = -1;

    public MethodPool(@Nonnull DexFile dexFile) {
        this.dexFile = dexFile;
    }

    public void intern(@Nonnull MethodReference method) {
        Integer prev = internedMethodIdItems.put(method, 0);
        if (prev == null) {
            dexFile.typePool.intern(method.getDefiningClass());
            dexFile.protoPool.intern(method);
            dexFile.stringPool.intern(method.getName());
        }
    }

    public int getIndex(@Nonnull MethodReference methodReference) {
        Integer index = internedMethodIdItems.get(methodReference);
        if (index == null) {
            throw new ExceptionWithContext("Method not found.: %s", ReferenceUtil.getMethodDescriptor(methodReference));
        }
        return index;
    }

    public int getIndexedSectionSize() {
        return internedMethodIdItems.size() * METHOD_ID_ITEM_SIZE;
    }

    public int getNumItems() {
        return internedMethodIdItems.size();
    }

    public int getSectionOffset() {
        if (sectionOffset < 0) {
            throw new ExceptionWithContext("Section offset has not been set yet!");
        }
        return sectionOffset;
    }

    public void write(@Nonnull DexWriter writer) throws IOException {
        List<MethodReference> methods = Lists.newArrayList(internedMethodIdItems.keySet());

        sectionOffset = writer.getPosition();
        int index = 0;
        for (MethodReference method: methods) {
            internedMethodIdItems.put(method, index++);
            writer.writeUshort(dexFile.typePool.getIndex(method.getDefiningClass()));
            writer.writeUshort(dexFile.protoPool.getIndex(method));
            writer.writeInt(dexFile.stringPool.getIndex(method.getName()));
        }
    }
}
