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

package org.jf.dexlib.Util;

import org.jf.dexlib.*;

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

public class Deodexerant {
    private final String host;
    private final int port;

    private final HashMap<TypeIdItem, ClassData> vtableMap = new HashMap<TypeIdItem, ClassData>();

    public final DexFile dexFile;

    private Socket socket = null;
    private PrintWriter out = null;
    private BufferedReader in = null;

    public Deodexerant(DexFile dexFile, String host, int port) {
        this.dexFile = dexFile;
        this.host = host;
        this.port = port;        
    }

    public InlineMethod lookupInlineMethod(int inlineMethodIndex) {
        connectIfNeeded();

        String response = sendCommand("I " + inlineMethodIndex);

        InlineMethodType type;

        if (response.startsWith("virtual")) {
            type = InlineMethodType.Virtual;
        } else if (response.startsWith("direct")) {
            type = InlineMethodType.Direct;
        } else if (response.startsWith("static")) {
            type = InlineMethodType.Static;
        } else {
            throw new RuntimeException("Invalid response from deodexerant");
        }


        int colon = response.indexOf(':');
        if (colon == -1) {
            throw new RuntimeException("Invalid response from deodexerant");
        }

        String methodDescriptor = response.substring(colon+2);

        Matcher m = fullMethodPattern.matcher(methodDescriptor);
        if (!m.matches()) {
            throw new RuntimeException("Invalid method descriptor: " + methodDescriptor);
        }

        String classType = m.group(1);
        String methodName = m.group(2);
        String methodParams = m.group(3);
        String methodRet = m.group(4);

        TypeIdItem classTypeItem = TypeIdItem.getInternedTypeIdItem(dexFile, classType);
        if (classTypeItem == null) {
            throw new RuntimeException("Could not find type " + classType + " in the dex file");
        }

        MethodIdItem method = parseAndResolveMethod(classTypeItem, methodName, methodParams, methodRet);
        if (method == null) {
            throw new RuntimeException("Could not find method " + methodDescriptor + " in the dex file");
        }

        return new InlineMethod(method, type);
    }

    public FieldIdItem lookupField(TypeIdItem type, int fieldOffset) {
        connectIfNeeded();

        String response = sendCommand("F " + type.getTypeDescriptor() + " " + fieldOffset);

        int colon = response.indexOf(':');
        if (colon == -1) {
            throw new RuntimeException("Invalid response from deodexerant");
        }

        String fieldDescriptor = response.substring(colon+2);

        return parseAndLookupField(fieldDescriptor);
    }

    private ClassData getClassData(TypeIdItem type) {
        ClassData classData = vtableMap.get(type);
        if (classData == null) {
            classData = new ClassData(type);
            vtableMap.put(type, classData);
        }
        return classData;
    }

    public MethodIdItem lookupVirtualMethod(TypeIdItem type, int methodIndex, boolean lookupSuper) {
        if (lookupSuper) {
            String classType = type.getTypeDescriptor();

            do
            {
                classType = lookupSuperclass(type.getTypeDescriptor());
                if (classType == null) {
                    throw new RuntimeException("Could not find any superclass for type " + type.getTypeDescriptor() +
                            " in the dex file");
                }
                type = TypeIdItem.getInternedTypeIdItem(dexFile, classType);
            } while (type == null);
        }

        ClassData classData = getClassData(type);

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
        connectIfNeeded();

        String response = sendCommand("C " + typeDescriptor1 + " " + typeDescriptor2);
        int colon = response.indexOf(':');
        if (colon == -1) {
            return null;
        }

        return response.substring(colon+2);
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
    private List<String> sendMultlineCommand(String cmd) {
        try {
            connectIfNeeded();

            out.println(cmd);
            out.flush();

            ArrayList<String> responseLines = new ArrayList<String>();
            String response = in.readLine();
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

    private MethodIdItem parseAndResolveMethod(TypeIdItem classType, String methodName, String methodParams,
                                               String methodRet) {
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
            methodIdItem = MethodIdItem.getInternedMethodIdItem(dexFile, classType, protoItem, methodNameItem);
            if (methodIdItem != null) {
                return methodIdItem;
            }

            String superclassDescriptor = lookupSuperclass(classType.getTypeDescriptor());
            classType = TypeIdItem.getInternedTypeIdItem(dexFile, superclassDescriptor);

            while (classType == null && superclassDescriptor != null) {
                superclassDescriptor = lookupSuperclass(superclassDescriptor);
                classType = TypeIdItem.getInternedTypeIdItem(dexFile, superclassDescriptor);
            }
        } while (classType != null);
        throw new RuntimeException("Could not find method in dex file");
    }

    private static final Pattern fullMethodPattern = Pattern.compile("(\\[*(?:L[^;]+;|[ZBSCIJFD]))->([^(]+)\\(([^)]*)\\)(.+)");
    private static final Pattern shortMethodPattern = Pattern.compile("([^(]+)\\(([^)]*)\\)(.+)");

    private static final Pattern fieldPattern = Pattern.compile("(\\[*L[^;]+;)->([^:]+):(.+)");
    private FieldIdItem parseAndLookupField(String field) {
        //expecting a string like Lsome/class;->someField:Lfield/type;

        Matcher m = fieldPattern.matcher(field);
        if (!m.matches()) {
            throw new RuntimeException("invalid field string: " + field);
        }

        String clazz = m.group(1);
        String fieldName = m.group(2);
        String fieldType = m.group(3);

        TypeIdItem classType = TypeIdItem.getInternedTypeIdItem(dexFile, clazz);
        StringIdItem fieldNameItem = StringIdItem.getInternedStringIdItem(dexFile, fieldName);
        TypeIdItem fieldTypeItem = TypeIdItem.getInternedTypeIdItem(dexFile, fieldType);

        FieldIdItem fieldIdItem;

        do {
            fieldIdItem = FieldIdItem.getInternedFieldIdItem(dexFile, classType, fieldTypeItem, fieldNameItem);
            if (fieldIdItem != null) {
                return fieldIdItem;
            }

            String superclassDescriptor = lookupSuperclass(classType.getTypeDescriptor());
            classType = TypeIdItem.getInternedTypeIdItem(dexFile, superclassDescriptor);

            while (classType == null && superclassDescriptor != null) {
                superclassDescriptor = lookupSuperclass(superclassDescriptor);
                classType = TypeIdItem.getInternedTypeIdItem(dexFile, superclassDescriptor);
            }

        } while (classType != null);
        throw new RuntimeException("Could not find field in dex file");
    }

    public enum InlineMethodType {
        Virtual,
        Direct,
        Static
    }

    public static class InlineMethod {
        public final MethodIdItem methodIdItem;
        public final InlineMethodType methodType;
        public InlineMethod(MethodIdItem methodIdItem, InlineMethodType methodType) {
            this.methodIdItem = methodIdItem;
            this.methodType = methodType;
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
        public final TypeIdItem ClassType;

        public boolean vtableLoaded;
        public String[] MethodNames;
        public String[] MethodParams;
        public String[] MethodRets;
        public MethodIdItem[] resolvedMethods;


        public ClassData(TypeIdItem classType) {
            this.ClassType = classType;
        }

        public MethodIdItem lookupMethod(int index) {
            if (!vtableLoaded) {
                loadvtable();
            }

            if (resolvedMethods[index] == null) {
                    resolvedMethods[index] = parseAndResolveMethod(ClassType, MethodNames[index], MethodParams[index],
                        MethodRets[index]);
            }
            return resolvedMethods[index]; 
        }

        private void loadvtable() {
            List<String> responseLines = sendMultlineCommand("V " + ClassType.getTypeDescriptor());

            MethodNames = new String[responseLines.size()];
            MethodParams = new String[responseLines.size()];
            MethodRets = new String[responseLines.size()];
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

                MethodNames[index] = m.group(1);
                MethodParams[index] = m.group(2);
                MethodRets[index] = m.group(3);
                index++;
            }

            vtableLoaded = true;
        }
    }
}
