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

package org.jf.dexlib2.dexbacked;

import junit.framework.Assert;
import org.jf.util.ExceptionWithContext;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;

public class DexFileBufferTest {
    @Test
    public void testReadSmallUintSuccess() {
        DexFileBuffer dexFile = new DexFileBuffer(new byte[] {0x11, 0x22, 0x33, 0x44});
        Assert.assertEquals(0x44332211, dexFile.readSmallUint(0));

        dexFile = new DexFileBuffer(new byte[] {0x00, 0x00, 0x00, 0x00});
        Assert.assertEquals(0, dexFile.readSmallUint(0));

        dexFile = new DexFileBuffer(new byte[] {(byte)0xff, (byte)0xff, (byte)0xff, 0x7f});
        Assert.assertEquals(0x7fffffff, dexFile.readSmallUint(0));
    }

    @Test(expected=ExceptionWithContext.class)
    public void testReadSmallUintTooLarge1() {
        DexFileBuffer dexFile = new DexFileBuffer(new byte[] {0x00, 0x00, 0x00, (byte)0x80});
        dexFile.readSmallUint(0);
    }

    @Test(expected=ExceptionWithContext.class)
    public void testReadSmallUintTooLarge2() {
        DexFileBuffer dexFile = new DexFileBuffer(new byte[] {(byte)0xff, (byte)0xff, (byte)0xff, (byte)0x80});
        dexFile.readSmallUint(0);
    }

    @Test(expected=ExceptionWithContext.class)
    public void testReadSmallUintTooLarge3() {
        DexFileBuffer dexFile = new DexFileBuffer(new byte[] {(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff});
        dexFile.readSmallUint(0);
    }

    @Test
    public void testReadUshort() {
        DexFileBuffer dexFile = new DexFileBuffer(new byte[] {0x11, 0x22});
        Assert.assertEquals(dexFile.readUshort(0), 0x2211);

        dexFile = new DexFileBuffer(new byte[] {0x00, 0x00});
        Assert.assertEquals(dexFile.readUshort(0), 0);

        dexFile = new DexFileBuffer(new byte[] {(byte)0xff, (byte)0xff});
        Assert.assertEquals(dexFile.readUshort(0), 0xffff);

        dexFile = new DexFileBuffer(new byte[] {(byte)0x00, (byte)0x80});
        Assert.assertEquals(dexFile.readUshort(0), 0x8000);

        dexFile = new DexFileBuffer(new byte[] {(byte)0xff, (byte)0x7f});
        Assert.assertEquals(dexFile.readUshort(0), 0x7fff);
    }

    @Test
    public void testReadUbyte() {
        byte[] buf = new byte[1];
        DexFileBuffer dexFile = new DexFileBuffer(buf);

        for (int i=0; i<=0xff; i++) {
            buf[0] = (byte)i;
            Assert.assertEquals(i, dexFile.readUbyte(0));
        }
    }

    @Test
    public void testReadLong() {
        DexFileBuffer dexFile = new DexFileBuffer(new byte[] {0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77});
        Assert.assertEquals(0x7766554433221100L, dexFile.readLong(0));

        dexFile = new DexFileBuffer(new byte[] {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
        Assert.assertEquals(0, dexFile.readLong(0));

        dexFile = new DexFileBuffer(new byte[] {(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff,
                (byte)0xff, (byte)0xff, (byte)0xff, 0x7f});
        Assert.assertEquals(Long.MAX_VALUE, dexFile.readLong(0));

        dexFile = new DexFileBuffer(new byte[] {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte)0x80});
        Assert.assertEquals(Long.MIN_VALUE, dexFile.readLong(0));

        dexFile = new DexFileBuffer(new byte[] {(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff,
                (byte)0xff, (byte)0xff, (byte)0xff, (byte)0x80});
        Assert.assertEquals(0x80ffffffffffffffL, dexFile.readLong(0));

        dexFile = new DexFileBuffer(new byte[] {(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff,
                (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff});
        Assert.assertEquals(-1, dexFile.readLong(0));

    }

    @Test
    public void testReadInt() {
        DexFileBuffer dexFile = new DexFileBuffer(new byte[] {0x11, 0x22, 0x33, 0x44});
        Assert.assertEquals(0x44332211, dexFile.readInt(0));

        dexFile = new DexFileBuffer(new byte[] {0x00, 0x00, 0x00, 0x00});
        Assert.assertEquals(0, dexFile.readInt(0));

        dexFile = new DexFileBuffer(new byte[] {(byte)0xff, (byte)0xff, (byte)0xff, 0x7f});
        Assert.assertEquals(Integer.MAX_VALUE, dexFile.readInt(0));

        dexFile = new DexFileBuffer(new byte[] {0x00, 0x00, 0x00, (byte)0x80});
        Assert.assertEquals(Integer.MIN_VALUE, dexFile.readInt(0));

        dexFile = new DexFileBuffer(new byte[] {(byte)0xff, (byte)0xff, (byte)0xff, (byte)0x80});
        Assert.assertEquals(0x80ffffff, dexFile.readInt(0));

        dexFile = new DexFileBuffer(new byte[] {(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff});
        Assert.assertEquals(-1, dexFile.readInt(0));
    }

    @Test
    public void testReadShort() {
        DexFileBuffer dexFile = new DexFileBuffer(new byte[] {0x11, 0x22});
        Assert.assertEquals(dexFile.readShort(0), 0x2211);

        dexFile = new DexFileBuffer(new byte[] {0x00, 0x00});
        Assert.assertEquals(dexFile.readShort(0), 0);

        dexFile = new DexFileBuffer(new byte[] {(byte)0xff, (byte)0xff});
        Assert.assertEquals(dexFile.readShort(0), -1);

        dexFile = new DexFileBuffer(new byte[] {(byte)0x00, (byte)0x80});
        Assert.assertEquals(dexFile.readShort(0), Short.MIN_VALUE);

        dexFile = new DexFileBuffer(new byte[] {(byte)0xff, (byte)0x7f});
        Assert.assertEquals(dexFile.readShort(0), 0x7fff);

        dexFile = new DexFileBuffer(new byte[] {(byte)0xff, (byte)0x80});
        Assert.assertEquals(dexFile.readShort(0), 0xffff80ff);
    }

    @Test
    public void testReadByte() {
        byte[] buf = new byte[1];
        DexFileBuffer dexFile = new DexFileBuffer(buf);

        for (int i=0; i<=0xff; i++) {
            buf[0] = (byte)i;
            Assert.assertEquals((byte)i, dexFile.readByte(0));
        }
    }

    @Test
    public void testReadRandom() {
        Random r = new Random(1234567890);
        ByteBuffer byteBuf = ByteBuffer.allocateDirect(4).order(ByteOrder.LITTLE_ENDIAN);
        byte[] buf = new byte[4];
        DexFileBuffer dexFile = new DexFileBuffer(buf);

        for (int i=0; i<10000; i++) {
            int val = r.nextInt();
            byteBuf.putInt(0, val);
            byteBuf.position(0);
            byteBuf.get(buf);

            boolean expectException = val < 0;
            try {
                int returnedVal = dexFile.readSmallUint(0);
                Assert.assertFalse(String.format("Didn't throw an exception for value: %x", val), expectException);
                Assert.assertEquals(val, returnedVal);
            } catch (Exception ex) {
                Assert.assertTrue(String.format("Threw an exception for value: %x", val), expectException);
            }

            Assert.assertEquals(val, dexFile.readInt(0));

            Assert.assertEquals(val & 0xFFFF, dexFile.readUshort(0));
            Assert.assertEquals((val >> 8) & 0xFFFF, dexFile.readUshort(1));
            Assert.assertEquals((val >> 16) & 0xFFFF, dexFile.readUshort(2));

            Assert.assertEquals((short)val, dexFile.readShort(0));
            Assert.assertEquals((short)(val >> 8), dexFile.readShort(1));
            Assert.assertEquals((short)(val >> 16), dexFile.readShort(2));
        }
    }

    @Test
    public void testReadLongRandom() {
        Random r = new Random(1234567890);
        ByteBuffer byteBuf = ByteBuffer.allocateDirect(8).order(ByteOrder.LITTLE_ENDIAN);
        byte[] buf = new byte[8];
        DexFileBuffer dexFile = new DexFileBuffer(buf);

        for (int i=0; i<10000; i++) {
            int val = r.nextInt();
            byteBuf.putLong(0, val);
            byteBuf.position(0);
            byteBuf.get(buf);

            Assert.assertEquals(val, dexFile.readLong(0));
        }
    }
}
