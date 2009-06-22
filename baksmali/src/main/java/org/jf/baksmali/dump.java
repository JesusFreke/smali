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

package org.jf.baksmali;

import org.jf.dexlib.DexFile;
import org.jf.dexlib.Util.ByteArrayAnnotatedOutput;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileOutputStream;

public class dump {

    public static void main(String[] args) throws IOException {
        Options options = new Options();

        Option helpOption = OptionBuilder.withLongOpt("help")
                                         .withDescription("prints the usage information for the --dump command")
                                         .create("?");

        Option outputFileOption = OptionBuilder.withLongOpt("output")
                                              .withDescription("the file where the dump will be written. The default is <dexfile>.dump, where <dexfile> is the name of the given dex file")
                                              .hasArg()
                                              .withArgName("FILE")
                                              .create("out");

        Option writeDexFileOption = OptionBuilder.withLongOpt("write-dex-file")
                                            .withDescription("optionally re-write out the dex file to the given file")
                                            .hasArg()
                                            .withArgName("FILE")
                                            .create("writedex");

        Option sortDexFileOption = OptionBuilder.withLongOpt("sort-dex-file")
                                                .withDescription("optionally sorts the items in the dex file before dumping/writing it")
                                                .create("sortdex");

        Option unsignedRegisterOption = OptionBuilder.withLongOpt("unsigned-registers")
                                                     .withDescription("always write the registers in the debug info as unsigned. By default we keep the same signed/unsigned format as what was in the original file")
                                                     .create("unsigned");

        options.addOption(helpOption);
        options.addOption(outputFileOption);
        options.addOption(writeDexFileOption);
        options.addOption(sortDexFileOption);
        options.addOption(unsignedRegisterOption);

        CommandLineParser parser = new PosixParser();
        CommandLine commandLine;

        try {
            commandLine = parser.parse(options, args);
        } catch (ParseException ex) {
            printHelp(options);
            return;
        }

        if (commandLine.hasOption("?")) {
            printHelp(options);
            return;
        }

        String[] leftover = commandLine.getArgs();

        if (leftover.length != 1) {
            printHelp(options);
            return;
        }

        String dexFileName = leftover[0];
        String outputDumpName = commandLine.getOptionValue("out", dexFileName + ".dump");

        boolean writeDex = false;
        String outputDexName = null;
        if (commandLine.hasOption("writedex")) {
            writeDex = true;
            outputDexName = commandLine.getOptionValue("writedex");
        }

        boolean sortDex = false;
        if (commandLine.hasOption("sortdex")) {
            sortDex = true;
        }

        boolean unsignedRegisters = false;
        if (commandLine.hasOption("unsigned")) {
            unsignedRegisters = true;
        }

        File dexFileFile = new File(dexFileName);
        if (!dexFileFile.exists()) {
            System.out.println("Can't find the file " + dexFileFile.toString());
            System.exit(1);
        }

        DexFile dexFile = new DexFile(new File(dexFileName), !unsignedRegisters);

        ByteArrayAnnotatedOutput out = new ByteArrayAnnotatedOutput();
        out.enableAnnotations(120, true);

        if (sortDex) {
            dexFile.place(true);
        }
        dexFile.writeTo(out);


        out.finishAnnotating();

        FileWriter writer = null;
        try {
            writer = new FileWriter(outputDumpName);
            out.writeAnnotationsTo(writer);
            writer.close();

            if (writeDex) {
                byte[] bytes = out.toByteArray();

                DexFile.calcSignature(bytes);
                DexFile.calcChecksum(bytes);

                FileOutputStream fileOutputStream = new FileOutputStream(outputDexName);
                fileOutputStream.write(bytes);
                fileOutputStream.close();
            }
        }catch (IOException ex) {
            if (writer != null) {
                writer.close();
            }
            throw ex;
        }
    }

    /**
     * Prints the usage message.
     */
    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar baksmali.jar -dump [options] <dexfile>",
                "Dumps the given dex file", options, "");
    }
}
