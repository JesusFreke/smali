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

import javax.annotation.Nonnull;

public class DexFileReader {
    public DexFileReader(DexFile dexFile, int offset) {
    }

    @Nonnull
    public DexFile getDexFile() {
        return null;
    }

    public int getOffset() {
        return 0;
    }

    public String getString(int stringIndex) {
        return null;
    }

    public int getFieldIdItemOffset(int fieldIndex) {
        return 0;
    }

    public int getMethodIdItemOffset(int methodIndex) {
        return 0;
    }

    public int getProtoIdItemOffset(int methodIndex) {
        return 0;
    }

    public String getType(int typeIndex) {
        return null;
    }

    public String getField(int fieldIndex) {
        return null;
    }

    public String getMethod(int fieldIndex) {
        return null;
    }

    public String getReference(int referenceType, int referenceIndex) {
        return null;
    }

    public int readSleb128() {
        return 0;
    }

    public int readSmallUleb128() {
        return 0;
    }

    public void skipUleb128() {
    }

    public int readSmallUint() {
        return 0;
    }

    public int readUshort() {
        return 0;
    }

    public int readUbyte() {
        // returns an int between 0 and 255
        return 0;
    }

    public int readByte() {
        // returns an int between -128 and 127
        return 0;
    }

    public int readUshort(int offset) {
        return 0;
    }

    public int readShort() {
        return 0;
    }

    public int readSizedInt(int bytes) {
        // bytes must be from 1 to 4. reads and interprets that many bytes as a little-endian sign-extended integer
        return 0;
    }

    public int readSizedSmallUint(int bytes) {
        // bytes must be from 1 to 4. reads and interprets that many bytes as a little-endian zero-extended integer
        return 0;
    }

    public int readSizedRightExtendedUint(int bytes) {
        // bytes must be from 1 to 4. reads and interprets that many bytes as a little-endian zero-right-extended
        // integer
        return 0;
    }

    public int readSizedRightExtendedUlong(int bytes) {
        // bytes must be from 1 to 8. reads and interprets that many bytes as a little-endian zero-right-extended
        // long
        return 0;
    }

    public long readSizedLong(int bytes) {
        // bytes must be from 1 to 8. reads and interprets that many bytes as a little-endian sign-extended long
        return 0;
    }

    public long readSizedUlong(int bytes) {
        // bytes must be from 1 to 8. reads and interprets that many bytes as a little-endian zero-extended long
        return 0;
    }

    public int readSmallUint(int offset) {
        return 0;
    }

    public int readInt() {
        return 0;
    }

    public long readLong() {
        return 0;
    }



    @Nonnull
    public DexFileReader atAbsolute(int offset) {
        return null;
    }

    // returns copy of this DexFileReader
    @Nonnull
    public DexFileReader copy() {
        return null;
    }

    public void skipByte() {
    }

    public void skipBytes(int i) {
    }
}