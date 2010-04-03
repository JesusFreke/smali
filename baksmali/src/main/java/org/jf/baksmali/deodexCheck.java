/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver (JesusFreke)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.baksmali;

import org.apache.commons.cli.*;
import org.jf.dexlib.Code.Analysis.ClassPath;

import java.util.ArrayList;
import java.util.List;

public class deodexCheck {
    public static void main(String[] args) {
        CommandLineParser parser = new PosixParser();
        CommandLine commandLine;

        Options options = buildOptions();

        try {
            commandLine = parser.parse(options, args);
        } catch (ParseException ex) {
            usage(options);
            return;
        }

        String bootClassPath = "core.jar:ext.jar:framework.jar:android.policy.jar:services.jar";
        List<String> bootClassPathDirs = new ArrayList<String>();
        bootClassPathDirs.add(".");
        String deodexerantHost = null;
        int deodexerantPort = 0;
        int classStartIndex = 0;


        String[] remainingArgs = commandLine.getArgs();


        if (commandLine.hasOption("v")) {
            main.version();
            return;
        }

        if (commandLine.hasOption("?")) {
            usage(options);
            return;
        }

        if (remainingArgs.length > 0) {
            usage(options);
            return;
        }


        if (commandLine.hasOption("c")) {
            String bcp = commandLine.getOptionValue("c");
            if (bcp.charAt(0) == ':') {
                bootClassPath = bootClassPath + bcp;
            } else {
                bootClassPath = bcp;
            }
        }

        if (commandLine.hasOption("i")) {
            try {
                classStartIndex = Integer.parseInt(commandLine.getOptionValue("i"));
            } catch (Exception ex) {
            }
        }

        if (commandLine.hasOption("d")) {
            bootClassPathDirs.add(commandLine.getOptionValue("d"));
        }

        if (commandLine.hasOption("x")) {
            String deodexerantAddress = commandLine.getOptionValue("x");
            String[] parts = deodexerantAddress.split(":");
            if (parts.length != 2) {
               System.err.println("Invalid deodexerant address. Expecting :<port> or <host>:<port>");
               System.exit(1);
            }

            deodexerantHost = parts[0];
            if (deodexerantHost.length() == 0) {
               deodexerantHost = "localhost";
            }
            try {
               deodexerantPort = Integer.parseInt(parts[1]);
            } catch (NumberFormatException ex) {
               System.err.println("Invalid port \"" + deodexerantPort + "\" for deodexerant address");
               System.exit(1);
            }
        }

        String[] bootClassPathDirsArray = new String[bootClassPathDirs.size()];
        for (int i=0; i<bootClassPathDirsArray.length; i++) {
            bootClassPathDirsArray[i] = bootClassPathDirs.get(i);
        }

        ClassPath.InitializeClassPath(bootClassPathDirsArray, bootClassPath==null?null:bootClassPath.split(":"), null,
                null, null, null);

        ClassPath.validateAgainstDeodexerant(deodexerantHost, deodexerantPort, classStartIndex);
    }

    /**
     * Prints the usage message.
     */
    private static void usage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(100);
        formatter.printHelp("java -classpath baksmali.jar deodexCheck -x HOST:PORT [options]",
                "disassembles and/or dumps a dex file", options, "");
    }

    private static Options buildOptions() {
        Options options = new Options();

        Option versionOption = OptionBuilder.withLongOpt("version")
                .withDescription("prints the version then exits")
                .create("v");

        Option helpOption = OptionBuilder.withLongOpt("help")
                .withDescription("prints the help message then exits")
                .create("?");

        Option classPathOption = OptionBuilder.withLongOpt("bootclasspath")
                .withDescription("the bootclasspath jars to use, for analysis. Defaults to " +
                        "core.jar:ext.jar:framework.jar:android.policy.jar:services.jar. If you specify a value that " +
                        "begins with a :, it will be appended to the default bootclasspath")
                .hasOptionalArg()
                .withArgName("BOOTCLASSPATH")
                .create("c");

        Option classPathDirOption = OptionBuilder.withLongOpt("bootclasspath-dir")
                .withDescription("the base folder to look for the bootclasspath files in. Defaults to the current " +
                        "directory.")
                .hasArg()
                .withArgName("DIR")
                .create("d");

        Option deodexerantOption = OptionBuilder.withLongOpt("deodexerant")
                .isRequired()
                .withDescription("connect to deodexerant on the specified HOST:PORT, and validate the virtual method " +
                        "indexes, field offsets and inline methods against what dexlib calculates")
                .hasArg()
                .withArgName("HOST:PORT")
                .create("x");

        Option classStartOption = OptionBuilder.withLongOpt("class-start-index")
                .withDescription("Start checking classes at the given class index")
                .hasArg()
                .withArgName("CLASSINDEX")
                .create("i");

        options.addOption(versionOption);
        options.addOption(helpOption);
        options.addOption(deodexerantOption);
        options.addOption(classPathOption);
        options.addOption(classPathDirOption);
        options.addOption(classStartOption);

        return options;
    }
}
