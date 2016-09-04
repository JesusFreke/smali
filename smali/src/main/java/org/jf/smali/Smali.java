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

package org.jf.smali;

import com.google.common.collect.Lists;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.Token;
import org.antlr.runtime.TokenSource;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.writer.builder.DexBuilder;
import org.jf.dexlib2.writer.io.FileDataStore;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

public class Smali {

    /**
     * Assemble the specified files, using the given options
     *
     * @param options a SmaliOptions object with the options to run smali with
     * @param input The files/directories to process
     * @return true if assembly completed with no errors, or false if errors were encountered
     */
    public static boolean assemble(final SmaliOptions options, String... input) throws IOException {
        return assemble(options, Arrays.asList(input));
    }

    /**
     * Assemble the specified files, using the given options
     *
     * @param options a SmaliOptions object with the options to run smali with
     * @param input The files/directories to process
     * @return true if assembly completed with no errors, or false if errors were encountered
     */
    public static boolean assemble(final SmaliOptions options, List<String> input) throws IOException {
        LinkedHashSet<File> filesToProcessSet = new LinkedHashSet<File>();

        for (String fileToProcess: input) {
            File argFile = new File(fileToProcess);

            if (!argFile.exists()) {
                throw new IllegalArgumentException("Cannot find file or directory \"" + fileToProcess + "\"");
            }

            if (argFile.isDirectory()) {
                getSmaliFilesInDir(argFile, filesToProcessSet);
            } else if (argFile.isFile()) {
                filesToProcessSet.add(argFile);
            }
        }

        boolean errors = false;

        final DexBuilder dexBuilder = DexBuilder.makeDexBuilder(
                Opcodes.forApi(options.apiLevel));

        ExecutorService executor = Executors.newFixedThreadPool(options.jobs);
        List<Future<Boolean>> tasks = Lists.newArrayList();

        for (final File file: filesToProcessSet) {
            tasks.add(executor.submit(new Callable<Boolean>() {
                @Override public Boolean call() throws Exception {
                    return assembleSmaliFile(file, dexBuilder, options);
                }
            }));
        }

        for (Future<Boolean> task: tasks) {
            while(true) {
                try {
                    try {
                        if (!task.get()) {
                            errors = true;
                        }
                    } catch (ExecutionException ex) {
                        throw new RuntimeException(ex);
                    }
                } catch (InterruptedException ex) {
                    continue;
                }
                break;
            }
        }

        executor.shutdown();

        if (errors) {
            return false;
        }

        dexBuilder.writeTo(new FileDataStore(new File(options.outputDexFile)));

        return true;
    }

    private static void getSmaliFilesInDir(@Nonnull File dir, @Nonnull Set<File> smaliFiles) {
        File[] files = dir.listFiles();
        if (files != null) {
            for(File file: files) {
                if (file.isDirectory()) {
                    getSmaliFilesInDir(file, smaliFiles);
                } else if (file.getName().endsWith(".smali")) {
                    smaliFiles.add(file);
                }
            }
        }
    }

    private static boolean assembleSmaliFile(File smaliFile, DexBuilder dexBuilder, SmaliOptions options)
            throws Exception {
        CommonTokenStream tokens;

        LexerErrorInterface lexer;

        FileInputStream fis = new FileInputStream(smaliFile);
        InputStreamReader reader = new InputStreamReader(fis, "UTF-8");

        lexer = new smaliFlexLexer(reader);
        ((smaliFlexLexer)lexer).setSourceFile(smaliFile);
        tokens = new CommonTokenStream((TokenSource)lexer);

        if (options.printTokens) {
            tokens.getTokens();

            for (int i=0; i<tokens.size(); i++) {
                Token token = tokens.get(i);
                if (token.getChannel() == smaliParser.HIDDEN) {
                    continue;
                }

                System.out.println(smaliParser.tokenNames[token.getType()] + ": " + token.getText());
            }

            System.out.flush();
        }

        smaliParser parser = new smaliParser(tokens);
        parser.setVerboseErrors(options.verboseErrors);
        parser.setAllowOdex(options.allowOdexOpcodes);
        parser.setApiLevel(options.apiLevel);

        smaliParser.smali_file_return result = parser.smali_file();

        if (parser.getNumberOfSyntaxErrors() > 0 || lexer.getNumberOfSyntaxErrors() > 0) {
            return false;
        }

        CommonTree t = result.getTree();

        CommonTreeNodeStream treeStream = new CommonTreeNodeStream(t);
        treeStream.setTokenStream(tokens);

        if (options.printTokens) {
            System.out.println(t.toStringTree());
        }

        smaliTreeWalker dexGen = new smaliTreeWalker(treeStream);
        dexGen.setApiLevel(options.apiLevel);

        dexGen.setVerboseErrors(options.verboseErrors);
        dexGen.setDexBuilder(dexBuilder);
        dexGen.smali_file();

        return dexGen.getNumberOfSyntaxErrors() == 0;
    }
}
