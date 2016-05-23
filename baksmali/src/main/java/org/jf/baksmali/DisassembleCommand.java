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
import com.beust.jcommander.validators.PositiveInteger;
import org.jf.dexlib2.DexFileFactory;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jf.dexlib2.analysis.ClassPath;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.OatFile;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.util.SyntheticAccessorResolver;
import org.jf.util.StringWrapper;
import org.jf.util.jcommander.CommaColonParameterSplitter;
import org.jf.util.jcommander.ExtendedParameter;
import org.jf.util.jcommander.ExtendedParameters;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Parameters(commandDescription = "Disassembles a dex file.")
@ExtendedParameters(
        commandName = "disassemble",
        commandAliases = { "dis", "d" })
public class DisassembleCommand extends DexInputCommand {

    @Parameter(names = {"-h", "-?", "--help"}, help = true,
            description = "Show usage information for this command.")
    private boolean help;

    @Parameter(names = {"-a", "--api"},
            description = "The numeric api level of the file being disassembled.")
    @ExtendedParameter(argumentNames = "api")
    private int apiLevel = 15;

    @Parameter(names = "--debug-info", arity = 1,
            description = "Whether to include debug information in the output (.local, .param, .line, etc.). Use " +
                    "--debug-info=false to disable.")
    @ExtendedParameter(argumentNames = "boolean")
    private boolean debugInfo = true;

    @Parameter(names = {"-b", "--bootclasspath"},
            description = "A comma/colon separated list of the jar/oat files to include in the " +
                    "bootclasspath when analyzing the dex file. If not specified, baksmali will attempt to choose an " +
                    "appropriate default. This is analogous to Android's BOOTCLASSPATH environment variable.",
            splitter = CommaColonParameterSplitter.class)
    @ExtendedParameter(argumentNames = "classpath")
    private List<String> bootClassPath = null;

    @Parameter(names = {"-c", "--classpath"},
            description = "A comma/colon separated list of additional jar/oat files to include in the classpath " +
                    "when analyzing the dex file. These will be added to the classpath after any bootclasspath " +
                    "entries.",
            splitter = CommaColonParameterSplitter.class)
    @ExtendedParameter(argumentNames = "classpath")
    private List<String> classPath = Lists.newArrayList();

    @Parameter(names = {"-d", "--classpath-dir"},
            description = "A directory to search for classpath files. This option can be used multiple times to " +
                    "specify multiple directories to search. They will be searched in the order they are provided.")
    @ExtendedParameter(argumentNames = "dirs")
    private List<String> classPathDirectories = Lists.newArrayList(".");

    @Parameter(names = {"--code-offsets"},
            description = "Add a comment before each instruction with it's code offset within the method.")
    private boolean codeOffsets = false;

    @Parameter(names = "--resolve-resources", arity=1,
            description = "This will attempt to find any resource id references within the bytecode and add a " +
                    "comment with the name of the resource being referenced. The value should be a comma/colon" +
                    "separated list of prefix=file pairs. For example R=res/values/public.xml:android.R=" +
                    "$ANDROID_HOME/platforms/android-19/data/res/values/public.xml")
    @ExtendedParameter(argumentNames = "resource spec")
    private List<String> resourceIdFiles = Lists.newArrayList();

    @Parameter(names = {"-j", "--jobs"},
            description = "The number of threads to use. Defaults to the number of cores available.",
            validateWith = PositiveInteger.class)
    @ExtendedParameter(argumentNames = "n")
    private int jobs = Runtime.getRuntime().availableProcessors();

    @Parameter(names = {"-l", "--use-locals"},
            description = "When disassembling, output the .locals directive with the number of non-parameter " +
                    "registers instead of the .registers directive with the total number of registers.")
    private boolean localsDirective = false;

    @Parameter(names = "--accessor-comments", arity = 1,
            description = "Generate helper comments for synthetic accessors. Use --accessor-comments=false to " +
                    "disable.")
    @ExtendedParameter(argumentNames = "boolean")
    private boolean accessorComments = true;

    @Parameter(names = "--normalize-virtual-methods",
            description = "Normalize virtual method references to use the base class where the method is " +
                    "originally declared.")
    private boolean normalizeVirtualMethods = false;

    @Parameter(names = {"-o", "--output"},
            description = "The directory to write the disassembled files to.")
    @ExtendedParameter(argumentNames = "dir")
    private String outputDir = "out";

    @Parameter(names = "--parameter-registers", arity = 1,
            description = "Use the pNN syntax for registers that refer to a method parameter on method entry. Use" +
                    "--parameter-registers=false to disable.")
    @ExtendedParameter(argumentNames = "boolean")
    private boolean parameterRegisters = true;

    @Parameter(names = {"-r", "--register-info"}, arity=1,
            description = "Add comments before/after each instruction with information about register types. " +
                    "The value is a comma-separated list of any of ALL, ALLPRE, ALLPOST, ARGS, DEST, MERGE and " +
                    "FULLMERGE. See \"baksmali help register-info\" for more information.")
    @ExtendedParameter(argumentNames = "register info specifier")
    private List<String> registerInfoTypes = Lists.newArrayList();

    @Parameter(names = "--sequential-labels",
            description = "Create label names using a sequential numbering scheme per label type, rather than " +
                    "using the bytecode address.")
    private boolean sequentialLabels = false;

    @Parameter(names = "--implicit-references",
            description = "Use implicit method and field references (without the class name) for methods and " +
                    "fields from the current class.")
    private boolean implicitReferences = false;

    @Parameter(names = "--experimental",
            description = "Enable experimental opcodes to be disassembled, even if they aren't necessarily " +
                    "supported in the Android runtime yet.")
    private boolean experimentalOpcodes = false;

    @Parameter(description = "A dex/apk/oat/odex file. For apk or oat files that contain multiple dex " +
            "files, you can specify which dex file to disassemble by appending the name of the dex file with a " +
            "colon. E.g. \"something.apk:classes2.dex\"")
    @ExtendedParameter(argumentNames = "file")
    private List<String> inputList = Lists.newArrayList();

    public DisassembleCommand(@Nonnull List<JCommander> commandAncestors) {
        super(commandAncestors);
    }

    public void run() {
        if (help || inputList == null || inputList.isEmpty()) {
            usage();
            return;
        }

        if (inputList.size() > 1) {
            System.err.println("Too many files specified");
            usage();
            return;
        }

        String input = inputList.get(0);
        File dexFileFile = new File(input);
        String dexFileEntry = null;
        if (!dexFileFile.exists()) {
            String filename = dexFileFile.getName();
            int colonIndex = filename.indexOf(':');

            if (colonIndex >= 0) {
                dexFileFile = new File(dexFileFile.getParent(), filename.substring(0, colonIndex));
                dexFileEntry = filename.substring(colonIndex + 1);
            }

            if (!dexFileFile.exists()) {
                System.err.println("Can't find the file " + input);
                System.exit(1);
            }
        }

        //Read in and parse the dex file
        DexBackedDexFile dexFile = null;
        try {
            dexFile = DexFileFactory.loadDexFile(dexFileFile, dexFileEntry, apiLevel, experimentalOpcodes);
        } catch (DexFileFactory.MultipleDexFilesException ex) {
            System.err.println(String.format("%s contains multiple dex files. You must specify which one to " +
                    "disassemble with the -e option", dexFileFile.getName()));
            System.err.println("Valid entries include:");

            for (OatFile.OatDexFile oatDexFile : ex.oatFile.getDexFiles()) {
                System.err.println(oatDexFile.filename);
            }
            System.exit(1);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        if (showDeodexWarning() && dexFile.hasOdexOpcodes()) {
            StringWrapper.printWrappedString(System.err,
                    "Warning: You are disassembling an odex/oat file without deodexing it. You won't be able to " +
                            "re-assemble the results unless you deodex it. See \"baksmali help deodex\"");
        }

        File outputDirectoryFile = new File(outputDir);
        if (!outputDirectoryFile.exists()) {
            if (!outputDirectoryFile.mkdirs()) {
                System.err.println("Can't create the output directory " + outputDir);
                System.exit(-1);
            }
        }

         if (!Baksmali.disassembleDexFile(dexFile, outputDirectoryFile, jobs, getOptions(dexFile))) {
             System.exit(-1);
         }
    }


    protected boolean needsClassPath() {
        return !registerInfoTypes.isEmpty() || normalizeVirtualMethods;
    }

    protected boolean shouldCheckPackagePrivateAccess() {
        return false;
    }

    protected boolean showDeodexWarning() {
        return true;
    }

    protected BaksmaliOptions getOptions(DexFile dexFile) {
        final BaksmaliOptions options = new BaksmaliOptions();

        if (needsClassPath()) {
            try {
                options.classPath = ClassPath.loadClassPath(classPathDirectories,
                        bootClassPath, classPath, dexFile, apiLevel, shouldCheckPackagePrivateAccess(),
                        experimentalOpcodes);
            } catch (Exception ex) {
                System.err.println("\n\nError occurred while loading class path files. Aborting.");
                ex.printStackTrace(System.err);
                System.exit(-1);
            }
        }

        if (!resourceIdFiles.isEmpty()) {
            Map<String, File> resourceFiles = Maps.newHashMap();

            for (String resourceIdFileSpec: resourceIdFiles) {
                int separatorIndex = resourceIdFileSpec.indexOf('=');
                if (separatorIndex == -1) {
                    System.err.println(String.format("Invalid resource id spec: %s", resourceIdFileSpec));
                    usage();
                    System.exit(-1);
                }
                String prefix = resourceIdFileSpec.substring(0, separatorIndex);
                String resourceIdFilePath = resourceIdFileSpec.substring(separatorIndex+1);
                File resourceIdFile = new File(resourceIdFilePath);

                if (!resourceIdFile.exists()) {
                    System.err.println(String.format("Can't find file: %s", resourceIdFilePath));
                    System.exit(-1);
                }

                resourceFiles.put(prefix, resourceIdFile);
            }

            options.loadResourceIds(resourceFiles);
        }

        options.parameterRegisters = parameterRegisters;
        options.localsDirective = localsDirective;
        options.sequentialLabels = sequentialLabels;
        options.debugInfo = debugInfo;
        options.codeOffsets = codeOffsets;
        options.accessorComments = accessorComments;
        options.experimentalOpcodes = experimentalOpcodes;
        options.implicitReferences = implicitReferences;
        options.normalizeVirtualMethods = normalizeVirtualMethods;

        options.registerInfo = 0;

        for (String registerInfoType: registerInfoTypes) {
            if (registerInfoType.equalsIgnoreCase("ALL")) {
                options.registerInfo  |= BaksmaliOptions.ALL;
            } else if (registerInfoType.equalsIgnoreCase("ALLPRE")) {
                options.registerInfo  |= BaksmaliOptions.ALLPRE;
            } else if (registerInfoType.equalsIgnoreCase("ALLPOST")) {
                options.registerInfo  |= BaksmaliOptions.ALLPOST;
            } else if (registerInfoType.equalsIgnoreCase("ARGS")) {
                options.registerInfo  |= BaksmaliOptions.ARGS;
            } else if (registerInfoType.equalsIgnoreCase("DEST")) {
                options.registerInfo  |= BaksmaliOptions.DEST;
            } else if (registerInfoType.equalsIgnoreCase("MERGE")) {
                options.registerInfo  |= BaksmaliOptions.MERGE;
            } else if (registerInfoType.equalsIgnoreCase("FULLMERGE")) {
                options.registerInfo  |= BaksmaliOptions.FULLMERGE;
            } else {
                System.err.println(String.format("Invalid register info type: %s", registerInfoType));
                usage();
                System.exit(-1);
            }

            if ((options.registerInfo & BaksmaliOptions.FULLMERGE) != 0) {
                options.registerInfo &= ~BaksmaliOptions.MERGE;
            }
        }

        if (accessorComments) {
            options.syntheticAccessorResolver = new SyntheticAccessorResolver(dexFile.getOpcodes(),
                    dexFile.getClasses());
        }

        return options;
    }
}
