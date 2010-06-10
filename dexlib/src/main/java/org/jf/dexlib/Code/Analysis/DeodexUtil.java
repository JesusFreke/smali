/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver (JesusFreke)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.dexlib.Code.Analysis;

import org.jf.dexlib.*;

import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeodexUtil {
    public static final int Virtual = 0;
    public static final int Direct = 1;
    public static final int Static = 2;

    private final InlineMethod[] inlineMethods;

    private final InlineMethod[] inlineMethods_35 = new InlineMethod[] {
            new InlineMethod(Static, "Lorg/apache/harmony/dalvik/NativeTestTarget;", "emptyInlineMethod", "", "V"),
            new InlineMethod(Virtual, "Ljava/lang/String;", "charAt", "I", "C"),
            new InlineMethod(Virtual, "Ljava/lang/String;", "compareTo", "Ljava/lang/String;", "I"),
            new InlineMethod(Virtual, "Ljava/lang/String;", "equals", "Ljava/lang/Object;", "Z"),
            new InlineMethod(Virtual, "Ljava/lang/String;", "length", "", "I"),
            new InlineMethod(Static, "Ljava/lang/Math;", "abs", "I", "I"),
            new InlineMethod(Static, "Ljava/lang/Math;", "abs", "J", "J"),
            new InlineMethod(Static, "Ljava/lang/Math;", "abs", "F", "F"),
            new InlineMethod(Static, "Ljava/lang/Math;", "abs", "D", "D"),
            new InlineMethod(Static, "Ljava/lang/Math;", "min", "II", "I"),
            new InlineMethod(Static, "Ljava/lang/Math;", "max", "II", "I"),
            new InlineMethod(Static, "Ljava/lang/Math;", "sqrt", "D", "D"),
            new InlineMethod(Static, "Ljava/lang/Math;", "cos", "D", "D"),
            new InlineMethod(Static, "Ljava/lang/Math;", "sin", "D", "D")
    };

    private final InlineMethod[] inlineMethods_36 = new InlineMethod[] {
            new InlineMethod(Static, "Lorg/apache/harmony/dalvik/NativeTestTarget;", "emptyInlineMethod", "", "V"),
            new InlineMethod(Virtual, "Ljava/lang/String;", "charAt", "I", "C"),
            new InlineMethod(Virtual, "Ljava/lang/String;", "compareTo", "Ljava/lang/String;", "I"),
            new InlineMethod(Virtual, "Ljava/lang/String;", "equals", "Ljava/lang/Object;", "Z"),
            new InlineMethod(Virtual, "Ljava/lang/String;", "indexOf", "I", "I"),
            new InlineMethod(Virtual, "Ljava/lang/String;", "indexOf", "II", "I"),
            new InlineMethod(Virtual, "Ljava/lang/String;", "length", "", "I"),
            new InlineMethod(Static, "Ljava/lang/Math;", "abs", "I", "I"),
            new InlineMethod(Static, "Ljava/lang/Math;", "abs", "J", "J"),
            new InlineMethod(Static, "Ljava/lang/Math;", "abs", "F", "F"),
            new InlineMethod(Static, "Ljava/lang/Math;", "abs", "D", "D"),
            new InlineMethod(Static, "Ljava/lang/Math;", "min", "II", "I"),
            new InlineMethod(Static, "Ljava/lang/Math;", "max", "II", "I"),
            new InlineMethod(Static, "Ljava/lang/Math;", "sqrt", "D", "D"),
            new InlineMethod(Static, "Ljava/lang/Math;", "cos", "D", "D"),
            new InlineMethod(Static, "Ljava/lang/Math;", "sin", "D", "D")
    };

    public final DexFile dexFile;

    public DeodexUtil(DexFile dexFile) {
        this.dexFile = dexFile;
        OdexHeader odexHeader = dexFile.getOdexHeader();
        if (odexHeader == null) {
            //if there isn't an odex header, why are we creating an DeodexUtil object?
            assert false;
            throw new RuntimeException("Cannot create a DeodexUtil object for a dex file without an odex header");
        } else if (odexHeader.version == 35) {
            inlineMethods = inlineMethods_35;
        } else if (odexHeader.version == 36) {
            inlineMethods = inlineMethods_36;
        } else {
            assert false;
            throw new RuntimeException(String.format("odex version %d isn't supported yet", odexHeader.version));
        }
    }

    public InlineMethod lookupInlineMethod(int inlineMethodIndex) {
        if (inlineMethodIndex >= inlineMethods.length) {
            throw new RuntimeException("Invalid inline method index " + inlineMethodIndex + ".");
        }
        return inlineMethods[inlineMethodIndex];
    }

    public FieldIdItem lookupField(ClassPath.ClassDef classDef, int fieldOffset) {
        String field = classDef.getInstanceField(fieldOffset);
        if (field == null) {
            return null;
        }

        return parseAndResolveField(classDef, field);
    }

    private static final Pattern shortMethodPattern = Pattern.compile("([^(]+)\\(([^)]*)\\)(.+)");

    public MethodIdItem lookupVirtualMethod(ClassPath.ClassDef classDef, int methodIndex) {
        String method = classDef.getVirtualMethod(methodIndex);
        if (method == null) {
            return null;
        }

        Matcher m = shortMethodPattern.matcher(method);
        if (!m.matches()) {
            assert false;
            throw new RuntimeException("Invalid method descriptor: " + method);
        }

        String methodName = m.group(1);
        String methodParams = m.group(2);
        String methodRet = m.group(3);

        if (classDef.isInterface()) {
            classDef = classDef.getSuperclass();
            assert classDef != null;
        }

        return parseAndResolveMethod(classDef, methodName, methodParams, methodRet);
    }

    private MethodIdItem parseAndResolveMethod(ClassPath.ClassDef classDef, String methodName, String methodParams,
                                               String methodRet) {
        StringIdItem methodNameItem = StringIdItem.lookupStringIdItem(dexFile, methodName);
        if (methodNameItem == null) {
            return null;
        }

        LinkedList<TypeIdItem> paramList = new LinkedList<TypeIdItem>();

        for (int i=0; i<methodParams.length(); i++) {
            TypeIdItem typeIdItem;

            switch (methodParams.charAt(i)) {
                case 'Z':
                case 'B':
                case 'S':
                case 'C':
                case 'I':
                case 'J':
                case 'F':
                case 'D':
                    typeIdItem = TypeIdItem.lookupTypeIdItem(dexFile, methodParams.substring(i,i+1));
                    break;
                case 'L':
                {
                    int end = methodParams.indexOf(';', i);
                    if (end == -1) {
                        throw new RuntimeException("invalid parameter in the method");
                    }

                    typeIdItem = TypeIdItem.lookupTypeIdItem(dexFile, methodParams.substring(i, end+1));
                    i = end;
                    break;
                }
                case '[':
                {
                    int end;
                    int typeStart = i+1;
                    while (typeStart < methodParams.length() && methodParams.charAt(typeStart) == '[') {
                        typeStart++;
                    }
                    switch (methodParams.charAt(typeStart)) {
                        case 'Z':
                        case 'B':
                        case 'S':
                        case 'C':
                        case 'I':
                        case 'J':
                        case 'F':
                        case 'D':
                            end = typeStart;
                            break;
                        case 'L':
                            end = methodParams.indexOf(';', typeStart);
                            if (end == -1) {
                                throw new RuntimeException("invalid parameter in the method");
                            }
                            break;
                        default:
                            throw new RuntimeException("invalid parameter in the method");
                    }

                    typeIdItem = TypeIdItem.lookupTypeIdItem(dexFile, methodParams.substring(i, end+1));
                    i = end;
                    break;
                }
                default:
                    throw new RuntimeException("invalid parameter in the method");
            }

            if (typeIdItem == null) {
                return null;
            }
            paramList.add(typeIdItem);
        }

        TypeListItem paramListItem = null;
        if (paramList.size() > 0) {
            paramListItem = TypeListItem.lookupTypeListItem(dexFile, paramList);
            if (paramListItem == null) {
                return null;
            }
        }

        TypeIdItem retType = TypeIdItem.lookupTypeIdItem(dexFile, methodRet);
        if (retType == null) {
            return null;
        }

        ProtoIdItem protoItem = ProtoIdItem.lookupProtoIdItem(dexFile, retType, paramListItem);
        if (protoItem == null) {
            return null;
        }

        ClassPath.ClassDef methodClassDef = classDef;

        do {
            TypeIdItem classTypeItem = TypeIdItem.lookupTypeIdItem(dexFile, methodClassDef.getClassType());

            if (classTypeItem != null) {
                MethodIdItem methodIdItem = MethodIdItem.lookupMethodIdItem(dexFile, classTypeItem, protoItem, methodNameItem);
                if (methodIdItem != null) {
                    return methodIdItem;
                }
            }

            methodClassDef = methodClassDef.getSuperclass();
        } while (methodClassDef != null);
        return null;
    }

    private FieldIdItem parseAndResolveField(ClassPath.ClassDef classDef, String field) {
        //expecting a string like someField:Lfield/type;
        String[] parts = field.split(":");
        if (parts.length != 2) {
            throw new RuntimeException("Invalid field descriptor " + field);
        }

        String fieldName = parts[0];
        String fieldType = parts[1];

        StringIdItem fieldNameItem = StringIdItem.lookupStringIdItem(dexFile, fieldName);
        if (fieldNameItem == null) {
            return null;
        }

        TypeIdItem fieldTypeItem = TypeIdItem.lookupTypeIdItem(dexFile, fieldType);
        if (fieldTypeItem == null) {
            return null;
        }

        ClassPath.ClassDef fieldClass = classDef;

        do {
            TypeIdItem classTypeItem = TypeIdItem.lookupTypeIdItem(dexFile, fieldClass.getClassType());
            if (classTypeItem == null) {
                continue;
            }

            FieldIdItem fieldIdItem = FieldIdItem.lookupFieldIdItem(dexFile, classTypeItem, fieldTypeItem, fieldNameItem);
            if (fieldIdItem != null) {
                return fieldIdItem;
            }

            fieldClass = fieldClass.getSuperclass();
        } while (fieldClass != null);

        return null;
    }

    /**
     * Compare the inline methods that we have against the given set of inline methods from deodexerant.
     * We want to make sure that each inline method in inlineMethods matches the method we have at the same
     * index. We may have more inline methods than we are given in inlineMethods - this shouldn't be a problem.
     * Newer versions of dalvik add additional inline methods, but (so far) have changed any existing ones.
     *
     * If anything doesn't look right, we just throw an exception
     * @param inlineMethods the inline methods from deodexerant
     */
    protected void checkInlineMethods(String[] inlineMethods) {
        if (inlineMethods.length > this.inlineMethods.length) {
            throw new ValidationException("Inline method count mismatch");
        }

        for (int i=0; i<inlineMethods.length; i++) {
            String inlineMethod = inlineMethods[i];
            int methodType;

            if (inlineMethod.startsWith("static")) {
                methodType = Static;
                inlineMethod = inlineMethod.substring(7);
            } else if (inlineMethod.startsWith("direct")) {
                methodType = Direct;
                inlineMethod = inlineMethod.substring(7);
            } else if (inlineMethod.startsWith("virtual")) {
                methodType = Virtual;
                inlineMethod = inlineMethod.substring(8);
            } else {
                throw new ValidationException("Could not parse inline method");
            }

            if (!inlineMethod.equals(this.inlineMethods[i].getMethodString())) {
                throw new ValidationException(String.format("Inline method mismatch. %s vs. %s", inlineMethod,
                        this.inlineMethods[i].getMethodString()));
            }

            if (methodType != this.inlineMethods[i].methodType) {
                throw new ValidationException(String.format("Inline method type mismatch. %d vs. %d", methodType,
                        this.inlineMethods[i].methodType));
            }
        }
    }

    public class InlineMethod {
        public final int methodType;
        public final String classType;
        public final String methodName;
        public final String parameters;
        public final String returnType;

        private MethodIdItem methodIdItem = null;

        protected InlineMethod(int methodType, String classType, String methodName, String parameters,
                               String returnType) {
            this.methodType = methodType;
            this.classType = classType;
            this.methodName = methodName;
            this.parameters = parameters;
            this.returnType = returnType;
        }

        public MethodIdItem getMethodIdItem() {
            if (methodIdItem == null) {
                loadMethod();
            }
            return methodIdItem;
        }

        private void loadMethod() {
            ClassPath.ClassDef classDef = ClassPath.getClassDef(classType);

            this.methodIdItem = parseAndResolveMethod(classDef, methodName, parameters, returnType);
        }

        public String getMethodString() {
            return String.format("%s->%s(%s)%s", classType, methodName, parameters, returnType);
        }
    }
}
