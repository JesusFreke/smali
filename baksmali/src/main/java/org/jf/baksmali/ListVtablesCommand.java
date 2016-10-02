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
import com.beust.jcommander.ParametersDelegate;
import org.jf.baksmali.AnalysisArguments.CheckPackagePrivateArgument;
import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.analysis.ClassProto;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Method;
import org.jf.util.jcommander.ExtendedParameter;
import org.jf.util.jcommander.ExtendedParameters;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;

@Parameters(commandDescription = "Lists the virtual method tables for classes in a dex file.")
@ExtendedParameters(
        commandName = "vtables",
        commandAliases = { "vtable", "v" })
public class ListVtablesCommand extends DexInputCommand {

    @Parameter(names = {"-h", "-?", "--help"}, help = true,
            description = "Show usage information")
    private boolean help;

    @ParametersDelegate
    private AnalysisArguments analysisArguments = new AnalysisArguments();

    @ParametersDelegate
    private CheckPackagePrivateArgument checkPackagePrivateArgument = new CheckPackagePrivateArgument();

    @Parameter(names = "--classes",
            description = "A comma separated list of classes. Only print the vtable for these classes")
    @ExtendedParameter(argumentNames = "classes")
    private List<String> classes = null;

    @Parameter(names = "--override-oat-version",
            description = "Uses a classpath for the given oat version, regardless of the actual oat version. This " +
                    "can be used, e.g. to list vtables from a dex file, as if they were in an oat file of the given " +
                    "version.")
    private int oatVersion = 0;

    public ListVtablesCommand(@Nonnull List<JCommander> commandAncestors) {
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
        loadDexFile(input, Opcodes.getDefault());

        BaksmaliOptions options = getOptions();
        if (options == null) {
            return;
        }

        try {
            if (classes != null && !classes.isEmpty()) {
                for (String cls: classes) {
                    listClassVtable((ClassProto)options.classPath.getClass(cls));
                }
                return;
            }

            for (ClassDef classDef : dexFile.getClasses()) {
                if (!AccessFlags.INTERFACE.isSet(classDef.getAccessFlags())) {
                    listClassVtable((ClassProto)options.classPath.getClass(classDef));
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void listClassVtable(ClassProto classProto) throws IOException {
        List<Method> methods = classProto.getVtable();
        String className = "Class " + classProto.getType() + " extends " + classProto.getSuperclass() +
                " : " + methods.size() + " methods\n";
        System.out.write(className.getBytes());
        for (int i = 0; i < methods.size(); i++) {
            Method method = methods.get(i);

            String methodString = i + ":" + method.getDefiningClass() + "->" + method.getName() + "(";
            for (CharSequence parameter : method.getParameterTypes()) {
                methodString += parameter;
            }
            methodString += ")" + method.getReturnType() + "\n";
            System.out.write(methodString.getBytes());
        }
        System.out.write("\n".getBytes());
    }

    protected BaksmaliOptions getOptions() {
        if (dexFile == null) {
            throw new IllegalStateException("You must call loadDexFile first");
        }

        final BaksmaliOptions options = new BaksmaliOptions();

        options.apiLevel = analysisArguments.apiLevel;

        try {
            options.classPath = analysisArguments.loadClassPathForDexFile(inputFile.getAbsoluteFile().getParentFile(),
                    dexFile, checkPackagePrivateArgument.checkPackagePrivateAccess, oatVersion);
        } catch (Exception ex) {
            System.err.println("Error occurred while loading class path files.");
            ex.printStackTrace(System.err);
            return null;
        }

        return options;
    }
}
