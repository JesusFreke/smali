/*
 * Copyright 2015, Google Inc.
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

package org.jf.baksmali;

import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.junit.Assert;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;

/**
 * A base test class for performing a test using a dex file as input
 */
/**
 * A base test class for performing a disassembly on a dex file and verifying the results
 *
 * The test accepts a single-class dex file as input. By default, the input dex file should be a resource at
 * [testDir]/[testName]Input.dex
 */
public abstract class DexTest {
    protected final String testDir;

    protected DexTest(@Nonnull String testDir) {
        this.testDir = testDir;
    }

    protected DexTest() {
        this.testDir = this.getClass().getSimpleName();
    }

    @Nonnull
    protected String getInputFilename(@Nonnull String testName) {
        return String.format("%s%s%sInput.dex", testDir, File.separatorChar, testName);
    }

    @Nonnull
    protected DexBackedDexFile getInputDexFile(@Nonnull String testName, @Nonnull BaksmaliOptions options) {
        try {
            // Load file from resources as a stream
            byte[] inputBytes = BaksmaliTestUtils.readResourceBytesFully(getInputFilename(testName));
            return new DexBackedDexFile(Opcodes.forApi(options.apiLevel), inputBytes);
        } catch (IOException ex) {
            Assert.fail();
        }
        return null;
    }
}
