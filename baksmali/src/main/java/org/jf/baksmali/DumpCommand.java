/*
 * Copyright 2016, Google Inc.
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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.OatFile;
import org.jf.dexlib2.dexbacked.raw.RawDexFile;
import org.jf.dexlib2.dexbacked.raw.util.DexAnnotator;
import org.jf.util.ConsoleUtil;

import javax.annotation.Nonnull;
import java.io.*;

@Parameters(commandDescription = "Prints an annotated hex dump for the given dex file")
public class DumpCommand implements Command {

    @Nonnull
    private final JCommander jc;

    @Parameter(names = {"-h", "-?", "--help"}, help = true,
            description = "Show usage information for this command.")
    private boolean help;

    @Parameter(names = {"-a", "--api"},
            description = "The numeric api level of the file being disassembled.")
    private int apiLevel = 15;

    @Parameter(names = "--experimental",
            description = "Enable experimental opcodes to be disassembled, even if they aren't necessarily " +
                    "supported in the Android runtime yet.")
    private boolean experimentalOpcodes = false;

    @Parameter(description = "<file> - A dex/apk/oat/odex file. For apk or oat files that contain multiple dex " +
            "files, you can specify which dex file to disassemble by appending the name of the dex file with a " +
            "colon. E.g. \"something.apk:classes2.dex\"")
    private String input;

    public DumpCommand(@Nonnull JCommander jc) {
        this.jc = jc;
    }

    public void run() {
        if (help || input == null || input.isEmpty()) {
            jc.usage("dump");
            return;
        }

        String inputDexPath = input;

        File dexFileFile = new File(inputDexPath);
        String dexFileEntry = null;
        if (!dexFileFile.exists()) {
            int colonIndex = inputDexPath.lastIndexOf(':');

            if (colonIndex >= 0) {
                dexFileFile = new File(inputDexPath.substring(0, colonIndex));
                dexFileEntry = inputDexPath.substring(colonIndex+1);
            }

            if (!dexFileFile.exists()) {
                System.err.println("Can't find the file " + inputDexPath);
                System.exit(1);
            }
        }

        DexBackedDexFile dexFile = null;
        try {
            dexFile = DexFileFactory.loadDexFile(dexFileFile, dexFileEntry, apiLevel, experimentalOpcodes);
        } catch (DexFileFactory.MultipleDexFilesException ex) {
            System.err.println(String.format("%s contains multiple dex files. You must specify which one to " +
                    "disassemble with the -e option", dexFileFile.getName()));
            System.err.println("Valid entries include:");

            for (OatFile.OatDexFile oatDexFile: ex.oatFile.getDexFiles()) {
                System.err.println(oatDexFile.filename);
            }
            System.exit(1);
        } catch (IOException ex) {
            System.err.println("There was an error while reading the dex file");
            ex.printStackTrace(System.err);
            System.exit(-1);
        }

        try {
            dump(dexFile, System.out, apiLevel);
        } catch (IOException ex) {
            System.err.println("There was an error while dumping the dex file");
            ex.printStackTrace(System.err);
        }
    }

    /**
     * Writes an annotated hex dump of the given dex file to output.
     *
     * @param dexFile The dex file to dump
     * @param output An OutputStream to write the annotated hex dump to. The caller is responsible for closing this
     *               when needed.
     * @param apiLevel The api level to use when dumping the dex file
     *
     * @throws IOException
     */
    public static void dump(@Nonnull DexBackedDexFile dexFile, @Nonnull OutputStream output, int apiLevel)
            throws IOException {
        Writer writer = new BufferedWriter(new OutputStreamWriter(output));

        int consoleWidth = ConsoleUtil.getConsoleWidth();
        if (consoleWidth <= 0) {
            consoleWidth = 120;
        }

        RawDexFile rawDexFile = new RawDexFile(Opcodes.forApi(apiLevel), dexFile);
        DexAnnotator annotator = new DexAnnotator(rawDexFile, consoleWidth);
        annotator.writeAnnotations(writer);
    }
}
