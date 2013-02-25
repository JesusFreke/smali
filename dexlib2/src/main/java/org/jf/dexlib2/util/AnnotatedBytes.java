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

package org.jf.dexlib2.util;

import com.google.common.collect.Lists;
import org.jf.util.Hex;
import org.jf.util.TwoColumnOutput;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * Collects/presents a set of textual annotations, each associated with a range of bytes
 */
public class AnnotatedBytes {
    @Nonnull private List<AnnotationItem> annotations = Lists.newArrayList();
    private int cursor;
    private int indentLevel;

    /** &gt;= 40 (if used); the desired maximum output width */
    private int outputWidth;

    /**
     * &gt;= 8 (if used); the number of bytes of hex output to use
     * in annotations
     */
    private int hexCols = 8;

    public AnnotatedBytes(int width) {
        this.outputWidth = width;
    }

    /**
     * Skips a portion of the binary output. This is equivalent to calling
     * annotate(offset-cursor, "");
     *
     * @param offset The offset to skip to
     */
    public void skipTo(int offset) {
        if (offset < cursor) {
            throw new IllegalArgumentException("skipTo can only skip forward");
        }
        int delta = offset - cursor;
        if (delta != 0) {
            annotate(delta, "");
        }
    }

    /**
     * Add an annotation of the given length at the current location.
     *
     * @param length the length of data being annotated
     * @param msg the annotation message
     * @param formatArgs format arguments to pass to String.format
     */
    public void annotate(int length, @Nonnull String msg, Object... formatArgs) {
        annotations.add(new AnnotationItem(cursor, indentLevel, String.format(msg, formatArgs)));
        cursor += length;
    }

    public void indent() {
        indentLevel++;
    }

    public void deindent() {
        indentLevel--;
        if (indentLevel < 0) {
            indentLevel = 0;
        }
    }

    public int getCursor() {
        return cursor;
    }

    private static class AnnotationItem {
        public final int offset;
        public final int indentLevel;
        public final String annotation;

        public AnnotationItem(int offset, int  indentLevel, String annotation) {
            this.offset = offset;
            this.indentLevel = indentLevel;
            this.annotation = annotation;
        }
    }

    /**
     * Gets the width of the right side containing the annotations
     * @return
     */
    public int getAnnotationWidth() {
        int leftWidth = 8 + (hexCols * 2) + (hexCols / 2);

        return outputWidth - leftWidth;
    }

    /**
     * Writes the annotated content of this instance to the given writer.
     *
     * @param out non-null; where to write to
     */
    public void writeAnnotations(Writer out, byte[] data) throws IOException {
        int rightWidth = getAnnotationWidth();
        int leftWidth = outputWidth - rightWidth - 1;

        StringBuilder padding = new StringBuilder();
        for (int i=0; i<1000; i++) {
            padding.append(' ');
        }

        TwoColumnOutput twoc = new TwoColumnOutput(out, leftWidth, rightWidth, "|");
        Writer left = twoc.getLeft();
        Writer right = twoc.getRight();
        int leftAt = 0; // left-hand byte output cursor
        int rightAt = 0; // right-hand annotation index
        int rightSz = annotations.size();

        while ((leftAt < cursor) && (rightAt < rightSz)) {
            AnnotationItem a = annotations.get(rightAt);
            int start = a.offset;
            int end;

            if (rightAt + 1 < annotations.size()) {
                end = annotations.get(rightAt+1).offset;
            } else {
                end = cursor;
            }
            String text;

             // This is an area with an annotation.
            text = padding.substring(0, a.indentLevel * 2) + a.annotation;
            rightAt++;

            left.write(Hex.dump(data, start, end - start, start, hexCols, 6));
            right.write(text);
            twoc.flush();
            leftAt = end;
        }

        if (leftAt < cursor) {
            // There is unannotated output at the end.
            left.write(Hex.dump(data, leftAt, cursor - leftAt, leftAt,
                    hexCols, 6));
        }

        while (rightAt < rightSz) {
            // There are zero-byte annotations at the end.
            right.write(annotations.get(rightAt).annotation);
            rightAt++;
        }

        twoc.flush();
    }
}