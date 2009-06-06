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
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.JesusFreke.smali;

import org.JesusFreke.dexlib.DexFile;
import org.JesusFreke.dexlib.util.ByteArrayOutput;
import org.JesusFreke.dexlib.util.ByteArrayAnnotatedOutput;
import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.Token;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class smali
{
    public static void main(String[] args) throws Exception
    {
        LinkedHashSet<File> filesToProcess = new LinkedHashSet<File>();

        boolean getFilesFromStdin = false;
        String outputFilename = "classes.dex";
        String dumpFilename = null;
        int dumpWidth = 120;

        int i;

        for (i=0; i<args.length; i++) {
            String arg = args[i];
            if (arg.equals("--") || !arg.startsWith("--")) {
                break;
            }
            if (arg.startsWith("--output=")) {
                outputFilename = arg.substring(arg.indexOf('=') + 1);
            } else if (arg.startsWith("--dump-to=")) {
                dumpFilename = arg.substring(arg.indexOf("=") + 1);
            } else if (arg.startsWith("--dump-width=")) {
                dumpWidth = Integer.parseInt(arg.substring(arg.indexOf("=") + 1));
            } else {
                System.err.println("unknown option: " + arg);
                throw new UsageException();
            }
        }

        for (i=i; i<args.length; i++) {
            String arg = args[i];

            if (arg.compareTo("-") == 0) {
                getFilesFromStdin = true;
            } else {
                File argFile = new File(arg);

                if (!argFile.exists()) {
                    throw new RuntimeException("Cannot find file or directory \"" + arg + "\"");
                }

                if (argFile.isDirectory()) {
                    getSmaliFilesInDir(argFile, filesToProcess);
                } else if (argFile.isFile()) {
                    filesToProcess.add(argFile);
                }
            }
        }

        if (getFilesFromStdin) {
            InputStreamReader isr = new InputStreamReader(System.in);
            BufferedReader in = new BufferedReader(isr);

            String line = in.readLine();
            while (line != null) {
                File file = new File(line);

                if (!file.exists()) {
                    throw new RuntimeException("Cannot find file or directory \"" + line + "\"");
                }

                if (file.isDirectory()) {
                    getSmaliFilesInDir(file, filesToProcess);   
                } else {
                    filesToProcess.add(file);
                }

                line = in.readLine();
            }
        }

        DexFile dexFile = DexFile.makeBlankDexFile();

        boolean errors = false;

        for (File file: filesToProcess) {
            if (!assembleSmaliFile(file, dexFile)) {
                errors = true;
            }
        }

        if (errors) {
            System.exit(1);
        }

        dexFile.place();
        try
        {
            ByteArrayAnnotatedOutput out = new ByteArrayAnnotatedOutput();

            if (dumpFilename != null) {
                out.enableAnnotations(dumpWidth, true);
            }

            dexFile.writeTo(out);

            byte[] bytes = out.toByteArray();

            DexFile.calcSignature(bytes);
            DexFile.calcChecksum(bytes);

            if (dumpFilename != null) {
                out.finishAnnotating();

                FileWriter fileWriter = new FileWriter("classes.dump");
                out.writeAnnotationsTo(fileWriter);
                fileWriter.close();
            }

            FileOutputStream fileOutputStream = new FileOutputStream(outputFilename);

            fileOutputStream.write(bytes);
            fileOutputStream.close();
        } catch (Exception ex)
        {
            System.out.println(ex.toString());
            System.exit(1);
        }
    }

    private static void getSmaliFilesInDir(File dir, Set<File> smaliFiles) {
        for(File file: dir.listFiles()) {
            if (file.isDirectory()) {
                getSmaliFilesInDir(file, smaliFiles);
            } else if (file.getName().endsWith(".smali")) {
                smaliFiles.add(file);
            }
        }
    }

    private static boolean assembleSmaliFile(File smaliFile, DexFile dexFile)
            throws Exception {
        ANTLRInputStream input = new ANTLRInputStream(new FileInputStream(smaliFile));
        input.name = smaliFile.getAbsolutePath();

        smaliLexer lexer = new smaliLexer(input);
       
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        smaliParser parser = new smaliParser(tokens);

        smaliParser.smali_file_return result = parser.smali_file();

        if (parser.getNumberOfSyntaxErrors() > 0 || lexer.getNumberOfLexerErrors() > 0) {
            return false;
        }


        CommonTree t = (CommonTree) result.getTree();

        CommonTreeNodeStream treeStream = new CommonTreeNodeStream(t);
        treeStream.setTokenStream(tokens);

        smaliTreeWalker dexGen = new smaliTreeWalker(treeStream);

        dexGen.dexFile = dexFile;
        dexGen.smali_file();

        if (dexGen.getNumberOfSyntaxErrors() > 0) {
            return false;
        }

        dexFile.ClassDefsSection.intern(dexFile, dexGen.classDefItem);
        return true;
    }
}