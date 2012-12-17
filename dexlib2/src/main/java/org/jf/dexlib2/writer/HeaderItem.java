/*
 * Copyright 2012, Google Inc.
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

package org.jf.dexlib2.writer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.Adler32;

public class HeaderItem {
    public final static int HEADER_ITEM_SIZE = 0x70;
    private static byte[] DEX_FILE_MAGIC = new byte[] {0x64, 0x65, 0x78, 0x0a, 0x30, 0x33, 0x35, 0x00 };
    private static int LITTLE_ENDIAN = 0x12345678;
    private static int CHECKSUM_SIZE = 4;
    private static int SIGNATURE_SIZE = 20;

    DexFile dexFile;

    public HeaderItem(DexFile dexFile) {
        this.dexFile = dexFile;
    }

    public int getSectionOffset() {
        return 0;
    }

    public int getNumItems() {
        return 1;
    }

    public int getSize() {
        return HEADER_ITEM_SIZE;
    }

    public void write(DexWriter writer, int dataOffset, int fileSize) throws IOException {
        writer.write(DEX_FILE_MAGIC);

        // checksum placeholder
        writer.writeInt(0);

        // signature placeholder
        writer.write(new byte[20]);

        writer.writeInt(fileSize);
        writer.writeInt(HEADER_ITEM_SIZE);
        writer.writeInt(LITTLE_ENDIAN);

        // link
        writer.writeInt(0);
        writer.writeInt(0);

        // map
        writer.writeInt(dexFile.mapItem.getSectionOffset());

        // index sections
        // TODO: double-check whether section offset for an empty section must be 0
        writeSectionInfo(writer, dexFile.stringPool.getNumItems(), dexFile.stringPool.getIndexSectionOffset());
        writeSectionInfo(writer, dexFile.typePool.getNumItems(), dexFile.typePool.getSectionOffset());
        writeSectionInfo(writer, dexFile.protoPool.getNumItems(), dexFile.protoPool.getSectionOffset());
        writeSectionInfo(writer, dexFile.fieldPool.getNumItems(), dexFile.fieldPool.getSectionOffset());
        writeSectionInfo(writer, dexFile.methodPool.getNumItems(), dexFile.methodPool.getSectionOffset());
        writeSectionInfo(writer, dexFile.classDefPool.getNumClassDefItems(), dexFile.classDefPool.getIndexSectionOffset());

        // data section
        writer.writeInt(fileSize - dataOffset);
        writer.writeInt(dataOffset);
    }

    public void updateSignature(FileChannel fileChannel) throws IOException {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }

        ByteBuffer buffer = ByteBuffer.allocate(128 * 1024);
        fileChannel.position(DEX_FILE_MAGIC.length + CHECKSUM_SIZE + SIGNATURE_SIZE);
        int bytesRead = fileChannel.read(buffer);
        while (bytesRead >= 0) {
            buffer.rewind();
            md.update(buffer);
            buffer.clear();
            bytesRead = fileChannel.read(buffer);
        }

        byte[] signature = md.digest();
        if (signature.length != SIGNATURE_SIZE) {
            throw new RuntimeException("unexpected digest write: " + signature.length + " bytes");
        }

        // write signature
        fileChannel.position(DEX_FILE_MAGIC.length + CHECKSUM_SIZE);
        fileChannel.write(ByteBuffer.wrap(signature));

        // flush
        fileChannel.force(false);
    }

    public void updateChecksum(FileChannel fileChannel) throws IOException {
        Adler32 a32 = new Adler32();

        ByteBuffer buffer = ByteBuffer.allocate(128 * 1024);
        fileChannel.position(DEX_FILE_MAGIC.length + CHECKSUM_SIZE);
        int bytesRead = fileChannel.read(buffer);
        while (bytesRead >= 0) {
            a32.update(buffer.array(), 0, bytesRead);
            buffer.clear();
            bytesRead = fileChannel.read(buffer);
        }

        // write checksum, utilizing logic in DexWriter to write the integer value properly
        fileChannel.position(DEX_FILE_MAGIC.length);
        int checksum = (int) a32.getValue();
        ByteArrayOutputStream checksumBuf = new ByteArrayOutputStream();
        DexWriter.writeInt(checksumBuf, checksum);
        fileChannel.write(ByteBuffer.wrap(checksumBuf.toByteArray()));

        // flush
        fileChannel.force(false);
    }

    private void writeSectionInfo(DexWriter writer, int numItems, int offset) throws IOException {
        writer.writeInt(numItems);
        if (numItems > 0) {
            writer.writeInt(offset);
        } else {
            writer.writeInt(0);
        }
    }
}
