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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.jf.baksmali.Adaptors.ClassDefinition;
import org.jf.dexlib2.analysis.ClassPath;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.util.SyntheticAccessorResolver;
import org.jf.util.ClassFileNameHandler;
import org.jf.util.IndentingWriter;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class baksmali {

    public static void disassembleDexFile(DexFile dexFile, final baksmaliOptions options) {
        if (options.registerInfo != 0 || options.deodex) {
            try {
                Iterable<String> extraClassPathEntries;
                if (options.extraClassPathEntries != null) {
                    extraClassPathEntries = options.extraClassPathEntries;
                } else {
                    extraClassPathEntries = ImmutableList.of();
                }

                options.classPath = ClassPath.fromClassPath(options.bootClassPathDirs,
                        Iterables.concat(options.bootClassPathEntries, extraClassPathEntries), dexFile,
                        options.apiLevel);
            } catch (Exception ex) {
                System.err.println("\n\nError occured while loading boot class path files. Aborting.");
                ex.printStackTrace(System.err);
                System.exit(1);
            }
        }

        File outputDirectoryFile = new File(options.outputDirectory);
        if (!outputDirectoryFile.exists()) {
            if (!outputDirectoryFile.mkdirs()) {
                System.err.println("Can't create the output directory " + options.outputDirectory);
                System.exit(1);
            }
        }

        //sort the classes, so that if we're on a case-insensitive file system and need to handle classes with file
        //name collisions, then we'll use the same name for each class, if the dex file goes through multiple
        //baksmali/smali cycles for some reason. If a class with a colliding name is added or removed, the filenames
        //may still change of course
        List<ClassDef> classDefs = new ArrayList<ClassDef>(dexFile.getClasses());
        Collections.sort(classDefs, new Comparator<ClassDef>() {
            public int compare(ClassDef classDef1, ClassDef classDef2) {
                return classDef1.getType().compareTo(classDef2.getType());
            }
        });
        classDefs = ImmutableList.copyOf(classDefs);

        if (!options.noAccessorComments) {
            options.syntheticAccessorResolver = new SyntheticAccessorResolver(classDefs);
        }

        final ClassFileNameHandler fileNameHandler = new ClassFileNameHandler(outputDirectoryFile, ".smali");

        ExecutorService executor = Executors.newFixedThreadPool(options.jobs);
        List<Future<Void>> tasks = Lists.newArrayList();

        for (final ClassDef classDef: classDefs) {
            tasks.add(executor.submit(new Callable<Void>() {
                @Override public Void call() throws Exception {
                    disassembleClass(classDef, fileNameHandler, options);
                    return null;
                }
            }));
        }

        for (Future<Void> task: tasks) {
            while(true) {
                try {
                    task.get();
                } catch (InterruptedException ex) {
                    continue;
                } catch (ExecutionException ex) {
                    throw new RuntimeException(ex);
                }
                break;
            }
        }

        executor.shutdown();
    }

    private static void disassembleClass(ClassDef classDef, ClassFileNameHandler fileNameHandler,
                                         baksmaliOptions options) {
        /**
         * The path for the disassembly file is based on the package name
         * The class descriptor will look something like:
         * Ljava/lang/Object;
         * Where the there is leading 'L' and a trailing ';', and the parts of the
         * package name are separated by '/'
         */
        String classDescriptor = classDef.getType();

        //validate that the descriptor is formatted like we expect
        if (classDescriptor.charAt(0) != 'L' ||
                classDescriptor.charAt(classDescriptor.length()-1) != ';') {
            System.err.println("Unrecognized class descriptor - " + classDescriptor + " - skipping class");
            return;
        }

        File smaliFile = fileNameHandler.getUniqueFilenameForClass(classDescriptor);

        //create and initialize the top level string template
        ClassDefinition classDefinition = new ClassDefinition(options, classDef);

        //write the disassembly
        Writer writer = null;
        try
        {
            File smaliParent = smaliFile.getParentFile();
            if (!smaliParent.exists()) {
                if (!smaliParent.mkdirs()) {
                    // check again, it's likely it was created in a different thread
                    if (!smaliParent.exists()) {
                        System.err.println("Unable to create directory " + smaliParent.toString() + " - skipping class");
                        return;
                    }
                }
            }

            if (!smaliFile.exists()){
                if (!smaliFile.createNewFile()) {
                    System.err.println("Unable to create file " + smaliFile.toString() + " - skipping class");
                    return;
                }
            }

            BufferedWriter bufWriter = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(smaliFile), "UTF8"));

            writer = new IndentingWriter(bufWriter);
            classDefinition.writeTo((IndentingWriter)writer);
        } catch (Exception ex) {
            System.err.println("\n\nError occured while disassembling class " + classDescriptor.replace('/', '.') + " - skipping class");
            ex.printStackTrace();
            // noinspection ResultOfMethodCallIgnored
            smaliFile.delete();
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

        if (!options.ignoreErrors && classDefinition.hadValidationErrors()) {
            System.exit(1);
        }
    }
}
