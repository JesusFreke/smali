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

package org.jf.dexlib;

import org.jf.dexlib.Util.Input;

import java.io.UnsupportedEncodingException;

public class OdexHeaderItem {

    /**
     * the file format magic number, represented as the
     * low-order bytes of a string
     */
    public static final String MAGIC = "dey\n035" + '\0';

    public final byte[] magic;
    public final int dexOffset;
    public final int dexLength;
    public final int depsOffset;
    public final int depsLength;
    public final int auxOffset;
    public final int auxLength;
    public final int flags;

    public OdexHeaderItem(Input in) {
        magic = in.readBytes(8);

        byte[] expectedMagic;
        try {
            expectedMagic = MAGIC.getBytes("US-ASCII");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }

        for (int i=0; i<8; i++) {
            if (expectedMagic[i] != magic[i]) {
                throw new RuntimeException("The magic value is not the expected value");
            }
        }

        dexOffset = in.readInt();
        dexLength = in.readInt();
        depsOffset = in.readInt();
        depsLength = in.readInt();
        auxOffset = in.readInt();
        auxLength = in.readInt();
        flags = in.readInt();
        in.readInt(); //padding
    }

}
