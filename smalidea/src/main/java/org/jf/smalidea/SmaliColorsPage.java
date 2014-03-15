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

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.Map;

public class SmaliColorsPage implements ColorSettingsPage {
    private static final AttributesDescriptor[] ATTRS;

    static {
        List<TextAttributesKey> keys = SmaliHighlightingColors.getAllKeys();

        ATTRS = new AttributesDescriptor[keys.size()];
        for (int i=0; i<keys.size(); i++) {
            TextAttributesKey key = keys.get(i);

            ATTRS[i] = new AttributesDescriptor(key.getExternalName(), key);
        }
    }

    @Nullable @Override public Icon getIcon() {
        return SmaliIcons.SmaliIcon;
    }

    @NotNull @Override public SyntaxHighlighter getHighlighter() {
        return new SmaliHighlighter();
    }

    @NotNull @Override public String getDemoText() {
        return ".class public Lorg/jf/smalidea/ColorExample;\n" +
                ".super Ljava/lang/Object;\n" +
                ".source \"ColorExample.smali\"\n" +
                "\n" +
                ".field public exampleField:I = 1234\n" +
                "\n" +
                ".field public boolField:Z = true\n" +
                "\n" +
                "# This is an example comment\n" +
                "\n" +
                ".method public constructor <init>()V\n" +
                "    .registers 1\n" +
                "    invoke-direct {p0}, Ljava/lang/Object;-><init>()V\n" +
                "    return-void\n" +
                ".end method\n" +
                "\n" +
                ".method public exampleMethod()V\n" +
                "    .registers 10\n" +
                "\n" +
                "    const v0, 1234\n" +
                "    const-string v1, \"An Example String\"\n" +
                "\n" +
                "    invoke-virtual {p0, v0, v1}, Lorg/jf/smalidea/ColorExample;->anotherMethod(ILjava/lang/String;)V\n" +
                "\n" +
                "    move v2, v1\n" +
                "    move v1, v0\n" +
                "    move v0, p0\n" +
                "\n" +
                "    invoke-virtual/range {v0 .. v2}, Lorg/jf/smalidea/ColorExample;->anotherMethod(ILjava/lang/String;)V\n" +
                "\n" +
                "    return-void\n" +
                ".end method\n" +
                "\n" +
                ".method public anotherMethod(ILjava/Lang/String;)V\n" +
                "    .registers 10\n" +
                "\n" +
                "    # This is another example comment\n" +
                "\n" +
                "    return-void\n" +
                ".end method\n" +
                "\n" +
                ".method public odexInstructions()V\n" +
                "    .registers 10\n" +
                "    invoke-virtual {p0}, vtable@0x1b\n" +
                "\n" +
                "    iget-quick p0, field@0x1\n" +
                "\n" +
                "    execute-inline {p0}, inline@0xa\n" +
                "\n" +
                "    throw-verification-error illegal-method-access, Lblah;->Blort()V\n" +
                ".end method";
    }

    @NotNull @Override public AttributesDescriptor[] getAttributeDescriptors() {
        return ATTRS;
    }

    @Nullable @Override public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        return null;
    }

    @NotNull @Override public ColorDescriptor[] getColorDescriptors() {
        return ColorDescriptor.EMPTY_ARRAY;
    }

    @NotNull @Override public String getDisplayName() {
        return "smali";
    }
}
