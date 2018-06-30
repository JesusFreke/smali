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
import com.beust.jcommander.ParametersDelegate;
import com.beust.jcommander.validators.PositiveInteger;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jf.dexlib2.util.SyntheticAccessorResolver;
import org.jf.util.ConsoleUtil;
import org.jf.util.StringWrapper;
import org.jf.util.jcommander.ExtendedParameter;
import org.jf.util.jcommander.ExtendedParameters;
import org.xml.sax.SAXException;

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

    @ParametersDelegate
    protected AnalysisArguments analysisArguments = new AnalysisArguments();

    @Parameter(names = {"--debug-info", "--di"}, arity = 1,
            description = "Whether to include debug information in the output (.local, .param, .line, etc.). True " +
                    "by default, use --debug-info=false to disable.")
    @ExtendedParameter(argumentNames = "boolean")
    private boolean debugInfo = true;

    @Parameter(names = {"--code-offsets", "--offsets", "--off"},
            description = "Add a comment before each instruction with it's code offset within the method.")
    private boolean codeOffsets = false;

    @Parameter(names = {"--resolve-resources", "--rr"}, arity = 2,
            description = "This will attempt to find any resource id references within the bytecode and add a " +
                    "comment with the name of the resource being referenced. The parameter accepts 2 values:" +
                    "an arbitrary resource prefix and the path to a public.xml file. For example: " +
                    "--resolve-resources android.R framework/res/values/public.xml. This option can be specified " +
                    "multiple times to provide resources from multiple packages.")
    @ExtendedParameter(argumentNames = {"resource prefix", "public.xml file"})
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

    @Parameter(names = {"--accessor-comments", "--ac"}, arity = 1,
            description = "Generate helper comments for synthetic accessors. True by default, use " +
                    "--accessor-comments=false to disable.")
    @ExtendedParameter(argumentNames = "boolean")
    private boolean accessorComments = true;

    @Parameter(names = {"--normalize-virtual-methods", "--norm", "--nvm"},
            description = "Normalize virtual method references to use the base class where the method is " +
                    "originally declared.")
    private boolean normalizeVirtualMethods = false;

    @Parameter(names = {"-o", "--output"},
            description = "The directory to write the disassembled files to.")
    @ExtendedParameter(argumentNames = "dir")
    private String outputDir = "out";

    @Parameter(names = {"--parameter-registers", "--preg", "--pr"}, arity = 1,
            description = "Use the pNN syntax for registers that refer to a method parameter on method entry. True " +
                    "by default, use --parameter-registers=false to disable.")
    @ExtendedParameter(argumentNames = "boolean")
    private boolean parameterRegisters = true;

    @Parameter(names = {"-r", "--register-info"},
            description = "Add comments before/after each instruction with information about register types. " +
                    "The value is a comma-separated list of any of ALL, ALLPRE, ALLPOST, ARGS, DEST, MERGE and " +
                    "FULLMERGE. See \"baksmali help register-info\" for more information.")
    @ExtendedParameter(argumentNames = "register info specifier")
    private List<String> registerInfoTypes = Lists.newArrayList();

    @Parameter(names = {"--sequential-labels", "--seq", "--sl"},
            description = "Create label names using a sequential numbering scheme per label type, rather than " +
                    "using the bytecode address.")
    private boolean sequentialLabels = false;

    @Parameter(names = {"--implicit-references", "--implicit", "--ir"},
            description = "Use implicit method and field references (without the class name) for methods and " +
                    "fields from the current class.")
    private boolean implicitReferences = false;

    @Parameter(names = "--allow-odex-opcodes",
            description = "Allows odex opcodes to be disassembled, even if the result won't be able to be reassembled.")
    private boolean allowOdex = false;

    @Parameter(names = "--classes",
            description = "A comma separated list of classes. Only disassemble these classes")
    @ExtendedParameter(argumentNames = "classes")
    private List<String> classes = null;

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
        loadDexFile(input);

        if (showDeodexWarning() && dexFile.hasOdexOpcodes()) {
            StringWrapper.printWrappedString(System.err,
                    "Warning: You are disassembling an odex/oat file without deodexing it. You won't be able to " +
                            "re-assemble the results unless you deodex it. See \"baksmali help deodex\"",
                    ConsoleUtil.getConsoleWidth());
        }

        File outputDirectoryFile = new File(outputDir);
        if (!outputDirectoryFile.exists()) {
            if (!outputDirectoryFile.mkdirs()) {
                System.err.println("Can't create the output directory " + outputDir);
                System.exit(-1);
            }
        }

        if (analysisArguments.classPathDirectories == null || analysisArguments.classPathDirectories.isEmpty()) {
            analysisArguments.classPathDirectories = Lists.newArrayList(inputFile.getAbsoluteFile().getParent());
        }

        if (!Baksmali.disassembleDexFile(dexFile, outputDirectoryFile, jobs, getOptions(), classes)) {
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

    protected BaksmaliOptions getOptions() {
        if (dexFile == null) {
            throw new IllegalStateException("You must call loadDexFile first");
        }

        final BaksmaliOptions options = new BaksmaliOptions();

        if (needsClassPath()) {
            try {
                options.classPath = analysisArguments.loadClassPathForDexFile(
                        inputFile.getAbsoluteFile().getParentFile(), dexFile, shouldCheckPackagePrivateAccess());
            } catch (Exception ex) {
                System.err.println("\n\nError occurred while loading class path files. Aborting.");
                ex.printStackTrace(System.err);
                System.exit(-1);
            }
        }

        if (!resourceIdFiles.isEmpty()) {
            Map<String, File> resourceFiles = Maps.newHashMap();

            assert (resourceIdFiles.size() % 2) == 0;
            for (int i=0; i<resourceIdFiles.size(); i+=2) {
                String resourcePrefix = resourceIdFiles.get(i);
                String publicXml = resourceIdFiles.get(i+1);

                File publicXmlFile = new File(publicXml);

                if (!publicXmlFile.exists()) {
                    System.err.println(String.format("Can't find file: %s", publicXmlFile));
                    System.exit(-1);
                }

                resourceFiles.put(resourcePrefix, publicXmlFile);
            }

            try {
                options.loadResourceIds(resourceFiles);
            } catch (IOException ex) {
                System.err.println("Error while loading resource files:");
                ex.printStackTrace(System.err);
                System.exit(-1);
            } catch (SAXException ex) {
                System.err.println("Error while loading resource files:");
                ex.printStackTrace(System.err);
                System.exit(-1);
            }
        }

        options.parameterRegisters = parameterRegisters;
        options.localsDirective = localsDirective;
        options.sequentialLabels = sequentialLabels;
        options.debugInfo = debugInfo;
        options.codeOffsets = codeOffsets;
        options.accessorComments = accessorComments;
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

        if (allowOdex) {
            options.allowOdex = true;
        }

        return options;
    }
}
