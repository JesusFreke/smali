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
import org.jf.baksmali.AnalysisArguments.CheckPackagePrivateArgument;
import org.jf.dexlib2.analysis.CustomInlineMethodResolver;
import org.jf.dexlib2.analysis.InlineMethodResolver;
import org.jf.dexlib2.dexbacked.DexBackedOdexFile;
import org.jf.util.jcommander.ExtendedParameter;
import org.jf.util.jcommander.ExtendedParameters;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.List;

@Parameters(commandDescription = "Deodexes an odex/oat file")
@ExtendedParameters(
        commandName = "deodex",
        commandAliases = { "de", "x" })
public class DeodexCommand extends DisassembleCommand {

    @ParametersDelegate
    protected CheckPackagePrivateArgument checkPackagePrivateArgument = new CheckPackagePrivateArgument();

    @Parameter(names = {"--inline-table", "--inline", "--it"},
            description = "Specify a file containing a custom inline method table to use. See the " +
                    "\"deodexerant\" tool in the smali github repository to dump the inline method table from a " +
                    "device that uses dalvik.")
    @ExtendedParameter(argumentNames = "file")
    private String inlineTable;

    public DeodexCommand(@Nonnull List<JCommander> commandAncestors) {
        super(commandAncestors);
    }

    @Override protected BaksmaliOptions getOptions() {
        BaksmaliOptions options = super.getOptions();

        options.deodex = true;

        if (dexFile instanceof DexBackedOdexFile) {
            if (inlineTable == null) {
                options.inlineResolver = InlineMethodResolver.createInlineMethodResolver(
                        ((DexBackedOdexFile)dexFile).getOdexVersion());
            } else {
                File inlineTableFile = new File(inlineTable);
                if (!inlineTableFile.exists()) {
                    System.err.println(String.format("Could not find file: %s", inlineTable));
                    System.exit(-1);
                }
                try {
                    options.inlineResolver = new CustomInlineMethodResolver(options.classPath, inlineTableFile);
                } catch (IOException ex) {
                    System.err.println(String.format("Error while reading file: %s", inlineTableFile));
                    ex.printStackTrace(System.err);
                    System.exit(-1);
                }
            }
        }

        return options;
    }

    @Override protected boolean shouldCheckPackagePrivateAccess() {
        return checkPackagePrivateArgument.checkPackagePrivateAccess;
    }

    @Override protected boolean needsClassPath() {
        return true;
    }

    @Override protected boolean showDeodexWarning() {
        return false;
    }
}
