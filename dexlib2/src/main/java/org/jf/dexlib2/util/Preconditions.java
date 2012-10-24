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

package org.jf.dexlib2.util;

import org.jf.dexlib2.Format;
import org.jf.dexlib2.Opcode;

public class Preconditions {
    public static void checkFormat(Opcode opcode, Format expectedFormat) {
        if (opcode.format != expectedFormat) {
            throw new IllegalArgumentException(
                    String.format("Invalid opcode %s for %s", opcode.name, expectedFormat.name()));
        }
    }

    public static int checkNibbleRegister(int register) {
        if ((register & 0xFFFFFFF0) != 0) {
            throw new IllegalArgumentException(
                    String.format("Invalid register: v%d. Must be between v0 and v15, inclusive.", register));
        }
        return register;
    }

    public static int checkByteRegister(int register) {
        if ((register & 0xFFFFFF00) != 0) {
            throw new IllegalArgumentException(
                    String.format("Invalid register: v%d. Must be between v0 and v255, inclusive.", register));
        }
        return register;
    }

    public static int checkShortRegister(int register) {
        if ((register & 0xFFFF0000) != 0) {
            throw new IllegalArgumentException(
                    String.format("Invalid register: v%d. Must be between v0 and v65535, inclusive.", register));
        }
        return register;
    }

    public static int checkNibbleLiteral(int literal) {
        if ((literal & 0xFFFFFFF0) != 0) {
            throw new IllegalArgumentException(
                    String.format("Invalid literal value: %d. Must be between -8 and 7, inclusive.", literal));
        }
        return literal;
    }

    public static int checkByteLiteral(int literal) {
        if ((literal & 0xFFFFFF00) != 0) {
            throw new IllegalArgumentException(
                    String.format("Invalid literal value: %d. Must be between -128 and 127, inclusive.", literal));
        }
        return literal;
    }

    public static int checkShortLiteral(int literal) {
        if ((literal & 0xFFFF0000) != 0) {
            throw new IllegalArgumentException(
                    String.format("Invalid literal value: %d. Must be between -32768 and 32767, inclusive.", literal));
        }
        return literal;
    }

    public static int checkIntegerHatLiteral(int literal) {
        if ((literal & 0xFFFF) != 0) {
            throw new IllegalArgumentException(
                    String.format("Invalid literal value: %d. Low 16 bits must be zeroed out.", literal));
        }
        return literal;
    }

    public static long checkLongHatLiteral(long literal) {
        if ((literal & 0xFFFFFFFFFFFFL) != 0) {
            throw new IllegalArgumentException(
                    String.format("Invalid literal value: %d. Low 16 bits must be zeroed out.", literal));
        }
        return literal;
    }

    public static int checkByteCodeOffset(int register) {
        if ((register & 0xFFFFFF00) != 0) {
            throw new IllegalArgumentException(
                    String.format("Invalid code offset: %d. Must be between -8 and 7, inclusive.", register));
        }
        return register;
    }

    public static int checkShortCodeOffset(int register) {
        if ((register & 0xFFFF0000) != 0) {
            throw new IllegalArgumentException(
                    String.format("Invalid code offset: %d. Must be between -32768 and 32767, inclusive.", register));
        }
        return register;
    }

    public static String checkReference(String reference, int referenceType) {
        //TODO: implement this
        return reference;
    }

    public static int check35cRegisterCount(int registerCount) {
        if (registerCount < 0 || registerCount > 5) {
            throw new IllegalArgumentException(
                    String.format("Invalid register count: %d. Must be between 0 and 5, inclusive.", registerCount));
        }
        return registerCount;
    }

    public static int check3rcRegisterCount(int registerCount) {
        if ((registerCount & 0xFFFFFF00) == 0) {
            throw new IllegalArgumentException(
                    String.format("Invalid register count: %d. Must be between 0 and 255, inclusive.", registerCount));
        }
        return registerCount;
    }

    public static void checkValueArg(int valueArg, int maxValue) {
        if (valueArg > maxValue) {
            throw new IllegalArgumentException(
                    String.format("Invalid value_arg value %d for an encoded_value. Expecting 0..%d, inclusive",
                            valueArg, maxValue));
        }
    }
}
