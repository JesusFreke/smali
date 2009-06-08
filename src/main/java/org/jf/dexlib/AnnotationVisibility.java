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

public enum AnnotationVisibility {
    BUILD((byte)0),
    RUNTIME((byte)1),
    SYSTEM((byte)2);

    public final byte value;
    private AnnotationVisibility(byte value) {
        this.value = value;
    }



    public static AnnotationVisibility fromValue(byte value) {
        if (value == 0) {
            return BUILD;
        } else if (value == 1) {
            return RUNTIME;
        } else if (value == 2) {
            return SYSTEM;
        }
        throw new RuntimeException(Integer.toString(value) + " is not a valid AnnotationVisibility value");
    }

    public static AnnotationVisibility fromName(String name) {
        if (name.compareTo("build") == 0) {
            return BUILD;
        }
        if (name.compareTo("runtime") == 0) {
            return RUNTIME;
        }
        if (name.compareTo("system") == 0) {
            return SYSTEM;
        }
        throw new RuntimeException(name + " is not a valid AnnotationVisibility name");
    }
}
