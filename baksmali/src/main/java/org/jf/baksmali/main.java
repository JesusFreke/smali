/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jf.baksmali;

import org.apache.commons.cli.*;
import org.jf.dexlib.DexFile;
import org.jf.dexlib.Util.Deodexerant;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;

public class main {

    public static final String VERSION;

    private static final Options options;

    static {
        options = new Options();
        buildOptions();

        InputStream templateStream = baksmali.class.getClassLoader().getResourceAsStream("baksmali.properties");
        Properties properties = new Properties();
        String version = "(unknown)";
        try {
            properties.load(templateStream);
            version = properties.getProperty("application.version");
        } catch (IOException ex) {
        }
        VERSION = version;
    }

    /**
     * This class is uninstantiable.
     */
    private main() {
    }

    /**
     * Run!
     */
    public static void main(String[] args) {
        CommandLineParser parser = new PosixParser();
        CommandLine commandLine;

        try {
            commandLine = parser.parse(options, args);
        } catch (ParseException ex) {
            usage();
            return;
        }

        boolean disassemble = true;
        boolean doDump = false;
        boolean write = false;
        boolean sort = false;
        boolean fixRegisters = false;
        boolean noParameterRegisters = false;
        boolean useLocalsDirective = false;
        boolean useSequentialLabels = false;
        boolean outputDebugInfo = true;


        String outputDirectory = "out";
        String dumpFileName = null;
        String outputDexFileName = null;
        String inputDexFileName = null;
        String deodexerantHost = null;
        int deodexerantPort = 0;

        String[] remainingArgs = commandLine.getArgs();

        if (commandLine.hasOption("v")) {
            version();
            return;
        }

        if (commandLine.hasOption("?")) {
            usage();
            return;
        }

        if (remainingArgs.length != 1) {
            usage();
            return;
        }

        inputDexFileName = remainingArgs[0];

        if (commandLine.hasOption("n")) {
            disassemble = false;
        }

        if (commandLine.hasOption("d")) {
            doDump = true;
            dumpFileName = commandLine.getOptionValue("d", inputDexFileName + ".dump");
        }

        if (commandLine.hasOption("w")) {
            write = true;
            outputDexFileName = commandLine.getOptionValue("w");
        }

        if (commandLine.hasOption("o")) {
            outputDirectory = commandLine.getOptionValue("o");
        }

        if (commandLine.hasOption("s")) {
            sort = true;
        }

        if (commandLine.hasOption("f")) {
            fixRegisters = true;
        }

        if (commandLine.hasOption("p")) {
            noParameterRegisters = true;
        }

        if (commandLine.hasOption("l")) {
            useLocalsDirective = true;
        }

        if (commandLine.hasOption("i")) {
            useSequentialLabels = true;
        }

        if (commandLine.hasOption("b")) {
            outputDebugInfo = false;
        }

        if (commandLine.hasOption("x")) {
            String deodexerantAddress = commandLine.getOptionValue("x");
            String[] parts = deodexerantAddress.split(":");
            if (parts.length != 2) {
                System.err.println("Invalid deodexerant address. Expecting :<port> or <host>:<port>");
                System.exit(1);
            }

            deodexerantHost = parts[0];
            if (deodexerantHost.length() == 0) {
                deodexerantHost = "localhost";
            }
            try {
                deodexerantPort = Integer.parseInt(parts[1]);
            } catch (NumberFormatException ex) {
                System.err.println("Invalid port \"" + deodexerantPort + "\" for deodexerant address");
                System.exit(1);
            }
        }

        try {
            File dexFileFile = new File(inputDexFileName);
            if (!dexFileFile.exists()) {
                System.err.println("Can't find the file " + inputDexFileName);
                System.exit(1);
            }

            //Read in and parse the dex file
            DexFile dexFile = new DexFile(dexFileFile, !fixRegisters);

            Deodexerant deodexerant = null;


            if (deodexerantHost != null) {
                if (!dexFile.isOdex()) {
                    System.err.println("-x cannot be used with a normal dex file. Ignoring -x");
                }
                deodexerant = new Deodexerant(dexFile, deodexerantHost, deodexerantPort);
            }

            if (dexFile.isOdex()) {
                if (doDump) {
                    System.err.println("-d cannot be used with on odex file. Ignoring -d");
                }
                if (write) {
                    System.err.println("-w cannot be used with an odex file. Ignoring -w");
                }
            }

            if (disassemble) {
                baksmali.disassembleDexFile(dexFile, deodexerant, outputDirectory, noParameterRegisters,
                        useLocalsDirective, useSequentialLabels, outputDebugInfo);
            }

            if ((doDump || write) && !dexFile.isOdex()) {
                try
                {
                    dump.dump(dexFile, dumpFileName, outputDexFileName, sort);
                }catch (IOException ex) {
                    System.err.println("Error occured while writing dump file");
                    ex.printStackTrace();
                }
            }
        } catch (RuntimeException ex) {
            System.err.println("\n\nUNEXPECTED TOP-LEVEL EXCEPTION:");
            ex.printStackTrace();
            System.exit(1);
        } catch (Throwable ex) {
            System.err.println("\n\nUNEXPECTED TOP-LEVEL ERROR:");
            ex.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Prints the usage message.
     */
    private static void usage() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar baksmali.jar [options] <dex-file>",
                "disassembles and/or dumps a dex file", options, "");
    }

    /**
     * Prints the version message.
     */
    private static void version() {
        System.out.println("baksmali " + VERSION + " (http://smali.googlecode.com)");
        System.out.println("Copyright (C) 2009 Ben Gruver");
        System.out.println("BSD license (http://www.opensource.org/licenses/bsd-license.php)");
        System.exit(0);
    }

    private static void buildOptions() {
        Option versionOption = OptionBuilder.withLongOpt("version")
                .withDescription("prints the version then exits")
                .create("v");

        Option helpOption = OptionBuilder.withLongOpt("help")
                .withDescription("prints the help message then exits")
                .create("?");

        Option noDisassemblyOption = OptionBuilder.withLongOpt("no-disassembly")
                .withDescription("suppresses the output of the disassembly")
                .create("n");

        Option dumpOption = OptionBuilder.withLongOpt("dump-to")
                .withDescription("dumps the given dex file into a single annotated dump file named FILE" +
                        " (<dexfile>.dump by default), along with the normal disassembly.")
                .hasOptionalArg()
                .withArgName("FILE")
                .create("d");

        Option writeDexOption = OptionBuilder.withLongOpt("write-dex")
                .withDescription("additionally rewrites the input dex file to FILE")
                .hasArg()
                .withArgName("FILE")
                .create("w");

        Option outputDirOption = OptionBuilder.withLongOpt("output")
                .withDescription("the directory where the disassembled files will be placed. The default is out")
                .hasArg()
                .withArgName("DIR")
                .create("o");

        Option sortOption = OptionBuilder.withLongOpt("sort")
                .withDescription("sort the items in the dex file into a canonical order before dumping/writing")
                .create("s");

        Option fixSignedRegisterOption = OptionBuilder.withLongOpt("fix-signed-registers")
                .withDescription("when dumping or rewriting, fix any registers in the debug info that are encoded as" +
                        " a signed value")
                .create("f");

        Option noParameterRegistersOption = OptionBuilder.withLongOpt("no-parameter-registers")
                .withDescription("use the v<n> syntax instead of the p<n> syntax for registers mapped to method" +
                        " parameters")
                .create("p");

        Option deodexerantOption = OptionBuilder.withLongOpt("deodexerant")
                .withDescription("connect to deodexerant on the specified HOST:PORT, and deodex the input odex"
                        + " file. This option is ignored if the input file is a dex file instead of an odex file")
                .hasArg()
                .withArgName("HOST:PORT")
                .create("x");

        Option useLocalsOption = OptionBuilder.withLongOpt("use-locals")
                .withDescription("output the .locals directive with the number of non-parameter registers, rather" +
                        " than the .register directive with the total number of register")
                .create("l");

        Option sequentialLabelsOption = OptionBuilder.withLongOpt("sequential-labels")
                .withDescription("create label names using a sequential numbering scheme per label type, rather than " +
                        "using the bytecode offset")
                .create("q");

        Option noDebugInfoOption = OptionBuilder.withLongOpt("no-debug-info")
                .withDescription("don't write out debug info (.local, .param, .line, etc.)")
                .create("b");

        options.addOption(versionOption);
        options.addOption(helpOption);
        options.addOption(dumpOption);
        options.addOption(noDisassemblyOption);
        options.addOption(writeDexOption);
        options.addOption(outputDirOption);
        options.addOption(sortOption);
        options.addOption(fixSignedRegisterOption);
        options.addOption(noParameterRegistersOption);
        options.addOption(deodexerantOption);
        options.addOption(useLocalsOption);
        options.addOption(sequentialLabelsOption);
        options.addOption(noDebugInfoOption);
    }
}