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
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.dexlib;

import org.jf.dexlib.util.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClassDataItem extends OffsettedItem<ClassDataItem> {
    private final ArrayList<EncodedField> staticFieldList = new ArrayList<EncodedField>();
    private final ArrayList<EncodedField> instanceFieldList = new ArrayList<EncodedField>();
    private final ArrayList<EncodedMethod> directMethodList = new ArrayList<EncodedMethod>();
    private final ArrayList<EncodedMethod> virtualMethodList = new ArrayList<EncodedMethod>();

    private final ListSizeField staticFieldsCountField;
    private final ListSizeField instanceFieldsCountField;
    private final ListSizeField directMethodsCountField;
    private final ListSizeField virtualMethodsCountField;
    private final EncodedMemberList<EncodedField> staticFieldsListField;
    private final EncodedMemberList<EncodedField> instanceFieldsListField;
    private final EncodedMemberList<EncodedMethod> directMethodsListField;
    private final EncodedMemberList<EncodedMethod> virtualMethodsListField;


    public ClassDataItem(final DexFile dexFile, int offset) {
        super(offset);

        fields = new Field[] {
                staticFieldsCountField = new ListSizeField(staticFieldList,
                        new Leb128Field("static_fields_size")),
                instanceFieldsCountField = new ListSizeField(instanceFieldList,
                        new Leb128Field("instance_fields_size")),
                directMethodsCountField = new ListSizeField(directMethodList,
                        new Leb128Field("direct_methods_size")),
                virtualMethodsCountField = new ListSizeField(virtualMethodList,
                        new Leb128Field("virtual_methods_size")),
                staticFieldsListField = new EncodedMemberList<EncodedField>(staticFieldList, "static_fields") {
                    protected EncodedField make(EncodedField previousField) {
                        return new EncodedField(dexFile, previousField);
                    }
                },
                instanceFieldsListField = new EncodedMemberList<EncodedField>(instanceFieldList, "instance_fields") {
                    protected EncodedField make(EncodedField previousField) {
                        return new EncodedField(dexFile, previousField);
                    }
                },
                directMethodsListField = new EncodedMemberList<EncodedMethod>(directMethodList, "direct_methods") {
                    protected EncodedMethod make(EncodedMethod previousMethod) {
                        return new EncodedMethod(dexFile, previousMethod);
                    }
                },
                virtualMethodsListField = new EncodedMemberList<EncodedMethod>(virtualMethodList, "virtual_methods") {
                    protected EncodedMethod make(EncodedMethod previousMethod) {
                        return new EncodedMethod(dexFile, previousMethod);
                    }
                }
        };
    }

    public void addMethod(EncodedMethod encodedMethod) {
        if (encodedMethod.isDirect()) {
            directMethodList.add(encodedMethod);
        } else {
            virtualMethodList.add(encodedMethod);
        }
    }

    public int addField(EncodedField encodedField) {
        if (encodedField.isStatic()) {
            int index = Collections.binarySearch(staticFieldList, encodedField);
            if (index >= 0) {
                throw new RuntimeException("A static field of that name and type is already present");
            }
            index = (index + 1) * -1;
            staticFieldList.add(index, encodedField);
            return index;
        } else {
            int index = Collections.binarySearch(instanceFieldList, encodedField);
            if (index >= 0) {
                throw new RuntimeException("An instance field of that name and type is already present");
            }
            index = (index + 1) * -1;
            instanceFieldList.add(index, encodedField);
            return index;
        }
    }

    public List<EncodedField> getStaticFields() {
        return Collections.unmodifiableList(staticFieldList);
    }

    public List<EncodedField> getInstanceFields() {
        return Collections.unmodifiableList(instanceFieldList);
    }

    public List<EncodedMethod> getDirectMethods() {
        return Collections.unmodifiableList(directMethodList);
    }

    public List<EncodedMethod> getVirtualMethods() {
        return Collections.unmodifiableList(virtualMethodList);
    }                                      

    private static abstract class EncodedMember<T extends EncodedMember<T>> extends CompositeField<T> implements Field<T>, Comparable<T> 
    {
        public EncodedMember(String fieldName) {
            super(fieldName);
        }

        protected abstract void setPreviousMember(T previousMember);
    }

    private static abstract class EncodedMemberList<T extends EncodedMember<T>>  implements Field<EncodedMemberList<T>> {
        private final ArrayList<T> list;
        private final String fieldName;

        public EncodedMemberList(ArrayList<T> list, String fieldName) {
            this.list = list;
            this.fieldName = fieldName;
        }

        public void writeTo(AnnotatedOutput out) {
            out.annotate(0, fieldName + ":");
            int i=0;
            for (T field: list) {
                out.annotate(0, "[0x" + Integer.toHexString(i) + "]");
                field.writeTo(out);
                i++;
            }
        }

        protected abstract T make(T previousField);

        public void readFrom(Input in) {
            for (int i = 0; i < list.size(); i++) {
                T previousField = null;
                if (i > 0) {
                    previousField = list.get(i-1);
                }
                T field = make(previousField);
                list.set(i, field);
                field.readFrom(in);
            }
        }

        public int place(int offset) {
            Collections.sort(list);

            T previousMember = null;
            for (T encodedMember: list) {
                encodedMember.setPreviousMember(previousMember);
                offset = encodedMember.place(offset);
                previousMember = encodedMember;
            }
            return offset;
        }

        public void copyTo(DexFile dexFile, EncodedMemberList<T> copy) {
            copy.list.clear();
            copy.list.ensureCapacity(list.size());
            for (int i = 0; i < list.size(); i++) {
                T previousField = null;
                if (i > 0) {
                    previousField = copy.list.get(i-1);
                }
                T fieldCopy = copy.make(previousField);
                list.get(i).copyTo(dexFile, fieldCopy);
                copy.list.add(fieldCopy);
            }
        }

        public int hashCode() {
            int h = 1;
            for (int i = 0; i < list.size(); i++) {
                h = h * 31 + list.get(i).hashCode();
            }
            return h;
        }

        public boolean equals(Object o) {
            if (!(o instanceof EncodedMemberList)) {
                return false;
            }

            EncodedMemberList<T> other = (EncodedMemberList<T>)o;
            if (list.size() != other.list.size()) {
                return false;
            }

            for (int i = 0; i < list.size(); i++) {
                if (!list.get(i).equals(other.list.get(i))) {
                    return false;
                }
            }
            return true;
        }
    }

    public static class EncodedField extends EncodedMember<EncodedField> {
        private final IndexedItemReference<FieldIdItem> fieldReferenceField;
        private final Leb128DeltaField fieldIndexField;
        private final Leb128Field accessFlagsField;

        public EncodedField(DexFile dexFile, final EncodedField previousField) {
            super("encoded_field");
            Leb128DeltaField previousIndexField = null;
            if (previousField != null) {
                previousIndexField = previousField.fieldIndexField;
            }


            fields = new Field[] {
                    fieldReferenceField = new IndexedItemReference<FieldIdItem>(dexFile.FieldIdsSection,
                            fieldIndexField = new Leb128DeltaField(previousIndexField, null), "field_idx_diff"),
                    accessFlagsField = new Leb128Field("access_flags")
            };
        }

        public EncodedField(DexFile dexFile, FieldIdItem field, int accessFlags) {
            super("encoded_field");
            fields = new Field[] {
                    this.fieldReferenceField = new IndexedItemReference<FieldIdItem>(dexFile, field,
                            fieldIndexField = new Leb128DeltaField(null), "field_idx_diff"),
                    this.accessFlagsField = new Leb128Field(accessFlags, "access_flags")
            };
        }

        protected void setPreviousMember(EncodedField previousField) {
            if (previousField != null) {
                fieldIndexField.setPreviousField(previousField.fieldIndexField);
            } else {
                fieldIndexField.setPreviousField(null);
            }
        }

        public int compareTo(EncodedField other)
        {
            return fieldReferenceField.getReference().compareTo(other.fieldReferenceField.getReference());
        }

        public boolean isStatic() {
            return (accessFlagsField.getCachedValue() & AccessFlags.STATIC.getValue()) != 0;
        }

        public FieldIdItem getField() {
            return fieldReferenceField.getReference();
        }

        public int getAccessFlags() {
            return accessFlagsField.getCachedValue();
        }
    }

    public static class EncodedMethod extends EncodedMember<EncodedMethod> {
        private final IndexedItemReference<MethodIdItem> methodReferenceField;
        private final Leb128DeltaField methodIndexField;
        private final Leb128Field accessFlagsField;
        private final OffsettedItemReference<CodeItem> codeItemReferenceField;

        public EncodedMethod(DexFile dexFile, final EncodedMethod previousMethod) {
            super("encedod_method");
            Leb128DeltaField previousIndexField = null;
            if (previousMethod != null) {
                previousIndexField = previousMethod.methodIndexField;
            }

            fields = new Field[] {
                    methodReferenceField = new IndexedItemReference<MethodIdItem>(dexFile.MethodIdsSection,
                            methodIndexField = new Leb128DeltaField(previousIndexField, null), "method_idx_diff"),
                    accessFlagsField = new Leb128Field("access_flags"),
                    codeItemReferenceField = new OffsettedItemReference<CodeItem>(dexFile.CodeItemsSection,
                            new Leb128Field(null), "code_off")
            };
        }

        public EncodedMethod(DexFile dexFile, MethodIdItem methodIdItem, int accessFlags, CodeItem codeItem) {
            super("encoded_method");
            fields = new Field[] {
                    this.methodReferenceField = new IndexedItemReference<MethodIdItem>(dexFile, methodIdItem,
                            methodIndexField = new Leb128DeltaField(null), "method_idx_diff"),
                    this.accessFlagsField = new Leb128Field(accessFlags, "access_flags"),
                    this.codeItemReferenceField = new OffsettedItemReference<CodeItem>(dexFile, codeItem,
                            new Leb128Field(null), "code_off")
            };
        }

        protected void setPreviousMember(EncodedMethod previousMethod) {
            if (previousMethod != null) {
                methodIndexField.setPreviousField(previousMethod.methodIndexField);
            } else {
                methodIndexField.setPreviousField(null);
            }
        }

        public int compareTo(EncodedMethod other) {
            return methodReferenceField.getReference().compareTo(other.methodReferenceField.getReference());
        }

        public boolean isDirect() {
            return ((accessFlagsField.getCachedValue() & (AccessFlags.STATIC.getValue() | AccessFlags.PRIVATE.getValue() |
                    AccessFlags.CONSTRUCTOR.getValue())) != 0);
        }

        public int getAccessFlags() {
            return accessFlagsField.getCachedValue();
        }

        public MethodIdItem getMethod() {
            return methodReferenceField.getReference();
        }

        public CodeItem getCodeItem() {
            return codeItemReferenceField.getReference();
        }
    }


    /**
     * An Leb128 integer that encodes its value as the difference between
     * itself and the previous Leb128DeltaField in the list. The first
     * item encodes the value as per normal
     */
    protected static class Leb128DeltaField extends Leb128Field {
        private Leb128DeltaField previousField = null;

        public Leb128DeltaField(String fieldName) {
            super(fieldName);
        }

        public void readFrom(Input in) {
            super.readFrom(in);
            value += getPreviousValue();
        }

        public int place(int offset) {
            return offset + Leb128Utils.unsignedLeb128Size(value - getPreviousValue());
        }

        private int getPreviousValue() {
            if (previousField == null) {
                return 0;
            }
            return previousField.value;
        }

        public void writeValue(Output out) {
            out.writeUnsignedLeb128(value - getPreviousValue());
        }

        public Leb128DeltaField(Leb128DeltaField previousField, String fieldName) {
            super(fieldName);
            this.previousField = previousField;
        }

        public void setPreviousField(Leb128DeltaField previousField) {
            this.previousField = previousField;
        }
    }

    protected int getAlignment() {
        return 1;
    }

    public ItemType getItemType() {
        return ItemType.TYPE_CLASS_DATA_ITEM;
    }

    public String getConciseIdentity() {
        return "class_data_item @0x" + Integer.toHexString(getOffset());
    }
}
