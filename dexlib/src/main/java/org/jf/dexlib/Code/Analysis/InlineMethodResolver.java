/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver
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

package org.jf.dexlib.Code.Analysis;

import org.jf.dexlib.Code.Format.Instruction35ms;
import org.jf.dexlib.Code.Format.Instruction3rms;
import org.jf.dexlib.Code.OdexedInvokeVirtual;
import org.jf.dexlib.MethodIdItem;

import static org.jf.dexlib.Code.Analysis.DeodexUtil.Static;
import static org.jf.dexlib.Code.Analysis.DeodexUtil.Virtual;
import static org.jf.dexlib.Code.Analysis.DeodexUtil.Direct;

abstract class InlineMethodResolver {
    public static InlineMethodResolver createInlineMethodResolver(DeodexUtil deodexUtil, int odexVersion) {
        if (odexVersion == 35) {
            return new InlineMethodResolver_version35(deodexUtil);
        } else if (odexVersion == 36) {
            return new InlineMethodResolver_version36(deodexUtil);
        } else {
            throw new RuntimeException(String.format("odex version %d is not supported yet", odexVersion));
        }
    }

    private InlineMethodResolver() {
    }

    public abstract DeodexUtil.InlineMethod resolveExecuteInline(AnalyzedInstruction instruction);

    private static class InlineMethodResolver_version35 extends InlineMethodResolver
    {
        private final DeodexUtil.InlineMethod[] inlineMethods;

        public InlineMethodResolver_version35(DeodexUtil deodexUtil) {
            inlineMethods = new DeodexUtil.InlineMethod[] {
                deodexUtil.new InlineMethod(Static, "Lorg/apache/harmony/dalvik/NativeTestTarget;", "emptyInlineMethod", "", "V"),
                deodexUtil.new InlineMethod(Virtual, "Ljava/lang/String;", "charAt", "I", "C"),
                deodexUtil.new InlineMethod(Virtual, "Ljava/lang/String;", "compareTo", "Ljava/lang/String;", "I"),
                deodexUtil.new InlineMethod(Virtual, "Ljava/lang/String;", "equals", "Ljava/lang/Object;", "Z"),
                deodexUtil.new InlineMethod(Virtual, "Ljava/lang/String;", "length", "", "I"),
                deodexUtil.new InlineMethod(Static, "Ljava/lang/Math;", "abs", "I", "I"),
                deodexUtil.new InlineMethod(Static, "Ljava/lang/Math;", "abs", "J", "J"),
                deodexUtil.new InlineMethod(Static, "Ljava/lang/Math;", "abs", "F", "F"),
                deodexUtil.new InlineMethod(Static, "Ljava/lang/Math;", "abs", "D", "D"),
                deodexUtil.new InlineMethod(Static, "Ljava/lang/Math;", "min", "II", "I"),
                deodexUtil.new InlineMethod(Static, "Ljava/lang/Math;", "max", "II", "I"),
                deodexUtil.new InlineMethod(Static, "Ljava/lang/Math;", "sqrt", "D", "D"),
                deodexUtil.new InlineMethod(Static, "Ljava/lang/Math;", "cos", "D", "D"),
                deodexUtil.new InlineMethod(Static, "Ljava/lang/Math;", "sin", "D", "D")
            };
        }

        @Override
        public DeodexUtil.InlineMethod resolveExecuteInline(AnalyzedInstruction analyzedInstruction) {
            assert analyzedInstruction.instruction instanceof OdexedInvokeVirtual;

            OdexedInvokeVirtual instruction = (OdexedInvokeVirtual)analyzedInstruction.instruction;
            int methodIndex = instruction.getMethodIndex();

            if (methodIndex < 0 || methodIndex >= inlineMethods.length) {
                throw new RuntimeException("Invalid method index: " + methodIndex);
            }
            return inlineMethods[methodIndex];
        }
    };

    private static class InlineMethodResolver_version36 extends InlineMethodResolver
    {
        private final DeodexUtil.InlineMethod[] inlineMethods;
        private final DeodexUtil.InlineMethod indexOfIMethod;
        private final DeodexUtil.InlineMethod indexOfIIMethod;
        private final DeodexUtil.InlineMethod fastIndexOfMethod;
        private final DeodexUtil.InlineMethod isEmptyMethod;


        public InlineMethodResolver_version36(DeodexUtil deodexUtil) {
            //The 5th and 6th entries differ between froyo and gingerbread. We have to look at the parameters being
            //passed to distinguish between them.

            //froyo
            indexOfIMethod = deodexUtil.new InlineMethod(Virtual, "Ljava/lang/String;", "indexOf", "I", "I");
            indexOfIIMethod = deodexUtil.new InlineMethod(Virtual, "Ljava/lang/String;", "indexOf", "II", "I");

            //gingerbread
            fastIndexOfMethod = deodexUtil.new InlineMethod(Direct, "Ljava/lang/String;", "fastIndexOf", "II", "I");
            isEmptyMethod = deodexUtil.new InlineMethod(Virtual, "Ljava/lang/String;", "isEmpty", "", "Z");

            inlineMethods = new DeodexUtil.InlineMethod[] {
                deodexUtil.new InlineMethod(Static, "Lorg/apache/harmony/dalvik/NativeTestTarget;", "emptyInlineMethod", "", "V"),
                deodexUtil.new InlineMethod(Virtual, "Ljava/lang/String;", "charAt", "I", "C"),
                deodexUtil.new InlineMethod(Virtual, "Ljava/lang/String;", "compareTo", "Ljava/lang/String;", "I"),
                deodexUtil.new InlineMethod(Virtual, "Ljava/lang/String;", "equals", "Ljava/lang/Object;", "Z"),
                //froyo: deodexUtil.new InlineMethod(Virtual, "Ljava/lang/String;", "indexOf", "I", "I"),
                //gingerbread: deodexUtil.new InlineMethod(Virtual, "Ljava/lang/String;", "fastIndexOf", "II", "I"),
                null,
                //froyo: deodexUtil.new InlineMethod(Virtual, "Ljava/lang/String;", "indexOf", "II", "I"),
                //gingerbread: deodexUtil.new InlineMethod(Virtual, "Ljava/lang/String;", "isEmpty", "", "Z"),
                null,
                deodexUtil.new InlineMethod(Virtual, "Ljava/lang/String;", "length", "", "I"),
                deodexUtil.new InlineMethod(Static, "Ljava/lang/Math;", "abs", "I", "I"),
                deodexUtil.new InlineMethod(Static, "Ljava/lang/Math;", "abs", "J", "J"),
                deodexUtil.new InlineMethod(Static, "Ljava/lang/Math;", "abs", "F", "F"),
                deodexUtil.new InlineMethod(Static, "Ljava/lang/Math;", "abs", "D", "D"),
                deodexUtil.new InlineMethod(Static, "Ljava/lang/Math;", "min", "II", "I"),
                deodexUtil.new InlineMethod(Static, "Ljava/lang/Math;", "max", "II", "I"),
                deodexUtil.new InlineMethod(Static, "Ljava/lang/Math;", "sqrt", "D", "D"),
                deodexUtil.new InlineMethod(Static, "Ljava/lang/Math;", "cos", "D", "D"),
                deodexUtil.new InlineMethod(Static, "Ljava/lang/Math;", "sin", "D", "D"),
                deodexUtil.new InlineMethod(Static, "Ljava/lang/Float;", "floatToIntBits", "F", "I"),
                deodexUtil.new InlineMethod(Static, "Ljava/lang/Float;", "floatToRawIntBits", "F", "I"),
                deodexUtil.new InlineMethod(Static, "Ljava/lang/Float;", "intBitsToFloat", "I", "F"),
                deodexUtil.new InlineMethod(Static, "Ljava/lang/Double;", "doubleToLongBits", "D", "J"),
                deodexUtil.new InlineMethod(Static, "Ljava/lang/Double;", "doubleToRawLongBits", "D", "J"),
                deodexUtil.new InlineMethod(Static, "Ljava/lang/Double;", "longBitsToDouble", "J", "D")
            };
        }

        @Override
        public DeodexUtil.InlineMethod resolveExecuteInline(AnalyzedInstruction analyzedInstruction) {
            assert analyzedInstruction.instruction instanceof OdexedInvokeVirtual;

            OdexedInvokeVirtual instruction = (OdexedInvokeVirtual)analyzedInstruction.instruction;
            int methodIndex = instruction.getMethodIndex();

            if (methodIndex < 0 || methodIndex >= inlineMethods.length) {
                throw new RuntimeException("Invalid method index: " + methodIndex);
            }

            if (methodIndex == 4) {
                int parameterCount = getParameterCount(instruction);
                if (parameterCount == 2) {
                    return indexOfIMethod;
                } else if (parameterCount == 3) {
                    return fastIndexOfMethod;
                } else {
                    throw new RuntimeException("Could not determine the correct inline method to use");
                }
            } else if (methodIndex == 5) {
                int parameterCount = getParameterCount(instruction);
                if (parameterCount == 3) {
                    return indexOfIIMethod;
                } else if (parameterCount == 1) {
                    return isEmptyMethod;
                } else {
                    throw new RuntimeException("Could not determine the correct inline method to use");
                }
            }

            return inlineMethods[methodIndex];
        }

        private int getParameterCount(OdexedInvokeVirtual instruction) {
            if (instruction instanceof Instruction35ms) {
                return ((Instruction35ms)instruction).getRegCount();
            } else {
                return ((Instruction3rms)instruction).getRegCount();
            }
        }
    };
}
