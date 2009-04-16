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

import org.JesusFreke.dexlib.ItemType;
import org.JesusFreke.dexlib.util.Output;

import java.io.UnsupportedEncodingException;

public class HeaderItem extends IndexedItem<HeaderItem> {
    /**
     * non-null; the file format magic number, represented as the
     * low-order bytes of a string
     */
    private static final String MAGIC = "dex\n035" + '\0';

    /** size of this section, in bytes */
    private static final int HEADER_SIZE = 0x70;

    /** the endianness tag */
    private static final int ENDIAN_TAG = 0x12345678;

    private final Field[] fields;

    protected Field[] getFields() {
        return fields;
    }

    private final FixedByteArrayField magic;
    private final IntegerField checksum;
    private final FixedByteArrayField signature;
    private final IntegerField fileSize;
    private final IntegerField headerSize;
    private final IntegerField endianTag;
    private final IntegerField linkSize;
    private final IntegerField linkOff;
    private final IntegerField mapOff;
    private final SectionHeaderInfo StringIdsInfo;
    private final SectionHeaderInfo TypeIdsInfo;
    private final SectionHeaderInfo ProtoIdsInfo;
    private final SectionHeaderInfo FieldIdsInfo;
    private final SectionHeaderInfo MethodIdsInfo;
    private final SectionHeaderInfo ClassDefsInfo;
    private final IntegerField dataSize;
    private final IntegerField dataOff;

    public HeaderItem(final DexFile file, int index) throws UnsupportedEncodingException {
        super(index);

        fields = new Field[] {
                magic = new FixedByteArrayField(MAGIC.getBytes("US-ASCII")),
                checksum = new IntegerField() {
                    public void writeTo(Output out) {
                        cacheValue(0);
                        super.writeTo(out);
                    }
                },
                signature = new FixedByteArrayField(20) {
                    public void writeTo(Output out) {
                        for (int i = 0; i < value.length; i++) {
                            value[i] = 0;
                        }
                        super.writeTo(out);
                    }
                },
                fileSize = new IntegerField() {
                    public void writeTo(Output out) {
                        cacheValue(file.getFileSize());
                        super.writeTo(out);
                    }
                },
                headerSize = new IntegerField(HEADER_SIZE),
                endianTag = new IntegerField(ENDIAN_TAG),
                linkSize = new IntegerField(0),
                linkOff = new IntegerField(0),
                mapOff = new IntegerField() {
                    public void writeTo(Output out) {
                        cacheValue(file.MapSection.getOffset());
                        super.writeTo(out);
                    }
                },
                StringIdsInfo = new SectionHeaderInfo() {
                    protected Section getSection() {
                        return file.StringIdsSection;
                    }
                },
                TypeIdsInfo = new SectionHeaderInfo() {
                     protected Section getSection() {
                         return file.TypeIdsSection;
                     }
                },
                ProtoIdsInfo = new SectionHeaderInfo() {
                     protected Section getSection() {
                         return file.ProtoIdsSection;
                     }
                },
                FieldIdsInfo = new SectionHeaderInfo() {
                     protected Section getSection() {
                         return file.FieldIdsSection;
                     }
                },
                MethodIdsInfo = new SectionHeaderInfo() {
                     protected Section getSection() {
                         return file.MethodIdsSection;
                     }
                },
                ClassDefsInfo = new SectionHeaderInfo() {
                     protected Section getSection() {
                         return file.ClassDefsSection;
                     }
                },
                dataSize = new IntegerField() {
                    public void writeTo(Output out) {
                        cacheValue(file.getDataSize());
                        super.writeTo(out);
                    }
                },
                dataOff = new IntegerField() {
                    public void writeTo(Output out) {
                        cacheValue(file.getDataOffset());
                        super.writeTo(out);
                    }
                }
        };
    }

    public int getMapOffset() {
        return mapOff.getCachedValue();
    }

    protected int getAlignment() {
        return 4;
    }

    public ItemType getItemType() {
        return ItemType.TYPE_HEADER_ITEM;
    }

    public int compareTo(HeaderItem o) {
        //there is only 1 header item
        return 0;
    }
}
