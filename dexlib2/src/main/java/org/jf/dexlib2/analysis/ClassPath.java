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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.analysis.reflection.ReflectionClassDef;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.immutable.ImmutableDexFile;
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class ClassPath {
    @Nonnull private final TypeProto unknownClass;
    @Nonnull private DexFile[] dexFiles;
    @Nonnull private HashMap<String, TypeProto> loadedClasses = Maps.newHashMap();

    /**
     * Creates a new ClassPath instance that can load classes from the given dex files
     *
     * @param classPath An array of DexFile objects. When loading a class, these dex files will be searched in order
     */
    public ClassPath(DexFile... classPath) throws IOException {
        this(classPath, true);
    }

    /**
     * Creates a new ClassPath instance that can load classes from the given dex files
     *
     * @param classPath An iterable of DexFile objects. When loading a class, these dex files will be searched in order
     */
    public ClassPath(Iterable<DexFile> classPath) {
        this(Iterables.toArray(classPath, DexFile.class), false);
    }

    private ClassPath(@Nonnull DexFile[] classPath, boolean copyArray) {
        if (copyArray) {
            dexFiles = new DexFile[classPath.length+1];
            System.arraycopy(classPath, 0, dexFiles, 0, classPath.length);
            // add fallbacks for certain special classes that must be present
            dexFiles[dexFiles.length - 1] = getBasicClasses();
        } else {
            dexFiles = classPath;
        }

        unknownClass = new UnknownClassProto(this);
        loadedClasses.put(unknownClass.getType(), unknownClass);

        loadPrimitiveType("Z");
        loadPrimitiveType("B");
        loadPrimitiveType("S");
        loadPrimitiveType("C");
        loadPrimitiveType("I");
        loadPrimitiveType("J");
        loadPrimitiveType("F");
        loadPrimitiveType("D");
        loadPrimitiveType("L");
    }

    private void loadPrimitiveType(String type) {
        loadedClasses.put(type, new PrimitiveProto(this, type));
    }

    private static DexFile getBasicClasses() {
        // fallbacks for some special classes that we assume are present
        return new ImmutableDexFile(ImmutableSet.of(
                new ReflectionClassDef(Class.class),
                new ReflectionClassDef(Cloneable.class),
                new ReflectionClassDef(Object.class),
                new ReflectionClassDef(Serializable.class),
                new ReflectionClassDef(String.class),
                new ReflectionClassDef(Throwable.class)));
    }

    @Nonnull
    public TypeProto getClass(CharSequence type) {
        String typeString = type.toString();
        TypeProto typeProto = loadedClasses.get(typeString);
        if (typeProto != null) {
            return typeProto;
        }

        if (type.charAt(0) == '[') {
            typeProto = new ArrayProto(this, typeString);
        } else {
            typeProto = new ClassProto(this, typeString);
        }
        // All primitive types are preloaded into loadedClasses, so we don't need to check for that here

        loadedClasses.put(typeString, typeProto);
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
    public TypeProto getUnknownClass() {
        return unknownClass;
    }

    @Nonnull
    public static ClassPath fromClassPath(Iterable<String> classPathDirs, Iterable<String> classPath, DexFile dexFile) {
        ArrayList<DexFile> dexFiles = Lists.newArrayList();

        for (String classPathEntry: classPath) {
            dexFiles.add(loadClassPathEntry(classPathDirs, classPathEntry));
        }
        dexFiles.add(dexFile);
        return new ClassPath(dexFiles);
    }

    @Nonnull
    private static DexFile loadClassPathEntry(Iterable<String> classPathDirs, String bootClassPathEntry) {
        for (String classPathDir: classPathDirs) {
            File rawEntry = new File(bootClassPathEntry);
            // strip off the path - we only care about the filename
            String entryName = rawEntry.getName();

            int extIndex = entryName.lastIndexOf(".");

            String baseEntryName;
            if (extIndex == -1) {
                baseEntryName = entryName;
            } else {
                baseEntryName = entryName.substring(0, extIndex);
            }

            for (String ext: new String[]{"", ".odex", ".jar", ".apk", ".zip"}) {
                File file = new File(classPathDir, baseEntryName + ext);

                if (file.exists()) {
                    if (!file.canRead()) {
                        System.err.println(String.format(
                                "warning: cannot open %s for reading. Will continue looking.", file.getPath()));
                    } else {
                        try {
                            return DexFileFactory.loadDexFile(file);
                        } catch (DexFileFactory.NoClassesDexException ex) {
                            // ignore and continue
                        } catch (Exception ex) {
                            throw ExceptionWithContext.withContext(ex,
                                    "Error while reading boot class path entry \"%s\"", bootClassPathEntry);
                        }
                    }
                }
            }
        }
        throw new ExceptionWithContext("Cannot locate boot class path file %s", bootClassPathEntry);
    }
}
