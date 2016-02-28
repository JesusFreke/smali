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

package org.jf.smalidea;

import com.google.common.collect.Lists;
import com.intellij.debugger.NoDataException;
import com.intellij.debugger.PositionManager;
import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.engine.DebugProcess;
import com.intellij.debugger.engine.DebugProcessListener;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluationContext;
import com.intellij.debugger.engine.jdi.VirtualMachineProxy;
import com.intellij.debugger.engine.managerThread.DebuggerManagerThread;
import com.intellij.debugger.requests.RequestManager;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import com.sun.jdi.*;
import org.jetbrains.annotations.NotNull;
import org.jf.dexlib2.Opcode;
import org.jf.smalidea.debugging.SmaliPositionManager;
import org.jf.smalidea.psi.impl.SmaliInstruction;
import org.junit.Assert;

import java.util.List;
import java.util.Map;

public class SmaliPositionManagerTest extends LightCodeInsightFixtureTestCase {
    private static final String testClass =
            "\n\n.class public Lmy/pkg/blah; .super Ljava/lang/Object;\n" +
                    ".method public getRandomParentType(I)I\n" +
                    "    .registers 4\n" +
                    "    .param p1, \"edge\"    # I\n" +
                    "\n" +
                    "    .prologue\n" +
                    "    const/4 v1, 0x2\n" +
                    "\n" +
                    "    .line 179\n" +
                    "    if-nez p1, :cond_5\n" +
                    "\n" +
                    "    move v0, v1\n" +
                    "\n" +
                    "    .line 185\n" +
                    "    :goto_4\n" +
                    "    return v0\n" +
                    "\n" +
                    "    .line 182\n" +
                    "    :cond_5\n" +
                    "    if-ne p1, v1, :cond_f\n" +
                    "\n" +
                    "    .line 183\n" +
                    "    sget-object v0, Lorg/jf/Penroser/PenroserApp;->random:Ljava/util/Random;\n" +
                    "\n" +
                    "    const/4 v1, 0x3\n" +
                    "\n" +
                    "    invoke-virtual {v0, v1}, Ljava/util/Random;->nextInt(I)I\n" +
                    "\n" +
                    "    move-result v0\n" +
                    "\n" +
                    "    goto :goto_4\n" +
                    "\n" +
                    "    .line 185\n" +
                    "    :cond_f\n" +
                    "    sget-object v0, Lorg/jf/Penroser/PenroserApp;->random:Ljava/util/Random;\n" +
                    "\n" +
                    "    invoke-virtual {v0, v1}, Ljava/util/Random;->nextInt(I)I\n" +
                    "\n" +
                    "    move-result v0\n" +
                    "\n" +
                    "    goto :goto_4\n" +
                    ".end method";

    public void testGetSourcePosition() throws NoDataException {
        myFixture.addFileToProject("my/pkg/blah.smali", testClass);

        SmaliPositionManager positionManager = new SmaliPositionManager(new MockDebugProcess());

        SourcePosition sourcePosition = positionManager.getSourcePosition(
                "my.pkg.blah", "getRandomParentType", "(I)I", 0);
        Assert.assertEquals(Opcode.CONST_4, ((SmaliInstruction)sourcePosition.getElementAt()).getOpcode());
        Assert.assertEquals(0, ((SmaliInstruction)sourcePosition.getElementAt()).getOffset());

        sourcePosition = positionManager.getSourcePosition("my.pkg.blah", "getRandomParentType", "(I)I", 10);
        Assert.assertEquals(Opcode.INVOKE_VIRTUAL, ((SmaliInstruction)sourcePosition.getElementAt()).getOpcode());
        Assert.assertEquals(20, ((SmaliInstruction)sourcePosition.getElementAt()).getOffset());
    }

    public void testGetAllClasses() throws NoDataException {
        myFixture.addFileToProject("my/pkg/blah.smali", testClass);

        SmaliPositionManager positionManager = new SmaliPositionManager(new MockDebugProcess());

        List<ReferenceType> classes = positionManager.getAllClasses(positionManager.getSourcePosition(
                "my.pkg.blah", "getRandomParentType", "(I)I", 0));
        Assert.assertEquals(1, classes.size());
        Assert.assertEquals("my.pkg.blah", classes.get(0).name());
    }

    private class MockDebugProcess implements DebugProcess {
        @Override public Project getProject() {
            return SmaliPositionManagerTest.this.getProject();
        }

        @Override public VirtualMachineProxy getVirtualMachineProxy() {
            return new VirtualMachineProxy() {
                @Override public List<ReferenceType> classesByName(final String s) {
                    return Lists.<ReferenceType>newArrayList(new MockReferenceType(s));
                }

                @Override public List<ReferenceType> allClasses() { return null; }
                @Override public boolean canGetBytecodes() { return false; }
                @Override public boolean versionHigher(String version) { return false; }
                @Override public boolean canWatchFieldModification() { return false; }
                @Override public boolean canWatchFieldAccess() { return false; }
                @Override public boolean canInvokeMethods() { return false; }
                @Override public DebugProcess getDebugProcess() { return null; }
                @Override public List<ReferenceType> nestedTypes(ReferenceType refType) { return null; }
            };
        }

        @Override public void addDebugProcessListener(DebugProcessListener listener) {}
        @Override public <T> T getUserData(Key<T> key) { return null; }
        @Override public <T> void putUserData(Key<T> key, T value) {}
        @Override public RequestManager getRequestsManager() { return null; }
        @Override public PositionManager getPositionManager() { return null; }
        @Override public void removeDebugProcessListener(DebugProcessListener listener) {}
        @Override public void appendPositionManager(PositionManager positionManager) {}
        @Override public void waitFor() {}
        @Override public void waitFor(long timeout) {}
        @Override public void stop(boolean forceTerminate) {}
        @Override public ExecutionResult getExecutionResult() { return null; }
        @Override public DebuggerManagerThread getManagerThread() { return null; }
        @Override public Value invokeMethod(EvaluationContext evaluationContext, ObjectReference objRef, Method method, List args) throws EvaluateException { return null; }
        @Override public Value invokeMethod(EvaluationContext evaluationContext, ClassType classType, Method method, List args) throws EvaluateException { return null; }
        @Override public Value invokeInstanceMethod(EvaluationContext evaluationContext, ObjectReference objRef, Method method, List args, int invocationOptions) throws EvaluateException { return null; }
        @Override public ReferenceType findClass(EvaluationContext evaluationContext, String name, ClassLoaderReference classLoader) throws EvaluateException { return null; }
        @Override public ArrayReference newInstance(ArrayType arrayType, int dimension) throws EvaluateException { return null; }
        @Override public ObjectReference newInstance(EvaluationContext evaluationContext, ClassType classType, Method constructor, List paramList) throws EvaluateException { return null; }
        @Override public boolean isAttached() { return false; }
        @Override public boolean isDetached() { return false; }
        @Override public boolean isDetaching() { return false; }
        @NotNull @Override public GlobalSearchScope getSearchScope() { return null; }
        @Override public void printToConsole(String text) {}
        @Override public ProcessHandler getProcessHandler() { return null; }
    }

    private static class MockReferenceType implements ReferenceType {
        private final String name;

        public MockReferenceType(String name) {
            this.name = name;
        }

        @Override public String name() {
            return name;
        }

        @Override public List<Field> allFields() { return null; }
        @Override public String genericSignature() { return null; }
        @Override public ClassLoaderReference classLoader() { return null; }
        @Override public String sourceName() throws AbsentInformationException { return null; }
        @Override public List<String> sourceNames(String s) throws AbsentInformationException { return null; }
        @Override public List<String> sourcePaths(String s) throws AbsentInformationException { return null; }
        @Override public String sourceDebugExtension() throws AbsentInformationException { return null; }
        @Override public boolean isStatic() { return false; }
        @Override public boolean isAbstract() { return false; }
        @Override public boolean isFinal() { return false; }
        @Override public boolean isPrepared() { return false; }
        @Override public boolean isVerified() { return false; }
        @Override public boolean isInitialized() { return false; }
        @Override public boolean failedToInitialize() { return false; }
        @Override public List<Field> fields() { return null; }
        @Override public List<Field> visibleFields() { return null; }
        @Override public Field fieldByName(String s) { return null; }
        @Override public List<Method> methods() { return null; }
        @Override public List<Method> visibleMethods() { return null; }
        @Override public List<Method> allMethods() { return null; }
        @Override public List<Method> methodsByName(String s) { return null; }
        @Override public List<Method> methodsByName(String s, String s1) { return null; }
        @Override public List<ReferenceType> nestedTypes() { return null; }
        @Override public Value getValue(Field field) { return null; }
        @Override public Map<Field, Value> getValues(List<? extends Field> list) { return null; }
        @Override public ClassObjectReference classObject() { return null; }
        @Override public List<Location> allLineLocations() throws AbsentInformationException { return null; }
        @Override public List<Location> allLineLocations(String s, String s1) throws AbsentInformationException { return null; }
        @Override public List<Location> locationsOfLine(int i) throws AbsentInformationException { return null; }
        @Override public List<Location> locationsOfLine(String s, String s1, int i) throws AbsentInformationException { return null; }
        @Override public List<String> availableStrata() { return null; }
        @Override public String defaultStratum() { return null; }
        @Override public List<ObjectReference> instances(long l) { return null; }
        @Override public int majorVersion() { return 0; }
        @Override public int minorVersion() { return 0; }
        @Override public int constantPoolCount() { return 0; }
        @Override public byte[] constantPool() { return new byte[0]; }
        @Override public int modifiers() { return 0; }
        @Override public boolean isPrivate() { return false; }
        @Override public boolean isPackagePrivate() { return false; }
        @Override public boolean isProtected() { return false; }
        @Override public boolean isPublic() { return false; }
        @Override public int compareTo(ReferenceType o) { return 0; }
        @Override public String signature() { return null; }
        @Override public VirtualMachine virtualMachine() { return null; }
    }
}
