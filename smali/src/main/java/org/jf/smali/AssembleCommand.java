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

package org.jf.smali;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.validators.PositiveInteger;
import org.jf.util.jcommander.Command;
import org.jf.util.jcommander.ExtendedParameter;
import org.jf.util.jcommander.ExtendedParameters;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;

@Parameters(commandDescription = "Assembles smali files into a dex file.")
@ExtendedParameters(
        commandName = "assemble",
        commandAliases = { "ass", "as", "a" })
public class AssembleCommand extends Command {

    @Parameter(names = {"-h", "-?", "--help"}, help = true,
            description = "Show usage information for this command.")
    private boolean help;

    @Parameter(names = {"-j", "--jobs"},
            description = "The number of threads to use. Defaults to the number of cores available.",
            validateWith = PositiveInteger.class)
    @ExtendedParameter(argumentNames = "n")
    private int jobs = Runtime.getRuntime().availableProcessors();

    @Parameter(names = {"-a", "--api"},
            description = "The numeric api level to use while assembling.")
    @ExtendedParameter(argumentNames = "api")
    private int apiLevel = 15;

    @Parameter(names = {"-o", "--output"},
            description = "The name/path of the dex file to write.")
    @ExtendedParameter(argumentNames = "file")
    private String output = "out.dex";

    @Parameter(names = "--verbose",
            description = "Generate verbose error messages.")
    private boolean verbose = false;

    @Parameter(names = {"--allow-odex-opcodes", "--allow-odex", "--ao"},
            description = "Allows the odex opcodes that dalvik doesn't reject to be assembled.")
    private boolean allowOdexOpcodes;

    @Parameter(description = "Assembles the given files. If a directory is specified, it will be " +
            "recursively searched for any files with a .smali prefix")
    @ExtendedParameter(argumentNames = "[<file>|<dir>]+")
    private List<String> input;

    public AssembleCommand(@Nonnull List<JCommander> commandAncestors) {
        super(commandAncestors);
    }

    @Override public void run() {
        if (help || input == null || input.isEmpty()) {
            usage();
            return;
        }

        try {
            Smali.assemble(getOptions(), input);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    protected SmaliOptions getOptions() {
        SmaliOptions options = new SmaliOptions();

        options.jobs = jobs;
        options.apiLevel = apiLevel;
        options.outputDexFile = output;
        options.allowOdexOpcodes = allowOdexOpcodes;
        options.verboseErrors = verbose;

        return options;
    }
}
