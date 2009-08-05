/*
 * [The "BSD licence"]
 * Copyright (c) 2009 Ben Gruver
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.dexlib.Util;

import java.io.*;

/**
 * Class that takes a combined output destination and provides two
 * output writers, one of which ends up writing to the left column and
 * one which goes on the right.
 */
public final class TwoColumnOutput {
    /** non-null; underlying writer for final output */
    private final Writer out;

    /** &gt; 0; the left column width */
    private final int leftWidth;

    /** non-null; pending left column output */
    private final StringBuffer leftBuf;

    /** non-null; pending right column output */
    private final StringBuffer rightBuf;

    /** non-null; left column writer */
    private final IndentingWriter leftColumn;

    /** non-null; right column writer */
    private final IndentingWriter rightColumn;

    /**
     * Turns the given two strings (with widths) and spacer into a formatted
     * two-column string.
     * 
     * @param s1 non-null; first string
     * @param width1 &gt; 0; width of the first column
     * @param spacer non-null; spacer string
     * @param s2 non-null; second string
     * @param width2 &gt; 0; width of the second column
     * @return non-null; an appropriately-formatted string
     */
    public static String toString(String s1, int width1, String spacer,
                                  String s2, int width2) {
        int len1 = s1.length();
        int len2 = s2.length();

        StringWriter sw = new StringWriter((len1 + len2) * 3);
        TwoColumnOutput twoOut =
            new TwoColumnOutput(sw, width1, width2, spacer);

        try {
            twoOut.getLeft().write(s1);
            twoOut.getRight().write(s2);
        } catch (IOException ex) {
            throw new RuntimeException("shouldn't happen", ex);
        }

        twoOut.flush();
        return sw.toString();
    }

    /**
     * Constructs an instance.
     * 
     * @param out non-null; writer to send final output to
     * @param leftWidth &gt; 0; width of the left column, in characters
     * @param rightWidth &gt; 0; width of the right column, in characters
     * @param spacer non-null; spacer string to sit between the two columns
     */
    public TwoColumnOutput(Writer out, int leftWidth, int rightWidth,
                           String spacer) {
        if (out == null) {
            throw new NullPointerException("out == null");
        }

        if (leftWidth < 1) {
            throw new IllegalArgumentException("leftWidth < 1");
        }

        if (rightWidth < 1) {
            throw new IllegalArgumentException("rightWidth < 1");
        }

        if (spacer == null) {
            throw new NullPointerException("spacer == null");
        }

        StringWriter leftWriter = new StringWriter(1000);
        StringWriter rightWriter = new StringWriter(1000);

        this.out = out;
        this.leftWidth = leftWidth;
        this.leftBuf = leftWriter.getBuffer();
        this.rightBuf = rightWriter.getBuffer();
        this.leftColumn = new IndentingWriter(leftWriter, leftWidth);
        this.rightColumn =
            new IndentingWriter(rightWriter, rightWidth, spacer);
    }

    /**
     * Constructs an instance.
     * 
     * @param out non-null; stream to send final output to
     * @param leftWidth &gt;= 1; width of the left column, in characters
     * @param rightWidth &gt;= 1; width of the right column, in characters
     * @param spacer non-null; spacer string to sit between the two columns
     */
    public TwoColumnOutput(OutputStream out, int leftWidth, int rightWidth,
                           String spacer) {
        this(new OutputStreamWriter(out), leftWidth, rightWidth, spacer);
    }

    /**
     * Gets the writer to use to write to the left column.
     * 
     * @return non-null; the left column writer
     */
    public Writer getLeft() {
        return leftColumn;
    }

    /**
     * Gets the writer to use to write to the right column.
     * 
     * @return non-null; the right column writer
     */
    public Writer getRight() {
        return rightColumn;
    }

    /**
     * Flushes the output. If there are more lines of pending output in one
     * column, then the other column will get filled with blank lines.
     */
    public void flush() {
        try {
            appendNewlineIfNecessary(leftBuf, leftColumn);
            appendNewlineIfNecessary(rightBuf, rightColumn);
            outputFullLines();
            flushLeft();
            flushRight();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Outputs to the final destination as many full line pairs as
     * there are in the pending output, removing those lines from
     * their respective buffers. This method terminates when at
     * least one of the two column buffers is empty.
     */
    private void outputFullLines() throws IOException {
        for (;;) {
            int leftLen = leftBuf.indexOf("\n");
            if (leftLen < 0) {
                return;
            }

            int rightLen = rightBuf.indexOf("\n");
            if (rightLen < 0) {
                return;
            }

            if (leftLen != 0) {
                out.write(leftBuf.substring(0, leftLen));
            }

            if (rightLen != 0) {
                writeSpaces(out, leftWidth - leftLen);
                out.write(rightBuf.substring(0, rightLen));
            }

            out.write('\n');

            leftBuf.delete(0, leftLen + 1);
            rightBuf.delete(0, rightLen + 1);
        }
    }

    /**
     * Flushes the left column buffer, printing it and clearing the buffer.
     * If the buffer is already empty, this does nothing.
     */
    private void flushLeft() throws IOException {
        appendNewlineIfNecessary(leftBuf, leftColumn);

        while (leftBuf.length() != 0) {
            rightColumn.write('\n');
            outputFullLines();
        }
    }

    /**
     * Flushes the right column buffer, printing it and clearing the buffer.
     * If the buffer is already empty, this does nothing.
     */
    private void flushRight() throws IOException {
        appendNewlineIfNecessary(rightBuf, rightColumn);

        while (rightBuf.length() != 0) {
            leftColumn.write('\n');
            outputFullLines();
        }
    }

    /**
     * Appends a newline to the given buffer via the given writer, but
     * only if it isn't empty and doesn't already end with one.
     * 
     * @param buf non-null; the buffer in question
     * @param out non-null; the writer to use
     */
    private static void appendNewlineIfNecessary(StringBuffer buf,
                                                 Writer out) 
            throws IOException {
        int len = buf.length();

        if ((len != 0) && (buf.charAt(len - 1) != '\n')) {
            out.write('\n');
        }
    }

    /**
     * Writes the given number of spaces to the given writer.
     * 
     * @param out non-null; where to write
     * @param amt &gt;= 0; the number of spaces to write
     */
    private static void writeSpaces(Writer out, int amt) throws IOException {
        while (amt > 0) {
            out.write(' ');
            amt--;
        }
    }
}
