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

package org.jf.dexlib2.writer.pool;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import org.jf.dexlib2.DebugItemType;
import org.jf.dexlib2.ReferenceType;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.debug.*;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.ReferenceInstruction;
import org.jf.dexlib2.iface.reference.*;
import org.jf.dexlib2.util.ReferenceUtil;
import org.jf.dexlib2.writer.ClassSection;
import org.jf.dexlib2.writer.DebugWriter;
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

public class ClassPool implements ClassSection<CharSequence, CharSequence,
        TypeListPool.Key<? extends Collection<? extends CharSequence>>, PoolClassDef, Field, PoolMethod,
        Set<? extends Annotation>, AnnotationSetRefPool.Key, EncodedArrayPool.Key,
        DebugItem, Instruction, ExceptionHandler> {
    @Nonnull private HashMap<String, PoolClassDef> internedItems = Maps.newHashMap();

    @Nonnull private final StringPool stringPool;
    @Nonnull private final TypePool typePool;
    @Nonnull private final FieldPool fieldPool;
    @Nonnull private final MethodPool methodPool;
    @Nonnull private final AnnotationSetPool annotationSetPool;
    @Nonnull private final AnnotationSetRefPool annotationSetRefPool;
    @Nonnull private final TypeListPool typeListPool;
    @Nonnull private final EncodedArrayPool encodedArrayPool;

    public ClassPool(@Nonnull StringPool stringPool,
                     @Nonnull TypePool typePool,
                     @Nonnull FieldPool fieldPool,
                     @Nonnull MethodPool methodPool,
                     @Nonnull AnnotationSetPool annotationSetPool,
                     @Nonnull AnnotationSetRefPool annotationSetRefPool,
                     @Nonnull TypeListPool typeListPool,
                     @Nonnull EncodedArrayPool encodedArrayPool) {
        this.stringPool = stringPool;
        this.typePool = typePool;
        this.fieldPool = fieldPool;
        this.methodPool = methodPool;
        this.annotationSetPool = annotationSetPool;
        this.annotationSetRefPool = annotationSetRefPool;
        this.typeListPool = typeListPool;
        this.encodedArrayPool = encodedArrayPool;
    }

    public void intern(@Nonnull ClassDef classDef) {
        PoolClassDef poolClassDef = new PoolClassDef(classDef);

        PoolClassDef prev = internedItems.put(poolClassDef.getType(), poolClassDef);
        if (prev != null) {
            throw new ExceptionWithContext("Class %s has already been interned", poolClassDef.getType());
        }

        typePool.intern(poolClassDef.getType());
        typePool.internNullable(poolClassDef.getSuperclass());
        typeListPool.intern(poolClassDef.getInterfaces());
        stringPool.internNullable(poolClassDef.getSourceFile());
        encodedArrayPool.intern(poolClassDef);

        HashSet<String> fields = new HashSet<String>();
        for (Field field: poolClassDef.getFields()) {
            String fieldDescriptor = ReferenceUtil.getShortFieldDescriptor(field);
            if (!fields.add(fieldDescriptor)) {
                throw new ExceptionWithContext("Multiple definitions for field %s->%s",
                        poolClassDef.getType(), fieldDescriptor);
            }
            fieldPool.intern(field);

            annotationSetPool.intern(field.getAnnotations());
        }

        HashSet<String> methods = new HashSet<String>();
        for (PoolMethod method: poolClassDef.getMethods()) {
            String methodDescriptor = ReferenceUtil.getShortMethodDescriptor(method);
            if (!methods.add(methodDescriptor)) {
                throw new ExceptionWithContext("Multiple definitions for method %s->%s",
                        poolClassDef.getType(), methodDescriptor);
            }
            methodPool.intern(method);
            internCode(method);
            internDebug(method);
            annotationSetPool.intern(method.getAnnotations());
            annotationSetRefPool.intern(method);
        }

        annotationSetPool.intern(poolClassDef.getAnnotations());
    }

    private void internCode(@Nonnull Method method) {
        // this also handles parameter names, which aren't directly tied to the MethodImplementation, even though the debug items are
        boolean hasInstruction = false;

        MethodImplementation methodImpl = method.getImplementation();
        if (methodImpl != null) {
            for (Instruction instruction: methodImpl.getInstructions()) {
                hasInstruction = true;
                if (instruction instanceof ReferenceInstruction) {
                    Reference reference = ((ReferenceInstruction)instruction).getReference();
                    switch (instruction.getOpcode().referenceType) {
                        case ReferenceType.STRING:
                            stringPool.intern((StringReference)reference);
                            break;
                        case ReferenceType.TYPE:
                            typePool.intern((TypeReference)reference);
                            break;
                        case ReferenceType.FIELD:
                            fieldPool.intern((FieldReference) reference);
                            break;
                        case ReferenceType.METHOD:
                            methodPool.intern((MethodReference)reference);
                            break;
                        default:
                            throw new ExceptionWithContext("Unrecognized reference type: %d",
                                    instruction.getOpcode().referenceType);
                    }
                }
            }

            List<? extends TryBlock> tryBlocks = methodImpl.getTryBlocks();
            if (!hasInstruction && tryBlocks.size() > 0) {
                throw new ExceptionWithContext("Method %s has no instructions, but has try blocks.",
                        ReferenceUtil.getMethodDescriptor(method));
            }

            for (TryBlock<? extends ExceptionHandler> tryBlock: methodImpl.getTryBlocks()) {
                for (ExceptionHandler handler: tryBlock.getExceptionHandlers()) {
                    typePool.internNullable(handler.getExceptionType());
                }
            }
        }
    }

    private void internDebug(@Nonnull Method method) {
        for (MethodParameter param: method.getParameters()) {
            String paramName = param.getName();
            if (paramName != null) {
                stringPool.intern(paramName);
            }
        }

        MethodImplementation methodImpl = method.getImplementation();
        if (methodImpl != null) {
            for (DebugItem debugItem: methodImpl.getDebugItems()) {
                switch (debugItem.getDebugItemType()) {
                    case DebugItemType.START_LOCAL:
                        StartLocal startLocal = (StartLocal)debugItem;
                        stringPool.internNullable(startLocal.getName());
                        typePool.internNullable(startLocal.getType());
                        stringPool.internNullable(startLocal.getSignature());
                        break;
                    case DebugItemType.SET_SOURCE_FILE:
                        stringPool.internNullable(((SetSourceFile) debugItem).getSourceFile());
                        break;
                }
            }
        }
    }

    private ImmutableList<PoolClassDef> sortedClasses = null;
    @Nonnull @Override public Collection<? extends PoolClassDef> getSortedClasses() {
        if (sortedClasses == null) {
            sortedClasses = Ordering.natural().immutableSortedCopy(internedItems.values());
        }
        return sortedClasses;
    }

    @Nullable @Override
    public Map.Entry<? extends PoolClassDef, Integer> getClassEntryByType(@Nullable CharSequence name) {
        if (name == null) {
            return null;
        }

        final PoolClassDef classDef = internedItems.get(name.toString());
        if (classDef == null) {
            return null;
        }

        return new Map.Entry<PoolClassDef, Integer>() {
            @Override public PoolClassDef getKey() {
                return classDef;
            }

            @Override public Integer getValue() {
                return classDef.classDefOffset;
            }

            @Override public Integer setValue(Integer value) {
                return classDef.classDefOffset = value;
            }
        };
    }

    @Nonnull @Override public CharSequence getType(@Nonnull PoolClassDef classDef) {
        return classDef.getType();
    }

    @Override public int getAccessFlags(@Nonnull PoolClassDef classDef) {
        return classDef.getAccessFlags();
    }

    @Nullable @Override public CharSequence getSuperclass(@Nonnull PoolClassDef classDef) {
        return classDef.getSuperclass();
    }

    @Nullable @Override public TypeListPool.Key<SortedSet<String>> getSortedInterfaces(@Nonnull PoolClassDef classDef) {
        return classDef.interfaces;
    }

    @Nullable @Override public CharSequence getSourceFile(@Nonnull PoolClassDef classDef) {
        return classDef.getSourceFile();
    }

    @Nullable @Override public EncodedArrayPool.Key getStaticInitializers(@Nonnull PoolClassDef classDef) {
        return EncodedArrayPool.Key.of(classDef);
    }

    @Nonnull @Override public Collection<? extends Field> getSortedStaticFields(@Nonnull PoolClassDef classDef) {
        return classDef.getStaticFields();
    }

    @Nonnull @Override public Collection<? extends Field> getSortedInstanceFields(@Nonnull PoolClassDef classDef) {
        return classDef.getInstanceFields();
    }

    @Nonnull @Override public Collection<PoolMethod> getSortedDirectMethods(@Nonnull PoolClassDef classDef) {
        return classDef.getDirectMethods();
    }

    @Nonnull @Override public Collection<PoolMethod> getSortedVirtualMethods(@Nonnull PoolClassDef classDef) {
        return classDef.getVirtualMethods();
    }

    @Override public int getFieldAccessFlags(@Nonnull Field field) {
        return field.getAccessFlags();
    }

    @Override public int getMethodAccessFlags(@Nonnull PoolMethod method) {
        return method.getAccessFlags();
    }

    @Nullable @Override public Set<? extends Annotation> getClassAnnotations(@Nonnull PoolClassDef classDef) {
        Set<? extends Annotation> annotations = classDef.getAnnotations();
        if (annotations.size() == 0) {
            return null;
        }
        return annotations;
    }

    @Nullable @Override public Set<? extends Annotation> getFieldAnnotations(@Nonnull Field field) {
        Set<? extends Annotation> annotations = field.getAnnotations();
        if (annotations.size() == 0) {
            return null;
        }
        return annotations;
    }

    @Nullable @Override public Set<? extends Annotation> getMethodAnnotations(@Nonnull PoolMethod method) {
        Set<? extends Annotation> annotations = method.getAnnotations();
        if (annotations.size() == 0) {
            return null;
        }
        return annotations;
    }

    @Nullable @Override public AnnotationSetRefPool.Key getParameterAnnotations(@Nonnull PoolMethod method) {
        AnnotationSetRefPool.Key key = new AnnotationSetRefPool.Key(method);
        Collection<Set<? extends Annotation>> annotations = key.getAnnotationSets();
        if (annotations.size() == 0) {
            return null;
        }
        return key;
    }

    @Nullable @Override public Iterable<? extends DebugItem> getDebugItems(@Nonnull PoolMethod method) {
        MethodImplementation impl = method.getImplementation();
        if (impl != null) {
            return impl.getDebugItems();
        }
        return null;
    }

    @Nullable @Override public Iterable<CharSequence> getParameterNames(@Nonnull PoolMethod method) {
        return Iterables.transform(method.getParameters(), new Function<MethodParameter, CharSequence>() {
            @Nullable @Override public CharSequence apply(MethodParameter input) {
                return input.getName();
            }
        });
    }

    @Override public int getRegisterCount(@Nonnull PoolMethod method) {
        MethodImplementation impl = method.getImplementation();
        if (impl != null) {
            return impl.getRegisterCount();
        }
        return 0;
    }

    @Nullable @Override public Iterable<? extends Instruction> getInstructions(@Nonnull PoolMethod method) {
        MethodImplementation impl = method.getImplementation();
        if (impl != null) {
            return impl.getInstructions();
        }
        return null;
    }

    @Nonnull @Override public List<? extends TryBlock<? extends ExceptionHandler>> getTryBlocks(
            @Nonnull PoolMethod method) {
        MethodImplementation impl = method.getImplementation();
        if (impl != null) {
            return impl.getTryBlocks();
        }
        return ImmutableList.of();
    }

    @Nullable @Override public CharSequence getExceptionType(@Nonnull ExceptionHandler handler) {
        return handler.getExceptionType();
    }

    @Override public void setAnnotationDirectoryOffset(@Nonnull PoolClassDef classDef, int offset) {
        classDef.annotationDirectoryOffset = offset;
    }

    @Override public int getAnnotationDirectoryOffset(@Nonnull PoolClassDef classDef) {
        return classDef.annotationDirectoryOffset;
    }

    @Override public void setCodeItemOffset(@Nonnull PoolMethod method, int offset) {
        method.codeItemOffset = offset;
    }

    @Override public int getCodeItemOffset(@Nonnull PoolMethod method) {
        return method.codeItemOffset;
    }

    @Override public void setDebugItemOffset(@Nonnull PoolMethod method, int offset) {
        method.debugInfoOffset = offset;
    }

    @Override public int getDebugItemOffset(@Nonnull PoolMethod method) {
        return method.debugInfoOffset;
    }

    @Override public void writeDebugItem(@Nonnull DebugWriter<CharSequence, CharSequence> writer,
                                         DebugItem debugItem) throws IOException {
        switch (debugItem.getDebugItemType()) {
            case DebugItemType.START_LOCAL: {
                StartLocal startLocal = (StartLocal)debugItem;
                writer.writeStartLocal(startLocal.getCodeAddress(),
                        startLocal.getRegister(),
                        startLocal.getName(),
                        startLocal.getType(),
                        startLocal.getSignature());
                break;
            }
            case DebugItemType.END_LOCAL: {
                EndLocal endLocal = (EndLocal)debugItem;
                writer.writeEndLocal(endLocal.getCodeAddress(), endLocal.getRegister());
                break;
            }
            case DebugItemType.RESTART_LOCAL: {
                RestartLocal restartLocal = (RestartLocal)debugItem;
                writer.writeRestartLocal(restartLocal.getCodeAddress(), restartLocal.getRegister());
                break;
            }
            case DebugItemType.PROLOGUE_END: {
                writer.writePrologueEnd(debugItem.getCodeAddress());
                break;
            }
            case DebugItemType.EPILOGUE_BEGIN: {
                writer.writeEpilogueBegin(debugItem.getCodeAddress());
                break;
            }
            case DebugItemType.LINE_NUMBER: {
                LineNumber lineNumber = (LineNumber)debugItem;
                writer.writeLineNumber(lineNumber.getCodeAddress(), lineNumber.getLineNumber());
                break;
            }
            case DebugItemType.SET_SOURCE_FILE: {
                SetSourceFile setSourceFile = (SetSourceFile)debugItem;
                writer.writeSetSourceFile(setSourceFile.getCodeAddress(), setSourceFile.getSourceFile());
            }
            default:
                throw new ExceptionWithContext("Unexpected debug item type: %d", debugItem.getDebugItemType());
        }
    }

    @Override public int getItemIndex(@Nonnull PoolClassDef classDef) {
        return classDef.classDefOffset;
    }

    @Nonnull @Override public Collection<? extends Map.Entry<PoolClassDef, Integer>> getItems() {
        class MapEntry implements Map.Entry<PoolClassDef, Integer> {
            PoolClassDef classDef = null;

            @Override public PoolClassDef getKey() {
                return classDef;
            }

            @Override public Integer getValue() {
                return classDef.classDefOffset;
            }

            @Override public Integer setValue(Integer value) {
                int prev = classDef.classDefOffset;
                classDef.classDefOffset = value;
                return prev;
            }
        }
        final MapEntry entry = new MapEntry();

        return new AbstractCollection<Entry<PoolClassDef, Integer>>() {
            @Nonnull @Override public Iterator<Entry<PoolClassDef, Integer>> iterator() {
                return new Iterator<Entry<PoolClassDef, Integer>>() {
                    Iterator<PoolClassDef> iter = internedItems.values().iterator();

                    @Override public boolean hasNext() {
                        return iter.hasNext();
                    }

                    @Override public Entry<PoolClassDef, Integer> next() {
                        entry.classDef = iter.next();
                        return entry;
                    }

                    @Override public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }

            @Override public int size() {
                return internedItems.size();
            }
        };
    }
}
