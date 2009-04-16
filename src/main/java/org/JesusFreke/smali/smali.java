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
import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;

import java.io.FileInputStream;
import java.io.FileOutputStream;

public class smali
{
    public static void main(String[] args) throws Exception
    {
        /*ANTLRStringStream input = new ANTLRStringStream("atest1btest2");
        testLexer lexer = new testLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        List l = tokens.getTokens();*/


        ANTLRInputStream input = new ANTLRInputStream(new FileInputStream(args[0]));
        smaliLexer lexer = new smaliLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        smaliParser parser = new smaliParser(tokens);
        smaliParser.smali_file_return result = parser.smali_file();
        CommonTree t = (CommonTree) result.getTree();

        CommonTreeNodeStream treeStream = new CommonTreeNodeStream(t);

        smaliTreeWalker dexGen = new smaliTreeWalker(treeStream);

        DexFile dexFile = DexFile.makeBlankDexFile();
        dexGen.dexFile = dexFile;
        dexGen.smali_file();

        dexFile.ClassDefsSection.intern(dexFile, dexGen.classDefItem);
        dexFile.place();
        try
        {
            ByteArrayOutput out = new ByteArrayOutput();
            dexFile.writeTo(out);

            byte[] bytes = out.toByteArray();

            DexFile.calcSignature(bytes);
            DexFile.calcChecksum(bytes);

            FileOutputStream fileOutputStream = new FileOutputStream("classes.dex");

            fileOutputStream.write(bytes);
            fileOutputStream.close();
        } catch (Exception ex)
        {
            System.out.println(ex.toString());
        }


        System.out.println("here");
    }
}