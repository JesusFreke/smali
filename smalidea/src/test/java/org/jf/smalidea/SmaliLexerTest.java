/*
 * Copyright 2014, Google Inc.
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

package org.jf.smalidea;

import com.intellij.lexer.Lexer;
import com.intellij.testFramework.LexerTestCase;

import java.util.Random;

/**
 * This is mostly just a smoke test to make sure the lexer is working. The lexer itself has its
 * own tests in the smali module
 */
public class SmaliLexerTest extends LexerTestCase {
    public void testHelloWorld() {
        String text =
                ".class public LHelloWorld;\n" +
                ".super Ljava/lang/Object;\n" +
                ".method public static main([Ljava/lang/String;)V\n" +
                "    .registers 2\n" +
                "    sget-object v0, Ljava/lang/System;->out:Ljava/io/PrintStream;\n" +
                "    const-string v1, \"Hello World!\"\n" +
                "    invoke-virtual {v0, v1}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V\n" +
                "    return-void\n" +
                ".end method";

        doTest(text,
                "CLASS_DIRECTIVE ('.class')\n" +
                "WHITE_SPACE (' ')\n" +
                "ACCESS_SPEC ('public')\n" +
                "WHITE_SPACE (' ')\n" +
                "CLASS_DESCRIPTOR ('LHelloWorld;')\n" +
                "WHITE_SPACE ('\\n')\n" +
                "SUPER_DIRECTIVE ('.super')\n" +
                "WHITE_SPACE (' ')\n" +
                "CLASS_DESCRIPTOR ('Ljava/lang/Object;')\n" +
                "WHITE_SPACE ('\\n')\n" +
                "METHOD_DIRECTIVE ('.method')\n" +
                "WHITE_SPACE (' ')\n" +
                "ACCESS_SPEC ('public')\n" +
                "WHITE_SPACE (' ')\n" +
                "ACCESS_SPEC ('static')\n" +
                "WHITE_SPACE (' ')\n" +
                "SIMPLE_NAME ('main')\n" +
                "OPEN_PAREN ('(')\n" +
                "ARRAY_TYPE_PREFIX ('[')\n" +
                "CLASS_DESCRIPTOR ('Ljava/lang/String;')\n" +
                "CLOSE_PAREN (')')\n" +
                "VOID_TYPE ('V')\n" +
                "WHITE_SPACE ('\\n    ')\n" +
                "REGISTERS_DIRECTIVE ('.registers')\n" +
                "WHITE_SPACE (' ')\n" +
                "POSITIVE_INTEGER_LITERAL ('2')\n" +
                "WHITE_SPACE ('\\n    ')\n" +
                "INSTRUCTION_FORMAT21c_FIELD ('sget-object')\n" +
                "WHITE_SPACE (' ')\n" +
                "REGISTER ('v0')\n" +
                "COMMA (',')\n" +
                "WHITE_SPACE (' ')\n" +
                "CLASS_DESCRIPTOR ('Ljava/lang/System;')\n" +
                "ARROW ('->')\n" +
                "SIMPLE_NAME ('out')\n" +
                "COLON (':')\n" +
                "CLASS_DESCRIPTOR ('Ljava/io/PrintStream;')\n" +
                "WHITE_SPACE ('\\n    ')\n" +
                "INSTRUCTION_FORMAT21c_STRING ('const-string')\n" +
                "WHITE_SPACE (' ')\n" +
                "REGISTER ('v1')\n" +
                "COMMA (',')\n" +
                "WHITE_SPACE (' ')\n" +
                "STRING_LITERAL ('\"Hello World!\"')\n" +
                "WHITE_SPACE ('\\n    ')\n" +
                "INSTRUCTION_FORMAT35c_METHOD ('invoke-virtual')\n" +
                "WHITE_SPACE (' ')\n" +
                "OPEN_BRACE ('{')\n" +
                "REGISTER ('v0')\n" +
                "COMMA (',')\n" +
                "WHITE_SPACE (' ')\n" +
                "REGISTER ('v1')\n" +
                "CLOSE_BRACE ('}')\n" +
                "COMMA (',')\n" +
                "WHITE_SPACE (' ')\n" +
                "CLASS_DESCRIPTOR ('Ljava/io/PrintStream;')\n" +
                "ARROW ('->')\n" +
                "SIMPLE_NAME ('println')\n" +
                "OPEN_PAREN ('(')\n" +
                "CLASS_DESCRIPTOR ('Ljava/lang/String;')\n" +
                "CLOSE_PAREN (')')\n" +
                "VOID_TYPE ('V')\n" +
                "WHITE_SPACE ('\\n    ')\n" +
                "INSTRUCTION_FORMAT10x ('return-void')\n" +
                "WHITE_SPACE ('\\n')\n" +
                "END_METHOD_DIRECTIVE ('.end method')"
        );
    }

    @Override protected Lexer createLexer() {
        return new SmaliLexer();
    }

    @Override protected String getDirPath() {
        return "";
    }

    public void testErrorToken() {
        String text = ".class public .blah";
        doTest(text,
                "CLASS_DIRECTIVE ('.class')\n" +
                        "WHITE_SPACE (' ')\n" +
                        "ACCESS_SPEC ('public')\n" +
                        "WHITE_SPACE (' ')\n" +
                        "BAD_CHARACTER ('.blah')\n");
    }

    /**
     * Type out an example smali file character by character, ensuring that no exceptions are thrown
     */
    public void testPartialText() {
        String text =
                ".class public LHelloWorld;\n" +
                        ".super Ljava/lang/Object;\n" +
                        ".method public static main([Ljava/lang/String;)V\n" +
                        "    .registers 2\n" +
                        "    sget-object v0, Ljava/lang/System;->out:Ljava/io/PrintStream;\n" +
                        "    const-string v1, \"Hello World!\"\n" +
                        "    invoke-virtual {v0, v1}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V\n" +
                        "    return-void\n" +
                        ".end method";

        for (int i=1; i<text.length(); i++) {
            printTokens(text.substring(i), 0);
        }
    }

    /**
     * Generate some random text and make sure the lexer doesn't throw any exceptions
     */
    public void testRandomText() {
        for (int i=0; i<100; i++) {
            String randomString = randomString(1000);

            printTokens(randomString, 0);
        }
    }

    private Random random = new Random(123456789);
    private String randomString(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<length; i++) {
            int type = random.nextInt(10);

            if (type == 9) {
                int randomCodepoint;
                do {
                    randomCodepoint = random.nextInt();
                } while(!Character.isValidCodePoint(randomCodepoint));
                sb.appendCodePoint(randomCodepoint);
            } else if (type == 8) {
                char randomChar;
                do {
                    randomChar = (char)random.nextInt(2^16);
                } while(!Character.isValidCodePoint(randomChar));
                sb.append(randomChar);
            } else if (type > 4) {
                sb.append((char)random.nextInt(256));
            } else if (type == 4) {
                sb.append(' ');
            } else {
                sb.append((char)random.nextInt(128));
            }
        }

        return sb.toString();
    }
}
