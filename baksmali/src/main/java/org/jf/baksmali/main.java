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

/**
 * Main class for baksmali. It recognizes enough options to be able to dispatch
 * to the right "actual" main.
 */
public class main {

    public static final String VERSION = "0.91";


    /**
     * This class is uninstantiable.
     */
    private main() {
        // This space intentionally left blank.
    }

    /**
     * Run!
     */
    public static void main(String[] args) {
        Options options = new Options();


        Option versionOption = OptionBuilder.withLongOpt("version")
                                            .withDescription("prints the version")
                                            .create("v");

        Option helpOption = OptionBuilder.withLongOpt("help")
                                         .withDescription("prints the help message")
                                         .create("?");

        Option disassembleOption = OptionBuilder.withLongOpt("disassemble")
                                                .withDescription("disassembles a dex file into individual files for each class that are placed into a folder structure that matches the package structure of the classes.")
                                                .create("dis");

        Option dumpOption = OptionBuilder.withLongOpt("dump")
                                         .withDescription("Dumps a dex file into a single annotated dump file named FILE")
                                         .create("dump");

        OptionGroup mainCommand = new OptionGroup();
        mainCommand.addOption(versionOption);
        mainCommand.addOption(helpOption);
        mainCommand.addOption(disassembleOption);
        mainCommand.addOption(dumpOption);
        mainCommand.setRequired(true);

        options.addOptionGroup(mainCommand);

        CommandLineParser parser = new PosixParser();

        try {
            parser.parse(options, new String[]{args[0]});
        } catch (ParseException ex) {
            printHelp(options);
            return;
        }

        try
        {

            String command = mainCommand.getSelected();
            if (command.equals("?")) {
                printHelp(options);
                return;
            }

            if (command.equals("v")) {
                version();
                return;
            }

            if (command.equals("dis")) {
                baksmali.main(without(args, 0));
                return;
            }

            if (command.equals("dump")) {
                dump.main(without(args, 0));
            }
        } catch (RuntimeException ex) {
            System.err.println("\nUNEXPECTED TOP-LEVEL EXCEPTION:");
            ex.printStackTrace();
            System.exit(1);
        } catch (Throwable ex) {
            System.err.println("\nUNEXPECTED TOP-LEVEL ERROR:");
            ex.printStackTrace();
            System.exit(2);
        }
    }

    /**
     * Prints the usage message.
     */
    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar baksmali.jar <command> [command-args]",
                "use <command> --help to see the options each command accepts", options, "");
    }

    /**
     * Prints the version message.
     */
    private static void version() {
        System.err.println("baksmali v" + VERSION);
        System.exit(0);
    }

    /**
     * Returns a copy of the given args array, but without the indicated
     * element.
     *
     * @param orig non-null; original array
     * @param n which element to omit
     * @return non-null; new array
     */
    private static String[] without(String[] orig, int n) {
        int len = orig.length - 1;
        String[] newa = new String[len];
        System.arraycopy(orig, 0, newa, 0, n);
        System.arraycopy(orig, n + 1, newa, n, len - n);
        return newa;
    }
}