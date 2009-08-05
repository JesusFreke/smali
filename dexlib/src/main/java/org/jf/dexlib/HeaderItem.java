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

package org.jf.dexlib;

import org.jf.dexlib.Util.AnnotatedOutput;
import org.jf.dexlib.Util.Input;

import java.io.UnsupportedEncodingException;

public class HeaderItem extends Item<HeaderItem> {
    /**
     * non-null; the file format magic number, represented as the
     * low-order bytes of a string
     */
    private static final String MAGIC = "dex\n035" + '\0';

    /** size of this section, in bytes */
    private static final int HEADER_SIZE = 0x70;

    /** the endianness constants */
    private static final int LITTLE_ENDIAN = 0x12345678;
    private static final int BIG_ENDIAN = 0x78562312;

    /**
     * Create a new uninitialized <code>HeaderItem</code>
     * @param dexFile The <code>DexFile</code> containing this <code>HeaderItem</code>
     */
    protected HeaderItem(final DexFile dexFile) {
        super(dexFile);
    }

    /** {@inheritDoc} */
    protected void readItem(Input in, ReadContext readContext) {
        byte[] expectedMagic;
        try {
            expectedMagic = MAGIC.getBytes("US-ASCII");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
        
        byte[] readMagic = in.readBytes(8);

        for (int i=0; i<8; i++) {
            if (expectedMagic[i] != readMagic[i]) {
                throw new RuntimeException("The magic value is not the expected value");
            }
        }

        in.readBytes(20); //checksum
        in.readInt(); //signature
        in.readInt(); //filesize
        if (in.readInt() != HEADER_SIZE) {
            throw new RuntimeException("The header size is not the expected value (0x70)");
        }

        int endianTag = in.readInt();
        if (endianTag == BIG_ENDIAN) {
            throw new RuntimeException("This dex file is big endian. Only little endian is currently supported.");
        } else if (endianTag != LITTLE_ENDIAN) {
            throw new RuntimeException("The endian tag is not 0x12345678 or 0x78563412");
        }

        //link_size
        if (in.readInt() != 0) {
            throw new RuntimeException("This dex file has a link section, which is not supported");
        }

        //link_off
        if (in.readInt() != 0) {
            throw new RuntimeException("This dex file has a link section, which is not supported");
        }

        int sectionSize;
        int sectionOffset;

        //map_offset
        sectionOffset = in.readInt();
        readContext.addSection(ItemType.TYPE_MAP_LIST, 1, sectionOffset);

        //string_id_item
        sectionSize = in.readInt();
        sectionOffset = in.readInt();
        readContext.addSection(ItemType.TYPE_STRING_ID_ITEM, sectionSize, sectionOffset);

        //type_id_item
        sectionSize = in.readInt();
        sectionOffset = in.readInt();
        readContext.addSection(ItemType.TYPE_TYPE_ID_ITEM, sectionSize, sectionOffset);

        //proto_id_item
        sectionSize = in.readInt();
        sectionOffset = in.readInt();
        readContext.addSection(ItemType.TYPE_PROTO_ID_ITEM, sectionSize, sectionOffset);

        //field_id_item
        sectionSize = in.readInt();
        sectionOffset = in.readInt();
        readContext.addSection(ItemType.TYPE_FIELD_ID_ITEM, sectionSize, sectionOffset);

        //method_id_item
        sectionSize = in.readInt();
        sectionOffset = in.readInt();
        readContext.addSection(ItemType.TYPE_METHOD_ID_ITEM, sectionSize, sectionOffset);

        //class_data_item
        sectionSize = in.readInt();
        sectionOffset = in.readInt();
        readContext.addSection(ItemType.TYPE_CLASS_DEF_ITEM, sectionSize, sectionOffset);

        in.readInt(); //data_size
        in.readInt(); //data_off
    }

    /** {@inheritDoc} */
    protected int placeItem(int offset) {
        return HEADER_SIZE;
    }

    /** {@inheritDoc} */
    protected void writeItem(AnnotatedOutput out) {
        if (out.annotates()) {
            //TODO: add human readable representations of the underlying data where appropriate
            out.annotate(8, "magic");
            out.annotate(4, "checksum");
            out.annotate(20, "signature");
            out.annotate(4, "file_size");
            out.annotate(4, "header_size");
            out.annotate(4, "endian_tag");
            out.annotate(4, "link_size");
            out.annotate(4, "link_off");
            out.annotate(4, "map_off");
            out.annotate(4, "string_ids_size");
            out.annotate(4, "string_ids_off");
            out.annotate(4, "type_ids_size");
            out.annotate(4, "type_ids_off");
            out.annotate(4, "proto_ids_size");
            out.annotate(4, "proto_ids_off");
            out.annotate(4, "field_ids_size");
            out.annotate(4, "field_ids_off");
            out.annotate(4, "method_ids_size");
            out.annotate(4, "method_ids_off");
            out.annotate(4, "class_defs_size");
            out.annotate(4, "class_defs_off");
            out.annotate(4, "data_size");
            out.annotate(4, "data_off");
        }

        byte[] magic;
        try {
            magic = MAGIC.getBytes("US-ASCII");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }

        out.write(magic);
        out.writeInt(0); //checksum
        out.write(new byte[20]); //signature
        out.writeInt(dexFile.getFileSize());
        out.writeInt(HEADER_SIZE);
        out.writeInt(LITTLE_ENDIAN);
        out.writeInt(0); //link_size
        out.writeInt(0); //link_off
        out.writeInt(dexFile.MapItem.getOffset());
        out.writeInt(dexFile.StringIdsSection.getItems().size());
        out.writeInt(dexFile.StringIdsSection.getOffset());
        out.writeInt(dexFile.TypeIdsSection.getItems().size());
        out.writeInt(dexFile.TypeIdsSection.getOffset());
        out.writeInt(dexFile.ProtoIdsSection.getItems().size());
        out.writeInt(dexFile.ProtoIdsSection.getOffset());
        out.writeInt(dexFile.FieldIdsSection.getItems().size());
        out.writeInt(dexFile.FieldIdsSection.getOffset());
        out.writeInt(dexFile.MethodIdsSection.getItems().size());
        out.writeInt(dexFile.MethodIdsSection.getOffset());
        out.writeInt(dexFile.ClassDefsSection.getItems().size());
        out.writeInt(dexFile.ClassDefsSection.getOffset());
        out.writeInt(dexFile.getDataSize());
        out.writeInt(dexFile.getDataOffset());        
    }

    /** {@inheritDoc} */
    public ItemType getItemType() {
        return ItemType.TYPE_HEADER_ITEM;
    }

    /** {@inheritDoc} */
    public String getConciseIdentity() {
        return "header_item";
    }

    /** {@inheritDoc} */
    public int compareTo(HeaderItem o) {
        //there is only 1 header item
        return 0;
    }
}
