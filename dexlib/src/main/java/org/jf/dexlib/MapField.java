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

public class MapField extends CompositeField<MapField> {
    private final ShortIntegerField sectionTypeField;
    private final SectionHeaderInfo sectionInfoField;

    protected MapField(final DexFile dexFile) {
        super("map_entry");
        fields = new Field[] {
                //TODO: add an annotation for the item type
                sectionTypeField = new ShortIntegerField("type"),
                new ShortIntegerField((short)0, "padding"),
                sectionInfoField = new SectionHeaderInfo("section") {
                    protected Section getSection() {
                        return dexFile.getSectionForType(getSectionItemType());
                    }
                }
        };
    }

    protected MapField(final DexFile dexFile, short sectionType) {
        this(dexFile);
        sectionTypeField.cacheValue(sectionType);
    }

    /**
     * Get the <code>ItemType</code> of the section that this map field represents
     * @return The <code>ItemType</code> of the section that this map field represents
     */
    public ItemType getSectionItemType() {
        return ItemType.fromInt(sectionTypeField.getCachedValue());
    }

    /**
     * Get the <code>Section</code> object that this map field represents 
     * @return The <code>Section</code> object that this map field represents 
     */
    public Section getSection() {
        Section s;
        return sectionInfoField.getSection();
    }

    /**
     * This returns the cached size of the section that this map field represents. This is used while
     * reading in the given section, to retrieve the size of the section that is stored in this map
     * field.
     * 
     * @return the cached size of the section that this map field represents
     */
    protected int getCachedSectionSize() {
        return sectionInfoField.getSectionSize();
    }

    /**
     * This returns the cached size of the section that this map field represents. This is used while
     * reading in the given section, to retrieve the offset of the section that is stored in this map
     * field
     * @return
     */
    protected int getCachedSectionOffset() {
        return sectionInfoField.getSectionOffset();
    }
}
