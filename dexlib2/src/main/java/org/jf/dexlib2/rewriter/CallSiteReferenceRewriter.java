/*
 * Copyright 2014, Google Inc.
 * Copyright 2022, Zimperium Inc.
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

package org.jf.dexlib2.rewriter;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.jf.dexlib2.base.reference.BaseCallSiteReference;
import org.jf.dexlib2.iface.reference.CallSiteReference;
import org.jf.dexlib2.iface.reference.MethodHandleReference;
import org.jf.dexlib2.iface.reference.MethodProtoReference;
import org.jf.dexlib2.iface.value.EncodedValue;

import javax.annotation.Nonnull;
import java.util.List;

public class CallSiteReferenceRewriter implements Rewriter<CallSiteReference> {
    @Nonnull protected final Rewriters rewriters;

    public CallSiteReferenceRewriter(@Nonnull Rewriters rewriters) {
        this.rewriters = rewriters;
    }

    @Nonnull @Override public CallSiteReference rewrite(@Nonnull CallSiteReference callSiteReference) {
        return new RewrittenCallSiteReference(callSiteReference);
    }

    protected class RewrittenCallSiteReference extends BaseCallSiteReference {
        @Nonnull protected CallSiteReference callSiteReference;

        public RewrittenCallSiteReference(@Nonnull CallSiteReference callSiteReference) {
            this.callSiteReference = callSiteReference;
        }

        @Override @Nonnull public String getName() {
            return callSiteReference.getName();
        }

        @Override @Nonnull public MethodHandleReference getMethodHandle() {
                return RewriterUtils.rewriteMethodHandleReference(
                        rewriters, callSiteReference.getMethodHandle());
        }

        @Override @Nonnull public String getMethodName() {
            return callSiteReference.getMethodName();
        }

        @Override @Nonnull public MethodProtoReference getMethodProto() {
            return RewriterUtils.rewriteMethodProtoReference(
                        rewriters.getTypeRewriter(),
                        callSiteReference.getMethodProto());
        }

        @Override @Nonnull public List<? extends EncodedValue> getExtraArguments() {
            return Lists.transform(callSiteReference.getExtraArguments(),
                    new Function<EncodedValue, EncodedValue>() {
                        @Nonnull @Override public EncodedValue apply(EncodedValue encodedValue) {
                            return RewriterUtils.rewriteValue(rewriters, encodedValue);
                        }
                    });
        }
    }
}
