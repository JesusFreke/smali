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

import javax.annotation.Nonnull;

@Parameters(commandDescription = "Lists various objects in a dex file.")
public class ListCommand implements Command {

    @Nonnull private final JCommander jc;
    @Nonnull private JCommander subJc;

    @Parameter(names = {"-h", "-?", "--help"}, help = true,
            description = "Show usage information")
    private boolean help;

    public ListCommand(@Nonnull JCommander jc) {
        this.jc = jc;
    }

    public void registerSubCommands() {
        subJc = jc.getCommands().get("list");
        subJc.addCommand("strings", new ListStringsCommand(subJc), "string", "str", "s");
        subJc.addCommand("methods", new ListMethodsCommand(subJc), "method", "m");
        subJc.addCommand("fields", new ListFieldsCommand(subJc), "field", "f");
        subJc.addCommand("types", new ListTypesCommand(subJc), "type", "t");
        subJc.addCommand("classes", new ListClassesCommand(subJc), "class", "c");
        subJc.addCommand("dex", new ListDexCommand(subJc), "d");
        subJc.addCommand("vtables", new ListVtablesCommand(subJc), "vtable", "v");
        subJc.addCommand("fieldoffsets", new ListFieldOffsetsCommand(subJc), "fieldoffset", "fo");
        subJc.addCommand("classpath", new ListClassPathCommand(subJc), "bootclasspath", "cp", "bcp");
    }

    @Override public void run() {
        if (help || subJc.getParsedCommand() == null) {
            subJc.usage();
            return;
        }

        Command command = (Command)subJc.getCommands().get(subJc.getParsedCommand()).getObjects().get(0);
        command.run();
    }
}
