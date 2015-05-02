/*
 * Copyright 2014, Google Inc.
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

package org.jf.smalidea.debugging;

import com.google.common.collect.Maps;
import com.intellij.debugger.DebuggerManagerEx;
import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.engine.DebugProcessImpl;
import com.intellij.debugger.engine.evaluation.*;
import com.intellij.debugger.engine.events.DebuggerCommandImpl;
import com.intellij.debugger.impl.DebuggerContextImpl;
import com.intellij.debugger.jdi.StackFrameProxyImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.JavaCodeFragment;
import com.intellij.psi.JavaRecursiveElementVisitor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLocalVariable;
import com.sun.jdi.*;
import com.sun.tools.jdi.LocalVariableImpl;
import com.sun.tools.jdi.LocationImpl;
import org.jf.dexlib2.analysis.AnalyzedInstruction;
import org.jf.dexlib2.analysis.RegisterType;
import org.jf.smalidea.SmaliFileType;
import org.jf.smalidea.SmaliLanguage;
import org.jf.smalidea.psi.impl.SmaliInstruction;
import org.jf.smalidea.psi.impl.SmaliMethod;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class SmaliCodeFragmentFactory extends DefaultCodeFragmentFactory {
    @Override
    public JavaCodeFragment createCodeFragment(TextWithImports item, PsiElement context, Project project) {
        return super.createCodeFragment(item, wrapContext(project, context), project);
    }

    @Override
    public boolean isContextAccepted(PsiElement contextElement) {
        if (contextElement == null) {
            return false;
        }
        return contextElement.getLanguage() == SmaliLanguage.INSTANCE;
    }

    @Override
    public JavaCodeFragment createPresentationCodeFragment(TextWithImports item, PsiElement context, Project project) {
        return super.createPresentationCodeFragment(item, wrapContext(project, context), project);
    }

    @Override public LanguageFileType getFileType() {
        return SmaliFileType.INSTANCE;
    }

    private PsiElement wrapContext(final Project project, final PsiElement originalContext) {
        if (project.isDefault()) return originalContext;

        final DebuggerContextImpl debuggerContext = DebuggerManagerEx.getInstanceEx(project).getContext();

        SourcePosition position = debuggerContext.getSourcePosition();
        SmaliInstruction currentInstruction = (SmaliInstruction)position.getElementAt();

        final SmaliMethod containingMethod = currentInstruction.getParentMethod();
        AnalyzedInstruction analyzedInstruction = currentInstruction.getAnalyzedInstruction();
        if (analyzedInstruction == null) {
            return originalContext;
        }

        final int firstParameterRegister = containingMethod.getRegisterCount() -
                containingMethod.getParameterRegisterCount();

        final Map<String, String> registerMap = Maps.newHashMap();
        StringBuilder variablesText = new StringBuilder();
        for (int i=0; i<containingMethod.getRegisterCount(); i++) {
            int parameterRegisterNumber = i - firstParameterRegister;

            RegisterType registerType = analyzedInstruction.getPreInstructionRegisterType(i);
            switch (registerType.category) {
                case RegisterType.UNKNOWN:
                case RegisterType.UNINIT:
                case RegisterType.CONFLICTED:
                case RegisterType.LONG_HI:
                case RegisterType.DOUBLE_HI:
                    continue;
                case RegisterType.NULL:
                case RegisterType.ONE:
                case RegisterType.INTEGER:
                    variablesText.append("int v").append(i).append(";\n");
                    registerMap.put("v" + i, "I");
                    if (parameterRegisterNumber >= 0) {
                        variablesText.append("int p").append(parameterRegisterNumber).append(";\n");
                        registerMap.put("p" + parameterRegisterNumber, "I");
                    }
                    break;
                case RegisterType.BOOLEAN:
                    variablesText.append("boolean v").append(i).append(";\n");
                    registerMap.put("v" + i, "Z");
                    if (parameterRegisterNumber >= 0) {
                        variablesText.append("boolean p").append(parameterRegisterNumber).append(";\n");
                        registerMap.put("p" + parameterRegisterNumber, "Z");
                    }
                    break;
                case RegisterType.BYTE:
                case RegisterType.POS_BYTE:
                    variablesText.append("byte v").append(i).append(";\n");
                    registerMap.put("v" + i, "B");
                    if (parameterRegisterNumber >= 0) {
                        variablesText.append("byte p").append(parameterRegisterNumber).append(";\n");
                        registerMap.put("p" + parameterRegisterNumber, "B");
                    }
                    break;
                case RegisterType.SHORT:
                case RegisterType.POS_SHORT:
                    variablesText.append("short v").append(i).append(";\n");
                    registerMap.put("v" + i, "S");
                    if (parameterRegisterNumber >= 0) {
                        variablesText.append("short p").append(parameterRegisterNumber).append(";\n");
                        registerMap.put("p" + parameterRegisterNumber, "S");
                    }
                    break;
                case RegisterType.CHAR:
                    variablesText.append("char v").append(i).append(";\n");
                    registerMap.put("v" + i, "C");
                    if (parameterRegisterNumber >= 0) {
                        variablesText.append("char p").append(parameterRegisterNumber).append(";\n");
                        registerMap.put("p" + parameterRegisterNumber, "C");
                    }
                    break;
                case RegisterType.FLOAT:
                    variablesText.append("float v").append(i).append(";\n");
                    registerMap.put("v" + i, "F");
                    if (parameterRegisterNumber >= 0) {
                        variablesText.append("float p").append(parameterRegisterNumber).append(";\n");
                        registerMap.put("p" + parameterRegisterNumber, "F");
                    }
                    break;
                case RegisterType.LONG_LO:
                    variablesText.append("long v").append(i).append(";\n");
                    registerMap.put("v" + i, "J");
                    if (parameterRegisterNumber >= 0) {
                        variablesText.append("long p").append(parameterRegisterNumber).append(";\n");
                        registerMap.put("p" + parameterRegisterNumber, "J");
                    }
                    break;
                case RegisterType.DOUBLE_LO:
                    variablesText.append("double v").append(i).append(";\n");
                    registerMap.put("v" + i, "D");
                    if (parameterRegisterNumber >= 0) {
                        variablesText.append("double p").append(parameterRegisterNumber).append(";\n");
                        registerMap.put("p" + parameterRegisterNumber, "D");
                    }
                    break;
                case RegisterType.UNINIT_REF:
                case RegisterType.UNINIT_THIS:
                case RegisterType.REFERENCE:
                    variablesText.append("Object v").append(i).append(";\n");
                    registerMap.put("v" + i, "Ljava/lang/Object;");
                    if (parameterRegisterNumber >= 0) {
                        variablesText.append("Object p").append(parameterRegisterNumber).append(";\n");
                        registerMap.put("p" + parameterRegisterNumber, "Ljava/lang/Object;");
                    }
                    break;
            }
        }
        final TextWithImportsImpl textWithImports = new TextWithImportsImpl(CodeFragmentKind.CODE_BLOCK,
                variablesText.toString(), "", getFileType());

        final JavaCodeFragment codeFragment = super.createCodeFragment(textWithImports, originalContext, project);

        codeFragment.accept(new JavaRecursiveElementVisitor() {
            @Override
            public void visitLocalVariable(final PsiLocalVariable variable) {
                final String name = variable.getName();
                if (registerMap.containsKey(name)) {
                    DebugProcessImpl process = debuggerContext.getDebugProcess();
                    if (process == null) {
                        return;
                    }

                    process.getManagerThread().invoke(new DebuggerCommandImpl() {
                        @Override protected void action() throws Exception {
                            int registerNumber = Integer.parseInt(name.substring(1));
                            if (name.charAt(0) == 'p') {
                                registerNumber += ApplicationManager.getApplication().runReadAction(new Computable<Integer>() {
                                    @Override public Integer compute() {
                                        return containingMethod.getRegisterCount() -
                                                containingMethod.getParameterRegisterCount();
                                    }
                                });
                            }
                            Value value = evaluateRegister(debuggerContext.createEvaluationContext(),
                                    containingMethod,
                                    registerNumber,
                                    registerMap.get(name));
                            variable.putUserData(CodeFragmentFactoryContextWrapper.LABEL_VARIABLE_VALUE_KEY, value);
                        }
                    });
                }
            }
        });

        int offset = variablesText.length() - 1;

        final PsiElement newContext = codeFragment.findElementAt(offset);
        if (newContext != null) {
            return newContext;
        }
        return originalContext;
    }

    public Value evaluateRegister(EvaluationContextImpl context, final SmaliMethod smaliMethod, final int registerNum,
                                  final String type) throws EvaluateException {
        final StackFrameProxyImpl frameProxy = context.getFrameProxy();
        if (frameProxy == null) {
            return null;
        }

        // the jdi APIs don't provide any way to get the value of an arbitrary register, so we use reflection
        // to create a LocalVariable instance for the register

        VirtualMachine vm = frameProxy.getVirtualMachine().getVirtualMachine();
        Location currentLocation = frameProxy.location();
        if (currentLocation == null) {
            return null;
        }

        Method method = currentLocation.method();

        try {
            final Constructor<LocalVariableImpl> localVariableConstructor = LocalVariableImpl.class.getDeclaredConstructor(
                    VirtualMachine.class, Method.class, Integer.TYPE, Location.class, Location.class, String.class,
                    String.class, String.class);
            localVariableConstructor.setAccessible(true);

            Constructor<LocationImpl> locationConstructor = LocationImpl.class.getDeclaredConstructor(
                    VirtualMachine.class, Method.class, Long.TYPE);
            locationConstructor.setAccessible(true);

            // TODO: use frameProxy.location().method().locationOfCodeIndex() here
            Location endLocation = locationConstructor.newInstance(vm, method, Integer.MAX_VALUE);

            LocalVariable localVariable = localVariableConstructor.newInstance(vm,
                    method,
                    mapRegister(frameProxy.getStackFrame().virtualMachine(), smaliMethod, registerNum),
                    method.locationOfCodeIndex(0),
                    endLocation,
                    String.format("v%d", registerNum), type, null);

            return frameProxy.getStackFrame().getValue(localVariable);
        } catch (NoSuchMethodException e) {
            return null;
        } catch (InstantiationException e) {
            return null;
        } catch (IllegalAccessException e) {
            return null;
        } catch (InvocationTargetException e) {
            return null;
        }
    }

    private static int mapRegister(final VirtualMachine vm, final SmaliMethod smaliMethod, final int register) {
        if (vm.version().equals("1.5.0")) {
            return mapRegisterForDalvik(smaliMethod, register);
        } else {
            return mapRegisterForArt(smaliMethod, register);
        }
    }

    private static int mapRegisterForArt(final SmaliMethod smaliMethod, final int register) {
        return ApplicationManager.getApplication().runReadAction(new Computable<Integer>() {
            @Override public Integer compute() {

                int totalRegisters = smaliMethod.getRegisterCount();
                int parameterRegisters = smaliMethod.getParameterRegisterCount();

                if (smaliMethod.getModifierList().hasModifierProperty("static")) {
                    return register;
                }

                // For ART, the parameter registers are rotated to the front
                if (register >= (totalRegisters - parameterRegisters)) {
                    return register - (totalRegisters - parameterRegisters);
                }
                return register + parameterRegisters;
            }
        });
    }

    private static int mapRegisterForDalvik(final SmaliMethod smaliMethod, final int register) {
        return ApplicationManager.getApplication().runReadAction(new Computable<Integer>() {
            @Override public Integer compute() {
                if (smaliMethod.getModifierList().hasModifierProperty("static")) {
                    return register;
                }

                int totalRegisters = smaliMethod.getRegisterCount();
                int parameterRegisters = smaliMethod.getParameterRegisterCount();

                // For dalvik, p0 is mapped to register 1, and register 0 is mapped to register 1000
                if (register == (totalRegisters - parameterRegisters)) {
                    return 0;
                }
                if (register == 0) {
                    return 1000;
                }
                return register;
            }
        });
    }
}

