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

import com.google.common.base.Preconditions;
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class DexWriter extends OutputStream {
    private static final int MAP_SIZE = 1024*1024;
    private static final int BUF_SIZE = 256*1024;

    @Nonnull private final FileChannel channel;
    private MappedByteBuffer byteBuffer;

    /** The position in the file at which byteBuffer starts. */
    private int mappedFilePosition;

    private byte[] buf = new byte[BUF_SIZE];
    /** The index within buf to write to */
    private int bufPosition;

    /**
     * A temporary buffer that can be used for larger writes. Can be replaced with a larger buffer if needed.
     * Must be at least 8 bytes
     */
    private byte[] tempBuf = new byte[8];

    /** A buffer of 0s we used for writing alignment values */
    private byte[] zeroBuf = new byte[3];

    public DexWriter(FileChannel channel, int position) throws IOException {
        this.channel = channel;
        this.mappedFilePosition = position;

        byteBuffer = channel.map(FileChannel.MapMode.READ_WRITE, position, MAP_SIZE);
    }

    @Override
    public void write(int b) throws IOException {
        if (bufPosition >= BUF_SIZE) {
            flushBuffer();
        }
        buf[bufPosition++] = (byte)b;
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        int toWrite = len;

        if (bufPosition == BUF_SIZE) {
            flushBuffer();
        }
        int remainingBuffer = BUF_SIZE - bufPosition;
        if (toWrite >= remainingBuffer) {
            // fill up and write out the current buffer
            System.arraycopy(b, 0, buf, bufPosition, remainingBuffer);
            bufPosition += remainingBuffer;
            toWrite -= remainingBuffer;
            flushBuffer();

            // skip the intermediate buffer while we have a full buffer's worth
            while (toWrite >= BUF_SIZE) {
                writeBufferToMap(b, len - toWrite, BUF_SIZE);
                toWrite -= BUF_SIZE;
            }
        }
        // write out the final chunk, if any
        if (toWrite > 0) {
            System.arraycopy(b, len-toWrite, buf, bufPosition, len);
            bufPosition += len;
        }
    }

    public void writeInt(int value) throws IOException {
        write(value);
        write(value >> 8);
        write(value >> 16);
        write(value >> 24);
    }

    public void writeShort(int value) throws IOException {
        if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
            throw new ExceptionWithContext("Short value out of range: %d", value);
        }
        write(value);
        write(value >> 8);
    }

    public void writeUshort(int value) throws IOException {
        if (value < 0 || value > 0xFFFF) {
            throw new ExceptionWithContext("Unsigned short value out of range: %d", value);
        }
        write(value);
        write(value >> 8);
    }

    public void writeUbyte(int value) throws IOException {
        if (value < 0 || value > 0xFF) {
            throw new ExceptionWithContext("Unsigned byte value out of range: %d", value);
        }
        write(value);
    }

    public void writeUleb128(int value) throws IOException {
        while (value > 0x7f) {
            write((value & 0x7f) | 0x80);
            value >>>= 7;
        }
        write(value);
    }

    public void writeSleb128(int value) throws IOException {
        if (value >= 0) {
            while (value > 0x3f) {
                write((value & 0x7f) | 0x80);
                value >>>= 7;
            }
            write(value & 0x7f);
        } else {
            while (value < -0x40) {
                write((value & 0x7f) | 0x80);
                value >>= 7;
            }
            write(value & 0x7f);
        }
    }

    /*    public static byte[] encodeSignedIntegralValue(long value) {
        int requiredBytes = getRequiredBytesForSignedIntegralValue(value);

        byte[] bytes = new byte[requiredBytes];

        for (int i = 0; i < requiredBytes; i++) {
            bytes[i] = (byte) value;
            value >>= 8;
        }
        return bytes;
    }*/


    /*
        public static long decodeUnsignedIntegralValue(byte[] bytes) {
        long value = 0;
        for (int i = 0; i < bytes.length; i++) {
            value |= (((long)(bytes[i] & 0xFF)) << i * 8);
        }
        return value;
    }
     */

    /*
    public static byte[] encodeUnsignedIntegralValue(long value) {
        int requiredBytes = getRequiredBytesForUnsignedIntegralValue(value);

        byte[] bytes = new byte[requiredBytes];

        for (int i = 0; i < requiredBytes; i++) {
            bytes[i] = (byte) value;
            value >>= 8;
        }
        return bytes;
    }
     */

    public void writeEncodedValueHeader(int valueType, int valueArg) throws IOException {
        write(valueType | (valueArg << 5));
    }

    public void writeEncodedInt(int valueType, int value) throws IOException {
        int index = 0;
        if (value >= 0) {
            while (value > 0x7f) {
                tempBuf[index++] = (byte)value;
                value >>= 8;
            }
        } else {
            while (value < -0x80) {
                tempBuf[index++] = (byte)value;
                value >>= 8;
            }
        }
        tempBuf[index++] = (byte)value;
        writeEncodedValueHeader(valueType, index);
        write(tempBuf, 0, index);
    }

    public void writeEncodedLong(int valueType, long value) throws IOException {
        int index = 0;
        if (value >= 0) {
            while (value > 0x7f) {
                tempBuf[index++] = (byte)value;
                value >>= 8;
            }
        } else {
            while (value < -0x80) {
                tempBuf[index++] = (byte)value;
                value >>= 8;
            }
        }
        tempBuf[index++] = (byte)value;
        writeEncodedValueHeader(valueType, index);
        write(tempBuf, 0, index);
    }

    public void writeEncodedUint(int valueType, int value) throws IOException {
        int index = 0;
        do {
            tempBuf[index++] = (byte)value;
            value >>= 8;
        } while (value != 0);
        writeEncodedValueHeader(valueType, index);
        write(tempBuf, 0, index);
    }

    public void writeEncodedFloat(int valueType, float value) throws IOException {
        int intValue = Float.floatToRawIntBits(value);

        int index = 3;
        do {
            buf[index--] = (byte)((intValue & 0xFF000000) >>> 24);
            intValue <<= 8;
        } while (intValue != 0);
        writeEncodedValueHeader(valueType, 4-index);
        write(buf, index+1, 4-index);
    }

    public void writeEncodedDouble(int valueType, double value) throws IOException {
        long longValue = Double.doubleToRawLongBits(value);

        int index = 7;
        do {
            buf[index--] = (byte)((longValue & 0xFF00000000000000L) >>> 56);
            longValue <<= 8;
        } while (longValue != 0);
        writeEncodedValueHeader(valueType, 7-index);
        write(buf, index+1, 7-index);
    }

    public void writeString(String string) throws IOException {
        int len = string.length();

        // make sure we have enough room in the temporary buffer
        if (tempBuf.length <= string.length()*3) {
            tempBuf = new byte[string.length()*3];
        }

        final byte[] buf = tempBuf;

        int bufPos = 0;
        for (int i = 0; i < len; i++) {
            char c = string.charAt(i);
            if ((c != 0) && (c < 0x80)) {
                buf[bufPos++] = (byte)c;
            } else if (c < 0x800) {
                buf[bufPos++] = (byte)(((c >> 6) & 0x1f) | 0xc0);
                buf[bufPos++] = (byte)((c & 0x3f) | 0x80);
            } else {
                buf[bufPos++] = (byte)(((c >> 12) & 0x0f) | 0xe0);
                buf[bufPos++] = (byte)(((c >> 6) & 0x3f) | 0x80);
                buf[bufPos++] = (byte)((c & 0x3f) | 0x80);
            }
        }
        write(buf, 0, bufPos);
    }

    public void align() throws IOException {
        int zeros = (-getPosition()) & 3;
        if (zeros > 0) {
            write(zeroBuf, 0, zeros);
        }
    }

    @Override
    public void flush() throws IOException {
        if (bufPosition > 0) {
            writeBufferToMap(buf, 0, bufPosition);
            bufPosition = 0;
        }
        byteBuffer.force();
        mappedFilePosition += byteBuffer.position();
        channel.position(mappedFilePosition + MAP_SIZE);
        channel.write(ByteBuffer.wrap(new byte[]{0}));
        byteBuffer = channel.map(FileChannel.MapMode.READ_WRITE, mappedFilePosition, MAP_SIZE);
    }

    @Override
    public void close() throws IOException {
        if (bufPosition > 0) {
            writeBufferToMap(buf, 0, bufPosition);
            bufPosition = 0;
        }
        byteBuffer.force();
        byteBuffer = null;
        buf = null;
        tempBuf = null;
    }

    private void flushBuffer() throws IOException {
        Preconditions.checkState(bufPosition == BUF_SIZE);
        writeBufferToMap(buf, 0, BUF_SIZE);
        bufPosition = 0;
    }

    private void writeBufferToMap(byte[] buf, int bufOffset, int len) throws IOException {
        // we always write BUF_SIZE, which evenly divides our mapped size, so we only care if remaining is 0 yet
        if (!byteBuffer.hasRemaining()) {
            byteBuffer.force();
            mappedFilePosition += MAP_SIZE;
            byteBuffer = channel.map(FileChannel.MapMode.READ_WRITE, mappedFilePosition, MAP_SIZE);
        }
        byteBuffer.put(buf, bufOffset, len);
    }

    public int getPosition() {
        return mappedFilePosition + byteBuffer.position() + bufPosition;
    }
}
