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

package org.jf.dexlib;

import org.jf.dexlib.Util.AnnotatedOutput;

public abstract class SectionHeaderInfo extends CompositeField<SectionHeaderInfo> {
    private final String sectionName;

    private final IntegerField sectionSizeField;
    private final IntegerField sectionOffsetField;
    

    public SectionHeaderInfo(String sectionName) {
        super(sectionName);
        fields = new Field[] {
                sectionSizeField = new IntegerField(sectionName + "_size"),
                sectionOffsetField = new IntegerField(sectionName + "_off")
        };
        this.sectionName = sectionName;
    }

    protected abstract Section getSection();

    public void writeTo(AnnotatedOutput out) {
        Section section = getSection();

        if (!section.isPlaced()) {
             throw new RuntimeException("Trying to write a reference to a section that hasn't been placed.");
        }

        int size = section.size();
        sectionSizeField.cacheValue(size);

        if (size == 0) {
            //we have to set the offset to 0 or dalvik will complain
            sectionOffsetField.cacheValue(0);
        } else {
            sectionOffsetField.cacheValue(section.getOffset());
        }
        
        super.writeTo(out);
    }

    public int place(int offset) {
        return super.place(offset);
    }

    public int getSectionSize() {
        return sectionSizeField.getCachedValue();
    }

    public int getSectionOffset() {
        return sectionOffsetField.getCachedValue();
    }
}
