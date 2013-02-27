/*
 * Copyright 2013, Google Inc.
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

package org.jf.dexlib2.dexbacked.raw;

import com.google.common.collect.ImmutableMap;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.util.FixedSizeList;
import org.jf.dexlib2.util.AnnotatedBytes;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

public class RawDexFile {
    @Nonnull private final byte[] buf;
    @Nonnull public final DexBackedDexFile dexFile;
    @Nonnull public final HeaderItem headerItem;

    public RawDexFile(byte[] buf) {
        this.buf = buf;
        this.dexFile = new DexBackedDexFile.Impl(buf);
        this.headerItem = new HeaderItem(dexFile);
    }

    public int getMapOffset() {
        return headerItem.getMapOffset();
    }

    public List<MapItem> getMapItems() {
        final int mapOffset = getMapOffset();
        final int mapSize = dexFile.readSmallUint(mapOffset);

        return new FixedSizeList<MapItem>() {
            @Override
            public MapItem readItem(int index) {
                int mapItemOffset = mapOffset + 4 + index * MapItem.ITEM_SIZE;
                return new MapItem(dexFile, mapItemOffset);
            }

            @Override public int size() {
                return mapSize;
            }
        };
    }

    private static final Map<Integer, SectionAnnotator> annotators;
    static {
        annotators = ImmutableMap.of(ItemType.TYPE_LIST, TypeListItem.getAnnotator());
    }

    public void dumpTo(Writer out, int width) throws IOException {
        AnnotatedBytes annotatedBytes = new AnnotatedBytes(width);
        HeaderItem.getAnnotator().annotateSection(annotatedBytes, dexFile, 1);

        int stringCount = headerItem.getStringCount();
        if (stringCount > 0) {
            annotatedBytes.skipTo(headerItem.getStringOffset());
            annotatedBytes.annotate(0, " ");
            StringIdItem.getAnnotator().annotateSection(annotatedBytes, dexFile, stringCount);
        }

        int typeCount = headerItem.getTypeCount();
        if (typeCount > 0) {
            annotatedBytes.skipTo(headerItem.getTypeOffset());
            annotatedBytes.annotate(0, " ");
            TypeIdItem.getAnnotator().annotateSection(annotatedBytes, dexFile, typeCount);
        }

        int protoCount = headerItem.getProtoCount();
        if (protoCount > 0) {
            annotatedBytes.skipTo(headerItem.getProtoOffset());
            annotatedBytes.annotate(0, " ");
            ProtoIdItem.getAnnotator().annotateSection(annotatedBytes, dexFile, protoCount);
        }

        int fieldCount = headerItem.getFieldCount();
        if (fieldCount > 0) {
            annotatedBytes.skipTo(headerItem.getFieldOffset());
            annotatedBytes.annotate(0, " ");
            FieldIdItem.getAnnotator().annotateSection(annotatedBytes, dexFile, fieldCount);
        }

        int methodCount = headerItem.getMethodCount();
        if (methodCount > 0) {
            annotatedBytes.skipTo(headerItem.getMethodOffset());
            annotatedBytes.annotate(0, " ");
            MethodIdItem.getAnnotator().annotateSection(annotatedBytes, dexFile, methodCount);
        }

        int classCount = headerItem.getClassCount();
        if (classCount > 0) {
            annotatedBytes.skipTo(headerItem.getClassOffset());
            annotatedBytes.annotate(0, " ");
            ClassDefItem.getAnnotator().annotateSection(annotatedBytes, dexFile, classCount);
        }

        for (MapItem mapItem: getMapItems()) {
            SectionAnnotator annotator = annotators.get(mapItem.getType());
            if (annotator != null) {
                annotatedBytes.skipTo(mapItem.getOffset());
                annotator.annotateSection(annotatedBytes, dexFile, mapItem.getItemCount());
            }
        }

        annotatedBytes.writeAnnotations(out, buf);
    }
}
