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

package org.jf.dexlib2.pool;

import com.google.common.collect.Lists;
import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.AnnotationVisibility;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.raw.MapItem;
import org.jf.dexlib2.dexbacked.raw.RawDexFile;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodParameter;
import org.jf.dexlib2.immutable.*;
import org.jf.dexlib2.writer.io.MemoryDataStore;
import org.jf.dexlib2.writer.pool.DexPool;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class RollbackTest {
    @Test
    public void testRollback() throws IOException {
        ClassDef class1 = new ImmutableClassDef("Lcls1;", AccessFlags.PUBLIC.getValue(), "Ljava/lang/Object;", null, null,
                Lists.newArrayList(new ImmutableAnnotation(AnnotationVisibility.RUNTIME, "Lannotation;", null)),
                Lists.<Field>newArrayList(
                        new ImmutableField("Lcls1;", "field1", "I", AccessFlags.PUBLIC.getValue(), null, null)
                ),
                Lists.<Method>newArrayList(
                        new ImmutableMethod("Lcls1", "method1",
                                Lists.<MethodParameter>newArrayList(new ImmutableMethodParameter("L", null, null)), "V",
                                AccessFlags.PUBLIC.getValue(), null, null))
                );

        ClassDef class2 = new ImmutableClassDef("Lcls2;", AccessFlags.PUBLIC.getValue(), "Ljava/lang/Object;", null, null,
                Lists.newArrayList(new ImmutableAnnotation(AnnotationVisibility.RUNTIME, "Lannotation2;", null)),
                Lists.<Field>newArrayList(
                        new ImmutableField("Lcls2;", "field2", "D", AccessFlags.PUBLIC.getValue(), null, null)
                ),
                Lists.<Method>newArrayList(
                        new ImmutableMethod("Lcls2;", "method2",
                                Lists.<MethodParameter>newArrayList(new ImmutableMethodParameter("D", null, null)), "V",
                                AccessFlags.PUBLIC.getValue(), null, null))
        );

        RawDexFile dexFile1;
        {
            MemoryDataStore dataStore = new MemoryDataStore();
            DexPool dexPool = new DexPool(Opcodes.getDefault());
            dexPool.internClass(class1);
            dexPool.mark();
            dexPool.internClass(class2);
            dexPool.reset();
            dexPool.writeTo(dataStore);
            dexFile1 = new RawDexFile(Opcodes.getDefault(), dataStore.getData());
        }

        RawDexFile dexFile2;
        {
            MemoryDataStore dataStore = new MemoryDataStore();
            DexPool dexPool = new DexPool(Opcodes.getDefault());
            dexPool.internClass(class1);
            dexPool.writeTo(dataStore);
            dexFile2 = new RawDexFile(Opcodes.getDefault(), dataStore.getData());
        }

        List<MapItem> mapItems1 = dexFile1.getMapItems();
        List<MapItem> mapItems2 = dexFile2.getMapItems();
        for (int i=0; i<mapItems1.size(); i++) {
            Assert.assertEquals(mapItems1.get(i).getType(), mapItems2.get(i).getType());
            Assert.assertEquals(mapItems1.get(i).getItemCount(), mapItems2.get(i).getItemCount());
        }
    }
}
