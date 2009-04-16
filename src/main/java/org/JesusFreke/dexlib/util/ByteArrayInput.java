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
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.JesusFreke.dexlib.util;

/**
 * Implementation of {@link Input} which reads the data from a
 * <code>byte[]</code> instance.
 *
 * <p><b>Note:</b> As per the {@link Input } interface, multi-byte
 * reads all use little-endian order.</p>
 */
public class ByteArrayInput
    implements Input {

    /** non-null; the data itself */
    private byte[] data;

    /** &gt;= 0; current read cursor */
    private int cursor;

    /**
     * Constructs an instance with the given data
     *
     * @param data non-null; data array to use for input
     */
    public ByteArrayInput(byte[] data) {
        if (data == null) {
            throw new NullPointerException("data == null");
        }

        this.data = data;
        this.cursor = 0;      
    }

    /**
     * Gets the underlying <code>byte[]</code> of this instance
     *
     * @return non-null; the <code>byte[]</code>
     */
    public byte[] getArray() {
        return data;
    }

    /** {@inheritDoc} */
    public int getCursor() {
        return cursor;
    }

    /** {@inheritDoc} */
    public void setCursor(int cursor) {
        if (cursor < 0 || cursor >= data.length)
            throw new IndexOutOfBoundsException("The provided cursor value " +
                    "is not within the bounds of this instance's data array");
        this.cursor = cursor;
    }

    /** {@inheritDoc} */
    public void assertCursor(int expectedCursor) {
        if (cursor != expectedCursor) {
            throw new ExceptionWithContext("expected cursor " +
                    expectedCursor + "; actual value: " + cursor);
        }
    }

    /** {@inheritDoc} */
    public byte readByte() {
        int readAt = cursor;
        int end = readAt + 1;

        if (end > data.length) {
            throwBounds();
        }

        cursor = end;
        return data[readAt];
    }

    /** {@inheritDoc} */
    public int readShort() {
        int readAt = cursor;
        int end = readAt + 2;

        if (end > data.length) {
            throwBounds();
        }

        cursor = end;
        return (int)((data[readAt] & 0xff) +
                ((data[readAt + 1] & 0xff) << 8));
    }

    /** {@inheritDoc} */
    public int readInt() {
        int readAt = cursor;
        int end = readAt + 4;

        if (end > data.length) {
            throwBounds();
        }

        cursor = end;
        return  (data[readAt] & 0xff) +
                ((data[readAt + 1] & 0xff) << 8) +
                ((data[readAt + 2] & 0xff) << 16) +
                ((data[readAt + 3] & 0xff) << 24);
    }

    /** {@inheritDoc} */
    public long readLong() {
        int readAt = cursor;
        int end = readAt + 8;

        if (end > data.length) {
            throwBounds();
        }

        cursor = end;

        /**
         * TODO: is there a faster/better way to do this?
         */
        return  (long)((data[readAt] & 0xff) +
                ((data[readAt + 1] & 0xff) << 8) +
                ((data[readAt + 2] & 0xff) << 16) +
                ((data[readAt + 3] & 0xff) << 24))

                +

                ((long)((data[readAt + 4] & 0xff)+
                ((data[readAt + 5] & 0xff) << 8) +
                ((data[readAt + 6] & 0xff) << 16) +
                ((data[readAt + 7] & 0xff) << 24)) << 32);
    }

    /** {@inheritDoc} */
    public int readUnsignedLeb128() {
        int end = cursor;
        int currentByteValue;
        int result;

        result = data[end++] & 0xff;
        if (result > 0x7f) {
            currentByteValue = data[end++] & 0xff;
            result = (result & 0x7f) | ((currentByteValue & 0x7f) << 7);
            if (currentByteValue > 0x7f) {
                currentByteValue = data[end++] & 0xff;
                result |= (currentByteValue & 0x7f) << 14;
                if (currentByteValue > 0x7f) {
                    currentByteValue = data[end++] & 0xff;
                    result |= (currentByteValue & 0x7f) << 21;
                    if (currentByteValue > 0x7f) {
                        currentByteValue = data[end++] & 0xff;
                        if (currentByteValue > 0x0f) {
                            throwInvalidLeb();
                        }
                        result |= currentByteValue << 28;
                    }
                }
            }
        }

        cursor = end;
        return result;
    }

    /** {@inheritDoc} */
    public int readSignedLeb128() {
        int end = cursor;
        int currentByteValue;
        int result;

        result = data[end++] & 0xff;
        if (result <= 0x7f) {
            result = (result << 25) >> 25;
        } else {
            currentByteValue = data[end++] & 0xff;
            result = (result & 0x7f) | ((currentByteValue & 0x7f) << 7);
            if (currentByteValue <= 0x7f) {
                result = (result << 18) >> 18;
            } else {
                currentByteValue = data[end++] & 0xff;
                result |= (currentByteValue & 0x7f) << 14;
                if (currentByteValue <= 0x7f) {
                    result = (result << 11) >> 11;
                } else {
                    currentByteValue = data[end++] & 0xff;
                    result |= (currentByteValue & 0x7f) << 21;
                    if (currentByteValue <= 0x7f) {
                        result = (result << 4) >> 4;
                    } else {
                        currentByteValue = data[end++] & 0xff;
                        if (currentByteValue > 0x0f) {
                            throwInvalidLeb();
                        }
                        result |= currentByteValue << 28;
                    }
                }
            }
        }

        cursor = end;
        return result;
    }

    /** {@inheritDoc} */
    public void read(byte[] bytes, int offset, int length) {
        int end = cursor + length;

        if (end > data.length) {
            throwBounds();
        }

        System.arraycopy(data, cursor, bytes, offset, length);
        cursor = end;
    }

    /** {@inheritDoc} */
    public void read(byte[] bytes) {
        int length = bytes.length;
        int end = cursor + length;

        if (end > data.length) {
            throwBounds();
        }

        System.arraycopy(data, cursor, bytes, 0, length);
        cursor = end;
    }

    /** {@inheritDoc} */
    public byte[] readBytes(int length) {
        int end = cursor + length;

        if (end > data.length) {
            throwBounds();
        }

        byte[] result = new byte[length];
        System.arraycopy(data, cursor, result, 0, length);
        cursor = end;
        return result;
    }

    /** {@inheritDoc} */
    public void skipBytes(int count) {
        int end = cursor + count;

        if (end > data.length) {
            throwBounds();
        }

        cursor = end;
    }

    /** {@inheritDoc} */
    public void alignTo(int alignment) {
        int mask = alignment - 1;

        if ((alignment < 0) || ((mask & alignment) != 0)) {
            throw new IllegalArgumentException("bogus alignment");
        }

        int end = (cursor + mask) & ~mask;

        if (end > data.length) {
            throwBounds();
        }
        
        cursor = end;
    }

    /**
     * Throws the excpetion for when an attempt is made to read past the
     * end of the instance.
     */
    private static void throwBounds() {
        throw new IndexOutOfBoundsException("attempt to read past the end");
    }

    /**
     * Throws the exception for when an invalid LEB128 value is encountered
     */
    private static void throwInvalidLeb() {
        throw new RuntimeException("invalid LEB128 integer encountered");
    }
}
