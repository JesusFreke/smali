/*
 * Copyright 2018, Google Inc.
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

package org.jf.dexlib2;

import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;

public class MethodHandleType {
    public static final int STATIC_PUT = 0;
    public static final int STATIC_GET = 1;
    public static final int INSTANCE_PUT = 2;
    public static final int INSTANCE_GET = 3;
    public static final int INVOKE_STATIC = 4;
    public static final int INVOKE_INSTANCE = 5;

    @Nonnull public static String toString(int methodHandleType) {
        switch (methodHandleType) {
            case STATIC_PUT:
                return "static_put";
            case STATIC_GET:
                return "static_get";
            case INSTANCE_PUT:
                return "instance_put";
            case INSTANCE_GET:
                return "instance_get";
            case INVOKE_STATIC:
                return "invoke_static";
            case INVOKE_INSTANCE:
                return "invoke_instance";
            default:
                throw new InvalidMethodHandleTypeException(methodHandleType);
        }
    }

    public static class InvalidMethodHandleTypeException extends ExceptionWithContext {
        private final int methodHandleType;

        public InvalidMethodHandleTypeException(int methodHandleType) {
            super("Invalid method handle type: %d", methodHandleType);
            this.methodHandleType = methodHandleType;
        }

        public InvalidMethodHandleTypeException(int methodHandleType, String message, Object... formatArgs) {
            super(message, formatArgs);
            this.methodHandleType = methodHandleType;
        }

        public int getMethodHandleType() {
            return methodHandleType;
        }
    }
}
