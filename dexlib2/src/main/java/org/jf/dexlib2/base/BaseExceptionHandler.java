/*
 * Copyright 2012, Google Inc.
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

package org.jf.dexlib2.base;

import com.google.common.base.Objects;
import com.google.common.primitives.Ints;
import org.jf.dexlib2.iface.ExceptionHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class BaseExceptionHandler implements ExceptionHandler {
    @Override
    public int hashCode() {
        String exceptionType = getExceptionType();
        int hashCode = exceptionType==null?0:exceptionType.hashCode();
        return hashCode*31 + getHandlerCodeAddress();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (o instanceof ExceptionHandler) {
            ExceptionHandler other = (ExceptionHandler)o;
            return Objects.equal(getExceptionType(), other.getExceptionType()) &&
                   (getHandlerCodeAddress() == other.getHandlerCodeAddress());
        }
        return false;
    }

    @Override
    public int compareTo(@Nonnull ExceptionHandler o) {
        int res;
        String exceptionType = getExceptionType();
        if (exceptionType == null) {
            if (o.getExceptionType() != null) {
                return 1;
            }
        } else {
            res = exceptionType.compareTo(o.getExceptionType());
            if (res != 0) return res;
        }
        return Ints.compare(getHandlerCodeAddress(), o.getHandlerCodeAddress());
    }
}
