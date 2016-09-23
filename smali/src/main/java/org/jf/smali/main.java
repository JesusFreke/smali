/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver (JesusFreke)
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

package org.jf.smali;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.Token;
import org.antlr.runtime.TokenSource;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.apache.commons.cli.*;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.writer.builder.DexBuilder;
import org.jf.dexlib2.writer.io.FileDataStore;
import org.jf.util.ConsoleUtil;
import org.jf.util.SmaliHelpFormatter;

import javax.annotation.Nonnull;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

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
        if (templateStream != null) {
            Properties properties = new Properties();
            String version = "(unknown)";
            try {
                properties.load(templateStream);
                version = properties.getProperty("application.version");
            } catch (IOException ex) {
                // just eat it
            }
            VERSION = version;
        } else {
            VERSION = "[unknown version]";
        }
    }


    /**
     * This class is uninstantiable.
     */
    private main() {
    }

    /**
     * A more programmatic-friendly entry point for smali
     *
     * @param options a SmaliOptions object with the options to run smali with
     * @param input The files/directories to process
     * @return true if assembly completed with no errors, or false if errors were encountered
     */
    public static boolean run(final SmaliOptions options, String... input) throws IOException {
        LinkedHashSet<File> filesToProcessSet = new LinkedHashSet<File>();

        for (String fileToProcess: input) {
            File argFile = new File(fileToProcess);

            if (!argFile.exists()) {
                throw new IllegalArgumentException("Cannot find file or directory \"" + fileToProcess + "\"");
            }

            if (argFile.isDirectory()) {
                getSmaliFilesInDir(argFile, filesToProcessSet);
            } else if (argFile.isFile()) {
                filesToProcessSet.add(argFile);
            }
        }

        boolean errors = false;

        final DexBuilder dexBuilder = DexBuilder.makeDexBuilder(
                Opcodes.forApi(options.apiLevel, options.experimental));

        ExecutorService executor = Executors.newFixedThreadPool(options.jobs);
        List<Future<Boolean>> tasks = Lists.newArrayList();

        for (final File file: filesToProcessSet) {
            tasks.add(executor.submit(new Callable<Boolean>() {
                @Override public Boolean call() throws Exception {
                    return assembleSmaliFile(file, dexBuilder, options);
                }
            }));
        }

        for (Future<Boolean> task: tasks) {
            while(true) {
                try {
                    try {
                        if (!task.get()) {
                            errors = true;
                        }
                    } catch (ExecutionException ex) {
                        throw new RuntimeException(ex);
                    }
                } catch (InterruptedException ex) {
                    continue;
                }
                break;
            }
        }

        executor.shutdown();

        if (errors) {
            return false;
        }

        if (options.listMethods) {
            if (Strings.isNullOrEmpty(options.methodListFilename)) {
                options.methodListFilename = options.outputDexFile + ".methods";
            }
            writeReferences(dexBuilder.getMethodReferences(), options.methodListFilename);
        }

        if (options.listFields) {
            if (Strings.isNullOrEmpty(options.fieldListFilename)) {
                options.fieldListFilename = options.outputDexFile + ".fields";
            }
            writeReferences(dexBuilder.getFieldReferences(), options.fieldListFilename);
        }

        if (options.listTypes) {
            if (Strings.isNullOrEmpty(options.typeListFilename)) {
                options.typeListFilename = options.outputDexFile + ".types";
            }
            writeReferences(dexBuilder.getTypeReferences(), options.typeListFilename);
        }

        dexBuilder.writeTo(new FileDataStore(new File(options.outputDexFile)));

        return true;
    }

    /**
     * Run!
     */
    public static void main(String[] args) {
        Locale locale = new Locale("en", "US");
        Locale.setDefault(locale);

        CommandLineParser parser = new PosixParser();
        CommandLine commandLine;

        try {
            commandLine = parser.parse(options, args);
        } catch (ParseException ex) {
            usage();
            return;
        }

        SmaliOptions smaliOptions = new SmaliOptions();

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
                    smaliOptions.outputDexFile = commandLine.getOptionValue("o");
                    break;
                case 'x':
                    smaliOptions.allowOdex = true;
                    break;
                case 'X':
                    smaliOptions.experimental = true;
                    break;
                case 'a':
                    smaliOptions.apiLevel = Integer.parseInt(commandLine.getOptionValue("a"));
                    break;
                case 'j':
                    smaliOptions.jobs = Integer.parseInt(commandLine.getOptionValue("j"));
                    break;
                case 'm':
                    smaliOptions.listMethods = true;
                    smaliOptions.methodListFilename = commandLine.getOptionValue("m");
                    break;
                case 'f':
                    smaliOptions.listFields = true;
                    smaliOptions.fieldListFilename = commandLine.getOptionValue("f");
                    break;
                case 't':
                    smaliOptions.listTypes = true;
                    smaliOptions.typeListFilename = commandLine.getOptionValue("t");
                    break;
                case 'V':
                    smaliOptions.verboseErrors = true;
                    break;
                case 'T':
                    smaliOptions.printTokens = true;
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
            if (!run(smaliOptions, remainingArgs)) {
                System.exit(1);
            }
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

    private static void writeReferences(List<String> references, String filename) {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new BufferedWriter(new FileWriter(filename)));

            for (String reference: Ordering.natural().sortedCopy(references)) {
                writer.println(reference);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    private static void getSmaliFilesInDir(@Nonnull File dir, @Nonnull Set<File> smaliFiles) {
        File[] files = dir.listFiles();
        if (files != null) {
            for(File file: files) {
                if (file.isDirectory()) {
                    getSmaliFilesInDir(file, smaliFiles);
                } else if (file.getName().endsWith(".smali")) {
                    smaliFiles.add(file);
                }
            }
        }
    }

    private static boolean assembleSmaliFile(File smaliFile, DexBuilder dexBuilder, SmaliOptions options)
            throws Exception {
        FileInputStream fis = null;
        
        try {
            fis = new FileInputStream(smaliFile);
            InputStreamReader reader = new InputStreamReader(fis, "UTF-8");
    
            LexerErrorInterface lexer = new smaliFlexLexer(reader);
            ((smaliFlexLexer)lexer).setSourceFile(smaliFile);
            CommonTokenStream tokens = new CommonTokenStream((TokenSource)lexer);
    
            if (options.printTokens) {
                tokens.getTokens();
    
                for (int i=0; i<tokens.size(); i++) {
                    Token token = tokens.get(i);
                    if (token.getChannel() == smaliParser.HIDDEN) {
                        continue;
                    }
    
                    System.out.println(smaliParser.tokenNames[token.getType()] + ": " + token.getText());
                }
    
                System.out.flush();
            }
    
            smaliParser parser = new smaliParser(tokens);
            parser.setVerboseErrors(options.verboseErrors);
            parser.setAllowOdex(options.allowOdex);
            parser.setApiLevel(options.apiLevel, options.experimental);
    
            smaliParser.smali_file_return result = parser.smali_file();
    
            if (parser.getNumberOfSyntaxErrors() > 0 || lexer.getNumberOfSyntaxErrors() > 0) {
                return false;
            }
    
            CommonTree t = result.getTree();
    
            CommonTreeNodeStream treeStream = new CommonTreeNodeStream(t);
            treeStream.setTokenStream(tokens);
    
            if (options.printTokens) {
                System.out.println(t.toStringTree());
            }
    
            smaliTreeWalker dexGen = new smaliTreeWalker(treeStream);
            dexGen.setApiLevel(options.apiLevel, options.experimental);
    
            dexGen.setVerboseErrors(options.verboseErrors);
            dexGen.setDexBuilder(dexBuilder);
            dexGen.smali_file();
            return dexGen.getNumberOfSyntaxErrors() == 0;
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
    }


    /**
     * Prints the usage message.
     */
    private static void usage(boolean printDebugOptions) {
        SmaliHelpFormatter formatter = new SmaliHelpFormatter();

        int consoleWidth = ConsoleUtil.getConsoleWidth();
        if (consoleWidth <= 0) {
            consoleWidth = 80;
        }

        formatter.setWidth(consoleWidth);

        formatter.printHelp("java -jar smali.jar [options] [--] [<smali-file>|folder]*",
                "assembles a set of smali files into a dex file", basicOptions, printDebugOptions?debugOptions:null);
    }

    private static void usage() {
        usage(false);
    }

    /**
     * Prints the version message.
     */
    private static void version() {
        System.out.println("smali " + VERSION + " (http://smali.googlecode.com)");
        System.out.println("Copyright (C) 2010 Ben Gruver (JesusFreke@JesusFreke.com)");
        System.out.println("BSD license (http://www.opensource.org/licenses/bsd-license.php)");
        System.exit(0);
    }

    @SuppressWarnings("AccessStaticViaInstance")
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

        Option allowOdexOption = OptionBuilder.withLongOpt("allow-odex-instructions")
                .withDescription("allow odex instructions to be compiled into the dex file. Only a few" +
                        " instructions are supported - the ones that can exist in a dead code path and not" +
                        " cause dalvik to reject the class")
                .create("x");

        Option apiLevelOption = OptionBuilder.withLongOpt("api-level")
                .withDescription("The numeric api-level of the file to generate, e.g. 14 for ICS. If not " +
                        "specified, it defaults to 15 (ICS).")
                .hasArg()
                .withArgName("API_LEVEL")
                .create("a");

        Option listMethodsOption = OptionBuilder.withLongOpt("list-methods")
                .withDescription("Lists all the method references to FILE" +
                        " (<output_dex_filename>.methods by default)")
                .hasOptionalArg()
                .withArgName("FILE")
                .create("m");

        Option listFieldsOption = OptionBuilder.withLongOpt("list-fields")
                .withDescription("Lists all the field references to FILE" +
                        " (<output_dex_filename>.fields by default)")
                .hasOptionalArg()
                .withArgName("FILE")
                .create("f");

        Option listClassesOption = OptionBuilder.withLongOpt("list-types")
                .withDescription("Lists all the type references to FILE" +
                        " (<output_dex_filename>.types by default)")
                .hasOptionalArg()
                .withArgName("FILE")
                .create("t");

        Option experimentalOption = OptionBuilder.withLongOpt("experimental")
                .withDescription("enable experimental opcodes to be assembled, even if they " +
                        " aren't necessarily supported by the Android runtime yet")
                .create("X");

        Option jobsOption = OptionBuilder.withLongOpt("jobs")
                .withDescription("The number of threads to use. Defaults to the number of cores available, up to a " +
                        "maximum of 6")
                .hasArg()
                .withArgName("NUM_THREADS")
                .create("j");

        Option verboseErrorsOption = OptionBuilder.withLongOpt("verbose-errors")
                .withDescription("Generate verbose error messages")
                .create("V");

        Option printTokensOption = OptionBuilder.withLongOpt("print-tokens")
                .withDescription("Print the name and text of each token")
                .create("T");

        basicOptions.addOption(versionOption);
        basicOptions.addOption(helpOption);
        basicOptions.addOption(outputOption);
        basicOptions.addOption(allowOdexOption);
        basicOptions.addOption(apiLevelOption);
        basicOptions.addOption(experimentalOption);
        basicOptions.addOption(jobsOption);
        basicOptions.addOption(listMethodsOption);
        basicOptions.addOption(listFieldsOption);
        basicOptions.addOption(listClassesOption);

        debugOptions.addOption(verboseErrorsOption);
        debugOptions.addOption(printTokensOption);

        for (Object option: basicOptions.getOptions()) {
            options.addOption((Option)option);
        }

        for (Object option: debugOptions.getOptions()) {
            options.addOption((Option)option);
        }
    }
}