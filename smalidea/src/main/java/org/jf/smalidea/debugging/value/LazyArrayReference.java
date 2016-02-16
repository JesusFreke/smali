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

import com.intellij.openapi.project.Project;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.Value;
import org.jf.smalidea.psi.impl.SmaliMethod;

import java.util.List;

public class LazyArrayReference extends LazyObjectReference<ArrayReference> implements ArrayReference {
    public LazyArrayReference(SmaliMethod method, Project project, int registerNumber, String type) {
        super(method, project, registerNumber, type);
    }

    public Value getValue(int index) {
        return getValue().getValue(index);
    }

    public List<Value> getValues() {
        return getValue().getValues();
    }

    public List<Value> getValues(int index, int length) {
        return getValue().getValues(index, length);
    }

    public int length() {
        return getValue().length();
    }

    public void setValue(int index, Value value) throws InvalidTypeException, ClassNotLoadedException {
        getValue().setValue(index, value);
    }

    public void setValues(int index, List<? extends Value> values, int srcIndex, int length) throws InvalidTypeException, ClassNotLoadedException {
        getValue().setValues(index, values, srcIndex, length);
    }

    public void setValues(List<? extends Value> values) throws InvalidTypeException, ClassNotLoadedException {
        getValue().setValues(values);
    }
}
