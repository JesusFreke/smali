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

package org.jf.dexlib;

import org.jf.dexlib.Util.Input;
import org.jf.dexlib.Util.Leb128Utils;
import org.jf.dexlib.Util.Output;

public class Leb128Field extends CachedIntegerValueField {
    public Leb128Field(String fieldName) {
        super(fieldName);
    }

    public Leb128Field(int value, String fieldName) {
        super(value, fieldName);
    }

    public void readFrom(Input in) {       
        value = in.readUnsignedLeb128();
    }

    public int place(int offset) {
        return offset + Leb128Utils.unsignedLeb128Size(value);
    }

    public void writeValue(Output out) {
        out.writeUnsignedLeb128(value);
    }

    /**
     * dx had a bug where it would write registers in the debug
     * info as signed leb 128 values instead of unsigned. This class
     * is used when it is important to keep the same format as the
     * file being read in - for example when the intent is to immediately
     * write the file back out (typically for dumping/annotation purposes)
     */
    public static class PossiblySignedLeb128Field extends Leb128Field {
        private boolean signed = false;

        public PossiblySignedLeb128Field(String fieldName) {
            super (fieldName);
        }

        public void readFrom(Input in) {
            int start = in.getCursor();
            value = in.readUnsignedLeb128();
            int end = in.getCursor();

            if (Leb128Utils.unsignedLeb128Size(value) != (end - start)) {
                signed = true;
            }
        }

        public int place(int offset) {
            if (signed) {
                return offset + Leb128Utils.signedLeb128Size(value);
            }
            return offset + Leb128Utils.unsignedLeb128Size(value);
        }

        public void writeValue(Output out) {
            if (signed) {
                out.writeSignedLeb128(value);
            } else {
                out.writeUnsignedLeb128(value);
            }
        }
    }
}
