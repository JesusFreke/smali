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

package org.jf.dexlib;

import org.jf.dexlib.Util.*;

import java.util.List;
import java.util.Collections;

public class ClassDataItem extends Item<ClassDataItem> {
    private EncodedField[] staticFields;
    private EncodedField[] instanceFields;
    private EncodedMethod[] directMethods;
    private EncodedMethod[] virtualMethods;

    private ClassDefItem parent = null;

    /**
     * Creates a new uninitialized <code>ClassDataItem</code>
     * @param dexFile The <code>DexFile</code> that this item belongs to
     */
    public ClassDataItem(final DexFile dexFile) {
        super(dexFile);
    }

    /**
     * Creates a new <code>ClassDataItem</code> with the given values
     * @param dexFile The <code>DexFile</code> that this item belongs to
     * @param staticFields The static fields for this class
     * @param instanceFields The instance fields for this class
     * @param directMethods The direct methods for this class
     * @param virtualMethods The virtual methods for this class
     */
    private ClassDataItem(DexFile dexFile, EncodedField[] staticFields, EncodedField[] instanceFields,
                         EncodedMethod[] directMethods, EncodedMethod[] virtualMethods) {
        super(dexFile);
        this.staticFields = staticFields==null?new EncodedField[0]:staticFields;
        this.instanceFields = instanceFields==null?new EncodedField[0]:instanceFields;
        this.directMethods = directMethods==null?new EncodedMethod[0]:directMethods;
        this.virtualMethods = virtualMethods==null?new EncodedMethod[0]:virtualMethods;
    }

    /**
     * Creates a new <code>ClassDataItem</code> with the given values
     * @param dexFile The <code>DexFile</code> that this item belongs to
     * @param staticFields The static fields for this class
     * @param instanceFields The instance fields for this class
     * @param directMethods The direct methods for this class
     * @param virtualMethods The virtual methods for this class
     * @return a new <code>ClassDataItem</code> with the given values
     */
    public static ClassDataItem getInternedClassDataItem(DexFile dexFile, List<EncodedField> staticFields,
                                                         List<EncodedField> instanceFields,
                                                         List<EncodedMethod> directMethods,
                                                         List<EncodedMethod> virtualMethods) {
        EncodedField[] staticFieldsArray = null;
        EncodedField[] instanceFieldsArray = null;
        EncodedMethod[] directMethodsArray = null;
        EncodedMethod[] virtualMethodsArray = null;

        if (staticFields != null && staticFields.size() > 0) {
            Collections.sort(staticFields);
            staticFieldsArray = new EncodedField[staticFields.size()];
            staticFields.toArray(staticFieldsArray);
        }

        if (instanceFields != null && instanceFields.size() > 0) {
            Collections.sort(instanceFields);
            instanceFieldsArray = new EncodedField[instanceFields.size()];
            instanceFields.toArray(instanceFieldsArray);
        }

        if (directMethods != null && directMethods.size() > 0) {
            Collections.sort(directMethods);
            directMethodsArray = new EncodedMethod[directMethods.size()];
            directMethods.toArray(directMethodsArray);
        }

        if (virtualMethods != null && virtualMethods.size() > 0) {
            Collections.sort(virtualMethods);
            virtualMethodsArray = new EncodedMethod[virtualMethods.size()];
            virtualMethods.toArray(virtualMethodsArray);
        }

        ClassDataItem classDataItem = new ClassDataItem(dexFile, staticFieldsArray, instanceFieldsArray,
                directMethodsArray, virtualMethodsArray);
        return dexFile.ClassDataSection.intern(classDataItem);
    }

    /** {@inheritDoc} */
    protected void readItem(Input in, ReadContext readContext) {
        staticFields = new EncodedField[in.readUnsignedLeb128()];
        instanceFields = new EncodedField[in.readUnsignedLeb128()];
        directMethods = new EncodedMethod[in.readUnsignedLeb128()];
        virtualMethods = new EncodedMethod[in.readUnsignedLeb128()];

        EncodedField previousEncodedField = null;
        for (int i=0; i<staticFields.length; i++) {
            try {
                staticFields[i] = previousEncodedField = new EncodedField(dexFile, in, previousEncodedField);
            } catch (Exception ex) {
                throw ExceptionWithContext.withContext(ex, "Error while reading static field at index " + i);
            }
        }

        previousEncodedField = null;
        for (int i=0; i<instanceFields.length; i++) {
            try {
                instanceFields[i] = previousEncodedField = new EncodedField(dexFile, in, previousEncodedField);
            } catch (Exception ex) {
                throw ExceptionWithContext.withContext(ex, "Error while reading instance field at index " + i);
            }
        }

        EncodedMethod previousEncodedMethod = null;
        for (int i=0; i<directMethods.length; i++) {
            try {
                directMethods[i] = previousEncodedMethod = new EncodedMethod(dexFile, readContext, in,
                        previousEncodedMethod);
            } catch (Exception ex) {
                throw ExceptionWithContext.withContext(ex, "Error while reading direct method at index " + i);
            }
        }

        previousEncodedMethod = null;
        for (int i=0; i<virtualMethods.length; i++) {
            try {
                virtualMethods[i] = previousEncodedMethod = new EncodedMethod(dexFile, readContext, in,
                        previousEncodedMethod);
            } catch (Exception ex) {
                throw ExceptionWithContext.withContext(ex, "Error while reading virtual method at index " + i);
            }
        }
    }

    /** {@inheritDoc} */
    protected int placeItem(int offset) {
        offset += Leb128Utils.unsignedLeb128Size(staticFields.length);
        offset += Leb128Utils.unsignedLeb128Size(instanceFields.length);
        offset += Leb128Utils.unsignedLeb128Size(directMethods.length);
        offset += Leb128Utils.unsignedLeb128Size(virtualMethods.length);

        EncodedField previousEncodedField = null;
        for (EncodedField encodedField: staticFields) {
            offset = encodedField.place(offset, previousEncodedField);
            previousEncodedField = encodedField;
        }

        previousEncodedField = null;
        for (EncodedField encodedField: instanceFields) {
            offset = encodedField.place(offset, previousEncodedField);
            previousEncodedField = encodedField;
        }

        EncodedMethod previousEncodedMethod = null;
        for (EncodedMethod encodedMethod: directMethods) {
            offset = encodedMethod.place(offset, previousEncodedMethod);
            previousEncodedMethod = encodedMethod;
        }

        previousEncodedMethod = null;
        for (EncodedMethod encodedMethod: virtualMethods) {
            offset = encodedMethod.place(offset, previousEncodedMethod);
            previousEncodedMethod = encodedMethod;
        }

        return offset;
    }

    /** {@inheritDoc} */
    protected void writeItem(AnnotatedOutput out) {
        if (out.annotates()) {
            out.annotate("static_fields_size: 0x" + Integer.toHexString(staticFields.length) + " (" +
                    staticFields.length + ")");
            out.writeUnsignedLeb128(staticFields.length);
            out.annotate("instance_fields_size: 0x" + Integer.toHexString(instanceFields.length) + " (" +
                    instanceFields.length + ")");
            out.writeUnsignedLeb128(instanceFields.length);
            out.annotate("direct_methods_size: 0x" + Integer.toHexString(directMethods.length) + " (" +
                    directMethods.length + ")");
            out.writeUnsignedLeb128(directMethods.length);
            out.annotate("virtual_methods_size: 0x" + Integer.toHexString(virtualMethods.length) + " (" +
                    virtualMethods.length + ")");
            out.writeUnsignedLeb128(virtualMethods.length);

            int index = 0;
            EncodedField previousEncodedField = null;
            for (EncodedField encodedField: staticFields) {
                out.annotate("[" + index++ + "] static_field");
                out.indent();
                encodedField.writeTo(out, previousEncodedField);
                out.deindent();
                previousEncodedField = encodedField;
            }

            index = 0;
            previousEncodedField = null;
            for (EncodedField encodedField: instanceFields) {
                out.annotate("[" + index++ + "] instance_field");
                out.indent();
                encodedField.writeTo(out, previousEncodedField);
                out.deindent();
                previousEncodedField = encodedField;
            }

            index = 0;
            EncodedMethod previousEncodedMethod = null;
            for (EncodedMethod encodedMethod: directMethods) {
                out.annotate("[" + index++ + "] direct_method");
                out.indent();
                encodedMethod.writeTo(out, previousEncodedMethod);
                out.deindent();
                previousEncodedMethod = encodedMethod;
            }

            index = 0;
            previousEncodedMethod = null;
            for (EncodedMethod encodedMethod: virtualMethods) {
                out.annotate("[" + index++ + "] virtual_method");
                out.indent();
                encodedMethod.writeTo(out, previousEncodedMethod);
                out.deindent();
                previousEncodedMethod = encodedMethod;
            }
        } else {
            out.writeUnsignedLeb128(staticFields.length);
            out.writeUnsignedLeb128(instanceFields.length);
            out.writeUnsignedLeb128(directMethods.length);
            out.writeUnsignedLeb128(virtualMethods.length);

            EncodedField previousEncodedField = null;
            for (EncodedField encodedField: staticFields) {
                encodedField.writeTo(out, previousEncodedField);
                previousEncodedField = encodedField;
            }

            previousEncodedField = null;
            for (EncodedField encodedField: instanceFields) {
                encodedField.writeTo(out, previousEncodedField);
                previousEncodedField = encodedField;
            }

            EncodedMethod previousEncodedMethod = null;
            for (EncodedMethod encodedMethod: directMethods) {
                encodedMethod.writeTo(out, previousEncodedMethod);
                previousEncodedMethod = encodedMethod;
            }

            previousEncodedMethod = null;
            for (EncodedMethod encodedMethod: virtualMethods) {
                encodedMethod.writeTo(out, previousEncodedMethod);
                previousEncodedMethod = encodedMethod;
            }
        }
    }

    /** {@inheritDoc} */
    public ItemType getItemType() {
        return ItemType.TYPE_CLASS_DATA_ITEM;
    }

    /** {@inheritDoc} */
    public String getConciseIdentity() {
        if (parent == null) {
            return "class_data_item @0x" + Integer.toHexString(getOffset());
        }
        return "class_data_item @0x" + Integer.toHexString(getOffset()) + " (" + parent.getClassType() +")";
    }

    /** {@inheritDoc} */
    public int compareTo(ClassDataItem other) {
        if (parent == null) {
            if (other.parent == null) {
                return 0;
            }
            return -1;
        }
        if (other.parent == null) {
            return 1;
        }
        return parent.compareTo(other.parent);
    }

    /**
     * Sets the <code>ClassDefItem</code> that this <code>ClassDataItem</code> is associated with
     * @param classDefItem the <code>ClassDefItem</code> that this <code>ClassDataItem</code> is associated with
     */
    protected void setParent(ClassDefItem classDefItem) {
        this.parent = classDefItem;
    }

    /**
     * @return the static fields for this class
     */
    public EncodedField[] getStaticFields() {
        return staticFields;
    }

    /**
     * @return the instance fields for this class
     */
    public EncodedField[] getInstanceFields() {
        return instanceFields;
    }

    /**
     * @return the direct methods for this class
     */
    public EncodedMethod[] getDirectMethods() {
        return directMethods;
    }

    /**
     * @return the virtual methods for this class
     */
    public EncodedMethod[] getVirtualMethods() {
        return virtualMethods;
    }

    public static class EncodedField implements Comparable<EncodedField> {
        /**
         * The <code>FieldIdItem</code> that this <code>EncodedField</code> is associated with
         */
        public final FieldIdItem field;

        /**
         * The access flags for this field
         */
        public final int accessFlags;

        /**
         * Constructs a new <code>EncodedField</code> with the given values
         * @param field The <code>FieldIdItem</code> that this <code>EncodedField</code> is associated with
         * @param accessFlags The access flags for this field
         */
        public EncodedField(FieldIdItem field, int accessFlags) {
            this.field = field;
            this.accessFlags = accessFlags;
        }

        /**
         * This is used internally to construct a new <code>EncodedField</code> while reading in a <code>DexFile</code>
         * @param dexFile The <code>DexFile</code> that is being read in
         * @param in the Input object to read the <code>EncodedField</code> from
         * @param previousEncodedField The previous <code>EncodedField</code> in the list containing this
         * <code>EncodedField</code>.
         */
        private EncodedField(DexFile dexFile, Input in, EncodedField previousEncodedField) {
            int previousIndex = previousEncodedField==null?0:previousEncodedField.field.getIndex();
            field = dexFile.FieldIdsSection.getItemByIndex(in.readUnsignedLeb128() + previousIndex);
            accessFlags = in.readUnsignedLeb128();
        }

        /**
         * Writes the <code>EncodedField</code> to the given <code>AnnotatedOutput</code> object
         * @param out the <code>AnnotatedOutput</code> object to write to
         * @param previousEncodedField The previous <code>EncodedField</code> in the list containing this
         * <code>EncodedField</code>.
         */
        private void writeTo(AnnotatedOutput out, EncodedField previousEncodedField) {
            int previousIndex = previousEncodedField==null?0:previousEncodedField.field.getIndex();

            if (out.annotates()) {
                out.annotate("field: " + field.getFieldString());
                out.writeUnsignedLeb128(field.getIndex() - previousIndex);
                out.annotate("access_flags: " + AccessFlags.formatAccessFlagsForField(accessFlags));
                out.writeUnsignedLeb128(accessFlags);
            }else {
                out.writeUnsignedLeb128(field.getIndex() - previousIndex);
                out.writeUnsignedLeb128(accessFlags);
            }
        }

        /**
         * Calculates the size of this <code>EncodedField</code> and returns the offset
         * immediately following it
         * @param offset the offset of this <code>EncodedField</code> in the <code>DexFile</code>
         * @param previousEncodedField The previous <code>EncodedField</code> in the list containing this
         * <code>EncodedField</code>.
         * @return the offset immediately following this <code>EncodedField</code>
         */
        private int place(int offset, EncodedField previousEncodedField) {
            int previousIndex = previousEncodedField==null?0:previousEncodedField.field.getIndex();

            offset += Leb128Utils.unsignedLeb128Size(field.getIndex() - previousIndex);
            offset += Leb128Utils.unsignedLeb128Size(accessFlags);
            return  offset;
        }

        /**
         * Compares this <code>EncodedField</code> to another, based on the comparison of the associated
         * <code>FieldIdItem</code>
         * @param other The <code>EncodedField</code> to compare against
         * @return a standard integer comparison value indicating the relationship
         */
        public int compareTo(EncodedField other)
        {
            return field.compareTo(other.field);
        }

        /**
         * @return true if this is a static field
         */
        public boolean isStatic() {
            return (accessFlags & AccessFlags.STATIC.getValue()) != 0;
        }
    }

    public static class EncodedMethod implements Comparable<EncodedMethod> {
        /**
         * The <code>MethodIdItem</code> that this <code>EncodedMethod</code> is associated with
         */
        public final MethodIdItem method;

        /**
         * The access flags for this method
         */
        public final int accessFlags;

        /**
         * The <code>CodeItem</code> containing the code for this method, or null if there is no code for this method
         * (i.e. an abstract method)
         */
        public final CodeItem codeItem;

        /**
         * Constructs a new <code>EncodedMethod</code> with the given values
         * @param method The <code>MethodIdItem</code> that this <code>EncodedMethod</code> is associated with
         * @param accessFlags The access flags for this method
         * @param codeItem The <code>CodeItem</code> containing the code for this method, or null if there is no code
         * for this method (i.e. an abstract method)
         */
        public EncodedMethod(MethodIdItem method, int accessFlags, CodeItem codeItem) {
            this.method = method;
            this.accessFlags = accessFlags;
            this.codeItem = codeItem;
            if (codeItem != null) {
                codeItem.setParent(this);
            }
        }

        /**
         * This is used internally to construct a new <code>EncodedMethod</code> while reading in a <code>DexFile</code>
         * @param dexFile The <code>DexFile</code> that is being read in
         * @param readContext a <code>ReadContext</code> object to hold information that is only needed while reading
         * in a file
         * @param in the Input object to read the <code>EncodedMethod</code> from
         * @param previousEncodedMethod The previous <code>EncodedMethod</code> in the list containing this
         * <code>EncodedMethod</code>.
         */
        public EncodedMethod(DexFile dexFile, ReadContext readContext, Input in, EncodedMethod previousEncodedMethod) {
            int previousIndex = previousEncodedMethod==null?0:previousEncodedMethod.method.getIndex();
            method = dexFile.MethodIdsSection.getItemByIndex(in.readUnsignedLeb128() + previousIndex);
            accessFlags = in.readUnsignedLeb128();
            codeItem = (CodeItem)readContext.getOptionalOffsettedItemByOffset(ItemType.TYPE_CODE_ITEM,
                    in.readUnsignedLeb128());
            if (codeItem != null) {
                codeItem.setParent(this);
            }
        }

        /**
         * Writes the <code>EncodedMethod</code> to the given <code>AnnotatedOutput</code> object
         * @param out the <code>AnnotatedOutput</code> object to write to
         * @param previousEncodedMethod The previous <code>EncodedMethod</code> in the list containing this
         * <code>EncodedMethod</code>.
         */
        private void writeTo(AnnotatedOutput out, EncodedMethod previousEncodedMethod) {
            int previousIndex = previousEncodedMethod==null?0:previousEncodedMethod.method.getIndex();

            if (out.annotates()) {
                out.annotate("method: " + method.getMethodString());
                out.writeUnsignedLeb128(method.getIndex() - previousIndex);
                out.annotate("access_flags: " + AccessFlags.formatAccessFlagsForMethod(accessFlags));
                out.writeUnsignedLeb128(accessFlags);
                if (codeItem != null) {
                    out.annotate("code_off: 0x" + Integer.toHexString(codeItem.getOffset()));
                    out.writeUnsignedLeb128(codeItem.getOffset());
                } else {
                    out.annotate("code_off: 0x0");
                    out.writeUnsignedLeb128(0);
                }
            }else {
                out.writeUnsignedLeb128(method.getIndex() - previousIndex);
                out.writeUnsignedLeb128(accessFlags);
                out.writeUnsignedLeb128(codeItem==null?0:codeItem.getOffset());
            }
        }

        /**
         * Calculates the size of this <code>EncodedMethod</code> and returns the offset
         * immediately following it
         * @param offset the offset of this <code>EncodedMethod</code> in the <code>DexFile</code>
         * @param previousEncodedMethod The previous <code>EncodedMethod</code> in the list containing this
         * <code>EncodedMethod</code>.
         * @return the offset immediately following this <code>EncodedField</code>
         */
        private int place(int offset, EncodedMethod previousEncodedMethod) {
            int previousIndex = previousEncodedMethod==null?0:previousEncodedMethod.method.getIndex();

            offset += Leb128Utils.unsignedLeb128Size(method.getIndex() - previousIndex);
            offset += Leb128Utils.unsignedLeb128Size(accessFlags);
            offset += codeItem==null?1:Leb128Utils.unsignedLeb128Size(codeItem.getOffset());
            return  offset;
        }

        /**
         * Compares this <code>EncodedMethod</code> to another, based on the comparison of the associated
         * <code>MethodIdItem</code>
         * @param other The <code>EncodedMethod</code> to compare against
         * @return a standard integer comparison value indicating the relationship
         */
        public int compareTo(EncodedMethod other) {
            return method.compareTo(other.method);
        }

        /**
         * @return true if this is a direct method
         */
        public boolean isDirect() {
            return ((accessFlags & (AccessFlags.STATIC.getValue() | AccessFlags.PRIVATE.getValue() |
                    AccessFlags.CONSTRUCTOR.getValue())) != 0);
        }
    }
}
