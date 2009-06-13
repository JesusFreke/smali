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
import org.jf.dexlib.CodeItem;
import org.jf.dexlib.MethodIdItem;
import org.jf.dexlib.code.Instruction;
import org.jf.dexlib.code.InstructionField;
import org.jf.dexlib.util.AccessFlags;

import java.util.List;
import java.util.ArrayList;

public class MethodDefinition {
    private ClassDataItem.EncodedMethod encodedMethod;
    private MethodIdItem methodIdItem;
    private CodeItem codeItem;

    public MethodDefinition(ClassDataItem.EncodedMethod encodedMethod) {
        this.encodedMethod = encodedMethod;
        this.methodIdItem = encodedMethod.getMethod();
        this.codeItem = encodedMethod.getCodeItem();
    }

    private String methodName = null;
    public String getMethodName() {
        if (methodName == null) {
            methodName = methodIdItem.getMethodName(); 
        }
        return methodName;
    }

    private List<String> accessFlags = null;
    public List<String> getAccessFlags() {
        if (accessFlags == null) {
            accessFlags = new ArrayList<String>();

            for (AccessFlags accessFlag: AccessFlags.getAccessFlagsForMethod(encodedMethod.getAccessFlags())) {
                accessFlags.add(accessFlag.toString());
            }
        }
        return accessFlags;
    }

    private String prototype = null;
    public String getPrototype() {
        if (prototype == null) {
            prototype = methodIdItem.getPrototype().getPrototypeString();
        }
        return prototype;
    }

    private Boolean hasCode = null;
    public boolean getHasCode() {
        if (hasCode == null) {
            hasCode = (codeItem != null);
        }
        return hasCode;
    }

    private String registerCount = null;
    public String getRegisterCount() {
        if (registerCount == null) {
            if (codeItem == null) {
                registerCount = "0";
            } else {
                registerCount = Integer.toString(codeItem.getRegisterCount());
            }
        }
        return registerCount;
    }

    //TODO: how best to abstract this? I don't like exposing dexlib's "Instruction" directly
    private List<Instruction> instructions = null;
    public List<Instruction> getInstructions() {
        if (instructions == null) {
            instructions = new ArrayList<Instruction>();
            if (codeItem != null) {
                for (InstructionField instruction: codeItem.getInstructions()) {
                    instructions.add(instruction.getInstruction());
                }
            }
        }
        return instructions;
    }
}
