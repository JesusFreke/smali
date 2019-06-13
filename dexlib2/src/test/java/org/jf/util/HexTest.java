/*
 * [The "BSD licence"]
 * Copyright (c) 2019 Ben Gruver (JesusFreke)
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

package org.jf.util;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class HexTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void testU8() {
        Assert.assertEquals("0000000000000000", Hex.u8(0L));
        Assert.assertEquals("0000016b5086c128", Hex.u8(1560424137000L));
        Assert.assertEquals("000462d53c8abac0", Hex.u8(1234567890123456L));
    }

    @Test
    public void testU4() {
        Assert.assertEquals("00000000", Hex.u4(0));
        Assert.assertEquals("00bc614e", Hex.u4(12345678));
        Assert.assertEquals("499602d2", Hex.u4(1234567890));
    }

    @Test
    public void testU3() {
        Assert.assertEquals("000000", Hex.u3(0));
        Assert.assertEquals("01e240", Hex.u3(123456));
        Assert.assertEquals("9602d2", Hex.u3(1234567890));
    }

    @Test
    public void testU2() {
        Assert.assertEquals("0000", Hex.u2(0));
        Assert.assertEquals("04d2", Hex.u2(1234));
        Assert.assertEquals("02d2", Hex.u2(1234567890));
    }

    @Test
    public void testU2or4() {
        Assert.assertEquals("0000", Hex.u2or4(0));
        Assert.assertEquals("04d2", Hex.u2or4(1234));
        Assert.assertEquals("0001e240", Hex.u2or4(123456));
        Assert.assertEquals("00bc614e", Hex.u2or4(12345678));
        Assert.assertEquals("499602d2", Hex.u2or4(1234567890));
    }

    @Test
    public void testU1() {
        Assert.assertEquals("00", Hex.u1(0));
        Assert.assertEquals("0c", Hex.u1(12));
        Assert.assertEquals("d2", Hex.u1(1234567890));
    }

    @Test
    public void testUNibble() {
        Assert.assertEquals("0", Hex.uNibble(0));
        Assert.assertEquals("1", Hex.uNibble(1));
        Assert.assertEquals("f", Hex.uNibble(999999999));
        Assert.assertEquals("2", Hex.uNibble(1234567890));
    }

    @Test
    public void testS8() {
        Assert.assertEquals("+0000000000000000", Hex.s8(0L));
        Assert.assertEquals("-7fffffffffffffff", Hex.s8(-9223372036854775807L));
        Assert.assertEquals("+002bdc545d6b4b87", Hex.s8(12345678901234567L));
    }

    @Test
    public void testS4() {
        Assert.assertEquals("+00000000", Hex.s4(0));
        Assert.assertEquals("-80000000", Hex.s4(-2147483648));
        Assert.assertEquals("+075bcd15", Hex.s4(123456789));
        Assert.assertEquals("+499602d2", Hex.s4(1234567890));
    }

    @Test
    public void testS2() {
        Assert.assertEquals("+0000", Hex.s2(0));
        Assert.assertEquals("-0000", Hex.s2(-2147483648));
        Assert.assertEquals("+3039", Hex.s2(12345));
        Assert.assertEquals("+02d2", Hex.s2(1234567890));
    }

    @Test
    public void testS1() {
        Assert.assertEquals("+00", Hex.s1(0));
        Assert.assertEquals("-00", Hex.s1(-2147483648));
        Assert.assertEquals("+7b", Hex.s1(123));
        Assert.assertEquals("+d2", Hex.s1(1234567890));
    }

    @Test
    public void testDump() {
        byte[] bytes1 = new byte[]{17, 16, 17, 17, 17};
        byte[] bytes2 = new byte[]{1, 1, 1, 1, 1, 1, 1, 0, 1, 1};

        Assert.assertEquals("", Hex.dump(bytes1, 5, 0, 0, 1, 3));
        Assert.assertEquals("ba: 0101 00\nbd: 0101\n",
                Hex.dump(bytes2, 5, 5, 186, 3, 2));
        Assert.assertEquals("00: 10\n",
                Hex.dump(new byte[]{16}, 0, 1, 0, 1, 2));
        Assert.assertEquals("00000000: 10\n",
                Hex.dump(new byte[]{16}, 0, 1, 0, 0, 3));
        Assert.assertEquals("0000: 10\n",
                Hex.dump(new byte[]{16}, 0, 1, 0, 0, 4));
        Assert.assertEquals("000000: 10\n",
                Hex.dump(new byte[]{16}, 0, 1, 0, 1, 6));
    }

    @Test
    public void testDumpthrowIllegalArgumentException() {
        byte[] bytes = new byte[]{17, 16, 17, 17, 17, 16, 17, 17, 17, 17};
        thrown.expect(IllegalArgumentException.class);
        Hex.dump(bytes, 4, 1, -2147483648, 1, 5);
        // Method is not expected to return due to exception thrown
    }

    @Test
    public void testDumpthrowIndexOutOfBoundsException() {
        thrown.expect(IndexOutOfBoundsException.class);
        Hex.dump(new byte[0], 4, 1, -2147483648, 1, 4);
        // Method is not expected to return due to exception thrown
    }
}
