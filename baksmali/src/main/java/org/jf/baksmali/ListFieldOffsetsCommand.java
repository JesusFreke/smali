/*
 * Copyright 2016, Google Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 * Neither the name of Google Inc. nor the names of its
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
import com.google.common.collect.Lists;
import org.jf.dexlib2.analysis.ClassPath;
import org.jf.dexlib2.analysis.ClassProto;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.util.SparseArray;
import org.jf.util.jcommander.CommaColonParameterSplitter;
import org.jf.util.jcommander.ExtendedParameter;
import org.jf.util.jcommander.ExtendedParameters;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Parameters(commandDescription = "Lists the instance field offsets for classes in a dex file.")
@ExtendedParameters(
        commandName = "fieldoffsets",
        commandAliases = { "fieldoffset", "fo" })
public class ListFieldOffsetsCommand extends DexInputCommand {

    @Parameter(names = {"-h", "-?", "--help"}, help = true,
            description = "Show usage information")
    private boolean help;

    @Parameter(names = {"-a", "--api"},
            description = "The numeric api level of the file being disassembled.")
    @ExtendedParameter(argumentNames = "api")
    private int apiLevel = 15;

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
    private List<String> classPath = new ArrayList<String>();

    @Parameter(names = {"-d", "--classpath-dir"},
            description = "A directory to search for classpath files. This option can be used multiple times to " +
                    "specify multiple directories to search. They will be searched in the order they are provided.")
    @ExtendedParameter(argumentNames = "dirs")
    private List<String> classPathDirectories = Lists.newArrayList(".");

    @Parameter(names = "--check-package-private-access",
            description = "Use the package-private access check when calculating vtable indexes. This should " +
                    "only be needed for 4.2.0 odexes. It was reverted in 4.2.1.")
    private boolean checkPackagePrivateAccess = false;

    @Parameter(names = "--experimental",
            description = "Enable experimental opcodes to be disassembled, even if they aren't necessarily " +
                    "supported in the Android runtime yet.")
    private boolean experimentalOpcodes = false;

    public ListFieldOffsetsCommand(@Nonnull List<JCommander> commandAncestors) {
        super(commandAncestors);
    }

    @Override public void run() {
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
        DexBackedDexFile dexFile = loadDexFile(input, 15, false);
        BaksmaliOptions options = getOptions(dexFile);

        try {
            for (ClassDef classDef: dexFile.getClasses()) {
                ClassProto classProto = (ClassProto) options.classPath.getClass(classDef);
                SparseArray<FieldReference> fields = classProto.getInstanceFields();
                String className = "Class "  + classDef.getType() + " : " + fields.size() + " instance fields\n";
                System.out.write(className.getBytes());
                for (int i=0;i<fields.size();i++) {
                    String field = fields.keyAt(i) + ":" + fields.valueAt(i).getType() + " " + fields.valueAt(i).getName() + "\n";
                    System.out.write(field.getBytes());
                }
                System.out.write("\n".getBytes());
            }
            System.out.close();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Nonnull
    private BaksmaliOptions getOptions(DexFile dexFile) {
        final BaksmaliOptions options = new BaksmaliOptions();

        options.apiLevel = apiLevel;

        try {
            options.classPath = ClassPath.loadClassPath(classPathDirectories,
                    bootClassPath, classPath, dexFile, apiLevel, checkPackagePrivateAccess, experimentalOpcodes);
        } catch (Exception ex) {
            System.err.println("Error occurred while loading class path files.");
            ex.printStackTrace(System.err);
            System.exit(-1);
        }

        options.experimentalOpcodes = experimentalOpcodes;

        return options;
    }
}
