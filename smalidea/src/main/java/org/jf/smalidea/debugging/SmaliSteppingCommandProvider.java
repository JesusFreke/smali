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

package org.jf.smalidea.debugging;

import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.engine.ContextUtil;
import com.intellij.debugger.engine.DebugProcessImpl.ResumeCommand;
import com.intellij.debugger.engine.SuspendContextImpl;
import com.intellij.debugger.engine.events.DebuggerCommandImpl;
import com.intellij.debugger.impl.JvmSteppingCommandProvider;
import com.sun.jdi.request.StepRequest;
import org.jetbrains.annotations.NotNull;
import org.jf.smalidea.SmaliLanguage;

public class SmaliSteppingCommandProvider extends JvmSteppingCommandProvider {
    @Override
    public ResumeCommand getStepOverCommand(@NotNull final SuspendContextImpl suspendContext, boolean ignoreBreakpoints,
                                            int stepSize) {

        final SourcePosition[] location = new SourcePosition[1];

        suspendContext.getDebugProcess().getManagerThread().invokeAndWait(new DebuggerCommandImpl() {
            @Override protected void action() throws Exception {
                location[0] = ContextUtil.getSourcePosition(suspendContext);
            }
        })                                                                        ;

        if (location[0] != null && location[0].getFile().getLanguage() == SmaliLanguage.INSTANCE) {
            return suspendContext.getDebugProcess().createStepOverCommand(suspendContext, ignoreBreakpoints,
                    StepRequest.STEP_MIN);
        }
        return null;
    }
}
