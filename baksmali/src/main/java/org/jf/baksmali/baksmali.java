/*
 * [The "BSD licence"]
 * Copyright (c) 2009 Ben Gruver
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

import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.jf.baksmali.Adaptors.ClassDefinition;
import org.jf.baksmali.Renderers.*;
import org.jf.dexlib.DexFile;
import org.jf.dexlib.ClassDefItem;
import org.apache.commons.cli.*;

import java.io.*;
import java.net.URL;

public class baksmali {
    public static void main(String[] args) throws Exception
    {
        Options options = new Options();

        Option helpOption = OptionBuilder.withLongOpt("help")
                                         .withDescription("prints the usage information for the -dis command")
                                         .create("?");

        Option outputDirOption = OptionBuilder.withLongOpt("output")
                                              .withDescription("the directory where the disassembled files will be placed. The default is out")
                                              .hasArg()
                                              .withArgName("DIR")
                                              .create("out");

        options.addOption(helpOption);
        options.addOption(outputDirOption);

        CommandLineParser parser = new PosixParser();
        CommandLine commandLine;

        try {
            commandLine = parser.parse(options, args);
        } catch (ParseException ex) {
            printHelp(options);
            return;
        }

        if (commandLine.hasOption("?")) {
            printHelp(options);
            return;
        }
        

        String[] leftover = commandLine.getArgs();

        if (leftover.length != 1) {
            printHelp(options);
            return;
        }

        String dexFileName = leftover[0];
        String outputDirName = commandLine.getOptionValue("out", "out");

        File dexFileFile = new File(dexFileName);
        if (!dexFileFile.exists()) {
            System.out.println("Can't find the file " + dexFileFile.toString());
            System.exit(1);
        }

        File outputDir = new File(outputDirName);
        if (!outputDir.exists()) {          
            if (!outputDir.mkdirs()) {
                System.out.println("Can't create the output directory " + outputDir.toString());
                System.exit(1);
            }
        }

        DexFile dexFile = new DexFile(dexFileFile);

        InputStream templateStream = baksmali.class.getClassLoader().getResourceAsStream("templates/baksmali.stg");
        StringTemplateGroup templates = new StringTemplateGroup(new InputStreamReader(templateStream));

        templates.registerRenderer(Long.class, new LongRenderer());
        templates.registerRenderer(Integer.class,  new IntegerRenderer());
        templates.registerRenderer(Short.class, new ShortRenderer());
        templates.registerRenderer(Byte.class, new ByteRenderer());
        templates.registerRenderer(Float.class, new FloatRenderer());
        templates.registerRenderer(Character.class, new CharRenderer());

        int classCount = dexFile.ClassDefsSection.size();
        for (int i=0; i<classCount; i++) {
            ClassDefItem classDef = dexFile.ClassDefsSection.getByIndex(i);

            String classDescriptor = classDef.getClassType().getTypeDescriptor();
            if (classDescriptor.charAt(0) != 'L' ||
                classDescriptor.charAt(classDescriptor.length()-1) != ';') {
                System.out.println("Unrecognized class descriptor - " + classDescriptor + " - skipping class");
                continue;
            }
            //trim off the leading L and trailing ;
            classDescriptor = classDescriptor.substring(1, classDescriptor.length()-1);
            String[] pathElements = classDescriptor.split("/");

            //build the path to the smali file to generate for this class
            StringBuilder smaliPath = new StringBuilder(outputDir.getPath());
            for (String pathElement: pathElements) {
                smaliPath.append(File.separatorChar);
                smaliPath.append(pathElement);
            }
            smaliPath.append(".smali");

            File smaliFile = new File(smaliPath.toString());

            StringTemplate smaliFileST = templates.getInstanceOf("smaliFile");
            smaliFileST.setAttribute("classDef", new ClassDefinition(classDef));

            String output = smaliFileST.toString();

            FileWriter writer = null;
            try
            {
                if (!smaliFile.getParentFile().exists()) {
                    if (!smaliFile.getParentFile().mkdirs()) {
                        System.out.println("Unable to create directory " + smaliFile.getParentFile().toString() + " - skipping class");
                        continue;
                    }
                }
                if (!smaliFile.exists()){
                    if (!smaliFile.createNewFile()) {
                        System.out.println("Unable to create file " + smaliFile.toString() + " - skipping class");
                        continue;
                    }
                }

                writer = new FileWriter(smaliFile);
                writer.write(output);
            }finally
            {
                if (writer != null)
                    writer.close();
            }
        }
    }

    /**
     * Prints the usage message.
     */
    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar baksmali.jar -dis [-out <DIR>] <dexfile>",
                "Disassembles the given dex file", options, "");
    }
}
