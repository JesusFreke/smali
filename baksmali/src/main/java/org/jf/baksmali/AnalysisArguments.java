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

import com.beust.jcommander.Parameter;
import com.google.common.collect.Lists;
import org.jf.util.jcommander.CommaColonParameterSplitter;
import org.jf.util.jcommander.ExtendedParameter;

import java.util.List;

public class AnalysisArguments {
    @Parameter(names = {"-a", "--api"},
            description = "The numeric api level of the file being disassembled.")
    @ExtendedParameter(argumentNames = "api")
    public int apiLevel = 15;

    @Parameter(names = {"-b", "--bootclasspath", "--bcp"},
            description = "A comma/colon separated list of the jar/oat files to include in the " +
                    "bootclasspath when analyzing the dex file. If not specified, baksmali will attempt to choose an " +
                    "appropriate default. This is analogous to Android's BOOTCLASSPATH environment variable.",
            splitter = CommaColonParameterSplitter.class)
    @ExtendedParameter(argumentNames = "classpath")
    public List<String> bootClassPath = null;

    @Parameter(names = {"-c", "--classpath", "--cp"},
            description = "A comma/colon separated list of additional jar/oat files to include in the classpath " +
                    "when analyzing the dex file. These will be added to the classpath after any bootclasspath " +
                    "entries.",
            splitter = CommaColonParameterSplitter.class)
    @ExtendedParameter(argumentNames = "classpath")
    public List<String> classPath = Lists.newArrayList();

    @Parameter(names = {"-d", "--classpath-dir", "--cpd", "--dir"},
            description = "A directory to search for classpath files. This option can be used multiple times to " +
                    "specify multiple directories to search. They will be searched in the order they are provided.")
    @ExtendedParameter(argumentNames = "dir")
    public List<String> classPathDirectories = Lists.newArrayList(".");

    @Parameter(names = "--experimental",
            description = "Enable experimental opcodes to be disassembled, even if they aren't necessarily " +
                    "supported in the Android runtime yet.")
    public boolean experimentalOpcodes = false;

    public static class CheckPackagePrivateArgument {
        @Parameter(names = {"--check-package-private-access", "--package-private", "--checkpp", "--pp"},
                description = "Use the package-private access check when calculating vtable indexes. This should " +
                        "only be needed for 4.2.0 odexes. It was reverted in 4.2.1.")
        public boolean checkPackagePrivateAccess = false;
    }
}
