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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.jf.dexlib2.analysis.reflection.ReflectionClassDef;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.immutable.ImmutableDexFile;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;

public class ClassPath {
    @Nonnull private final TypeProto unknownClass;
    @Nonnull private DexFile[] dexFiles;
    @Nonnull private HashMap<String, TypeProto> loadedClasses = Maps.newHashMap();

    /**
     * Creates a new ClassPath instance that can load classes from the given dex files
     *
     * @param classPath An array of DexBackedDexFile objects. When loading a class, these dex files will be searched
     *                  in order
     */
    public ClassPath(DexFile... classPath) throws IOException {
        dexFiles = new DexFile[classPath.length+1];
        System.arraycopy(classPath, 0, dexFiles, 0, classPath.length);
        // add fallbacks for certain special classes that must be present
        dexFiles[dexFiles.length - 1] = getBasicClasses();

        unknownClass = new UnknownClassProto(this);
        loadedClasses.put(unknownClass.getType(), unknownClass);
    }

    private static DexFile getBasicClasses() {
        return new ImmutableDexFile(ImmutableSet.of(
                new ReflectionClassDef(Object.class),
                new ReflectionClassDef(Cloneable.class),
                new ReflectionClassDef(Serializable.class)));
    }

    @Nonnull
    public TypeProto getClass(String type) {
        TypeProto typeProto = loadedClasses.get(type);
        if (typeProto != null) {
            return typeProto;
        }

        if (type.charAt(0) == '[') {
            typeProto = new ArrayProto(this, type);
        } else {
            typeProto = new ClassProto(this, type);
        }

        loadedClasses.put(type, typeProto);
        return typeProto;
    }

    @Nonnull
    public ClassDef getClassDef(String type) {
        // TODO: need a <= O(log) way to look up classes
        for (DexFile dexFile: dexFiles) {
            for (ClassDef classDef: dexFile.getClasses()) {
                if (classDef.getType().equals(type)) {
                    return classDef;
                }
            }
        }
        throw new UnresolvedClassException("Could not resolve class %s", type);
    }

    @Nonnull
    public RegisterType getRegisterTypeForType(@Nonnull String type) {
        switch (type.charAt(0)) {
            case 'Z':
                return RegisterType.getRegisterType(RegisterType.BOOLEAN, null);
            case 'B':
                return RegisterType.getRegisterType(RegisterType.BYTE, null);
            case 'S':
                return RegisterType.getRegisterType(RegisterType.SHORT, null);
            case 'C':
                return RegisterType.getRegisterType(RegisterType.CHAR, null);
            case 'I':
                return RegisterType.getRegisterType(RegisterType.INTEGER, null);
            case 'F':
                return RegisterType.getRegisterType(RegisterType.FLOAT, null);
            case 'J':
                return RegisterType.getRegisterType(RegisterType.LONG_LO, null);
            case 'D':
                return RegisterType.getRegisterType(RegisterType.DOUBLE_LO, null);
            case 'L':
            case 'U':
            case '[':
                return RegisterType.getRegisterType(RegisterType.REFERENCE, getClass(type));
            default:
                throw new RuntimeException("Invalid type: " + type);
        }
    }

    @Nonnull
    public TypeProto getUnknownClass() {
        return unknownClass;
    }
}
