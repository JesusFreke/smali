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

    private final static Options options;

    static {
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
        boolean rewriteLabels = false;

        String outputDexFile = "out.dex";
        String dumpFileName = null;

        String[] remainingArgs = commandLine.getArgs();

        if (commandLine.hasOption("v")) {
            version();
            return;
        }

        if (commandLine.hasOption("?")) {
            usage();
            return;
        }

        if (remainingArgs.length == 0) {
            usage();
            return;
        }

        if (commandLine.hasOption("r")) {
            rewriteLabels = true;
        }

        if (commandLine.hasOption("o")) {
            if (rewriteLabels) {
                System.err.println("The --output/-o option is not compatible with the --rewrite-labels/-r option. Ignoring");
            } else {
                outputDexFile = commandLine.getOptionValue("o");
            }
        }

        if (commandLine.hasOption("d")) {
            if (rewriteLabels) {
                System.err.println("The --dump/-d option is not compatible with the --rewrite-labels/-r option. Ignoring");
            } else {
                dumpFileName = commandLine.getOptionValue("d", outputDexFile + ".dump");
            }
        }

        if (commandLine.hasOption("s")) {
            if (rewriteLabels) {
                System.err.println("The --sort/-s option is not compatible with the --rewrite-labels/-r option. Ignoring");
            } else {
                sort = true;
            }
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

            if (rewriteLabels) {
                if (doRewriteLabels(filesToProcess)) {
                    System.exit(1);
                }
                System.exit(0);
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

    private static boolean doRewriteLabels(Set<File> files)
        throws Exception {
        boolean errorOccurred = false;

        for (File file: files) {
            ANTLRInputStream input = new ANTLRInputStream(new FileInputStream(file));
            input.name = file.getAbsolutePath();

            smaliLexer_old lexer = new smaliLexer_old(input);

            if (lexer.getNumberOfLexerErrors() > 0) {
                errorOccurred = true;
                continue;
            }

            TokenRewriteStream tokens = new TokenRewriteStream(lexer);
            labelConverter parser = new labelConverter(tokens);
            parser.top();

            if (parser.getNumberOfSyntaxErrors() > 0) {
                errorOccurred = true;
                continue;
            }

            FileWriter writer = null;
            try
            {
                writer = new FileWriter(file);
                writer.write(tokens.toString());
            } finally {
                if (writer != null) {
                    writer.close();
                }
            }
        }

        return !errorOccurred;
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
    private static void usage() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar smali.jar [options] [--] [<smali-file>|folder]*",
                "assembles a set of smali files into a dex file, and optionally generats an annotated dump of the output file", options, "");
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
                .withDescription("prints the help message then exits")
                .create("?");

        Option dumpOption = OptionBuilder.withLongOpt("dump-to")
                .withDescription("additionally writes a dump of written dex file to FILE (<dexfile>.dump by default)")
                .hasOptionalArg()
                .withArgName("FILE")
                .create("d");

        Option outputOption = OptionBuilder.withLongOpt("output")
                .withDescription("the name of the dex file that will be written. The default is out.dex")
                .hasArg()
                .withArgName("FILE")
                .create("o");

        Option sortOption = OptionBuilder.withLongOpt("sort")
                .withDescription("sort the items in the dex file into a canonical order before writing")
                .create("s");

        Option rewriteLabelOption = OptionBuilder.withLongOpt("rewrite-labels")
                .withDescription("rewrite the input smali files, converting any labels in the old (pre .97) format to the new format")
                .create("r");

        options.addOption(versionOption);
        options.addOption(helpOption);
        options.addOption(dumpOption);
        options.addOption(outputOption);
        options.addOption(sortOption);
        options.addOption(rewriteLabelOption);
    }
}