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

/**
 * Wrapper for a <code>byte[]</code>, which provides read-only access and
 * can "reveal" a partial slice of the underlying array.
 *
 * <b>Note:</b> Multibyte accessors all use big-endian order.
 */
public final class ByteArray {
    /** non-null; underlying array */
    private final byte[] bytes;

    /** <code>&gt;= 0</code>; start index of the slice (inclusive) */
    private final int start;

    /** <code>&gt;= 0, &lt;= bytes.length</code>; size computed as
     * <code>end - start</code> (in the constructor) */
    private final int size;

    /**
     * Constructs an instance.
     *
     * @param bytes non-null; the underlying array
     * @param start <code>&gt;= 0</code>; start index of the slice (inclusive)
     * @param end <code>&gt;= start, &lt;= bytes.length</code>; end index of
     * the slice (exclusive)
     */
    public ByteArray(byte[] bytes, int start, int end) {
        if (bytes == null) {
            throw new NullPointerException("bytes == null");
        }

        if (start < 0) {
            throw new IllegalArgumentException("start < 0");
        }

        if (end < start) {
            throw new IllegalArgumentException("end < start");
        }

        if (end > bytes.length) {
            throw new IllegalArgumentException("end > bytes.length");
        }

        this.bytes = bytes;
        this.start = start;
        this.size = end - start;
    }

    /**
     * Constructs an instance from an entire <code>byte[]</code>.
     *
     * @param bytes non-null; the underlying array
     */
    public ByteArray(byte[] bytes) {
        this(bytes, 0, bytes.length);
    }

    /**
     * Gets the size of the array, in bytes.
     *
     * @return &gt;= 0; the size
     */
    public int size() {
        return size;
    }

    /**
     * Returns a slice (that is, a sub-array) of this instance.
     *
     * @param start <code>&gt;= 0</code>; start index of the slice (inclusive)
     * @param end <code>&gt;= start, &lt;= size()</code>; end index of
     * the slice (exclusive)
     * @return non-null; the slice
     */
    public ByteArray slice(int start, int end) {
        checkOffsets(start, end);
        return new ByteArray(bytes, start + this.start, end + this.start);
    }

    /**
     * Returns the offset into the given array represented by the given
     * offset into this instance.
     *
     * @param offset offset into this instance
     * @param bytes non-null; (alleged) underlying array
     * @return corresponding offset into <code>bytes</code>
     * @throws IllegalArgumentException thrown if <code>bytes</code> is
     * not the underlying array of this instance
     */
    public int underlyingOffset(int offset, byte[] bytes) {
        if (bytes != this.bytes) {
            throw new IllegalArgumentException("wrong bytes");
        }

        return start + offset;
    }

    /**
     * Gets the <code>signed byte</code> value at a particular offset.
     *
     * @param off <code>&gt;= 0, &lt; size(); offset to fetch
     * @return <code>signed byte</code> at that offset
     */
    public int getByte(int off) {
        checkOffsets(off, off + 1);
        return getByte0(off);
    }

    /**
     * Gets the <code>signed short</code> value at a particular offset.
     *
     * @param off <code>&gt;= 0, &lt; (size() - 1); offset to fetch
     * @return <code>signed short</code> at that offset
     */
    public int getShort(int off) {
        checkOffsets(off, off + 2);
        return (getByte0(off) << 8) | getUnsignedByte0(off + 1);
    }

    /**
     * Gets the <code>signed int</code> value at a particular offset.
     *
     * @param off <code>&gt;= 0, &lt; (size() - 3); offset to fetch
     * @return <code>signed int</code> at that offset
     */
    public int getInt(int off) {
        checkOffsets(off, off + 4);
        return (getByte0(off) << 24) |
            (getUnsignedByte0(off + 1) << 16) |
            (getUnsignedByte0(off + 2) << 8) |
            getUnsignedByte0(off + 3);
    }

    /**
     * Gets the <code>signed long</code> value at a particular offset.
     *
     * @param off <code>&gt;= 0, &lt; (size() - 7); offset to fetch
     * @return <code>signed int</code> at that offset
     */
    public long getLong(int off) {
        checkOffsets(off, off + 8);
        int part1 = (getByte0(off) << 24) |
            (getUnsignedByte0(off + 1) << 16) |
            (getUnsignedByte0(off + 2) << 8) |
            getUnsignedByte0(off + 3);
        int part2 = (getByte0(off + 4) << 24) |
            (getUnsignedByte0(off + 5) << 16) |
            (getUnsignedByte0(off + 6) << 8) |
            getUnsignedByte0(off + 7);

        return (part2 & 0xffffffffL) | ((long) part1) << 32;
    }

    /**
     * Gets the <code>unsigned byte</code> value at a particular offset.
     *
     * @param off <code>&gt;= 0, &lt; size(); offset to fetch
     * @return <code>unsigned byte</code> at that offset
     */
    public int getUnsignedByte(int off) {
        checkOffsets(off, off + 1);
        return getUnsignedByte0(off);
    }

    /**
     * Gets the <code>unsigned short</code> value at a particular offset.
     *
     * @param off <code>&gt;= 0, &lt; (size() - 1); offset to fetch
     * @return <code>unsigned short</code> at that offset
     */
    public int getUnsignedShort(int off) {
        checkOffsets(off, off + 2);
        return (getUnsignedByte0(off) << 8) | getUnsignedByte0(off + 1);
    }

    /**
     * Copies the contents of this instance into the given raw
     * <code>byte[]</code> at the given offset. The given array must be
     * large enough.
     *
     * @param out non-null; array to hold the output
     * @param offset non-null; index into <code>out</code> for the first
     * byte of output
     */
    public void getBytes(byte[] out, int offset) {
        if ((out.length - offset) < size) {
            throw new IndexOutOfBoundsException("(out.length - offset) < " +
                                                "size()");
        }

        System.arraycopy(bytes, start, out, offset, size);
    }

    /**
     * Checks a range of offsets for validity, throwing if invalid.
     *
     * @param s start offset (inclusive)
     * @param e end offset (exclusive)
     */
    private void checkOffsets(int s, int e) {
        if ((s < 0) || (e < s) || (e > size)) {
            throw new IllegalArgumentException("bad range: " + s + ".." + e +
                                               "; actual size " + size);
        }
    }

    /**
     * Gets the <code>signed byte</code> value at the given offset,
     * without doing any argument checking.
     *
     * @param off offset to fetch
     * @return byte at that offset
     */
    private int getByte0(int off) {
        return bytes[start + off];
    }

    /**
     * Gets the <code>unsigned byte</code> value at the given offset,
     * without doing any argument checking.
     *
     * @param off offset to fetch
     * @return byte at that offset
     */
    private int getUnsignedByte0(int off) {
        return bytes[start + off] & 0xff;
    }
}