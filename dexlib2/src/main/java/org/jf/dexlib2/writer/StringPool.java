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
import org.jf.util.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class StringPool {
    private final static int STRING_ID_ITEM_SIZE = 4;
    @Nonnull private final Map<String, Integer> internedStringIdItems = Maps.newHashMap();

    public void intern(@Nonnull CharSequence string) {
        internedStringIdItems.put(string.toString(), 0);
    }

    public void internNullable(@Nullable CharSequence string) {
        if (string != null) {
            intern(string);
        }
    }

    public int getIndex(@Nonnull CharSequence string) {
        Integer index = internedStringIdItems.get(string.toString());
        if (index == null) {
            throw new ExceptionWithContext("String not found.: %s",
                    StringUtils.escapeString(string.toString()));
        }
        return index;
    }

    public int getIndexNullable(@Nullable CharSequence string) {
        if (string == null) {
            return -1;
        }
        return getIndex(string);
    }

    public int getIndexedSectionSize() {
        return internedStringIdItems.size() * STRING_ID_ITEM_SIZE;
    }

    public void write(@Nonnull DexWriter indexWriter, @Nonnull DexWriter offsetWriter) throws IOException {
        List<String> strings = Lists.newArrayList(internedStringIdItems.keySet());
        Collections.sort(strings);

        int index = 0;
        for (String string: strings) {
            internedStringIdItems.put(string, index++);
            indexWriter.writeInt(offsetWriter.getPosition());
            offsetWriter.writeUleb128(string.length());
            offsetWriter.writeString(string);
            offsetWriter.write(0);
        }
    }
}
