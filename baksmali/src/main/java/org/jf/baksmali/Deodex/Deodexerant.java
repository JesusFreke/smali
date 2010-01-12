/*
 * [The "BSD licence"]
 * Copyright (c) 2009 Ben Gruver
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

package org.jf.baksmali.Deodex;

import org.jf.dexlib.*;
import org.jf.dexlib.Util.SparseArray;

import java.net.Socket;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * This class handles communication with the deodexerant helper binary,
 * as well as caching the results of any deodexerant lookups
 */
public class Deodexerant {
    private final String host;
    private final int port;

    private final HashMap<String, ClassData> vtableMap = new HashMap<String, ClassData>();
    private final HashMap<CommonSuperclassLookup, String> cachedCommonSuperclassLookup =
            new HashMap<CommonSuperclassLookup, String>();
    private InlineMethod[] inlineMethods;

    public final DexFile dexFile;

    private Socket socket = null;
    private PrintWriter out = null;
    private BufferedReader in = null;

    public Deodexerant(DexFile dexFile, String host, int port) {
        this.dexFile = dexFile;
        this.host = host;
        this.port = port;
    }

    private void loadInlineMethods() {
        List<String> responseLines = sendMultilineCommand("I");

        inlineMethods = new InlineMethod[responseLines.size()];
        for (int i=0; i<inlineMethods.length; i++) {
            String response = responseLines.get(i);
            if (!response.startsWith("inline: ")) {
                throw new RuntimeException("Invalid response from deodexerant");
            }

            String[] parts = response.substring(8).split(" ");
            if (parts.length != 2) {
                throw new RuntimeException("Invalid response from deodexerant");
            }

            String methodType = parts[0];
            InlineMethodType type;
            if (methodType.equals("virtual")) {
                type = InlineMethodType.Virtual;
            }  else if (methodType.equals("direct")) {
                type = InlineMethodType.Direct;
            } else if (methodType.equals("static")) {
                type = InlineMethodType.Static;
            } else {
                throw new RuntimeException("Invalid response from deodexerant");
            }

            String methodDescriptor = parts[1];
            inlineMethods[i] = new InlineMethod(methodDescriptor, type);
        }
    }

    public InlineMethod lookupInlineMethod(int inlineMethodIndex) {
        if (inlineMethods == null) {
            loadInlineMethods();
        }

        if (inlineMethodIndex >= inlineMethods.length) {
            throw new RuntimeException("Invalid inline method index " + inlineMethodIndex + ". Too big.");
        }

        return inlineMethods[inlineMethodIndex];
    }

    private TypeIdItem resolveTypeOrSupertype(String type) {
        TypeIdItem typeItem = TypeIdItem.getInternedTypeIdItem(dexFile, type);

        while (typeItem == null) {
            type = lookupSuperclass(type);
            if (type == null) {
                throw new RuntimeException("Could not find the type or a supertype of " + type + " in the dex file");
            }

            typeItem = TypeIdItem.getInternedTypeIdItem(dexFile, type);
        }
        return typeItem;
    }

    public FieldIdItem lookupField(String type, int fieldOffset) {
        ClassData classData = getClassData(type);
        return classData.lookupField(fieldOffset);
    }

    private ClassData getClassData(String type) {
        ClassData classData = vtableMap.get(type);
        if (classData == null) {
            classData = new ClassData(type);
            vtableMap.put(type, classData);
        }
        return classData;
    }

    public MethodIdItem lookupVirtualMethod(String classType, int methodIndex, boolean lookupSuper) {
        if (lookupSuper) {
            classType = lookupSuperclass(classType);
        }

        ClassData classData = getClassData(classType);

        return classData.lookupMethod(methodIndex);
    }

    public String lookupSuperclass(String typeDescriptor) {
        connectIfNeeded();

        String response = sendCommand("P " + typeDescriptor);
        int colon = response.indexOf(':');
        if (colon == -1) {
            throw new RuntimeException("Invalid response from deodexerant");
        }

        String type = response.substring(colon+2);
        if (type.length() == 0) {
            return null;
        }
        return type;
    }

    public String lookupCommonSuperclass(String typeDescriptor1, String typeDescriptor2) {
        CommonSuperclassLookup lookup = new CommonSuperclassLookup(typeDescriptor1, typeDescriptor2);
        String result = cachedCommonSuperclassLookup.get(lookup);
        if (result == null) {
            String response = sendCommand("C " + typeDescriptor1 + " " + typeDescriptor2);
            int colon = response.indexOf(':');
            if (colon == -1) {
                return null;
            }

            result = response.substring(colon+2);

            cachedCommonSuperclassLookup.put(lookup, result);
        }
        return result;
    }

    private String sendCommand(String cmd) {
        try {
            connectIfNeeded();

            out.println(cmd);
            out.flush();
            String response = in.readLine();
            if (response.startsWith("err")) {
                String error = response.substring(5);
                throw new RuntimeException(error);
            }
            return response;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    //The command is still just a single line, but we're expecting a multi-line
    //response. The repsonse is considered finished when a line starting with "err"
    //or with "done" is encountered
    private List<String> sendMultilineCommand(String cmd) {
        try {
            connectIfNeeded();

            out.println(cmd);
            out.flush();

            ArrayList<String> responseLines = new ArrayList<String>();
            String response = in.readLine();
            if (response == null) {
                throw new RuntimeException("Error talking to deodexerant");
            }
            while (!response.startsWith("done"))
            {
                if (response.startsWith("err")) {
                    throw new RuntimeException(response.substring(5));
                }

                responseLines.add(response);
                response = in.readLine();
            }

            return responseLines;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void connectIfNeeded() {
        try {
            if (socket != null) {
                return;
            }

            socket = new Socket(host, port);

            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private MethodIdItem parseAndResolveMethod(String classType, String methodName, String methodParams,
                                               String methodRet) {
        TypeIdItem classTypeItem = resolveTypeOrSupertype(classType);

        StringIdItem methodNameItem = StringIdItem.getInternedStringIdItem(dexFile, methodName);
        if (methodNameItem == null) {
            return null;
        }

        LinkedList<TypeIdItem> paramList = new LinkedList<TypeIdItem>();

        for (int i=0; i<methodParams.length(); i++) {
            switch (methodParams.charAt(i)) {
                case 'Z':
                case 'B':
                case 'S':
                case 'C':
                case 'I':
                case 'J':
                case 'F':
                case 'D':
                    paramList.add(getType(Character.toString(methodParams.charAt(i))));
                    break;
                case 'L':
                {
                    int end = methodParams.indexOf(';', i);
                    if (end == -1) {
                        throw new RuntimeException("invalid parameter in the method");
                    }

                    paramList.add(getType(methodParams.substring(i, end+1)));
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

                    paramList.add(getType(methodParams.substring(i, end+1)));
                    i = end;
                    break;
                }
                default:
                    throw new RuntimeException("invalid parameter in the method");
            }
        }

        TypeListItem paramListItem = null;
        if (paramList.size() > 0) {
            paramListItem = TypeListItem.getInternedTypeListItem(dexFile, paramList);
            if (paramListItem == null) {
                throw new RuntimeException("Could not find type list item in dex file");
            }
        }

        TypeIdItem retType = getType(methodRet);

        ProtoIdItem protoItem = ProtoIdItem.getInternedProtoIdItem(dexFile, retType, paramListItem);
        if (protoItem == null) {
            return null;
        }

        MethodIdItem methodIdItem;

        do {
            methodIdItem = MethodIdItem.getInternedMethodIdItem(dexFile, classTypeItem, protoItem, methodNameItem);
            if (methodIdItem != null) {
                return methodIdItem;
            }

            String superclassDescriptor = lookupSuperclass(classTypeItem.getTypeDescriptor());
            if (superclassDescriptor == null) {
                return null;
            }
            classTypeItem = TypeIdItem.getInternedTypeIdItem(dexFile, superclassDescriptor);

            while (classTypeItem == null && superclassDescriptor != null) {
                superclassDescriptor = lookupSuperclass(superclassDescriptor);
                classTypeItem = TypeIdItem.getInternedTypeIdItem(dexFile, superclassDescriptor);
            }
        } while (true);
    }

    private static final Pattern fullMethodPattern = Pattern.compile("(\\[*(?:L[^;]+;|[ZBSCIJFD]))->([^(]+)\\(([^)]*)\\)(.+)");
    private static final Pattern shortMethodPattern = Pattern.compile("([^(]+)\\(([^)]*)\\)(.+)");
    //private static final Pattern fieldPattern = Pattern.compile("(\\[*L[^;]+;)->([^:]+):(.+)");


    private FieldIdItem parseAndResolveField(String classType, String field) {
        //expecting a string like someField:Lfield/type;
        String[] parts = field.split(":");
        if (parts.length != 2) {
            throw new RuntimeException("Invalid field descriptor " + field);
        }

        TypeIdItem classTypeItem = resolveTypeOrSupertype(classType);
        if (classTypeItem == null) {
            return null;
        }
        String fieldName = parts[0];
        String fieldType = parts[1];

        StringIdItem fieldNameItem = StringIdItem.getInternedStringIdItem(dexFile, fieldName);
        if (fieldNameItem == null) {
            return null;
        }

        TypeIdItem fieldTypeItem = TypeIdItem.getInternedTypeIdItem(dexFile, fieldType);
        if (fieldTypeItem == null) {
            return null;
        }

        FieldIdItem fieldIdItem;

        do {
            fieldIdItem = FieldIdItem.getInternedFieldIdItem(dexFile, classTypeItem, fieldTypeItem, fieldNameItem);
            if (fieldIdItem != null) {
                return fieldIdItem;
            }

            String superclassDescriptor = lookupSuperclass(classTypeItem.getTypeDescriptor());
            classTypeItem = TypeIdItem.getInternedTypeIdItem(dexFile, superclassDescriptor);

            while (classTypeItem == null && superclassDescriptor != null) {
                superclassDescriptor = lookupSuperclass(superclassDescriptor);
                classTypeItem = TypeIdItem.getInternedTypeIdItem(dexFile, superclassDescriptor);
            }
        } while (classTypeItem != null);
        throw new RuntimeException("Could not find field in dex file");
    }

    public enum InlineMethodType {
        Virtual,
        Direct,
        Static
    }

    public class InlineMethod {
        public final String inlineMethodDescriptor;
        private final InlineMethodType methodType;
        private MethodIdItem methodIdItem = null;

        public InlineMethod(String inlineMethodDescriptor, InlineMethodType methodType) {
            this.inlineMethodDescriptor = inlineMethodDescriptor;
            this.methodType = methodType;
        }

        public MethodIdItem getMethodIdItem() {
            if (methodIdItem == null) {
                loadMethod();
            }
            return methodIdItem;
        }

        public InlineMethodType getMethodType() {
            return methodType;
        }

        private void loadMethod() {
            Matcher m = fullMethodPattern.matcher(inlineMethodDescriptor);
            if (!m.matches()) {
                throw new RuntimeException("Invalid method descriptor: " + inlineMethodDescriptor);
            }

            String classType = m.group(1);
            String methodName = m.group(2);
            String methodParams = m.group(3);
            String methodRet = m.group(4);

            MethodIdItem method = parseAndResolveMethod(classType, methodName, methodParams, methodRet);
            if (method == null) {
                throw new RuntimeException("Could not resolve method " + inlineMethodDescriptor);
            }
            this.methodIdItem = method;
        }
    }

    private TypeIdItem getType(String typeDescriptor) {
        TypeIdItem type = TypeIdItem.getInternedTypeIdItem(dexFile, typeDescriptor);
        if (type == null) {
            throw new RuntimeException("Could not find type \"" + typeDescriptor + "\" in dex file");
        }
        return type;
    }



    private class ClassData {
        private final String ClassType;

        private boolean vtableLoaded = false;
        private String[] methodNames;
        private String[] methodParams;
        private String[] methodRets;
        private MethodIdItem[] resolvedMethods;

        private boolean fieldsLoaded = false;
        private SparseArray<String> instanceFields;
        private SparseArray<FieldIdItem> resolvedFields;


        public ClassData(String classType) {
            this.ClassType = classType;
        }

        public MethodIdItem lookupMethod(int index) {
            if (!vtableLoaded) {
                loadvtable();
            }

            if (index >= resolvedMethods.length) {
                throw new RuntimeException("Invalid vtable index " + index + ". Too large.");
            }
            if (resolvedMethods[index] == null) {
                    resolvedMethods[index] = parseAndResolveMethod(ClassType, methodNames[index], methodParams[index],
                        methodRets[index]);
            }
            return resolvedMethods[index];
        }

        public FieldIdItem lookupField(int fieldOffset) {
            if (!fieldsLoaded) {
                loadFields();
            }

            FieldIdItem fieldIdItem = resolvedFields.get(fieldOffset);
            if (fieldIdItem == null) {
                String field = instanceFields.get(fieldOffset);
                if (field == null) {
                    throw new RuntimeException("Invalid field offset " + fieldOffset);
                }
                fieldIdItem = parseAndResolveField(ClassType, field);
                if (fieldIdItem != null) {
                    resolvedFields.put(fieldOffset, fieldIdItem);
                }
            }
            return fieldIdItem;

        }

        private void loadvtable() {
            List<String> responseLines = sendMultilineCommand("V " + ClassType);

            methodNames = new String[responseLines.size()];
            methodParams = new String[responseLines.size()];
            methodRets = new String[responseLines.size()];
            resolvedMethods = new MethodIdItem[responseLines.size()];

            int index = 0;
            for (String vtableEntry: responseLines) {
                if (!vtableEntry.startsWith("vtable: ")) {
                    throw new RuntimeException("Invalid response from deodexerant");
                }

                String method = vtableEntry.substring(8);
                Matcher m = shortMethodPattern.matcher(method);
                if (!m.matches()) {
                    throw new RuntimeException("invalid method string: " + method);
                }

                methodNames[index] = m.group(1);
                methodParams[index] = m.group(2);
                methodRets[index] = m.group(3);
                index++;
            }

            vtableLoaded = true;
        }

        private void loadFields() {
            List<String> responseLines = sendMultilineCommand("F " + ClassType);

            instanceFields = new SparseArray<String>(responseLines.size());
            resolvedFields = new SparseArray<FieldIdItem>(responseLines.size());

            for (String fieldLine: responseLines) {
                if (!fieldLine.startsWith("field: ")) {
                    throw new RuntimeException("Invalid response from deodexerant");
                }

                String field = fieldLine.substring(7);
                String[] parts = field.split(" ");
                if (parts.length != 2) {
                    throw new RuntimeException("Invalid response from deodexerant");
                }

                int fieldOffset = Integer.parseInt(parts[0]);
                instanceFields.put(fieldOffset, parts[1]);
            }

            fieldsLoaded = true;
        }
    }

    private static class CommonSuperclassLookup {
        public final String Type1;
        public final String Type2;

        public CommonSuperclassLookup(String type1, String type2) {
            this.Type1 = type1;
            this.Type2 = type2;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CommonSuperclassLookup that = (CommonSuperclassLookup) o;

            return Type1.equals(that.Type1) && Type2.equals(that.Type2);
        }

        @Override
        public int hashCode() {
            return Type1.hashCode() + 31 * Type2.hashCode();
        }
    }
}
