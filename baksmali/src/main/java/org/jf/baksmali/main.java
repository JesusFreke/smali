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
import org.jf.baksmali.Renderers.*;
import org.antlr.stringtemplate.StringTemplateGroup;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
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
        boolean readOnly = false;

        String outputDirectory = "out";
        String dumpFileName = null;
        String outputDexFileName = null;
        String inputDexFileName = null;

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

        if (commandLine.hasOption("r")) {
            readOnly = true;
        }

        if (commandLine.hasOption("d")) {
            doDump = true;
            dumpFileName = commandLine.getOptionValue("d", inputDexFileName + ".dump");
        }

        if (commandLine.hasOption("D")) {
            doDump = true;
            disassemble = false;
            dumpFileName = commandLine.getOptionValue("D", inputDexFileName + ".dump");
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

        try {
            File dexFileFile = new File(inputDexFileName);
            if (!dexFileFile.exists()) {
                System.err.println("Can't find the file " + inputDexFileName);
                System.exit(1);
            }

            //Read in and parse the dex file
            DexFile dexFile = new DexFile(dexFileFile, !fixRegisters);

            if (readOnly) {
                return;
            }

            if (disassemble) {
                baksmali.disassembleDexFile(dexFile, outputDirectory);
            }

            if (doDump || write) {
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

        Option readonlyOption = OptionBuilder.withLongOpt("read-only")
                .withDescription("reads in the dex file and then exits")
                .create("r");

        Option dumpOption = OptionBuilder.withLongOpt("dump-to")
                .withDescription("dumps the given dex file into a single annotated dump file named FILE (<dexfile>.dump by default), along with the normal disassembly.")
                .hasOptionalArg()
                .withArgName("FILE")
                .create("d");

        Option dumpOnlyOption = OptionBuilder.withLongOpt("dump-only")
                .withDescription("dumps the given dex file into a single annotated dump file named FILE (<dexfile>.dump by default), and does not generate the disassembly")
                .hasOptionalArg()
                .withArgName("FILE")
                .create("D");

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
                .withDescription("when dumping or rewriting, fix any registers in the debug info that are encoded as a signed value")
                .create("f");

        OptionGroup dumpCommand = new OptionGroup();
        dumpCommand.addOption(dumpOption);
        dumpCommand.addOption(dumpOnlyOption);
        dumpCommand.addOption(readonlyOption);

        options.addOption(versionOption);
        options.addOption(helpOption);
        options.addOptionGroup(dumpCommand);
        options.addOption(writeDexOption);
        options.addOption(outputDirOption);
        options.addOption(sortOption);
        options.addOption(fixSignedRegisterOption);
    }
}