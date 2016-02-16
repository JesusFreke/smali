/*
 * Copyright 2016, Google Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 * Neither the name of Google Inc. nor the names of its
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

package org.jf.smalidea.debugging.value;

import com.intellij.debugger.DebuggerManagerEx;
import com.intellij.debugger.engine.DebugProcessImpl;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluationContext;
import com.intellij.debugger.impl.DebuggerContextImpl;
import com.intellij.debugger.jdi.VirtualMachineProxyImpl;
import com.intellij.openapi.project.Project;
import com.sun.jdi.Type;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import org.jf.smalidea.debugging.SmaliCodeFragmentFactory;
import org.jf.smalidea.psi.impl.SmaliMethod;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class LazyValue<T extends Value> implements Value {
    private final int registerNumber;
    private final Project project;
    private final SmaliMethod method;
    private final String type;

    private EvaluationContext evaluationContext;
    private Value value;

    public LazyValue(SmaliMethod method, Project project, int registerNumber, String type) {
        this.method = method;
        this.project = project;
        this.registerNumber = registerNumber;
        this.type = type;
    }

    public static LazyValue create(@Nonnull SmaliMethod method, @Nonnull Project project, int registerNumber,
                                   @Nonnull String type) {
        if (type.equals("B")) {
            return new LazyByteValue(method, project, registerNumber, type);
        } else if (type.equals("S")) {
            return new LazyShortValue(method, project, registerNumber, type);
        } else if (type.equals("J")) {
            return new LazyLongValue(method, project, registerNumber, type);
        } else if (type.equals("I")) {
            return new LazyIntegerValue(method, project, registerNumber, type);
        } else if (type.equals("F")) {
            return new LazyFloatValue(method, project, registerNumber, type);
        } else if (type.equals("D")) {
            return new LazyDoubleValue(method, project, registerNumber, type);
        } else if (type.equals("Z")) {
            return new LazyBooleanValue(method, project, registerNumber, type);
        } else if (type.equals("C")) {
            return new LazyCharValue(method, project, registerNumber, type);
        } else if (type.equals("V")) {
            return new LazyVoidValue(method, project, registerNumber, type);
        } else if (type.startsWith("[")) {
            return new LazyArrayReference(method, project, registerNumber, type);
        } else if (type.equals("Ljava/lang/String;")) {
            return new LazyStringReference(method, project, registerNumber, type);
        } else if (type.equals("Ljava/lang/Class;")) {
            return new LazyClassObjectReference(method, project, registerNumber, type);
        } else if (type.equals("Ljava/lang/ThreadGroup;")) {
            return new LazyThreadGroupReference(method, project, registerNumber, type);
        } else if (type.equals("Ljava/lang/Thread;")) {
            return new LazyThreadReference(method, project, registerNumber, type);
        } else if (type.equals("Ljava/lang/ClassLoader;")) {
            return new LazyClassLoaderReference(method, project, registerNumber, type);
        } else if (type.startsWith("L")) {
            return new LazyObjectReference(method, project, registerNumber, type);
        }
        return new LazyValue(method, project, registerNumber, type);
    }

    @Nullable
    private T getNullableValue() {
        if (value == null) {
            try {
                if (evaluationContext == null) {
                    final DebuggerContextImpl debuggerContext = DebuggerManagerEx.getInstanceEx(project).getContext();
                    evaluationContext = debuggerContext.createEvaluationContext();
                    if (evaluationContext == null) {
                        return null;
                    }
                }

                value = SmaliCodeFragmentFactory.evaluateRegister(evaluationContext, method, registerNumber, type);
                evaluationContext = null;
            } catch (EvaluateException ex) {
                return null;
            }
        }
        return (T)value;
    }

    @Nonnull
    protected T getValue() {
        T value = getNullableValue();
        assert value != null;
        return value;
    }

    @Override
    public Type type() {
        return getValue().type();
    }

    @Override
    public VirtualMachine virtualMachine() {
        if (evaluationContext != null) {
            return ((VirtualMachineProxyImpl)evaluationContext.getDebugProcess().getVirtualMachineProxy())
                    .getVirtualMachine();
        } else {
            final DebuggerContextImpl debuggerContext = DebuggerManagerEx.getInstanceEx(project).getContext();
            final DebugProcessImpl process = debuggerContext.getDebugProcess();
            if (process != null) {
                return process.getVirtualMachineProxy().getVirtualMachine();
            }
        }
        return null;
    }

    public void setEvaluationContext(@Nonnull EvaluationContext evaluationContext) {
        this.evaluationContext = evaluationContext;
    }

    @Override public boolean equals(Object obj) {
        Value value = getNullableValue();
        if (value != null) {
            return value.equals(obj);
        }
        return super.equals(obj);
    }

    @Override public int hashCode() {
        Value value = getNullableValue();
        if (value != null) {
            return value.hashCode();
        }
        return super.hashCode();
    }

    @Override public String toString() {
        Value value = getNullableValue();
        if (value != null) {
            return value.toString();
        }
        return super.toString();
    }
}
