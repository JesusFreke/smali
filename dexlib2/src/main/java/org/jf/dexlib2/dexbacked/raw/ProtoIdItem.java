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

public class ProtoIdItem {
    public static final int ITEM_SIZE = 12;

    public static final int SHORTY_OFFSET = 0;
    public static final int RETURN_TYPE_OFFSET = 4;
    public static final int PARAMETERS_OFFSET = 8;

    public static SectionAnnotator getAnnotator() {
        return new SectionAnnotator() {
            @Override
            public void annotateSection(@Nonnull AnnotatedBytes out, @Nonnull DexBackedDexFile dexFile, int length) {
                if (length > 0) {
                    out.annotate(0, "-----------------------------");
                    out.annotate(0, "proto_id_item section");
                    out.annotate(0, "-----------------------------");
                    out.annotate(0, "");

                    for (int i=0; i<length; i++) {
                        out.annotate(0, "[%d] proto_id_item", i);
                        out.indent();
                        annotateProto(out, dexFile);
                        out.deindent();
                    }
                }
            }
        };
    }

    private static void annotateProto(@Nonnull AnnotatedBytes out, @Nonnull DexBackedDexFile dexFile) {
        int shortyIndex = dexFile.readSmallUint(out.getCursor());
        out.annotate(4, "shorty_idx = %s", StringIdItem.getReferenceAnnotation(dexFile, shortyIndex));

        int returnTypeIndex = dexFile.readSmallUint(out.getCursor());
        out.annotate(4, "return_type_idx = %s", TypeIdItem.getReferenceAnnotation(dexFile, returnTypeIndex));

        // TODO: add formatted type list to output
        int parametersOffset = dexFile.readSmallUint(out.getCursor());
        if (parametersOffset != 0) {
            out.annotate(4, "parameters_off = type_list_item[0x%x]", parametersOffset);
        } else {
            out.annotate(4, "parameters_off = 0");
        }
    }
}
