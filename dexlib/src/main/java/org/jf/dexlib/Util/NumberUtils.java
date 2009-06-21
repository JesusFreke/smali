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

public class NumberUtils {

    public static byte decodeHighSignedNibble(byte b) {
        return (byte)(b >> 4);
    }

    public static byte decodeHighUnsignedNibble(byte b) {
        return (byte)((b & 0xFF) >>> 4);
    }

    public static byte decodeLowUnsignedNibble(byte b) {
        return (byte)(b & 0x0F);
    }

    public static short decodeUnsignedByte(byte b) {
        return (short)(b & 0xFF);
    }

    public static short decodeShort(byte lsb, byte msb) {
        return (short)
               (    (lsb & 0xFF) |
                    (msb << 8)
               );
    }

    public static int decodeUnsignedShort(byte lsb, byte msb) {
        return  ((msb & 0xFF) << 8) |
                 (lsb & 0xFF);
    }

    public static int decodeInt(byte lsb, byte mlsb, byte mmsb, byte msb) {
        return (lsb & 0xFF) |
               ((mlsb & 0xFF) << 8) |
               ((mmsb & 0xFF) << 16) |
               (msb << 24);
    }

    public static long decodeLong(byte[] array, int startIndex) {
        return  array[startIndex++] |
                (array[startIndex++] >> 8) |
                (array[startIndex++] >> 16) |
                (array[startIndex++] >> 24) |
                ((long)array[startIndex++] >> 32) |
                ((long)array[startIndex++] >> 40) |
                ((long)array[startIndex++] >> 48) |
                ((long)array[startIndex++] >> 56);
    }
}
