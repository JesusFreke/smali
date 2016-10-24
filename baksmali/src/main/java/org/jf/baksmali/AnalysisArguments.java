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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.jf.dexlib2.analysis.ClassPath;
import org.jf.dexlib2.analysis.ClassPathResolver;
import org.jf.dexlib2.dexbacked.OatFile.OatDexFile;
import org.jf.dexlib2.iface.DexFile;
import org.jf.util.jcommander.ColonParameterSplitter;
import org.jf.util.jcommander.ExtendedParameter;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.jf.dexlib2.analysis.ClassPath.NOT_ART;

public class AnalysisArguments {
    @Parameter(names = {"-b", "--bootclasspath", "--bcp"},
            description = "A colon separated list of the files to include in the bootclasspath when analyzing the " +
                    "dex file. If not specified, baksmali will attempt to choose an " +
                    "appropriate default. When analyzing oat files, this can simply be the path to the device's " +
                    "boot.oat file. A single empty string can be used to specify that an empty bootclasspath should " +
                    "be used. (e.g. --bootclasspath \"\") See baksmali help classpath for more information.",
            splitter = ColonParameterSplitter.class)
    @ExtendedParameter(argumentNames = "classpath")
    public List<String> bootClassPath = null;

    @Parameter(names = {"-c", "--classpath", "--cp"},
            description = "A colon separated list of additional files to include in the classpath when analyzing the " +
                    "dex file. These will be added to the classpath after any bootclasspath entries.",
            splitter = ColonParameterSplitter.class)
    @ExtendedParameter(argumentNames = "classpath")
    public List<String> classPath = Lists.newArrayList();

    @Parameter(names = {"-d", "--classpath-dir", "--cpd", "--dir"},
            description = "A directory to search for classpath files. This option can be used multiple times to " +
                    "specify multiple directories to search. They will be searched in the order they are provided.")
    @ExtendedParameter(argumentNames = "dir")
    public List<String> classPathDirectories = null;

    public static class CheckPackagePrivateArgument {
        @Parameter(names = {"--check-package-private-access", "--package-private", "--checkpp", "--pp"},
                description = "Use the package-private access check when calculating vtable indexes. This is enabled " +
                        "by default for oat files. For odex files, this is only needed for odexes from 4.2.0. It " +
                        "was reverted in 4.2.1.")
        public boolean checkPackagePrivateAccess = false;
    }

    @Nonnull
    public ClassPath loadClassPathForDexFile(@Nonnull File dexFileDir, @Nonnull DexFile dexFile,
                                             boolean checkPackagePrivateAccess) throws IOException {
        return loadClassPathForDexFile(dexFileDir, dexFile, checkPackagePrivateAccess, NOT_ART);
    }

    @Nonnull
    public ClassPath loadClassPathForDexFile(@Nonnull File dexFileDir, @Nonnull DexFile dexFile,
                                             boolean checkPackagePrivateAccess, int oatVersion)
            throws IOException {
        ClassPathResolver resolver;

        // By default, oatVersion should be NOT_ART, and we'll automatically set it if dexFile is an oat file. In some
        // cases the caller may choose to override the oat version, in which case we should use the given oat version
        // regardless of the actual version of the oat file
        if (oatVersion == NOT_ART) {
            if (dexFile instanceof OatDexFile) {
                checkPackagePrivateAccess = true;
                oatVersion = ((OatDexFile)dexFile).getContainer().getOatVersion();
            }
        } else {
            // this should always be true for ART
            checkPackagePrivateAccess = true;
        }

        if (classPathDirectories == null || classPathDirectories.size() == 0) {
            classPathDirectories = Lists.newArrayList(dexFileDir.getPath());
        }

        List<String> filteredClassPathDirectories = Lists.newArrayList();
        if (classPathDirectories != null) {
            for (String dir: classPathDirectories) {
                File file = new File(dir);
                if (!file.exists()) {
                    System.err.println(String.format("Warning: directory %s does not exist. Ignoring.", dir));
                } else if (!file.isDirectory()) {
                    System.err.println(String.format("Warning: %s is not a directory. Ignoring.", dir));
                } else {
                    filteredClassPathDirectories.add(dir);
                }
            }
        }

        if (bootClassPath == null) {
            // TODO: we should be able to get the api from the Opcodes object associated with the dexFile..
            // except that the oat version -> api mapping doesn't fully work yet
            resolver = new ClassPathResolver(filteredClassPathDirectories, classPath, dexFile);
        }  else if (bootClassPath.size() == 1 && bootClassPath.get(0).length() == 0) {
            // --bootclasspath "" is a special case, denoting that no bootclasspath should be used
            resolver = new ClassPathResolver(
                    ImmutableList.<String>of(), ImmutableList.<String>of(), classPath, dexFile);
        } else {
            resolver = new ClassPathResolver(filteredClassPathDirectories, bootClassPath, classPath, dexFile);
        }

        if (oatVersion == 0 && dexFile instanceof OatDexFile) {
            oatVersion = ((OatDexFile)dexFile).getContainer().getOatVersion();
        }
        return new ClassPath(resolver.getResolvedClassProviders(), checkPackagePrivateAccess, oatVersion);
    }
}
