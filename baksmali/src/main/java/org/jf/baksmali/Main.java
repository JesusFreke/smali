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
import org.jf.baksmali.HelpCommand.HlepCommand;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Main {
    public static final String VERSION = loadVersion();

    @Parameter(names = {"-h", "-?", "--help"}, help = true,
            description = "Show usage information")
    private boolean help;

    @Parameter(names = {"-v", "--version"}, help = true,
            description = "Print the version of baksmali and then exit")
    public boolean version;

    public static void main(String[] args) {
        Main main = new Main();

        JCommander jc = new JCommander(main);

        jc.addCommand("disassemble", new DisassembleCommand(jc), "dis", "d");
        jc.addCommand("deodex", new DeodexCommand(jc), "de", "x");
        jc.addCommand("dump", new DumpCommand(jc), "du");
        jc.addCommand("help", new HelpCommand(jc), "h");
        jc.addCommand("hlep", new HlepCommand(jc));

        jc.parse(args);

        if (jc.getParsedCommand() == null || main.help) {
            jc.usage();
            return;
        }

        if (main.version) {
            version();
            return;
        }

        Command command = (Command)jc.getCommands().get(jc.getParsedCommand()).getObjects().get(0);
        command.run();
    }

    protected static void version() {
        System.out.println("baksmali " + VERSION + " (http://smali.googlecode.com)");
        System.out.println("Copyright (C) 2010 Ben Gruver (JesusFreke@JesusFreke.com)");
        System.out.println("BSD license (http://www.opensource.org/licenses/bsd-license.php)");
        System.exit(0);
    }

    private static String loadVersion() {
        InputStream propertiesStream = Baksmali.class.getClassLoader().getResourceAsStream("baksmali.properties");
        String version = "[unknown version]";
        if (propertiesStream != null) {
            Properties properties = new Properties();
            try {
                properties.load(propertiesStream);
                version = properties.getProperty("application.version");
            } catch (IOException ex) {
                // ignore
            }
        }
        return version;
    }
}
