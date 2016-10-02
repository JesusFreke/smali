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
import com.google.common.collect.Iterables;
import org.jf.util.ConsoleUtil;
import org.jf.util.jcommander.*;

import javax.annotation.Nonnull;
import java.util.List;

@Parameters(commandDescription = "Shows usage information")
@ExtendedParameters(
        commandName = "help",
        commandAliases = "h")
public class ListHelpCommand extends Command {

    @Parameter(description = "If specified, show the detailed usage information for the given commands")
    @ExtendedParameter(argumentNames = "commands")
    private List<String> commands;

    public ListHelpCommand(@Nonnull List<JCommander> commandAncestors) {
        super(commandAncestors);
    }

    public void run() {
        if (commands == null || commands.isEmpty()) {
            System.out.println(new HelpFormatter()
                    .width(ConsoleUtil.getConsoleWidth())
                    .format(commandAncestors));
        } else {
            boolean printedHelp = false;
            JCommander parentJc = Iterables.getLast(commandAncestors);
            for (String cmd : commands) {
                JCommander command = ExtendedCommands.getSubcommand(parentJc, cmd);
                if (command == null) {
                    System.err.println("No such command: " + cmd);
                } else {
                    printedHelp = true;
                    System.out.println(new HelpFormatter()
                            .width(ConsoleUtil.getConsoleWidth())
                            .format(((Command)command.getObjects().get(0)).getCommandHierarchy()));
                }
            }
            if (!printedHelp) {
                System.out.println(new HelpFormatter()
                        .width(ConsoleUtil.getConsoleWidth())
                        .format(commandAncestors));
            }
        }
    }

    @Parameters(hidden =  true)
    @ExtendedParameters(commandName = "hlep")
    public static class ListHlepCommand extends ListHelpCommand {
        public ListHlepCommand(@Nonnull List<JCommander> commandAncestors) {
            super(commandAncestors);
        }
    }
}
