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

package org.JesusFreke.dexlib;

import org.JesusFreke.dexlib.util.Output;
import org.JesusFreke.dexlib.util.Input;

public abstract class SectionHeaderInfo implements Field<SectionHeaderInfo> {
    private int sectionSize;
    private int sectionOffset;

    public SectionHeaderInfo() {
    }

    protected abstract Section getSection();

    public void writeTo(Output out) {
        Section section = getSection();

        if (!section.isPlaced()) {
             throw new RuntimeException("Trying to write a reference to a section that hasn't been placed.");
        }
        sectionSize = section.size();
        sectionOffset = section.getOffset();

        out.writeInt(sectionSize);
        out.writeInt(sectionOffset);
    }

    public void readFrom(Input in) {
        sectionSize = in.readInt();
        sectionOffset = in.readInt();
    }

    public int getSectionSize() {
        return sectionSize;
    }

    public int getSectionOffset() {
        return sectionOffset;
    }

    public int place(int offset) {
        Section section = getSection();
        sectionSize = section.size();
        sectionOffset = section.getOffset();

        return offset+8;
    }

    public void copyTo(DexFile dexFile, SectionHeaderInfo copy) {
        /**
         * do nothing. the section size and offset are dynamically generated
         * when the copy is written
         */
    }
}
