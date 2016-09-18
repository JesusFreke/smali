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
import org.jf.baksmali.ListHelpCommand.ListHlepCommand;
import org.jf.util.jcommander.Command;
import org.jf.util.jcommander.ExtendedCommands;
import org.jf.util.jcommander.ExtendedParameters;

import javax.annotation.Nonnull;
import java.util.List;

@Parameters(commandDescription = "Lists various objects in a dex file.")
@ExtendedParameters(
        commandName = "list",
        commandAliases = "l")
public class ListCommand extends Command {

    @Parameter(names = {"-h", "-?", "--help"}, help = true,
            description = "Show usage information")
    private boolean help;

    public ListCommand(@Nonnull List<JCommander> commandAncestors) {
        super(commandAncestors);
    }

    @Override protected void setupCommand(JCommander jc) {
        List<JCommander> hierarchy = getCommandHierarchy();

        ExtendedCommands.addExtendedCommand(jc, new ListStringsCommand(hierarchy));
        ExtendedCommands.addExtendedCommand(jc, new ListMethodsCommand(hierarchy));
        ExtendedCommands.addExtendedCommand(jc, new ListFieldsCommand(hierarchy));
        ExtendedCommands.addExtendedCommand(jc, new ListTypesCommand(hierarchy));
        ExtendedCommands.addExtendedCommand(jc, new ListClassesCommand(hierarchy));
        ExtendedCommands.addExtendedCommand(jc, new ListDexCommand(hierarchy));
        ExtendedCommands.addExtendedCommand(jc, new ListVtablesCommand(hierarchy));
        ExtendedCommands.addExtendedCommand(jc, new ListFieldOffsetsCommand(hierarchy));
        ExtendedCommands.addExtendedCommand(jc, new ListDependenciesCommand(hierarchy));
        ExtendedCommands.addExtendedCommand(jc, new ListHelpCommand(hierarchy));
        ExtendedCommands.addExtendedCommand(jc, new ListHlepCommand(hierarchy));
    }

    @Override public void run() {
        JCommander jc = getJCommander();
        if (help || jc.getParsedCommand() == null) {
            usage();
            return;
        }

        Command command = (Command)jc.getCommands().get(jc.getParsedCommand()).getObjects().get(0);
        command.run();
    }
}
