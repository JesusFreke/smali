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
import org.jf.dexlib2.analysis.CustomInlineMethodResolver;
import org.jf.dexlib2.analysis.InlineMethodResolver;
import org.jf.dexlib2.dexbacked.DexBackedOdexFile;
import org.jf.dexlib2.iface.DexFile;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;

@Parameters(commandDescription = "Deodexes an odex/oat file")
public class DeodexCommand extends DisassembleCommand {
    @Parameter(names = "--check-package-private-access",
            description = "Use the package-private access check when calculating vtable indexes. This should " +
                    "only be needed for 4.2.0 odexes. It was reverted in 4.2.1.")
    private boolean checkPackagePrivateAccess = false;

    @Parameter(names = "--inline-table",
            description = "Specify a file containing a custom inline method table to use. See the " +
                    "\"deodexerant\" tool in the smali github repository to dump the inline method table from a " +
                    "device that uses dalvik.")
    private String inlineTable;

    public DeodexCommand(@Nonnull JCommander jc) {
        super(jc);
    }

    @Override protected BaksmaliOptions getOptions(DexFile dexFile) {
        BaksmaliOptions options = super.getOptions(dexFile);

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
        return checkPackagePrivateAccess;
    }

    @Override protected boolean needsClassPath() {
        return true;
    }

    @Override protected boolean showDeodexWarning() {
        return false;
    }
}
