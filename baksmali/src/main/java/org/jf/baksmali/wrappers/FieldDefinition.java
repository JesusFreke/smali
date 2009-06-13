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

package org.jf.baksmali.wrappers;

import org.jf.dexlib.ClassDataItem;
import org.jf.dexlib.FieldIdItem;
import org.jf.dexlib.util.AccessFlags;

import java.util.List;
import java.util.ArrayList;

public class FieldDefinition {
    private ClassDataItem.EncodedField encodedField;
    private FieldIdItem fieldIdItem;

    public FieldDefinition(ClassDataItem.EncodedField encodedField) {
        this.encodedField = encodedField;
        this.fieldIdItem = encodedField.getFieldReference();
    }

    private List<String> accessFlags = null;
    public List<String> getAccessFlags() {
        if (accessFlags == null) {
            accessFlags = new ArrayList<String>();

            for (AccessFlags accessFlag: AccessFlags.getAccessFlagsForField(encodedField.getAccessFlags())) {
                accessFlags.add(accessFlag.toString());
            }
        }
        return accessFlags;
    }

    private String fieldName = null;
    public String getFieldName() {
        if (fieldName == null) {
            fieldName = fieldIdItem.getFieldName().getStringValue();
        }
        return fieldName;
    }

    private String fieldType = null;
    public String getFieldType() {
        if (fieldType == null) {
            fieldType = fieldIdItem.getFieldType().getTypeDescriptor();
        }
        return fieldType;
    }
}
