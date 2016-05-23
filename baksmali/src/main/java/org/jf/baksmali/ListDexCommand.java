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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.collect.Lists;
import org.jf.dexlib2.dexbacked.OatFile;
import org.jf.dexlib2.dexbacked.raw.HeaderItem;
import org.jf.dexlib2.dexbacked.raw.OdexHeaderItem;
import org.jf.util.jcommander.Command;
import org.jf.util.jcommander.ExtendedParameter;
import org.jf.util.jcommander.ExtendedParameters;

import javax.annotation.Nonnull;
import java.io.*;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

@Parameters(commandDescription = "Lists the dex files in an apk/oat file.")
@ExtendedParameters(
        commandName = "dex",
        commandAliases = "d")
public class ListDexCommand extends Command {

    @Parameter(names = {"-h", "-?", "--help"}, help = true,
            description = "Show usage information")
    private boolean help;

    @Parameter(description = "An apk or oat file.")
    @ExtendedParameter(argumentNames = "file")
    private List<String> inputList = Lists.newArrayList();

    public ListDexCommand(@Nonnull List<JCommander> commandAncestors) {
        super(commandAncestors);
    }

    @Override public void run() {
        if (help || inputList == null || inputList.isEmpty()) {
            usage();
            return;
        }

        if (inputList.size() > 1) {
            System.err.println("Too many files specified");
            usage();
            return;
        }

        String input = inputList.get(0);
        File file = new File(input);

        if (!file.exists()) {
            System.err.println(String.format("Could not find the file: %s", input));
        }

        try {
            ZipFile zipFile = new ZipFile(input);

            byte[] magic = new byte[8];

            for (ZipEntry zipEntry : Collections.list(zipFile.entries())) {
                try {
                    InputStream inputStream = zipFile.getInputStream(zipEntry);

                    int totalBytesRead = 0;
                    while (totalBytesRead < 8) {
                        int bytesRead = inputStream.read(magic, totalBytesRead, 8 - totalBytesRead);
                        if (bytesRead == -1) {
                            break;
                        }
                        totalBytesRead += bytesRead;
                    }

                    if (totalBytesRead == 8) {
                        if (HeaderItem.verifyMagic(magic, 0) || OdexHeaderItem.verifyMagic(magic)) {
                            System.out.println(zipEntry.getName());
                        }
                    }
                } catch (ZipException ex) {
                    // ignore and keep looking
                    continue;
                }
            }
        } catch (ZipException ex) {
            // ignore
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        try {
            InputStream inputStream = new BufferedInputStream(new FileInputStream(input));
            OatFile oatFile = OatFile.fromInputStream(inputStream);

            for (OatFile.OatDexFile oatDexFile: oatFile.getDexFiles()) {
                System.out.println(oatDexFile.filename);
            }
        } catch (OatFile.NotAnOatFileException ex) {
            // just eat it
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
