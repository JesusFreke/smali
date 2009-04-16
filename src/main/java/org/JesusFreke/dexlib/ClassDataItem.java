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

package org.JesusFreke.dexlib;

import org.JesusFreke.dexlib.ItemType;
import org.JesusFreke.dexlib.util.Output;
import org.JesusFreke.dexlib.util.Input;
import org.JesusFreke.dexlib.util.AccessFlags;

import java.util.ArrayList;
import java.util.Collections;

public class ClassDataItem extends OffsettedItem<ClassDataItem> {
    private final Field[] fields;

    private final ArrayList<EncodedField> staticFieldList = new ArrayList<EncodedField>();
    private final ArrayList<EncodedField> instanceFieldList = new ArrayList<EncodedField>();
    private final ArrayList<EncodedMethod> directMethodList = new ArrayList<EncodedMethod>();
    private final ArrayList<EncodedMethod> virtualMethodList = new ArrayList<EncodedMethod>();

    private final ListSizeField staticFieldsCount;
    private final ListSizeField instanceFieldsCount;
    private final ListSizeField directMethodsCount;
    private final ListSizeField virtualMethodsCount;
    private final EncodedMemberList<EncodedField> staticFields;
    private final EncodedMemberList<EncodedField> instanceFields;
    private final EncodedMemberList<EncodedMethod> directMethods;
    private final EncodedMemberList<EncodedMethod> virtualMethods;


    public ClassDataItem(final DexFile dexFile, int offset) {
        super(offset);

        fields = new Field[] {
                staticFieldsCount = new ListSizeField(staticFieldList, new Leb128Field()),
                instanceFieldsCount = new ListSizeField(instanceFieldList, new Leb128Field()),
                directMethodsCount = new ListSizeField(directMethodList, new Leb128Field()),
                virtualMethodsCount = new ListSizeField(virtualMethodList, new Leb128Field()),
                staticFields = new EncodedMemberList<EncodedField>(staticFieldList) {
                    protected EncodedField make(EncodedField previousField) {
                        return new EncodedField(dexFile, previousField);
                    }
                },
                instanceFields = new EncodedMemberList<EncodedField>(instanceFieldList) {
                    protected EncodedField make(EncodedField previousField) {
                        return new EncodedField(dexFile, previousField);
                    }
                },
                directMethods = new EncodedMemberList<EncodedMethod>(directMethodList) {
                    protected EncodedMethod make(EncodedMethod previousMethod) {
                        return new EncodedMethod(dexFile, previousMethod);
                    }
                },
                virtualMethods = new EncodedMemberList<EncodedMethod>(virtualMethodList) {
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
            staticFieldList.add(encodedField);
            Collections.sort(staticFieldList);
            return Collections.binarySearch(staticFieldList, encodedField);
        } else {
            instanceFieldList.add(encodedField);
            Collections.sort(instanceFieldList);
            return Collections.binarySearch(instanceFieldList, encodedField);
        }
    }

    public EncodedField getStaticFieldAtIndex(int i)
    {
        return staticFieldList.get(i);
    }

    private static abstract class EncodedMember<T extends EncodedMember<T>> extends CompositeField<T> implements Field<T>, Comparable<T> 
    {
        protected abstract void setPreviousMember(T previousMember);
    }

    private static abstract class EncodedMemberList<T extends EncodedMember<T>>  implements Field<EncodedMemberList<T>> {
        private final ArrayList<T> list;

        public EncodedMemberList(ArrayList<T> list) {
            this.list = list;
        }

        public void writeTo(Output out) {
            for (T field: list) {
                field.writeTo(out);
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
        private final Field[] fields;

        private final IndexedItemReference<FieldIdItem> field;
        private final Leb128DeltaField fieldIndexField;
        private final Leb128Field accessFlags;

        public EncodedField(DexFile dexFile, final EncodedField previousField) {
            Leb128DeltaField previousIndexField = null;
            if (previousField != null) {
                previousIndexField = previousField.fieldIndexField;
            }


            fields = new Field[] {
                    field = new IndexedItemReference<FieldIdItem>(dexFile.FieldIdsSection,
                            fieldIndexField = new Leb128DeltaField(previousIndexField)),
                    accessFlags = new Leb128Field()
            };
        }

        public EncodedField(DexFile dexFile, FieldIdItem field, int accessFlags) {
            fields = new Field[] {
                    this.field = new IndexedItemReference<FieldIdItem>(dexFile, field,
                            fieldIndexField = new Leb128DeltaField(null)),
                    this.accessFlags = new Leb128Field(accessFlags)                    
            };
        }

        protected void setPreviousMember(EncodedField previousField) {
            if (previousField != null) {
                fieldIndexField.setPreviousField(previousField.fieldIndexField);
            } else {
                fieldIndexField.setPreviousField(null);
            }
        }

        protected Field[] getFields() {
            return fields;
        }

        public int compareTo(EncodedField other)
        {
            return field.getReference().compareTo(other.field.getReference());
        }

        public boolean isStatic() {
            return (accessFlags.getCachedValue() & AccessFlags.STATIC) != 0;
        }

        public FieldIdItem getField() {
            return field.getReference();
        }
    }

    public static class EncodedMethod extends EncodedMember<EncodedMethod> {
        private final Field[] fields;

        private final IndexedItemReference<MethodIdItem> method;
        private final Leb128DeltaField methodIndexField;
        private final Leb128Field accessFlags;
        private final OffsettedItemReference<CodeItem> codeItem;

        public EncodedMethod(DexFile dexFile, final EncodedMethod previousMethod) {
            Leb128DeltaField previousIndexField = null;
            if (previousMethod != null) {
                previousIndexField = previousMethod.methodIndexField;
            }

            fields = new Field[] {
                    method = new IndexedItemReference<MethodIdItem>(dexFile.MethodIdsSection,
                            methodIndexField = new Leb128DeltaField(previousIndexField)),
                    accessFlags = new Leb128Field(),
                    codeItem = new OffsettedItemReference<CodeItem>(dexFile.CodeItemsSection, new Leb128Field())
            };
        }

        public EncodedMethod(DexFile dexFile, MethodIdItem methodIdItem, int accessFlags, CodeItem codeItem) {
            fields = new Field[] {
                    this.method = new IndexedItemReference<MethodIdItem>(dexFile, methodIdItem,
                            methodIndexField = new Leb128DeltaField(null)),
                    this.accessFlags = new Leb128Field(accessFlags),
                    this.codeItem = new OffsettedItemReference<CodeItem>(dexFile, codeItem, new Leb128Field())
            };
        }

        protected void setPreviousMember(EncodedMethod previousMethod) {
            if (previousMethod != null) {
                methodIndexField.setPreviousField(previousMethod.methodIndexField);
            } else {
                methodIndexField.setPreviousField(null);
            }
        }

        protected Field[] getFields() {
            return fields;
        }

        public int compareTo(EncodedMethod other)
        {
            return method.getReference().compareTo(other.method.getReference());
        }

        public boolean isDirect() {
            return ((accessFlags.getCachedValue() & (AccessFlags.STATIC | AccessFlags.PRIVATE | AccessFlags.CONSTRUCTOR)) != 0);
        }
    }


    /**
     * An Leb128 integer that encodes its value as the difference between
     * itself and the previous Leb128DeltaField in the list. The first
     * item encodes the value as per normal
     */
    protected static class Leb128DeltaField extends Leb128Field {
        private Leb128DeltaField previousField;

        public Leb128DeltaField(Leb128DeltaField previousField) {
            this.previousField = previousField;
        }

        public void setPreviousField(Leb128DeltaField previousField) {
            this.previousField = previousField;
        }

        public int getCachedValue() {
            if (previousField != null) {
                return previousField.getCachedValue() + super.getCachedValue();
            } else {
                return super.getCachedValue();
            }
        }

        public void cacheValue(int value) {
            if (previousField != null) {
                super.cacheValue(value - previousField.getCachedValue());
            } else {
                super.cacheValue(value);
            }
        }
    }

    protected int getAlignment() {
        return 1;
    }

    protected Field[] getFields() {
        return fields;
    }

    public ItemType getItemType() {
        return ItemType.TYPE_CLASS_DATA_ITEM;
    }
}
