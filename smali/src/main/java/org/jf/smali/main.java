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

package org.jf.smali;

import org.apache.commons.cli.*;
import org.jf.dexlib.DexFile;
import org.jf.dexlib.CodeItem;
import org.jf.dexlib.Code.InstructionIterator;
import org.jf.dexlib.Code.Opcode;
import org.jf.dexlib.Code.Format.Format;
import org.jf.dexlib.Util.ByteArrayAnnotatedOutput;
import org.jf.dexlib.Util.FileUtils;
import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.TokenRewriteStream;
import org.antlr.runtime.Lexer;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;

import java.io.*;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Properties;

/**
 * Main class for smali. It recognizes enough options to be able to dispatch
 * to the right "actual" main.
 */
public class main {

    public static final String VERSION;

    private final static Options basicOptions;
    private final static Options debugOptions;
    private final static Options options;

    static {
        basicOptions = new Options();
        debugOptions = new Options();
        options = new Options();
        buildOptions();

        InputStream templateStream = main.class.getClassLoader().getResourceAsStream("smali.properties");
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

        boolean sort = false;
        boolean fixStringConst = true;
        boolean fixGoto = true;

        String outputDexFile = "out.dex";
        String dumpFileName = null;

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
                    outputDexFile = commandLine.getOptionValue("o");
                    break;
                case 'D':
                    dumpFileName = commandLine.getOptionValue("D", outputDexFile + ".dump");
                    break;
                case 'S':
                    sort = true;
                    break;
                case 'C':
                    fixStringConst = false;
                    break;
                case 'G':
                    fixGoto = false;
                    break;
                default:
                    assert false;
            }
        }

        if (remainingArgs.length == 0) {
            usage();
            return;
        }

        try {
            LinkedHashSet<File> filesToProcess = new LinkedHashSet<File>();

            for (String arg: remainingArgs) {
                    File argFile = new File(arg);

                    if (!argFile.exists()) {
                        throw new RuntimeException("Cannot find file or directory \"" + arg + "\"");
                    }

                    if (argFile.isDirectory()) {
                        getSmaliFilesInDir(argFile, filesToProcess);
                    } else if (argFile.isFile()) {
                        filesToProcess.add(argFile);
                    }
            }

            DexFile dexFile = new DexFile();

            boolean errors = false;

            for (File file: filesToProcess) {
                if (!assembleSmaliFile(file, dexFile)) {
                    errors = true;
                }
            }

            if (errors) {
                System.exit(1);
            }


            if (sort) {
                dexFile.setSortAllItems(true);
            }

            if (fixStringConst || fixGoto) {
                fixInstructions(dexFile, fixStringConst, fixGoto);
            }

            dexFile.place();

            ByteArrayAnnotatedOutput out = new ByteArrayAnnotatedOutput();

            if (dumpFileName != null) {
                out.enableAnnotations(120, true);
            }

            dexFile.writeTo(out);

            byte[] bytes = out.toByteArray();

            DexFile.calcSignature(bytes);
            DexFile.calcChecksum(bytes);

            if (dumpFileName != null) {
                out.finishAnnotating();

                FileWriter fileWriter = new FileWriter(dumpFileName);
                out.writeAnnotationsTo(fileWriter);
                fileWriter.close();
            }

            FileOutputStream fileOutputStream = new FileOutputStream(outputDexFile);

            fileOutputStream.write(bytes);
            fileOutputStream.close();
        } catch (RuntimeException ex) {
            System.err.println("\nUNEXPECTED TOP-LEVEL EXCEPTION:");
            ex.printStackTrace();
            System.exit(2);
        } catch (Throwable ex) {
            System.err.println("\nUNEXPECTED TOP-LEVEL ERROR:");
            ex.printStackTrace();
            System.exit(3);
        }
    }

    private static void getSmaliFilesInDir(File dir, Set<File> smaliFiles) {
        for(File file: dir.listFiles()) {
            if (file.isDirectory()) {
                getSmaliFilesInDir(file, smaliFiles);
            } else if (file.getName().endsWith(".smali")) {
                smaliFiles.add(file);
            }
        }
    }

    private static void fixInstructions(DexFile dexFile, boolean fixStringConst, boolean fixGoto) {
        dexFile.place();

        byte[] newInsns = null;

        for (CodeItem codeItem: dexFile.CodeItemsSection.getItems()) {
            codeItem.fixInstructions(fixStringConst, fixGoto);
        }
    }

    private static boolean assembleSmaliFile(File smaliFile, DexFile dexFile)
            throws Exception {
        ANTLRInputStream input = new ANTLRInputStream(new FileInputStream(smaliFile));
        input.name = smaliFile.getAbsolutePath();

        smaliLexer lexer = new smaliLexer(input);

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        smaliParser parser = new smaliParser(tokens);

        smaliParser.smali_file_return result = parser.smali_file();

        if (parser.getNumberOfSyntaxErrors() > 0 || lexer.getNumberOfLexerErrors() > 0) {
            return false;
        }

        CommonTree t = (CommonTree) result.getTree();

        CommonTreeNodeStream treeStream = new CommonTreeNodeStream(t);
        treeStream.setTokenStream(tokens);

        smaliTreeWalker dexGen = new smaliTreeWalker(treeStream);

        dexGen.dexFile = dexFile;
        dexGen.smali_file();

        if (dexGen.getNumberOfSyntaxErrors() > 0) {
            return false;
        }

        return true;
    }


    /**
     * Prints the usage message.
     */
    private static void usage(boolean printDebugOptions) {
        smaliHelpFormatter formatter = new smaliHelpFormatter();
        formatter.setWidth(100);

        formatter.printHelp("java -jar smali.jar [options] [--] [<smali-file>|folder]*",
                "assembles a set of smali files into a dex file", basicOptions, "");

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
        System.out.println("smali " + VERSION + " (http://smali.googlecode.com)");
        System.out.println("Copyright (C) 2009 Ben Gruver");
        System.out.println("BSD license (http://www.opensource.org/licenses/bsd-license.php)");
        System.exit(0);
    }

    private static void buildOptions() {
        Option versionOption = OptionBuilder.withLongOpt("version")
                .withDescription("prints the version then exits")
                .create("v");

        Option helpOption = OptionBuilder.withLongOpt("help")
                .withDescription("prints the help message then exits. Specify twice for debug options")
                .create("?");

        Option outputOption = OptionBuilder.withLongOpt("output")
                .withDescription("the name of the dex file that will be written. The default is out.dex")
                .hasArg()
                .withArgName("FILE")
                .create("o");

        Option dumpOption = OptionBuilder.withLongOpt("dump-to")
                .withDescription("additionally writes a dump of written dex file to FILE (<dexfile>.dump by default)")
                .hasOptionalArg()
                .withArgName("FILE")
                .create("D");

        Option sortOption = OptionBuilder.withLongOpt("sort")
                .withDescription("sort the items in the dex file into a canonical order before writing")
                .create("S");

        Option noFixStringConstOption = OptionBuilder.withLongOpt("no-fix-string-const")
                .withDescription("Don't replace string-const instructions with string-const/jumbo where appropriate")
                .create("C");

        Option noFixGotoOption = OptionBuilder.withLongOpt("no-fix-goto")
                .withDescription("Don't replace goto type instructions with a larger version where appropriate")
                .create("G");

        basicOptions.addOption(versionOption);
        basicOptions.addOption(helpOption);
        basicOptions.addOption(outputOption);

        debugOptions.addOption(dumpOption);
        debugOptions.addOption(sortOption);
        debugOptions.addOption(noFixStringConstOption);
        debugOptions.addOption(noFixGotoOption);

        for (Object option: basicOptions.getOptions()) {
            options.addOption((Option)option);
        }

        for (Object option: debugOptions.getOptions()) {
            options.addOption((Option)option);
        }
    }
}