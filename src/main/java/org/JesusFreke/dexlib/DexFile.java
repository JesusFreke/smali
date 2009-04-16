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

import org.JesusFreke.dexlib.util.Input;
import org.JesusFreke.dexlib.util.ByteArrayInput;
import org.JesusFreke.dexlib.util.Output;
import org.JesusFreke.dexlib.util.FileUtils;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.zip.Adler32;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.DigestException;

public class DexFile
{
    private final HashMap<ItemType, Section> sectionsByType;
    private final IndexedSection[] indexedSections;
    private final OffsettedSection[] offsettedSections;
    private int fileSize;
    private int dataOffset;
    private int dataSize;

    private final DexFile dexFile = this;

    private DexFile() {
        sectionsByType = new HashMap<ItemType, Section>(18);

        sectionsByType.put(ItemType.TYPE_ANNOTATION_ITEM, AnnotationsSection);
        sectionsByType.put(ItemType.TYPE_ANNOTATION_SET_ITEM, AnnotationSetsSection);
        sectionsByType.put(ItemType.TYPE_ANNOTATION_SET_REF_LIST, AnnotationSetRefListsSection);
        sectionsByType.put(ItemType.TYPE_ANNOTATIONS_DIRECTORY_ITEM, AnnotationDirectoriesSection);
        sectionsByType.put(ItemType.TYPE_CLASS_DATA_ITEM, ClassDataSection);
        sectionsByType.put(ItemType.TYPE_CLASS_DEF_ITEM, ClassDefsSection);
        sectionsByType.put(ItemType.TYPE_CODE_ITEM, CodeItemsSection);
        sectionsByType.put(ItemType.TYPE_DEBUG_INFO_ITEM, DebugInfoItemsSection);
        sectionsByType.put(ItemType.TYPE_ENCODED_ARRAY_ITEM, EncodedArraysSection);
        sectionsByType.put(ItemType.TYPE_FIELD_ID_ITEM, FieldIdsSection);
        sectionsByType.put(ItemType.TYPE_HEADER_ITEM, HeaderItemSection);
        sectionsByType.put(ItemType.TYPE_MAP_LIST, MapSection);
        sectionsByType.put(ItemType.TYPE_METHOD_ID_ITEM, MethodIdsSection);
        sectionsByType.put(ItemType.TYPE_PROTO_ID_ITEM, ProtoIdsSection);
        sectionsByType.put(ItemType.TYPE_STRING_DATA_ITEM, StringDataSection);
        sectionsByType.put(ItemType.TYPE_STRING_ID_ITEM, StringIdsSection);
        sectionsByType.put(ItemType.TYPE_TYPE_ID_ITEM, TypeIdsSection);
        sectionsByType.put(ItemType.TYPE_TYPE_LIST, TypeListsSection);

        indexedSections = new IndexedSection[] {
                StringIdsSection,
                TypeIdsSection,
                ProtoIdsSection,
                FieldIdsSection,
                MethodIdsSection,
                ClassDefsSection
        };

        offsettedSections = new OffsettedSection[] {
                AnnotationSetsSection,
                CodeItemsSection,
                AnnotationDirectoriesSection,
                TypeListsSection,
                StringDataSection,
                DebugInfoItemsSection,
                AnnotationsSection,
                EncodedArraysSection,
                ClassDataSection,

                AnnotationSetRefListsSection
        };
    }

    public DexFile(File file) {
        this();
        Input in = new ByteArrayInput(FileUtils.readFile(file));

        HeaderItemSection.readFrom(1, in);
        HeaderItem headerItem = HeaderItemSection.items.get(0);

        in.setCursor(headerItem.getMapOffset());

        MapSection.readFrom(1, in);

        for (MapField mapField: MapSection.items.get(0).getMapEntries()) {
            Section section = sectionsByType.get(mapField.getSectionItemType());
            if (section != null) {
                in.setCursor(mapField.getSectionOffset());
                section.readFrom(mapField.getSectionSize(), in);
            }
        }
    }

    public static DexFile makeBlankDexFile() {
        DexFile dexFile = new DexFile();
        try
        {
            dexFile.HeaderItemSection.intern(dexFile, new HeaderItem(dexFile, 0));
        } catch (Exception ex) {
            throw new RuntimeException(ex);  
        }

        dexFile.MapSection.intern(dexFile, MapItem.makeBlankMapItem(dexFile));
        return dexFile;
    }


    public <T extends Item> Section<T> getSectionForItem(T item) {
        return sectionsByType.get(item.getItemType());
    }

    public Section getSectionForType(ItemType itemType) {
        return sectionsByType.get(itemType);
    }

    public int getFileSize() {
        return fileSize;
    }

    public int getDataOffset() {
        return dataOffset;
    }

    public int getDataSize() {
        return dataSize;
    }

    public void place() {
        int offset = 0;

        offset = HeaderItemSection.place(offset);
        for (IndexedSection indexedSection: indexedSections) {
            indexedSection.unplace();
            offset = indexedSection.place(offset);
        }

        dataOffset = offset;

        for (OffsettedSection offsettedSection: offsettedSections) {
            offsettedSection.unplace();

            offset = offsettedSection.place(offset);
        }

        offset = MapSection.place(offset);

        dataSize = offset - dataOffset;
        fileSize = offset;
    }

    public void writeTo(Output out) {
        HeaderItemSection.writeTo(out);
        for (IndexedSection indexedSection: indexedSections) {
            indexedSection.writeTo(out);
        }

        for (OffsettedSection offsettedSection: offsettedSections) {
            offsettedSection.writeTo(out);
        }

        MapSection.writeTo(out);
    }

    public ClassDefItem getClassByName(String className) {
        for (ClassDefItem classDefItem: ClassDefsSection.items) {
            if (classDefItem.getClassName().equals(className)) {
                return classDefItem;
            }
        }
        throw new RuntimeException("class not found");
    }

    public MethodIdItem[] getMethodsByClass(TypeIdItem classType) {
        ArrayList<MethodIdItem> methods = new ArrayList<MethodIdItem>();

        for (MethodIdItem methodIdItem: MethodIdsSection.items) {
            if (methodIdItem.getClassType() == classType) {
                methods.add(methodIdItem);
            }
        }

        return methods.toArray(new MethodIdItem[0]);
    }



    public final IndexedSection<HeaderItem> HeaderItemSection = new IndexedSection<HeaderItem>() {
        protected HeaderItem make(int index) {
            try {
                return new HeaderItem(dexFile, index);
            } catch (UnsupportedEncodingException ex) {
                //TODO: handle this
                throw new RuntimeException(ex);
            }
        }
    };

    public final IndexedSection<StringIdItem> StringIdsSection = new IndexedSection<StringIdItem>() {
        protected StringIdItem make(int index) {
            return new StringIdItem(dexFile, index);
        }
    };

    public final IndexedSection<TypeIdItem> TypeIdsSection = new IndexedSection<TypeIdItem>() {
        protected TypeIdItem make(int index) {
            return new TypeIdItem(dexFile, index);
        }
    };

    public final IndexedSection<ProtoIdItem> ProtoIdsSection = new IndexedSection<ProtoIdItem>() {
        protected ProtoIdItem make(int index) {
            return new ProtoIdItem(dexFile, index);
        }
    };

    public final IndexedSection<FieldIdItem> FieldIdsSection = new IndexedSection<FieldIdItem>() {
        protected FieldIdItem make(int index) {
            return new FieldIdItem(dexFile, index);
        }
    };

    public final IndexedSection<MethodIdItem> MethodIdsSection = new IndexedSection<MethodIdItem>() {
        protected MethodIdItem make(int index) {
            return new MethodIdItem(dexFile, index);
        }
    };

    public final IndexedSection<ClassDefItem> ClassDefsSection = new IndexedSection<ClassDefItem>() {
        protected ClassDefItem make(int index) {
            return new ClassDefItem(dexFile, index);
        }

        public int place(int offset) {
            int ret = ClassDefItem.placeClassDefItems(this, offset);

            this.offset = items.get(0).getOffset();
            return ret;
        }
    };

    public final IndexedSection<MapItem> MapSection = new IndexedSection<MapItem>() {
        protected MapItem make(int index) {
            return new MapItem(dexFile, index);
        }

        public MapItem intern(DexFile dexFile, MapItem item) {
            this.items.add(item);
            return item;
        }
    };
    
    public final OffsettedSection<TypeListItem> TypeListsSection = new OffsettedSection<TypeListItem>() {
        protected TypeListItem make(int offset) {
            return new TypeListItem(dexFile, offset);
        }
    };
    
    public final OffsettedSection<AnnotationSetRefList> AnnotationSetRefListsSection =
            new OffsettedSection<AnnotationSetRefList>() {
                protected AnnotationSetRefList make(int offset) {
                    return new AnnotationSetRefList(dexFile, offset);
                }
            };
    
    public final OffsettedSection<AnnotationSetItem> AnnotationSetsSection =
            new OffsettedSection<AnnotationSetItem>() {
                protected AnnotationSetItem make(int offset) {
                    return new AnnotationSetItem(dexFile, offset);
                }
            };

    public final OffsettedSection<ClassDataItem> ClassDataSection = new OffsettedSection<ClassDataItem>() {
        protected ClassDataItem make(int offset) {
            return new ClassDataItem(dexFile, offset);
        }
    };

    public final OffsettedSection<CodeItem> CodeItemsSection = new OffsettedSection<CodeItem>() {
        protected CodeItem make(int offset) {
            return new CodeItem(dexFile, offset);
        }
    };

    public final OffsettedSection<StringDataItem> StringDataSection = new OffsettedSection<StringDataItem>() {
        protected StringDataItem make(int offset) {
            return new StringDataItem(offset);
        }
    };

    public final OffsettedSection<DebugInfoItem> DebugInfoItemsSection = new OffsettedSection<DebugInfoItem>() {
        protected DebugInfoItem make(int offset) {
            return new DebugInfoItem(dexFile, offset); 
        }
    };

    public final OffsettedSection<AnnotationItem> AnnotationsSection = new OffsettedSection<AnnotationItem>() {
        protected AnnotationItem make(int offset) {
            return new AnnotationItem(dexFile, offset);
        }
    };

    public final OffsettedSection<EncodedArrayItem> EncodedArraysSection = new OffsettedSection<EncodedArrayItem>() {
        protected EncodedArrayItem make(int offset) {
            return new EncodedArrayItem(dexFile, offset);
        }
    };
    
    public final OffsettedSection<AnnotationDirectoryItem> AnnotationDirectoriesSection =
            new OffsettedSection<AnnotationDirectoryItem>() {
                protected AnnotationDirectoryItem make(int offset) {
                    return new AnnotationDirectoryItem(dexFile, offset);
                }
            };


    /**
     * Calculates the signature for the <code>.dex</code> file in the
     * given array, and modify the array to contain it.
     *
     * @param bytes non-null; the bytes of the file
     */
    public static void calcSignature(byte[] bytes) {
        MessageDigest md;

        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }

        md.update(bytes, 32, bytes.length - 32);

        try {
            int amt = md.digest(bytes, 12, 20);
            if (amt != 20) {
                throw new RuntimeException("unexpected digest write: " + amt +
                                           " bytes");
            }
        } catch (DigestException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Calculates the checksum for the <code>.dex</code> file in the
     * given array, and modify the array to contain it.
     *
     * @param bytes non-null; the bytes of the file
     */
    public static void calcChecksum(byte[] bytes) {
        Adler32 a32 = new Adler32();

        a32.update(bytes, 12, bytes.length - 12);

        int sum = (int) a32.getValue();

        bytes[8]  = (byte) sum;
        bytes[9]  = (byte) (sum >> 8);
        bytes[10] = (byte) (sum >> 16);
        bytes[11] = (byte) (sum >> 24);
    }
}
