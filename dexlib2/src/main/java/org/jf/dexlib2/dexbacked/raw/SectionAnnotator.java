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

import org.jf.dexlib2.util.AnnotatedBytes;
import org.jf.util.AlignmentUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class SectionAnnotator {
    @Nonnull public abstract String getItemName();
    protected abstract void annotateItem(@Nonnull AnnotatedBytes out, @Nonnull RawDexFile dexFile, int itemIndex);

    /**
     * Write out annotations for this section
     *
     * @param out The AnnotatedBytes object to annotate to
     * @param dexFile The DexBackedDexFile representing the dex file being annotated
     * @param itemCount The number of items in the section (from the header/map)
     */
    public void annotateSection(@Nonnull AnnotatedBytes out, @Nonnull RawDexFile dexFile, int itemCount) {
        String itemName = getItemName();
        int itemAlignment = getItemAlignment();
        if (itemCount > 0) {
            out.annotate(0, "-----------------------------");
            out.annotate(0, "%s section", itemName);
            out.annotate(0, "-----------------------------");
            out.annotate(0, "");

            for (int i=0; i<itemCount; i++) {
                out.moveTo(AlignmentUtils.alignOffset(out.getCursor(), itemAlignment));

                String itemIdentity = getItemIdentity(dexFile, i, out.getCursor());
                if (itemIdentity != null) {
                    out.annotate(0, "[%d] %s: %s", i, itemName, itemIdentity);
                } else {
                    out.annotate(0, "[%d] %s", i, itemName);
                }
                out.indent();
                annotateItem(out, dexFile, i);
                out.deindent();
            }
        }
    }

    @Nullable public String getItemIdentity(@Nonnull RawDexFile dexFile, int itemIndex, int itemOffset) {
        return null;
    }

    public int getItemAlignment() {
        return 1;
    }
}
