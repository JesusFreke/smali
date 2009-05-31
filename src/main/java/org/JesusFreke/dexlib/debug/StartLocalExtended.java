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

package org.JesusFreke.dexlib.debug;

import org.JesusFreke.dexlib.*;

public class StartLocalExtended extends CompositeField<StartLocalExtended> implements DebugInstruction<StartLocalExtended> {
    private final ByteField opcodeField;
    //TODO: signed or unsigned leb?
    private final SignedLeb128Field registerNumber;
    private final IndexedItemReference<StringIdItem> localName;
    private final IndexedItemReference<TypeIdItem> localType;
    private final IndexedItemReference<StringIdItem> signature;

    public StartLocalExtended(DexFile dexFile) {
        super("DBG_START_LOCAL_EXTENDED");
        fields = new Field[] {
                opcodeField = new ByteField((byte)0x04, "opcode"),
                registerNumber = new SignedLeb128Field("register_num"),
                localName = new IndexedItemReference<StringIdItem>(dexFile.StringIdsSection,
                        new Leb128p1Field(null), "name_idx"),
                localType = new IndexedItemReference<TypeIdItem>(dexFile.TypeIdsSection,
                        new Leb128p1Field(null), "type_idx"),
                signature = new IndexedItemReference<StringIdItem>(dexFile.StringIdsSection,
                        new Leb128p1Field(null), "sig_idx")
        };
    }

    public StartLocalExtended(DexFile dexFile, int registerNumber, StringIdItem localName, TypeIdItem localType,
                              StringIdItem signature) {
        this(dexFile);
        this.registerNumber.cacheValue(registerNumber);
        this.localName.setReference(localName);
        this.localType.setReference(localType);
        this.signature.setReference(signature);
    }

    public byte getOpcode() {
        return 0x04;
    }
}
