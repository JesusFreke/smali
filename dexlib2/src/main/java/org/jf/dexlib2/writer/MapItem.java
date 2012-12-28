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

import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;
import java.io.IOException;

public class MapItem {
    DexFile dexFile;
    private int sectionOffset = -1;

    public MapItem(DexFile dexFile) {
        this.dexFile = dexFile;
    }

    public int getSectionOffset() {
        if (sectionOffset < 0) {
            throw new ExceptionWithContext("Section offset has not been set yet!");
        }
        return sectionOffset;
    }

    public void write(@Nonnull DexWriter writer) throws IOException {
        writer.align();
        sectionOffset = writer.getPosition();
        int numItems = calcNumItems();

        writer.writeInt(numItems);

        // index section
        writeItem(writer, DexItemType.HEADER_ITEM, 1, 0);
        writeItem(writer, DexItemType.STRING_ID_ITEM, dexFile.stringPool.getNumItems(), dexFile.stringPool.getIndexSectionOffset());
        writeItem(writer, DexItemType.TYPE_ID_ITEM, dexFile.typePool.getNumItems(), dexFile.typePool.getSectionOffset());
        writeItem(writer, DexItemType.PROTO_ID_ITEM, dexFile.protoPool.getNumItems(), dexFile.protoPool.getSectionOffset());
        writeItem(writer, DexItemType.FIELD_ID_ITEM, dexFile.fieldPool.getNumItems(), dexFile.fieldPool.getSectionOffset());
        writeItem(writer, DexItemType.METHOD_ID_ITEM, dexFile.methodPool.getNumItems(), dexFile.methodPool.getSectionOffset());
        writeItem(writer, DexItemType.CLASS_DEF_ITEM, dexFile.classDefPool.getNumClassDefItems(), dexFile.classDefPool.getIndexSectionOffset());

        // data section
        writeItem(writer, DexItemType.STRING_DATA_ITEM, dexFile.stringPool.getNumItems(), dexFile.stringPool.getDataSectionOffset());
        writeItem(writer, DexItemType.TYPE_LIST, dexFile.typeListPool.getNumItems(), dexFile.typeListPool.getSectionOffset());
        writeItem(writer, DexItemType.ENCODED_ARRAY_ITEM, dexFile.encodedArrayPool.getNumItems(), dexFile.encodedArrayPool.getSectionOffset());
        writeItem(writer, DexItemType.ANNOTATION_ITEM, dexFile.annotationPool.getNumItems(), dexFile.annotationPool.getSectionOffset());
        writeItem(writer, DexItemType.ANNOTATION_SET_ITEM, dexFile.annotationSetPool.getNumItems(), dexFile.annotationSetPool.getSectionOffset());
        writeItem(writer, DexItemType.ANNOTATION_SET_REF_LIST, dexFile.annotationSetRefPool.getNumItems(), dexFile.annotationSetRefPool.getSectionOffset());
        writeItem(writer, DexItemType.ANNOTATION_DIRECTORY_ITEM, dexFile.annotationDirectoryPool.getNumItems(), dexFile.annotationDirectoryPool.getSectionOffset());
        writeItem(writer, DexItemType.DEBUG_INFO_ITEM, dexFile.debugInfoPool.getNumItems(), dexFile.debugInfoPool.getSectionOffset());
        writeItem(writer, DexItemType.CODE_ITEM, dexFile.codeItemPool.getNumItems(), dexFile.codeItemPool.getSectionOffset());
        writeItem(writer, DexItemType.CLASS_DATA_ITEM, dexFile.classDefPool.getNumClassDataItems(), dexFile.classDefPool.getDataSectionOffset());
        writeItem(writer, DexItemType.MAP_LIST, 1, sectionOffset);
    }

    private int calcNumItems() {
        int numItems = 0;

        // header item
        numItems++;

        if (dexFile.stringPool.getNumItems() > 0) {
            numItems += 2; // index and data
        }
        if (dexFile.typePool.getNumItems() > 0) {
            numItems++;
        }
        if (dexFile.protoPool.getNumItems() > 0) {
            numItems++;
        }
        if (dexFile.fieldPool.getNumItems() > 0) {
            numItems++;
        }
        if (dexFile.methodPool.getNumItems() > 0) {
            numItems++;
        }
        if (dexFile.typeListPool.getNumItems() > 0) {
            numItems++;
        }
        if (dexFile.encodedArrayPool.getNumItems() > 0) {
            numItems++;
        }
        if (dexFile.annotationPool.getNumItems() > 0) {
            numItems++;
        }
        if (dexFile.annotationSetPool.getNumItems() > 0) {
            numItems++;
        }
        if (dexFile.annotationSetRefPool.getNumItems() > 0) {
            numItems++;
        }
        if (dexFile.annotationDirectoryPool.getNumItems() > 0) {
            numItems++;
        }
        if (dexFile.debugInfoPool.getNumItems() > 0) {
            numItems++;
        }
        if (dexFile.codeItemPool.getNumItems() > 0) {
            numItems++;
        }
        if (dexFile.classDefPool.getNumClassDefItems() > 0) {
            numItems++;
        }
        if (dexFile.classDefPool.getNumClassDataItems() > 0) {
            numItems++;
        }
        // map item itself
        numItems++;

        return numItems;
    }

    private void writeItem(DexWriter writer, int type, int size, int offset) throws IOException {
        if (size > 0) {
            writer.writeUshort(type);
            writer.writeUshort(0);
            writer.writeInt(size);
            writer.writeInt(offset);
        }
    }
}
