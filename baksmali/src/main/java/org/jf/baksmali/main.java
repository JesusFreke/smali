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
import org.jf.baksmali.Deodex.Deodexerant;
import org.jf.dexlib.DexFile;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;

public class main {

    public static final String VERSION;

    private static final Options basicOptions;
    private static final Options debugOptions;
    private static final Options options;

    public static final int ALL = 1;
    public static final int ALLPRE = 2;
    public static final int ALLPOST = 4;
    public static final int ARGS = 8;
    public static final int DEST = 16;
    public static final int MERGE = 32;
    public static final int FULLMERGE = 64;

    static {
        options = new Options();
        basicOptions = new Options();
        debugOptions = new Options();
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

        int registerInfo = 0;

        String outputDirectory = "out";
        String dumpFileName = null;
        String outputDexFileName = null;
        String inputDexFileName = null;
        String deodexerantHost = null;
        String bootClassPath = "core.jar:ext.jar:framework.jar:android.policy.jar:services.jar";
        String bootClassPathDir = ".";
        int deodexerantPort = 0;

        String[] remainingArgs = commandLine.getArgs();

        Option[] options = commandLine.getOptions();

        for (int i=0; i<options.length; i++) {
            Option option = options[i];
            String opt = option.getOpt();

            switch (opt.charAt(0)) {
                case 'v':
                    version();
                    return;
                case '?':
                    while (++i < options.length) {
                        if (options[i].getOpt().charAt(0) == '?') {
                            usage(true);
                            return;
                        }
                    }
                    usage(false);
                    return;
                case 'o':
                    outputDirectory = commandLine.getOptionValue("o");
                    break;
                case 'p':
                    noParameterRegisters = true;
                    break;
                case 'l':
                    useLocalsDirective = true;
                    break;
                case 's':
                    useSequentialLabels = true;
                    break;
                case 'b':
                    outputDebugInfo = false;
                    break;
                case 'd':
                    bootClassPathDir = commandLine.getOptionValue("d");
                    break;
                case 'r':
                    String[] values = commandLine.getOptionValues('r');

                    if (values == null || values.length == 0) {
                        registerInfo = ARGS | DEST | MERGE;
                    } else {
                        for (String value: values) {
                            if (value.equalsIgnoreCase("ALL")) {
                                registerInfo |= ALL;
                            } else if (value.equalsIgnoreCase("ALLPRE")) {
                                registerInfo |= ALLPRE;
                            } else if (value.equalsIgnoreCase("ALLPOST")) {
                                registerInfo |= ALLPOST;
                            } else if (value.equalsIgnoreCase("ARGS")) {
                                registerInfo |= ARGS;
                            } else if (value.equalsIgnoreCase("DEST")) {
                                registerInfo |= DEST;
                            } else if (value.equalsIgnoreCase("MERGE")) {
                                registerInfo |= MERGE;
                            } else if (value.equalsIgnoreCase("FULLMERGE")) {
                                registerInfo |= FULLMERGE;
                            } else {
                                usage();
                                return;
                            }
                        }

                        if ((registerInfo & FULLMERGE) != 0) {
                            registerInfo &= ~MERGE;
                        }
                    }
                    break;
                case 'c':
                    String bcp = commandLine.getOptionValue("c");
                    if (bcp.charAt(0) == ':') {
                        bootClassPath = bootClassPath + bcp;
                    } else {
                        bootClassPath = bcp;
                    }
                    break;
                case 'x':
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
                    break;
                case 'N':
                    disassemble = false;
                    break;
                case 'D':
                    doDump = true;
                    dumpFileName = commandLine.getOptionValue("D", inputDexFileName + ".dump");
                    break;
                case 'W':
                    write = true;
                    outputDexFileName = commandLine.getOptionValue("W");
                    break;
                case 'S':
                    sort = true;
                    break;
                case 'F':
                    fixRegisters = true;
                    break;
                default:
                    assert false;
            }
        }

        if (remainingArgs.length != 1) {
            usage();
            return;
        }

        inputDexFileName = remainingArgs[0];

        try {
            File dexFileFile = new File(inputDexFileName);
            if (!dexFileFile.exists()) {
                System.err.println("Can't find the file " + inputDexFileName);
                System.exit(1);
            }

            //Read in and parse the dex file
            DexFile dexFile = new DexFile(dexFileFile, !fixRegisters, false);

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
                if (deodexerant == null) {
                    System.err.println("Warning: You are disassembling an odex file without deodexing it. You");
                    System.err.println("won't be able to re-assemble the results unless you use deodexerant, and");
                    System.err.println("the -x option for baksmali");
                }
            }

            if (disassemble) {
                baksmali.disassembleDexFile(dexFile, deodexerant, outputDirectory, bootClassPathDir, bootClassPath,
                        noParameterRegisters, useLocalsDirective, useSequentialLabels, outputDebugInfo, registerInfo);
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
    private static void usage(boolean printDebugOptions) {
        baksmaliHelpFormatter formatter = new baksmaliHelpFormatter();
        formatter.setWidth(100);

        formatter.printHelp("java -jar baksmali.jar [options] <dex-file>",
                "disassembles and/or dumps a dex file", basicOptions, "");

        if (printDebugOptions) {
            System.out.println();
            System.out.println("Debug Options:");

            StringBuffer sb = new StringBuffer();
            formatter.renderOptions(sb, debugOptions);
            System.out.println(sb.toString());
        }
    }

    private static void usage() {
        usage(false);
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
                .create("N");

        Option dumpOption = OptionBuilder.withLongOpt("dump-to")
                .withDescription("dumps the given dex file into a single annotated dump file named FILE" +
                        " (<dexfile>.dump by default), along with the normal disassembly.")
                .hasOptionalArg()
                .withArgName("FILE")
                .create("D");

        Option writeDexOption = OptionBuilder.withLongOpt("write-dex")
                .withDescription("additionally rewrites the input dex file to FILE")
                .hasArg()
                .withArgName("FILE")
                .create("W");

        Option outputDirOption = OptionBuilder.withLongOpt("output")
                .withDescription("the directory where the disassembled files will be placed. The default is out")
                .hasArg()
                .withArgName("DIR")
                .create("o");

        Option sortOption = OptionBuilder.withLongOpt("sort")
                .withDescription("sort the items in the dex file into a canonical order before dumping/writing")
                .create("S");

        Option fixSignedRegisterOption = OptionBuilder.withLongOpt("fix-signed-registers")
                .withDescription("when dumping or rewriting, fix any registers in the debug info that are encoded as" +
                        " a signed value")
                .create("F");

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
                        "using the bytecode address")
                .create("s");

        Option noDebugInfoOption = OptionBuilder.withLongOpt("no-debug-info")
                .withDescription("don't write out debug info (.local, .param, .line, etc.)")
                .create("b");

        Option registerInfoOption = OptionBuilder.withLongOpt("register-info")
                .hasOptionalArgs()
                .withArgName("REGISTER_INFO_TYPES")
                .withValueSeparator(',')
                .withDescription("print the specificed type(s) of register information for each instruction. " +
                        "\"ARGS,DEST,MERGE\" is the default if no types are specified.\nValid values are:\nALL: all " +
                        "pre- and post-instruction registers.\nALLPRE: all pre-instruction registers\nALLPOST: all " +
                        "post-instruction registers\nARGS: any pre-instruction registers used as arguments to the " +
                        "instruction\nDEST: the post-instruction destination register, if any\nMERGE: Any " +
                        "pre-instruction register has been merged from more than 1 different post-instruction " +
                        "register from its predecessors\nFULLMERGE: For each register that would be printed by " +
                        "MERGE, also show the incoming register types that were merged")
                .create("r");

        Option classPathOption = OptionBuilder.withLongOpt("bootclasspath")
                .withDescription("the bootclasspath jars to use, for analysis. Defaults to " +
                        "core.jar:ext.jar:framework.jar:android.policy.jar:services.jar. If you specify a value that " +
                        "begins with a :, it will be appended to the default bootclasspath")
                .hasOptionalArg()
                .withArgName("BOOTCLASSPATH")
                .create("c");

        Option classPathDirOption = OptionBuilder.withLongOpt("bootclasspath-dir")
                .withDescription("the base folder to look for the bootclasspath files in. Defaults to the current " +
                        "directory")
                .hasArg()
                .withArgName("DIR")
                .create("d");

        basicOptions.addOption(versionOption);
        basicOptions.addOption(helpOption);
        basicOptions.addOption(outputDirOption);
        basicOptions.addOption(noParameterRegistersOption);
        basicOptions.addOption(deodexerantOption);
        basicOptions.addOption(useLocalsOption);
        basicOptions.addOption(sequentialLabelsOption);
        basicOptions.addOption(noDebugInfoOption);
        basicOptions.addOption(registerInfoOption);
        basicOptions.addOption(classPathOption);
        basicOptions.addOption(classPathDirOption);

        debugOptions.addOption(dumpOption);
        debugOptions.addOption(noDisassemblyOption);
        debugOptions.addOption(writeDexOption);
        debugOptions.addOption(sortOption);
        debugOptions.addOption(fixSignedRegisterOption);


        for (Object option: basicOptions.getOptions()) {
            options.addOption((Option)option);
        }
        for (Object option: debugOptions.getOptions()) {
            options.addOption((Option)option);
        }        
    }
}