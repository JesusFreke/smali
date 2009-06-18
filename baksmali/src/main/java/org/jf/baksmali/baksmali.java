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

import java.io.File;
import java.io.FileReader;

public class baksmali {
    public static void main(String[] args) throws Exception
    {
        String dexFileName = args[0];
        String outputDir = args[1];

        DexFile dexFile = new DexFile(new File(dexFileName));

        StringTemplateGroup templates = new StringTemplateGroup(
                new FileReader("src/main/resources/templates/baksmali.stg"));

        templates.registerRenderer(Long.class, new LongRenderer());
        templates.registerRenderer(Integer.class,  new IntegerRenderer());
        templates.registerRenderer(Short.class, new ShortRenderer());
        templates.registerRenderer(Byte.class, new ByteRenderer());
        templates.registerRenderer(Float.class, new FloatRenderer());
        templates.registerRenderer(Character.class, new CharRenderer());

        StringTemplate smaliFileST = templates.getInstanceOf("smaliFile");

        smaliFileST.setAttribute("classDef", new ClassDefinition(dexFile.ClassDefsSection.getByIndex(0)));

        System.out.println(smaliFileST.toString());
    }
}
