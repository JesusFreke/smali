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

import org.jf.dexlib.util.AnnotatedOutput;

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

    private final FixedByteArrayField magicField;
    private final IntegerField checksumField;
    private final FixedByteArrayField signatureField;
    private final IntegerField fileSizeField;
    private final IntegerField headerSizeField;
    private final IntegerField endianTagField;
    private final IntegerField linkSizeField;
    private final IntegerField linkOffField;
    private final IntegerField mapOffField;
    private final SectionHeaderInfo StringIdsHeaderField;
    private final SectionHeaderInfo TypeIdsHeaderField;
    private final SectionHeaderInfo ProtoIdsHeaderField;
    private final SectionHeaderInfo FieldIdsHeaderField;
    private final SectionHeaderInfo MethodIdsHeaderField;
    private final SectionHeaderInfo ClassDefsHeaderField;
    private final IntegerField dataSizeField;
    private final IntegerField dataOffField;

    public HeaderItem(final DexFile file, int index) {
        super(index);

        try
        {
            fields = new Field[] {
                    magicField = new FixedByteArrayField(MAGIC.getBytes("US-ASCII"), "magic"),
                    checksumField = new IntegerField("checksum") {
                        public void writeTo(AnnotatedOutput out) {
                            cacheValue(0);
                            super.writeTo(out);
                        }
                    },
                    signatureField = new FixedByteArrayField(20, "signature") {
                        public void writeTo(AnnotatedOutput out) {
                            for (int i = 0; i < value.length; i++) {
                                value[i] = 0;
                            }
                            super.writeTo(out);
                        }
                    },
                    fileSizeField = new IntegerField("file_size") {
                        public void writeTo(AnnotatedOutput out) {
                            cacheValue(file.getFileSize());
                            super.writeTo(out);
                        }
                    },
                    headerSizeField = new IntegerField(HEADER_SIZE,"header_size"),
                    endianTagField = new IntegerField(ENDIAN_TAG,"endian_tag"),
                    linkSizeField = new IntegerField(0,"link_size"),
                    linkOffField = new IntegerField(0,"link_off"),
                    mapOffField = new IntegerField("map_off") {
                        public void writeTo(AnnotatedOutput out) {
                            cacheValue(file.MapSection.getOffset());
                            super.writeTo(out);
                        }
                    },
                    StringIdsHeaderField = new SectionHeaderInfo("string_ids") {
                        protected Section getSection() {
                            return file.StringIdsSection;
                        }
                    },
                    TypeIdsHeaderField = new SectionHeaderInfo("type_ids") {
                         protected Section getSection() {
                             return file.TypeIdsSection;
                         }
                    },
                    ProtoIdsHeaderField = new SectionHeaderInfo("proto_ids") {
                         protected Section getSection() {
                             return file.ProtoIdsSection;
                         }
                    },
                    FieldIdsHeaderField = new SectionHeaderInfo("field_ids") {
                         protected Section getSection() {
                             return file.FieldIdsSection;
                         }
                    },
                    MethodIdsHeaderField = new SectionHeaderInfo("method_ids") {
                         protected Section getSection() {
                             return file.MethodIdsSection;
                         }
                    },
                    ClassDefsHeaderField = new SectionHeaderInfo("class_defs") {
                         protected Section getSection() {
                             return file.ClassDefsSection;
                         }
                    },
                    dataSizeField = new IntegerField("data_size") {
                        public void writeTo(AnnotatedOutput out) {
                            cacheValue(file.getDataSize());
                            super.writeTo(out);
                        }
                    },
                    dataOffField = new IntegerField("data_off") {
                        public void writeTo(AnnotatedOutput out) {
                            cacheValue(file.getDataOffset());
                            super.writeTo(out);
                        }
                    }
            };
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("Error while creating the magic header field.", ex);
        }
    }

    public int getMapOffset() {
        return mapOffField.getCachedValue();
    }

    protected int getAlignment() {
        return 4;
    }

    public ItemType getItemType() {
        return ItemType.TYPE_HEADER_ITEM;
    }

    public String getConciseIdentity() {
        return "header_item";
    }

    public int compareTo(HeaderItem o) {
        //there is only 1 header item
        return 0;
    }
}
