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

import com.intellij.debugger.NoDataException;
import com.intellij.debugger.PositionManager;
import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.engine.DebugProcess;
import com.intellij.debugger.requests.ClassPrepareRequestor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.search.GlobalSearchScope;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.request.ClassPrepareRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jf.smalidea.psi.impl.SmaliClass;
import org.jf.smalidea.psi.impl.SmaliFile;
import org.jf.smalidea.psi.impl.SmaliMethod;
import org.jf.smalidea.psi.index.SmaliClassNameIndex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SmaliPositionManager implements PositionManager {
    private final DebugProcess debugProcess;

    public SmaliPositionManager(DebugProcess debugProcess) {
        this.debugProcess = debugProcess;
    }

    public SourcePosition getSourcePosition(String declaringType, String methodName, String methodSignature,
                                            int codeIndex) throws NoDataException {
        Collection<SmaliClass> classes = SmaliClassNameIndex.INSTANCE.get(declaringType,
                debugProcess.getProject(), GlobalSearchScope.projectScope(debugProcess.getProject()));

        if (classes.size() > 0) {
            SmaliClass smaliClass = classes.iterator().next();

            // TODO: make an index for this?
            for (SmaliMethod smaliMethod: smaliClass.getMethods()) {
                if (smaliMethod.getName().equals(methodName) &&
                        smaliMethod.getMethodPrototype().getText().equals(methodSignature)) {
                    return smaliMethod.getSourcePositionForCodeOffset(codeIndex * 2);
                }
            }
        }

        throw NoDataException.INSTANCE;
    }

    @Override
    public SourcePosition getSourcePosition(@Nullable Location location) throws NoDataException {
        if (location == null) {
            throw NoDataException.INSTANCE;
        }

        return getSourcePosition(location.declaringType().name(), location.method().name(),
                location.method().signature(), (int)location.codeIndex());
    }

    @Override @NotNull
    public List<ReferenceType> getAllClasses(@NotNull SourcePosition classPosition) throws NoDataException {
        if (!(classPosition.getElementAt().getContainingFile() instanceof SmaliFile)) {
            throw NoDataException.INSTANCE;
        }

        String className = getClassFromPosition(classPosition);
        return debugProcess.getVirtualMachineProxy().classesByName(className);
    }

    @NotNull
    private String getClassFromPosition(@NotNull final SourcePosition position) {
        return ApplicationManager.getApplication().runReadAction(new Computable<String>() {
            @Override public String compute() {
                SmaliClass smaliClass = ((SmaliFile)position.getElementAt().getContainingFile()).getPsiClass();
                if (smaliClass == null) {
                    return "";
                }
                return smaliClass.getQualifiedName();
            }
        });
    }

    @Override @NotNull
    public List<Location> locationsOfLine(@NotNull final ReferenceType type,
                                          @NotNull final SourcePosition position) throws NoDataException {
        if (!(position.getElementAt().getContainingFile() instanceof SmaliFile)) {
            throw NoDataException.INSTANCE;
        }

        final ArrayList<Location> locations = new ArrayList<Location>(1);

        ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            public void run() {
                String typeName = type.name();
                Collection<SmaliClass> classes = SmaliClassNameIndex.INSTANCE.get(typeName, debugProcess.getProject(),
                        GlobalSearchScope.projectScope(debugProcess.getProject()));

                if (classes.size() > 0) {
                    final SmaliClass smaliClass = classes.iterator().next();

                    Location location = smaliClass.getLocationForSourcePosition(type, position);

                    if (location != null) {
                        locations.add(location);
                    }
                }
            }
        });
        return locations;
    }

    @Override
    public ClassPrepareRequest createPrepareRequest(@NotNull final ClassPrepareRequestor requestor,
                                                    @NotNull final SourcePosition position) throws NoDataException {
        Computable<Boolean> isSmaliFile = new Computable<Boolean>() {
            @Override
            public Boolean compute() {
                return position.getFile() instanceof SmaliFile;
            }
        };

        ApplicationManager.getApplication().runReadAction(isSmaliFile);

        if (!isSmaliFile.compute()) {
            throw NoDataException.INSTANCE;
        }

        String className = getClassFromPosition(position);
        return debugProcess.getRequestsManager().createClassPrepareRequest(new ClassPrepareRequestor() {
            @Override
            public void processClassPrepare(DebugProcess debuggerProcess, ReferenceType referenceType) {
                requestor.processClassPrepare(debuggerProcess, referenceType);
            }
        }, className);
    }
}