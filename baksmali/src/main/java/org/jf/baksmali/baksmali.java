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
import org.jf.baksmali.Deodex.*;
import org.jf.baksmali.Renderers.*;
import org.jf.dexlib.DexFile;
import org.jf.dexlib.ClassDefItem;
import org.jf.dexlib.StringIdItem;

import java.io.*;

public class baksmali {
    public static boolean noParameterRegisters = false;
    public static boolean useLocalsDirective = false;
    public static boolean useSequentialLabels = false;
    public static boolean outputDebugInfo = true;
    public static DeodexUtil deodexUtil = null;

    public static void disassembleDexFile(DexFile dexFile, Deodexerant deodexerant, String outputDirectory,
                                          boolean noParameterRegisters, boolean useLocalsDirective,
                                          boolean useSequentialLabels, boolean outputDebugInfo)
    {
        baksmali.noParameterRegisters = noParameterRegisters;
        baksmali.useLocalsDirective = useLocalsDirective;
        baksmali.useSequentialLabels = useSequentialLabels;
        baksmali.outputDebugInfo = outputDebugInfo;

        if (deodexerant != null) {
            baksmali.deodexUtil = new DeodexUtil(deodexerant);
        }

        File outputDirectoryFile = new File(outputDirectory);
        if (!outputDirectoryFile.exists()) {
            if (!outputDirectoryFile.mkdirs()) {
                System.err.println("Can't create the output directory " + outputDirectory);
                System.exit(1);
            }
        }

        //load and initialize the templates
        InputStream templateStream = baksmali.class.getClassLoader().getResourceAsStream("templates/baksmali.stg");
        StringTemplateGroup templates = new StringTemplateGroup(new InputStreamReader(templateStream));
        templates.registerRenderer(Long.class, new LongRenderer());
        templates.registerRenderer(Integer.class,  new IntegerRenderer());
        templates.registerRenderer(Short.class, new ShortRenderer());
        templates.registerRenderer(Byte.class, new ByteRenderer());
        templates.registerRenderer(Float.class, new FloatRenderer());
        templates.registerRenderer(Character.class, new CharRenderer());
        templates.registerRenderer(StringIdItem.class, new StringIdItemRenderer());


        for (ClassDefItem classDefItem: dexFile.ClassDefsSection.getItems()) {
            /**
             * The path for the disassembly file is based on the package name
             * The class descriptor will look something like:
             * Ljava/lang/Object;
             * Where the there is leading 'L' and a trailing ';', and the parts of the
             * package name are separated by '/'
             */

            String classDescriptor = classDefItem.getClassType().getTypeDescriptor();

            //validate that the descriptor is formatted like we expect
            if (classDescriptor.charAt(0) != 'L' ||
                classDescriptor.charAt(classDescriptor.length()-1) != ';') {
                System.err.println("Unrecognized class descriptor - " + classDescriptor + " - skipping class");
                continue;
            }

            //trim off the leading L and trailing ;
            classDescriptor = classDescriptor.substring(1, classDescriptor.length()-1);

            //trim off the leading 'L' and trailing ';', and get the individual package elements
            String[] pathElements = classDescriptor.split("/");

            //build the path to the smali file to generate for this class
            StringBuilder smaliPath = new StringBuilder(outputDirectory);
            for (String pathElement: pathElements) {
                smaliPath.append(File.separatorChar);
                smaliPath.append(pathElement);
            }
            smaliPath.append(".smali");

            File smaliFile = new File(smaliPath.toString());

            //create and initialize the top level string template
            ClassDefinition classDefinition = new ClassDefinition(templates, classDefItem);

            StringTemplate smaliFileST = classDefinition.createTemplate();

            //generate the disassembly
            String output = smaliFileST.toString();

            //write the disassembly
            FileWriter writer = null;
            try
            {
                File smaliParent = smaliFile.getParentFile();
                if (!smaliParent.exists()) {
                    if (!smaliParent.mkdirs()) {
                        System.err.println("Unable to create directory " + smaliParent.toString() + " - skipping class");
                        continue;
                    }
                }

                if (!smaliFile.exists()){
                    if (!smaliFile.createNewFile()) {
                        System.err.println("Unable to create file " + smaliFile.toString() + " - skipping class");
                        continue;
                    }
                }

                writer = new FileWriter(smaliFile);
                writer.write(output);
            } catch (Throwable ex) {
                System.err.println("\n\nError occured while disassembling class " + classDescriptor.replace('/', '.') + " - skipping class");
                ex.printStackTrace();
            }
            finally
            {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (Throwable ex) {
                        System.err.println("\n\nError occured while closing file " + smaliFile.toString());
                        ex.printStackTrace();
                    }
                }
            }
        }
    }
}
