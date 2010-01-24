package org.jf.dexlib.Code.Analysis;

import org.jf.dexlib.*;
import static org.jf.dexlib.ClassDataItem.EncodedMethod;
import static org.jf.dexlib.ClassDataItem.EncodedField;

import org.jf.dexlib.Util.AccessFlags;
import org.jf.dexlib.Util.ExceptionWithContext;
import org.jf.dexlib.Util.SparseArray;

import java.io.File;
import java.util.*;

public class ClassPath {
    private static ClassPath theClassPath = null;

    private final HashMap<String, ClassDef> classDefs;
    protected final ClassDef javaLangObjectClassDef; //Ljava/lang/Object;

    public static void InitializeClassPath(String[] bootClassPath, DexFile dexFile) {
        if (theClassPath != null) {
            throw new ExceptionWithContext("Cannot initialize ClassPath multiple times");
        }

        theClassPath = new ClassPath(bootClassPath, dexFile);
    }

    private ClassPath(String[] bootClassPath, DexFile dexFile) {
        if (bootClassPath == null || bootClassPath.length == 0) {
            throw new ExceptionWithContext("No BOOTCLASSPATH entries were given");
        }

        classDefs = new HashMap<String, ClassDef>();

        for (String bootClassPathEntry: bootClassPath) {
            loadBootClassPath(bootClassPathEntry);
        }

        loadDexFile(dexFile);

        try {
            javaLangObjectClassDef = getClassDef("Ljava/lang/Object;");
        } catch (ClassNotFoundException ex) {
            throw ExceptionWithContext.withContext(ex, "Ljava/lang/Object; must be present in the classpath");
        }

        for (String primitiveType: new String[]{"Z", "B", "S", "C", "I", "J", "F", "D"}) {
            ClassDef classDef = new PrimitiveClassDef(primitiveType);
            classDefs.put(primitiveType, classDef);
        }
    }

    private void loadBootClassPath(String bootClassPathEntry) {
        File file = new File(bootClassPathEntry);

        if (!file.exists()) {
            throw new ExceptionWithContext("ClassPath entry \"" + bootClassPathEntry + "\" does not exist.");
        }

        if (!file.canRead()) {
            throw new ExceptionWithContext("Cannot read ClassPath entry \"" + bootClassPathEntry + "\".");
        }

        DexFile dexFile;
        try {
            dexFile = new DexFile(file);
        } catch (Exception ex) {
            throw ExceptionWithContext.withContext(ex, "Error while reading ClassPath entry \"" +
                    bootClassPathEntry + "\".");
        }

        loadDexFile(dexFile);
    }

    private void loadDexFile(DexFile dexFile) {
        for (ClassDefItem classDefItem: dexFile.ClassDefsSection.getItems()) {
            //TODO: need to check if the class already exists. (and if so, what to do about it?)
            ClassDef classDef = new ClassDef(classDefItem);
            classDefs.put(classDef.getClassType(), classDef);
            classDef.dumpVtable();
            classDef.dumpFields();
        }
    }

    private static class ClassNotFoundException extends ExceptionWithContext {
        public ClassNotFoundException(String message) {
            super(message);
        }
    }

    public static ClassDef getClassDef(String classType)  {
        ClassDef classDef = theClassPath.classDefs.get(classType);
        if (classDef == null) {
            //if it's an array class, try to create it
            if (classType.charAt(0) == '[') {
                return theClassPath.createArrayClassDef(classType);
            } else {
                throw new ClassNotFoundException("Class " + classType + " cannot be found");
            }
        }
        return classDef;
    }

    public static ClassDef getClassDef(TypeIdItem classType) {
        return getClassDef(classType.getTypeDescriptor());
    }

    //256 [ characters
    private static final String arrayPrefix = "[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[" +
        "[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[" +
        "[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[";
    private static ClassDef getArrayClassDefByElementClassAndDimension(ClassDef classDef, int arrayDimension) {
        return getClassDef(arrayPrefix.substring(256 - arrayDimension) + classDef.classType);
    }

    private static ClassDef createArrayClassDef(String arrayClassName) {
        assert arrayClassName != null;
        assert arrayClassName.charAt(0) == '[';

        ArrayClassDef arrayClassDef = new ArrayClassDef(arrayClassName);
        if (arrayClassDef.elementClass == null) {
            return null;
        }

        theClassPath.classDefs.put(arrayClassName, arrayClassDef);
        return arrayClassDef;
    }

    public static ClassDef getCommonSuperclass(ClassDef class1, ClassDef class2) {
        if (class1 == class2) {
            return class1;
        }

        if (class1 == null) {
            return class2;
        }

        if (class2 == null) {
            return class1;
        }

        //TODO: do we want to handle primitive types here? I don't think so.. (if not, add assert)

        if (!class1.isInterface && class2.isInterface) {
            if (class1.implementsInterface(class2)) {
                return class2;
            }
            return theClassPath.javaLangObjectClassDef;
        }

        if (!class2.isInterface && class1.isInterface) {
            if (class2.implementsInterface(class1)) {
                return class1;
            }
            return theClassPath.javaLangObjectClassDef;
        }

        if (class1 instanceof ArrayClassDef && class2 instanceof ArrayClassDef) {
            return getCommonArraySuperclass((ArrayClassDef)class1, (ArrayClassDef)class2);
        }

        //we've got two non-array reference types. Find the class depth of each, and then move up the longer one
        //so that both classes are at the same class depth, and then move each class up until they match

        //we don't strictly need to keep track of the class depth separately, but it's probably slightly faster
        //to do so, rather than calling getClassDepth() many times
        int class1Depth = class1.getClassDepth();
        int class2Depth = class2.getClassDepth();

        while (class1Depth > class2Depth) {
            class1 = class1.superclass;
            class1Depth--;
        }

        while (class2Depth > class1Depth) {
            class2 = class2.superclass;
            class2Depth--;
        }

        while (class1Depth > 0) {
            if (class1 == class2) {
                return class1;
            }
            class1 = class1.superclass;
            class1Depth--;
            class2 = class2.superclass;
            class2Depth--;
        }

        return class1;
    }

    private static ClassDef getCommonArraySuperclass(ArrayClassDef class1, ArrayClassDef class2) {
        assert class1 != class2;

        //If one of the arrays is a primitive array, then the only option is to return java.lang.Object
        //TODO: might it be possible to merge something like int[] and short[] into int[]? (I don't think so..)
        if (class1.elementClass instanceof PrimitiveClassDef || class2.elementClass instanceof PrimitiveClassDef) {
            return theClassPath.javaLangObjectClassDef;
        }

        //if the two arrays have the same number of dimensions, then we should return an array class with the
        //same number of dimensions, for the common superclass of the 2 element classes
        if (class1.arrayDimensions == class2.arrayDimensions) {
            ClassDef commonElementClass = getCommonSuperclass(class1.elementClass, class2.elementClass);
            return getArrayClassDefByElementClassAndDimension(commonElementClass, class1.arrayDimensions);
        }

        //something like String[][][] and String[][] should be merged to Object[][]
        //this also holds when the element classes aren't the same (but are both reference types)
        int dimensions = Math.min(class1.arrayDimensions, class2.arrayDimensions);
        return getArrayClassDefByElementClassAndDimension(theClassPath.javaLangObjectClassDef, dimensions);
    }

    public static class ArrayClassDef extends ClassDef {
        private final ClassDef elementClass;
        private final int arrayDimensions;

        protected ArrayClassDef(String arrayClassType) {
            super(arrayClassType, true);
            assert arrayClassType.charAt(0) == '[';

            int i=0;
            while (arrayClassType.charAt(i) == '[') i++;

            String elementClassType = arrayClassType.substring(i);

            if (i>256) {
                throw new ExceptionWithContext("Error while creating array class for element type " + elementClassType +
                        " with " + i + " dimensions. The maximum number of dimensions is 256");
            }

            try {
                elementClass = ClassPath.getClassDef(arrayClassType.substring(i));
            } catch (ClassNotFoundException ex) {
                throw ExceptionWithContext.withContext(ex, "Error while creating array class " + arrayClassType);
            }
            arrayDimensions = i;
        }

        public ClassDef getElementClass() {
            return elementClass;
        }

        public int getArrayDimensions() {
            return arrayDimensions;
        }
    }

    public static class PrimitiveClassDef extends ClassDef {
        protected PrimitiveClassDef(String primitiveClassType) {
            super(primitiveClassType, false);
        }
    }

    public static class ClassDef {
        private final String classType;
        private final ClassDef superclass;
        /**
         * This is a list of all of the interfaces that a class implements, either directly or indirectly. It includes
         * all interfaces implemented by the superclass, and all super-interfaces of any implemented interface. The
         * intention is to make it easier to determine whether the class implements a given interface or not.
         */
        private final TreeSet<ClassDef> implementedInterfaces;

        private final boolean isInterface;

        private final int classDepth;

        private final String[] vtable;
        private final HashMap<String, Integer> virtualMethodLookup;

        private final SparseArray<String> instanceFields;
        private final HashMap<String, Integer> instanceFieldLookup;

        /**
         * This constructor is used for the ArrayClassDef and PrimitiveClassDef subclasses
         * @param classType the class type
         * @param isArrayType whether this is an array ClassDef or a primitive ClassDef
         */
        protected ClassDef(String classType, boolean isArrayType) {
            if (isArrayType) {
                assert (classType.charAt(0) == '[');
                this.classType = classType;
                this.superclass = ClassPath.theClassPath.javaLangObjectClassDef;
                implementedInterfaces = new TreeSet<ClassDef>();
                implementedInterfaces.add(ClassPath.getClassDef("Ljava/lang/Cloneable;"));
                implementedInterfaces.add(ClassPath.getClassDef("Ljava/io/Serializable;"));
                isInterface = false;

                vtable = superclass.vtable;
                virtualMethodLookup = superclass.virtualMethodLookup;

                instanceFields = superclass.instanceFields;
                instanceFieldLookup = superclass.instanceFieldLookup;
                classDepth = 1; //1 off from java.lang.Object
            } else {
                //primitive type
                this.classType = classType;
                this.superclass = null;
                implementedInterfaces = null;
                isInterface = false;
                vtable = null;
                virtualMethodLookup = null;
                instanceFields = null;
                instanceFieldLookup = null;
                classDepth = 0; //TODO: maybe use -1 to indicate not applicable?
            }
        }

        protected ClassDef(ClassDefItem classDefItem) {
            classType = classDefItem.getClassType().getTypeDescriptor();

            isInterface = (classDefItem.getAccessFlags() & AccessFlags.INTERFACE.getValue()) != 0;

            superclass = loadSuperclass(classDefItem);
            if (superclass == null) {
                classDepth = 0;
            } else {
                classDepth = superclass.classDepth + 1;
            }

            implementedInterfaces = loadAllImplementedInterfaces(classDefItem);

            vtable = loadVtable(classDefItem);
            virtualMethodLookup = new HashMap<String, Integer>((int)Math.ceil(vtable.length / .7f), .75f);
            for (int i=0; i<vtable.length; i++) {
                virtualMethodLookup.put(vtable[i], i);
            }

            instanceFields = loadFields(classDefItem);
            instanceFieldLookup = new HashMap<String, Integer>((int)Math.ceil(instanceFields.size() / .7f), .75f);
            for (int i=0; i<instanceFields.size(); i++) {
                instanceFieldLookup.put(instanceFields.get(i), i);
            }
        }

        public String getClassType() {
            return classType;
        }

        public ClassDef getSuperclass() {
            return superclass;
        }

        public int getClassDepth() {
            return classDepth;
        }

        public boolean isInterface() {
            return this.isInterface;
        }

        public boolean extendsClass(ClassDef superclassDef) {
            if (superclassDef == null) {
                return false;
            }

            if (this == superclassDef) {
                return true;
            }

            int superclassDepth = superclassDef.classDepth;
            ClassDef ancestor = this;
            while (ancestor.classDepth > superclassDepth) {
                ancestor = ancestor.getSuperclass();
            }

            return ancestor == superclassDef;
        }

        /**
         * Returns true if this class implements the given interface. This searches the interfaces that this class
         * directly implements, any interface implemented by this class's superclasses, and any super-interface of
         * any of these interfaces.
         * @param interfaceDef the interface
         * @return true if this class implements the given interface
         */
        public boolean implementsInterface(ClassDef interfaceDef) {
            return implementedInterfaces.contains(interfaceDef);
        }

        //TODO: GROT
        public void dumpVtable() {
            System.out.println(classType + " methods:");
            int i=0;
            for (String method: vtable) {
                System.out.println(i + ":\t" + method);
                i++;
            }
        }

        //TODO: GROT
        public void dumpFields() {
            System.out.println(classType + " fields:");
            for (int i=0; i<instanceFields.size(); i++) {
                int fieldOffset = instanceFields.keyAt(i);
                System.out.println(fieldOffset + ":\t" + instanceFields.valueAt(i));
            }
        }

        private void swap(byte[] fieldTypes, String[] fields, int position1, int position2) {
            byte tempType = fieldTypes[position1];
            fieldTypes[position1] = fieldTypes[position2];
            fieldTypes[position2] = tempType;

            String tempField = fields[position1];
            fields[position1] = fields[position2];
            fields[position2] = tempField;
        }

        private ClassDef loadSuperclass(ClassDefItem classDefItem) {
            if (classDefItem.getClassType().getTypeDescriptor().equals("Ljava/lang/Object;")) {
                if (classDefItem.getSuperclass() != null) {
                    throw new ExceptionWithContext("Invalid superclass " +
                            classDefItem.getSuperclass().getTypeDescriptor() + " for Ljava/lang/Object;. " +
                            "The Object class cannot have a superclass");
                }
                return null;
            } else {
                TypeIdItem superClass = classDefItem.getSuperclass();
                if (superClass == null) {
                    throw new ExceptionWithContext(classDefItem.getClassType().getTypeDescriptor() +
                            " has no superclass");
                }

                ClassDef superclass = ClassPath.getClassDef(superClass.getTypeDescriptor());

                if (!isInterface && superclass.isInterface) {
                    throw new ValidationException("Class " + classType + " has the interface " + superclass.classType +
                            " as its superclass");
                }
                if (isInterface && !superclass.isInterface && superclass !=
                        ClassPath.theClassPath.javaLangObjectClassDef) {
                    throw new ValidationException("Interface " + classType + " has the non-interface class " +
                            superclass.classType + " as its superclass");
                }

                return superclass;
            }
        }

        private TreeSet<ClassDef> loadAllImplementedInterfaces(ClassDefItem classDefItem) {
            assert classType != null;
            assert classType.equals("Ljava/lang/Object;") || superclass != null;
            assert classDefItem != null;

            TreeSet<ClassDef> implementedInterfaceSet = new TreeSet<ClassDef>();

            if (superclass != null) {
                for (ClassDef interfaceDef: superclass.implementedInterfaces) {
                    implementedInterfaceSet.add(interfaceDef);
                }
            }

            TypeListItem interfaces = classDefItem.getInterfaces();
            if (interfaces != null) {
                for (TypeIdItem interfaceType: interfaces.getTypes()) {
                    ClassDef interfaceDef = ClassPath.getClassDef(interfaceType.getTypeDescriptor());
                    assert interfaceDef.isInterface;
                    implementedInterfaceSet.add(interfaceDef);

                    interfaceDef = interfaceDef.getSuperclass();
                    while (!interfaceDef.getClassType().equals("Ljava/lang/Object;")) {
                        assert interfaceDef.isInterface;
                        implementedInterfaceSet.add(interfaceDef);
                        interfaceDef = interfaceDef.getSuperclass();
                    }
                }
            }

            return implementedInterfaceSet;
        }

        private String[] loadVtable(ClassDefItem classDefItem) {
            //TODO: it might be useful to keep track of which class's implementation is used for each virtual method. In other words, associate the implementing class type with each vtable entry
            List<String> virtualMethodList = new LinkedList<String>();
            //use a temp hash table, so that we can construct the final lookup with an appropriate
            //capacity, based on the number of virtual methods
            HashMap<String, Integer> tempVirtualMethodLookup = new HashMap<String, Integer>();

            //copy the virtual methods from the superclass
            int methodIndex = 0;
            if (superclass != null) {
                for (String method: superclass.vtable) {
                    virtualMethodList.add(method);
                    tempVirtualMethodLookup.put(method, methodIndex++);
                }

                assert superclass.instanceFields != null;
            }


            //iterate over the virtual methods in the current class, and only add them when we don't already have the
            //method (i.e. if it was implemented by the superclass)
            ClassDataItem classDataItem = classDefItem.getClassData();
            if (classDataItem != null) {
                EncodedMethod[] virtualMethods = classDataItem.getVirtualMethods();
                if (virtualMethods != null) {
                    for (EncodedMethod virtualMethod: virtualMethods) {
                        String methodString = virtualMethod.method.getMethodString();
                        if (tempVirtualMethodLookup.get(methodString) == null) {
                            virtualMethodList.add(methodString);
                        }
                    }
                }
            }

            String[] vtable = new String[virtualMethodList.size()];
            for (int i=0; i<virtualMethodList.size(); i++) {
                vtable[i] = virtualMethodList.get(i);
            }

            return vtable;
        }

        private SparseArray<String> loadFields(ClassDefItem classDefItem) {
            //This is a bit of an "involved" operation. We need to follow the same algorithm that dalvik uses to
            //arrange fields, so that we end up with the same field offsets (which is needed for deodexing).

            final byte REFERENCE = 0;
            final byte WIDE = 1;
            final byte OTHER = 2;

            ClassDataItem classDataItem = classDefItem.getClassData();

            String[] fields = null;
            //the "type" for each field in fields. 0=reference,1=wide,2=other
            byte[] fieldTypes = null;

            if (classDataItem != null) {
                EncodedField[] encodedFields = classDataItem.getInstanceFields();
                if (encodedFields != null) {
                    fields = new String[encodedFields.length];
                    fieldTypes = new byte[encodedFields.length];

                    for (int i=0; i<encodedFields.length; i++) {
                        EncodedField encodedField = encodedFields[i];
                        String fieldType = encodedField.field.getFieldType().getTypeDescriptor();
                        String field = String.format("%s:%s", encodedField.field.getFieldName().getStringValue(),
                                fieldType);
                        fieldTypes[i] = getFieldType(field);
                        fields[i] = field;
                    }
                }
            }

            if (fields == null) {
                fields = new String[0];
                fieldTypes = new byte[0];
            }

            //The first operation is to move all of the reference fields to the front. To do this, find the first
            //non-reference field, then find the last reference field, swap them and repeat
            int back = fields.length - 1;
            int front;
            for (front = 0; front<fields.length; front++) {
                if (fieldTypes[front] != REFERENCE) {
                    while (back > front) {
                        if (fieldTypes[back] == REFERENCE) {
                            swap(fieldTypes, fields, front, back--);
                            break;
                        }
                        back--;
                    }
                }

                if (fieldTypes[front] != REFERENCE) {
                    break;
                }
            }

            //next, we need to group all the wide fields after the reference fields. But the wide fields have to be
            //8-byte aligned. If we're on an odd field index, we need to insert a 32-bit field. If the next field
            //is already a 32-bit field, use that. Otherwise, find the first 32-bit field from the end and swap it in.
            //If there are no 32-bit fields, do nothing for now. We'll add padding when calculating the field offsets
            if (front < fields.length && (front % 2) != 0) {
                if (fieldTypes[front] == WIDE) {
                    //we need to swap in a 32-bit field, so the wide fields will be correctly aligned
                    back = fields.length - 1;
                    while (back > front) {
                        if (fieldTypes[back] == OTHER) {
                            swap(fieldTypes, fields, front++, back);
                            break;
                        }
                        back--;
                    }
                } else {
                    //there's already a 32-bit field here that we can use
                    front++;
                }
            }

            //do the swap thing for wide fields
            back = fields.length - 1;
            for (; front<fields.length; front++) {
                if (fieldTypes[front] != WIDE) {
                    while (back > front) {
                        if (fieldTypes[back] == WIDE) {
                            swap(fieldTypes, fields, front, back--);
                            break;
                        }
                        back--;
                    }
                }

                if (fieldTypes[front] != WIDE) {
                    break;
                }
            }

            int superFieldCount = 0;
            if (superclass != null) {
                superclass.instanceFields.size();
            }

            //now the fields are in the correct order. Add them to the SparseArray and lookup, and calculate the offsets
            int totalFieldCount = superFieldCount + fields.length;
            SparseArray<String> instanceFields = new SparseArray<String>(totalFieldCount);

            int fieldOffset;

            if (superclass != null && superFieldCount > 0) {
                for (int i=0; i<superFieldCount; i++) {
                    instanceFields.append(superclass.instanceFields.keyAt(i), superclass.instanceFields.valueAt(i));
                }

                fieldOffset = instanceFields.keyAt(superFieldCount-1);

                String lastSuperField = superclass.instanceFields.valueAt(superFieldCount-1);
                assert lastSuperField.indexOf(':') >= 0;
                assert lastSuperField.indexOf(':') < superFieldCount-1; //the ':' shouldn't be the last char
                char fieldType = lastSuperField.charAt(lastSuperField.indexOf(':') + 1);
                if (fieldType == 'J' || fieldType == 'D') {
                    fieldOffset += 8;
                } else {
                    fieldOffset += 4;
                }
            } else {
                //the field values start at 8 bytes into the DataObject dalvik structure
                fieldOffset = 8;
            }

            boolean gotDouble = false;
            for (int i=0; i<fields.length; i++) {
                String field = fields[i];

                //add padding to align the wide fields, if needed
                if (fieldTypes[i] == WIDE && !gotDouble) {
                    if (!gotDouble) {
                        if (fieldOffset % 8 != 0) {
                            assert fieldOffset % 8 == 4;
                            fieldOffset += 4;
                        }
                        gotDouble = true;
                    }
                }

                instanceFields.append(fieldOffset, field);
                if (fieldTypes[i] == WIDE) {
                    fieldOffset += 8;
                } else {
                    fieldOffset += 4;
                }
            }
            return instanceFields;
        }

        private byte getFieldType(String field) {
            int sepIndex = field.indexOf(':');

            //we could use sepIndex >= field.length()-1 instead, but that's too easy to mistake for an off-by-one error
            if (sepIndex < 0 || sepIndex == field.length()-1 || sepIndex >= field.length()) {
                assert false;
                throw new ExceptionWithContext("Invalid field format: " + field);
            }
            switch (field.charAt(sepIndex+1)) {
                case '[':
                case 'L':
                    return 0; //REFERENCE
                case 'J':
                case 'D':
                    return 1; //WIDE
                default:
                    return 2; //OTHER
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ClassDef)) return false;

            ClassDef classDef = (ClassDef) o;

            return classType.equals(classDef.classType);
        }

        @Override
        public int hashCode() {
            return classType.hashCode();
        }
    }
}
