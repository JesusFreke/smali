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
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.util.ReferenceUtil;
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class FieldPool {
    public final static int FIELD_ID_ITEM_SIZE = 0x08;

    @Nonnull private final Map<FieldReference, Integer> internedFieldIdItems = Maps.newHashMap();
    @Nonnull private final DexFile dexFile;
    private int sectionOffset = DexFile.NO_OFFSET;

    public FieldPool(@Nonnull DexFile dexFile) {
        this.dexFile = dexFile;
    }

    public void intern(@Nonnull FieldReference field) {
        Integer prev = internedFieldIdItems.put(field, 0);
        if (prev == null) {
            dexFile.typePool.intern(field.getDefiningClass());
            dexFile.stringPool.intern(field.getName());
            dexFile.typePool.intern(field.getType());
        }
    }

    public int getIndex(@Nonnull FieldReference fieldReference) {
        Integer index = internedFieldIdItems.get(fieldReference);
        if (index == null) {
            throw new ExceptionWithContext("Field not found.: %s", ReferenceUtil.getFieldDescriptor(fieldReference));
        }
        return index;
    }

    public int getIndexedSectionSize() {
        return internedFieldIdItems.size() * FIELD_ID_ITEM_SIZE;
    }

    public int getNumItems() {
        return internedFieldIdItems.size();
    }

    public int getSectionOffset() {
        if (sectionOffset < 0) {
            throw new ExceptionWithContext("Section offset has not been set yet!");
        }
        return sectionOffset;
    }

    public void write(@Nonnull DexWriter writer) throws IOException {
        List<FieldReference> fields = Lists.newArrayList(internedFieldIdItems.keySet());
        Collections.sort(fields);

        sectionOffset = 0;
        if (getNumItems() > 0) {
            sectionOffset = writer.getPosition();
        }

        int index = 0;
        for (FieldReference field: fields) {
            internedFieldIdItems.put(field, index++);
            writer.writeUshort(dexFile.typePool.getIndex(field.getDefiningClass()));
            writer.writeUshort(dexFile.typePool.getIndex(field.getType()));
            writer.writeInt(dexFile.stringPool.getIndex(field.getName()));
        }
    }
}
