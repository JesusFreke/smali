/*
 * Copyright 2013, Google Inc.
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

package org.jf.dexlib2.analysis;

import com.google.common.collect.Maps;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.ClassDef;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.HashMap;

public class ClassPath {
    private DexBackedDexFile[] dexFiles;

    private HashMap<String, ClassProto> loadedClasses = Maps.newHashMap();

    /**
     * Creates a new ClassPath instance that can load classes from the given dex files
     *
     * @param classPath An array of DexBackedDexFile objects. When loading a class, these dex files will be searched
     *                  in order
     */
    public ClassPath(DexBackedDexFile[] classPath) throws IOException {
        dexFiles = new DexBackedDexFile[classPath.length];
        System.arraycopy(classPath, 0, dexFiles, 0, classPath.length);
    }

    @Nonnull
    public ClassProto getClass(String type) {
        ClassProto loadedClass = loadedClasses.get(type);
        if (loadedClass != null) {
            return loadedClass;
        }
        ClassProto classProto = new ClassProto(this, type);
        loadedClasses.put(type, classProto);
        return classProto;
    }

    @Nonnull
    public ClassDef getClassDef(String type) {
        for (DexBackedDexFile dexFile: dexFiles) {
            for (ClassDef classDef: dexFile.getClasses()) {
                if (classDef.getType().equals(type)) {
                    return classDef;
                }
            }
        }
        throw new UnresolvedClassException("Could not resolve class %s", type);
    }
}
