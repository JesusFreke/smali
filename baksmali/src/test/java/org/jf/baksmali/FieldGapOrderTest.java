/*
 * Copyright 2015, Google Inc.
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

package org.jf.baksmali;

import com.google.common.collect.Lists;
import org.jf.dexlib2.analysis.ClassPath;
import org.jf.dexlib2.analysis.ClassProto;
import org.jf.dexlib2.analysis.DexClassProvider;
import org.jf.dexlib2.iface.DexFile;
import org.junit.Assert;
import org.junit.Test;

public class FieldGapOrderTest extends DexTest {
    @Test
    public void testOldOrder() {
        DexFile dexFile = getInputDexFile("FieldGapOrder", new BaksmaliOptions());
        Assert.assertEquals(3, dexFile.getClasses().size());

        ClassPath classPath = new ClassPath(Lists.newArrayList(new DexClassProvider(dexFile)), false, 66);
        ClassProto classProto = (ClassProto)classPath.getClass("LGapOrder;");
        Assert.assertEquals("r1", classProto.getFieldByOffset(12).getName());
        Assert.assertEquals("r2", classProto.getFieldByOffset(16).getName());
        Assert.assertEquals("d", classProto.getFieldByOffset(24).getName());
        Assert.assertEquals("s", classProto.getFieldByOffset(36).getName());
        Assert.assertEquals("i", classProto.getFieldByOffset(32).getName());
    }

    @Test
    public void testNewOrder() {
        DexFile dexFile = getInputDexFile("FieldGapOrder", new BaksmaliOptions());
        Assert.assertEquals(3, dexFile.getClasses().size());

        ClassPath classPath = new ClassPath(Lists.newArrayList(new DexClassProvider(dexFile)), false, 67);
        ClassProto classProto = (ClassProto)classPath.getClass("LGapOrder;");
        Assert.assertEquals("s", classProto.getFieldByOffset(10).getName());
        Assert.assertEquals("r1", classProto.getFieldByOffset(12).getName());
        Assert.assertEquals("r2", classProto.getFieldByOffset(16).getName());
        Assert.assertEquals("i", classProto.getFieldByOffset(20).getName());
        Assert.assertEquals("d", classProto.getFieldByOffset(24).getName());
    }
}
