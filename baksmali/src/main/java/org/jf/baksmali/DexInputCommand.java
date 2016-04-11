/*
 * Copyright 2016, Google Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 * Neither the name of Google Inc. nor the names of its
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

import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.OatFile;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;

/**
 * This class implements common functionality for commands that need to load a dex file based on
 * command line input
 */
public abstract class DexInputCommand implements Command {

    /**
     * Parses a dex file input from the user and loads the given dex file.
     *
     * @param input The name of a dex, apk, odex or oat file. For apk or oat files with multiple dex files, the input
     * can additionally consist of a colon followed by a specific dex entry to load.
     * @param apiLevel The api level to load the dex file with
     * @param experimentalOpcodes whether experimental opcodes should be allowed
     * @return The loaded DexBackedDexFile
     */
    @Nonnull
    protected DexBackedDexFile loadDexFile(@Nonnull String input, int apiLevel, boolean experimentalOpcodes) {
        File dexFileFile = new File(input);
        String dexFileEntry = null;

        int previousIndex = input.length();
        while (!dexFileFile.exists()) {
            int colonIndex = input.lastIndexOf(':', previousIndex - 1);

            if (colonIndex >= 0) {
                dexFileFile = new File(input.substring(0, colonIndex));
                dexFileEntry = input.substring(colonIndex + 1);
                previousIndex = colonIndex;
            } else {
                break;
            }
        }

        if (!dexFileFile.exists()) {
            System.err.println("Can't find the file " + input);
            System.exit(1);
        }

        if (!dexFileFile.exists()) {
            int colonIndex = input.lastIndexOf(':');

            if (colonIndex >= 0) {
                dexFileFile = new File(input.substring(0, colonIndex));
                dexFileEntry = input.substring(colonIndex + 1);
            }

            if (!dexFileFile.exists()) {
                System.err.println("Can't find the file " + input);
                System.exit(1);
            }
        }

        try {
            return DexFileFactory.loadDexFile(dexFileFile, dexFileEntry, apiLevel, experimentalOpcodes);
        } catch (DexFileFactory.MultipleDexFilesException ex) {
            System.err.println(String.format("%s is an oat file that contains multiple dex files. You must specify " +
                    "which one to load. E.g. To load the \"core.dex\" entry from boot.oat, you should use " +
                    "\"boot.oat:core.dex\"", dexFileFile));
            System.err.println("Valid entries include:");

            for (OatFile.OatDexFile oatDexFile : ex.oatFile.getDexFiles()) {
                System.err.println(oatDexFile.filename);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        // execution can never actually reach here
        throw new IllegalStateException();
    }
}
