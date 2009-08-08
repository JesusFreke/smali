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

package org.jf.baksmali.Adaptors;

import org.jf.dexlib.CodeItem;
import org.jf.baksmali.baksmali;

/**
 * This class contains the logic used for formatting registers
 */
public class RegisterFormatter {

    /**
     * This method is used (only) by format 3rc (the format that uses a range of regsiters like {v1 .. v10}) to format
     * it's registers. If both registers are parameter registers, they will be formatted as such, otherwise they will
     * both be formatted as normal registers
     * @param codeItem
     * @param startRegister
     * @param lastRegister
     * @return an array of 2 strings containing the formatted registers
     */
    public static String[] formatFormat3rcRegisters(CodeItem codeItem, int startRegister, int lastRegister) {
        if (!baksmali.noParameterRegisters) {
            int parameterRegisterCount = codeItem.getParent().method.getPrototype().getParameterRegisterCount()
                + (codeItem.getParent().isDirect()?0:1);
            int registerCount = codeItem.getRegisterCount();

            assert startRegister <= lastRegister;
            
            if (startRegister >= registerCount - parameterRegisterCount) {
                return new String[] {"p" + (startRegister - (registerCount - parameterRegisterCount)),
                                     "p" + (lastRegister - (registerCount - parameterRegisterCount))};
            }
        }
        return new String[] {"v" + startRegister,
                             "v" + lastRegister};
    }

    /**
     * Formats a register with the appropriate format - with either the normal v<n> format or the p<n> parameter format.
     *
     * It uses the register and parameter information from the give <code>CodeItem</code> to determine if the given
     * register is a normal or parameter register.
     * @param codeItem
     * @param register
     * @return The formatted register
     */
    public static String formatRegister(CodeItem codeItem, int register) {
        if (!baksmali.noParameterRegisters) {
            int parameterRegisterCount = codeItem.getParent().method.getPrototype().getParameterRegisterCount()
                + (codeItem.getParent().isDirect()?0:1);
            int registerCount = codeItem.getRegisterCount();
            if (register >= registerCount - parameterRegisterCount) {
                return "p" + (register - (registerCount - parameterRegisterCount));
            }
        }
        return "v" + register;
    }
}
