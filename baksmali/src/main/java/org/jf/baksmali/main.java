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

package org.jf.baksmali;

import com.google.common.collect.Lists;
import org.apache.commons.cli.*;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.DexFileFactory.MultipleDexFilesException;
import org.jf.dexlib2.analysis.InlineMethodResolver;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.DexBackedOdexFile;
import org.jf.dexlib2.dexbacked.OatFile.OatDexFile;
import org.jf.util.ConsoleUtil;
import org.jf.util.SmaliHelpFormatter;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public class main {

    public static final String VERSION;

    private static final Options basicOptions;
    private static final Options debugOptions;
    private static final Options options;

    static {
        options = new Options();
        basicOptions = new Options();
        debugOptions = new Options();
        buildOptions();

        InputStream templateStream = baksmali.class.getClassLoader().getResourceAsStream("baksmali.properties");
        if (templateStream != null) {
            Properties properties = new Properties();
            String version = "(unknown)";
            try {
                properties.load(templateStream);
                version = properties.getProperty("application.version");
            } catch (IOException ex) {
                // ignore
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
     * Run!
     */
    public static void main(String[] args) throws IOException {
        Locale locale = new Locale("en", "US");
        Locale.setDefault(locale);

        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine;

        try {
            commandLine = parser.parse(options, args);
        } catch (ParseException ex) {
            usage();
            return;
        }

        baksmaliOptions options = new baksmaliOptions();

        boolean disassemble = true;
        boolean doDump = false;
        String dumpFileName = null;
        boolean setBootClassPath = false;

        String[] remainingArgs = commandLine.getArgs();
        Option[] clOptions = commandLine.getOptions();

        for (int i=0; i<clOptions.length; i++) {
            Option option = clOptions[i];
            String opt = option.getOpt();

            switch (opt.charAt(0)) {
                case 'v':
                    version();
                    return;
                case '?':
                    while (++i < clOptions.length) {
                        if (clOptions[i].getOpt().charAt(0) == '?') {
                            usage(true);
                            return;
                        }
                    }
                    usage(false);
                    return;
                case 'o':
                    options.outputDirectory = commandLine.getOptionValue("o");
                    break;
                case 'p':
                    options.noParameterRegisters = true;
                    break;
                case 'l':
                    options.useLocalsDirective = true;
                    break;
                case 's':
                    options.useSequentialLabels = true;
                    break;
                case 'b':
                    options.outputDebugInfo = false;
                    break;
                case 'd':
                    options.bootClassPathDirs.add(option.getValue());
                    break;
                case 'f':
                    options.addCodeOffsets = true;
                    break;
                case 'r':
                    String[] values = commandLine.getOptionValues('r');
                    int registerInfo = 0;

                    if (values == null || values.length == 0) {
                        registerInfo = baksmaliOptions.ARGS | baksmaliOptions.DEST;
                    } else {
                        for (String value: values) {
                            if (value.equalsIgnoreCase("ALL")) {
                                registerInfo |= baksmaliOptions.ALL;
                            } else if (value.equalsIgnoreCase("ALLPRE")) {
                                registerInfo |= baksmaliOptions.ALLPRE;
                            } else if (value.equalsIgnoreCase("ALLPOST")) {
                                registerInfo |= baksmaliOptions.ALLPOST;
                            } else if (value.equalsIgnoreCase("ARGS")) {
                                registerInfo |= baksmaliOptions.ARGS;
                            } else if (value.equalsIgnoreCase("DEST")) {
                                registerInfo |= baksmaliOptions.DEST;
                            } else if (value.equalsIgnoreCase("MERGE")) {
                                registerInfo |= baksmaliOptions.MERGE;
                            } else if (value.equalsIgnoreCase("FULLMERGE")) {
                                registerInfo |= baksmaliOptions.FULLMERGE;
                            } else {
                                usage();
                                return;
                            }
                        }

                        if ((registerInfo & baksmaliOptions.FULLMERGE) != 0) {
                            registerInfo &= ~baksmaliOptions.MERGE;
                        }
                    }
                    options.registerInfo = registerInfo;
                    break;
                case 'c':
                    String bcp = commandLine.getOptionValue("c");
                    if (bcp != null && bcp.charAt(0) == ':') {
                        options.addExtraClassPath(bcp);
                    } else {
                        setBootClassPath = true;
                        options.setBootClassPath(bcp);
                    }
                    break;
                case 'x':
                    options.deodex = true;
                    break;
                case 'X':
                    options.experimental = true;
                    break;
                case 'm':
                    options.noAccessorComments = true;
                    break;
                case 'a':
                    options.apiLevel = Integer.parseInt(commandLine.getOptionValue("a"));
                    break;
                case 'j':
                    options.jobs = Integer.parseInt(commandLine.getOptionValue("j"));
                    break;
                case 'i':
                    String rif = commandLine.getOptionValue("i");
                    options.setResourceIdFiles(rif);
                    break;
                case 't':
                    options.useImplicitReferences = true;
                    break;
                case 'e':
                    options.dexEntry = commandLine.getOptionValue("e");
                    break;
                case 'k':
                    options.checkPackagePrivateAccess = true;
                    break;
                case 'n':
                    options.normalizeVirtualMethods = true;
                    break;
                case 'N':
                    disassemble = false;
                    break;
                case 'D':
                    doDump = true;
                    dumpFileName = commandLine.getOptionValue("D");
                    break;
                case 'I':
                    options.ignoreErrors = true;
                    break;
                case 'T':
                    options.customInlineDefinitions = new File(commandLine.getOptionValue("T"));
                    break;
                default:
                    assert false;
            }
        }

        if (remainingArgs.length != 1) {
            usage();
            return;
        }

        if (options.jobs <= 0) {
            options.jobs = Runtime.getRuntime().availableProcessors();
            if (options.jobs > 6) {
                options.jobs = 6;
            }
        }

        String inputDexFileName = remainingArgs[0];

        File dexFileFile = new File(inputDexFileName);
        if (!dexFileFile.exists()) {
            System.err.println("Can't find the file " + inputDexFileName);
            System.exit(1);
        }

        //Read in and parse the dex file
        DexBackedDexFile dexFile = null;
        try {
            dexFile = DexFileFactory.loadDexFile(dexFileFile, options.dexEntry, options.apiLevel, options.experimental);
        } catch (MultipleDexFilesException ex) {
            System.err.println(String.format("%s contains multiple dex files. You must specify which one to " +
                    "disassemble with the -e option", dexFileFile.getName()));
            System.err.println("Valid entries include:");
            for (OatDexFile oatDexFile: ex.oatFile.getDexFiles()) {
                System.err.println(oatDexFile.filename);
            }
            System.exit(1);
        }

        if (dexFile.hasOdexOpcodes()) {
            if (!options.deodex) {
                System.err.println("Warning: You are disassembling an odex file without deodexing it. You");
                System.err.println("won't be able to re-assemble the results unless you deodex it with the -x");
                System.err.println("option");
                options.allowOdex = true;
            }
        } else {
            options.deodex = false;
        }

        if (!setBootClassPath && (options.deodex || options.registerInfo != 0 || options.normalizeVirtualMethods)) {
            if (dexFile instanceof DexBackedOdexFile) {
                options.bootClassPathEntries = ((DexBackedOdexFile)dexFile).getDependencies();
            } else {
                options.bootClassPathEntries = getDefaultBootClassPathForApi(options.apiLevel,
                        options.experimental);
            }
        }

        if (options.customInlineDefinitions == null && dexFile instanceof DexBackedOdexFile) {
            options.inlineResolver =
                    InlineMethodResolver.createInlineMethodResolver(
                            ((DexBackedOdexFile)dexFile).getOdexVersion());
        }

        boolean errorOccurred = false;
        if (disassemble) {
            errorOccurred = !baksmali.disassembleDexFile(dexFile, options);
        }

        if (doDump) {
            if (dumpFileName == null) {
                dumpFileName = commandLine.getOptionValue(inputDexFileName + ".dump");
            }
            dump.dump(dexFile, dumpFileName, options.apiLevel, options.experimental);
        }

        if (errorOccurred) {
            System.exit(1);
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

        formatter.printHelp("java -jar baksmali.jar [options] <dex-file>",
                "disassembles and/or dumps a dex file", basicOptions, printDebugOptions?debugOptions:null);
    }

    private static void usage() {
        usage(false);
    }

    /**
     * Prints the version message.
     */
    protected static void version() {
        System.out.println("baksmali " + VERSION + " (http://smali.googlecode.com)");
        System.out.println("Copyright (C) 2010 Ben Gruver (JesusFreke@JesusFreke.com)");
        System.out.println("BSD license (http://www.opensource.org/licenses/bsd-license.php)");
        System.exit(0);
    }

    @SuppressWarnings("AccessStaticViaInstance")
    private static void buildOptions() {
        Option versionOption = Option.builder("v")
                .longOpt("version")
                .desc("prints the version then exits")
                .build();

        Option helpOption = Option.builder("?")
                .longOpt("help")
                .desc("prints the help message then exits. Specify twice for debug options")
                .build();

        Option outputDirOption = Option.builder("o")
                .longOpt("output")
                .desc("the directory where the disassembled files will be placed. The default is out")
                .hasArg()
                .argName("DIR")
                .build();

        Option noParameterRegistersOption = Option.builder("p")
                .longOpt("no-parameter-registers")
                .desc("use the v<n> syntax instead of the p<n> syntax for registers mapped to method " +
                        "parameters")
                .build();

        Option deodexerantOption = Option.builder("x")
                .longOpt("deodex")
                .desc("deodex the given odex file. This option is ignored if the input file is not an " +
                        "odex file")
                .build();

        Option experimentalOption = Option.builder("X")
                .longOpt("experimental")
                .desc("enable experimental opcodes to be disassembled, even if they aren't necessarily supported in the Android runtime yet")
                .build();

        Option useLocalsOption = Option.builder("l")
                .longOpt("use-locals")
                .desc("output the .locals directive with the number of non-parameter registers, rather" +
                        " than the .register directive with the total number of register")
                .build();

        Option sequentialLabelsOption = Option.builder("s")
                .longOpt("sequential-labels")
                .desc("create label names using a sequential numbering scheme per label type, rather than " +
                        "using the bytecode address")
                .build();

        Option noDebugInfoOption = Option.builder("b")
                .longOpt("no-debug-info")
                .desc("don't write out debug info (.local, .param, .line, etc.)")
                .build();

        Option registerInfoOption = Option.builder("r")
                .longOpt("register-info")
                .optionalArg(true)
                .hasArgs()
                .valueSeparator(',')
                .argName("REGISTER_INFO_TYPES")
                .desc("print the specificed type(s) of register information for each instruction. " +
                        "\"ARGS,DEST\" is the default if no types are specified.\nValid values are:\nALL: all " +
                        "pre- and post-instruction registers.\nALLPRE: all pre-instruction registers\nALLPOST: all " +
                        "post-instruction registers\nARGS: any pre-instruction registers used as arguments to the " +
                        "instruction\nDEST: the post-instruction destination register, if any\nMERGE: Any " +
                        "pre-instruction register has been merged from more than 1 different post-instruction " +
                        "register from its predecessors\nFULLMERGE: For each register that would be printed by " +
                        "MERGE, also show the incoming register types that were merged")
                .build();

        Option classPathOption = Option.builder("c")
                .longOpt("bootclasspath")
                .desc("A colon-separated list of bootclasspath jar/oat files to use for analysis. Add an " +
                        "initial colon to specify that the jars/oats should be appended to the default bootclasspath " +
                        "instead of replacing it")
                .hasArg()
                .optionalArg(true)
                .argName("BOOTCLASSPATH")
                .build();

        Option classPathDirOption = Option.builder("d")
                .longOpt("bootclasspath-dir")
                .desc("the base folder to look for the bootclasspath files in. Defaults to the current " +
                        "directory")
                .hasArg()
                .argName("DIR")
                .build();

        Option codeOffsetOption = Option.builder("f")
                .longOpt("code-offsets")
                .desc("add comments to the disassembly containing the code offset for each address")
                .build();

        Option noAccessorCommentsOption = Option.builder("m")
                .longOpt("no-accessor-comments")
                .desc("don't output helper comments for synthetic accessors")
                .build();

        Option apiLevelOption = Option.builder("a")
                .longOpt("api-level")
                .desc("The numeric api-level of the file being disassembled. If not " +
                        "specified, it defaults to 15 (ICS).")
                .hasArg()
                .argName("API_LEVEL")
                .build();

        Option jobsOption = Option.builder("j")
                .longOpt("jobs")
                .desc("The number of threads to use. Defaults to the number of cores available, up to a " +
                        "maximum of 6")
                .hasArg()
                .argName("NUM_THREADS")
                .build();

        Option resourceIdFilesOption = Option.builder("i")
                .longOpt("resource-id-files")
                .desc("the resource ID files to use, for analysis. A colon-separated list of prefix=file " +
                        "pairs.  For example R=res/values/public.xml:" +
                        "android.R=$ANDROID_HOME/platforms/android-19/data/res/values/public.xml")
                .hasArg()
                .argName("FILES")
                .build();

        Option noImplicitReferencesOption = Option.builder("t")
                .longOpt("no-implicit-references")
                .desc("Use implicit (type-less) method and field references")
                .build();

        Option checkPackagePrivateAccessOption = Option.builder("k")
                .longOpt("check-package-private-access")
                .desc("When deodexing, use the package-private access check when calculating vtable " +
                        "indexes. It should only be needed for 4.2.0 odexes. The functionality was reverted for " +
                        "4.2.1.")
                .build();

        Option normalizeVirtualMethods = Option.builder("n")
                .longOpt("normalize-virtual-methods")
                .desc("Normalize virtual method references to the reference the base method.")
                .build();

        Option dumpOption = Option.builder("D")
                .longOpt("dump-to")
                .desc("dumps the given dex file into a single annotated dump file named FILE" +
                        " (<dexfile>.dump by default), along with the normal disassembly")
                .hasArg()
                .optionalArg(true)
                .argName("FILE")
                .build();

        Option ignoreErrorsOption = Option.builder("I")
                .longOpt("ignore-errors")
                .desc("ignores any non-fatal errors that occur while disassembling/deodexing," +
                        " ignoring the class if needed, and continuing with the next class. The default" +
                        " behavior is to stop disassembling and exit once an error is encountered")
                .build();

        Option noDisassemblyOption = Option.builder("N")
                .longOpt("no-disassembly")
                .desc("suppresses the output of the disassembly")
                .build();

        Option inlineTableOption = Option.builder("T")
                .longOpt("inline-table")
                .desc("specify a file containing a custom inline method table to use for deodexing")
                .hasArg()
                .argName("FILE")
                .build();

        Option dexEntryOption = Option.builder("e")
                .longOpt("dex-file")
                .desc("looks for dex file named DEX_FILE, defaults to classes.dex")
                .argName("DEX_FILE")
                .hasArg()
                .build();

        basicOptions.addOption(versionOption);
        basicOptions.addOption(helpOption);
        basicOptions.addOption(outputDirOption);
        basicOptions.addOption(noParameterRegistersOption);
        basicOptions.addOption(deodexerantOption);
        basicOptions.addOption(experimentalOption);
        basicOptions.addOption(useLocalsOption);
        basicOptions.addOption(sequentialLabelsOption);
        basicOptions.addOption(noDebugInfoOption);
        basicOptions.addOption(registerInfoOption);
        basicOptions.addOption(classPathOption);
        basicOptions.addOption(classPathDirOption);
        basicOptions.addOption(codeOffsetOption);
        basicOptions.addOption(noAccessorCommentsOption);
        basicOptions.addOption(apiLevelOption);
        basicOptions.addOption(jobsOption);
        basicOptions.addOption(resourceIdFilesOption);
        basicOptions.addOption(noImplicitReferencesOption);
        basicOptions.addOption(dexEntryOption);
        basicOptions.addOption(checkPackagePrivateAccessOption);
        basicOptions.addOption(normalizeVirtualMethods);

        debugOptions.addOption(dumpOption);
        debugOptions.addOption(ignoreErrorsOption);
        debugOptions.addOption(noDisassemblyOption);
        debugOptions.addOption(inlineTableOption);

        for (Object option: basicOptions.getOptions()) {
            options.addOption((Option)option);
        }
        for (Object option: debugOptions.getOptions()) {
            options.addOption((Option)option);
        }
    }

    @Nonnull
    private static List<String> getDefaultBootClassPathForApi(int apiLevel, boolean experimental) {
        if (apiLevel < 9) {
            return Lists.newArrayList(
                    "/system/framework/core.jar",
                    "/system/framework/ext.jar",
                    "/system/framework/framework.jar",
                    "/system/framework/android.policy.jar",
                    "/system/framework/services.jar");
        } else if (apiLevel < 12) {
            return Lists.newArrayList(
                    "/system/framework/core.jar",
                    "/system/framework/bouncycastle.jar",
                    "/system/framework/ext.jar",
                    "/system/framework/framework.jar",
                    "/system/framework/android.policy.jar",
                    "/system/framework/services.jar",
                    "/system/framework/core-junit.jar");
        } else if (apiLevel < 14) {
            return Lists.newArrayList(
                    "/system/framework/core.jar",
                    "/system/framework/apache-xml.jar",
                    "/system/framework/bouncycastle.jar",
                    "/system/framework/ext.jar",
                    "/system/framework/framework.jar",
                    "/system/framework/android.policy.jar",
                    "/system/framework/services.jar",
                    "/system/framework/core-junit.jar");
        } else if (apiLevel < 16) {
            return Lists.newArrayList(
                    "/system/framework/core.jar",
                    "/system/framework/core-junit.jar",
                    "/system/framework/bouncycastle.jar",
                    "/system/framework/ext.jar",
                    "/system/framework/framework.jar",
                    "/system/framework/android.policy.jar",
                    "/system/framework/services.jar",
                    "/system/framework/apache-xml.jar",
                    "/system/framework/filterfw.jar");
        } else if (apiLevel < 21) {
            // this is correct as of api 17/4.2.2
            return Lists.newArrayList(
                    "/system/framework/core.jar",
                    "/system/framework/core-junit.jar",
                    "/system/framework/bouncycastle.jar",
                    "/system/framework/ext.jar",
                    "/system/framework/framework.jar",
                    "/system/framework/telephony-common.jar",
                    "/system/framework/mms-common.jar",
                    "/system/framework/android.policy.jar",
                    "/system/framework/services.jar",
                    "/system/framework/apache-xml.jar");
        } else { // api >= 21
            // TODO: verify, add new ones?
            return Lists.newArrayList(
                    "/system/framework/core-libart.jar",
                    "/system/framework/conscrypt.jar",
                    "/system/framework/okhttp.jar",
                    "/system/framework/core-junit.jar",
                    "/system/framework/bouncycastle.jar",
                    "/system/framework/ext.jar",
                    "/system/framework/framework.jar",
                    "/system/framework/telephony-common.jar",
                    "/system/framework/voip-common.jar",
                    "/system/framework/ims-common.jar",
                    "/system/framework/mms-common.jar",
                    "/system/framework/android.policy.jar",
                    "/system/framework/apache-xml.jar");
        }
    }
}
