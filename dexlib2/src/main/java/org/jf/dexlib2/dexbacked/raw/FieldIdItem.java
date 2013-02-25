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

import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.util.AnnotatedBytes;

import javax.annotation.Nonnull;

public class FieldIdItem {
    public static final int ITEM_SIZE = 8;

    public static final int CLASS_OFFSET = 0;
    public static final int TYPE_OFFSET = 2;
    public static final int NAME_OFFSET = 4;

    @Nonnull
    public static SectionAnnotator getAnnotator() {
        return new SectionAnnotator() {
            @Nonnull @Override public String getItemName() {
                return "field_id_item";
            }

            @Override
            protected void annotateItem(@Nonnull AnnotatedBytes out, @Nonnull DexBackedDexFile dexFile, int itemIndex) {
                int classIndex = dexFile.readUshort(out.getCursor());
                out.annotate(2, "class_idx = %s", TypeIdItem.getReferenceAnnotation(dexFile, classIndex));

                int typeIndex = dexFile.readUshort(out.getCursor());
                out.annotate(2, "return_type_idx = %s", TypeIdItem.getReferenceAnnotation(dexFile, typeIndex));

                int nameIndex = dexFile.readSmallUint(out.getCursor());
                out.annotate(4, "name_idx = %s", StringIdItem.getReferenceAnnotation(dexFile, nameIndex));
            }
        };
    }
}
