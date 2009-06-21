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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MapItem extends IndexedItem<MapItem> {
    private ArrayList<MapField> mapEntries = new ArrayList<MapField>();

    public MapItem(final DexFile dexFile, int index) {
        super(index);

        fields = new Field[] {
                new ListSizeField(mapEntries, new IntegerField("size")),
                new FieldListField<MapField>(mapEntries, "map_entry") {
                    protected MapField make() {
                        return new MapField(dexFile);
                    }
                }
        };
    }

    public int place(int index, int offset) {

        //we have to check if there are any empty sections before we place this item,
        //because we need to remove the empty sections, which will change the size of
        //this item
        for (int i=0; i<mapEntries.size(); i++) {
            MapField mapField = mapEntries.get(i);
            if (mapField.getSection().size() == 0) {
                mapEntries.remove(i--);
            }
        }
        
        offset = super.place(index, offset);

        //make sure the map items are in the same order as the corresponding sections
        Collections.sort(mapEntries, new Comparator<MapField>() {
            public int compare(MapField o1, MapField o2) {
                return ((Integer)o1.getSection().getOffset()).compareTo(o2.getSection().getOffset());
            }
        });

        return offset;
    }

    public static MapItem makeBlankMapItem(DexFile dexFile) {
        MapItem mapItem = new MapItem(dexFile, 0);

        mapItem.mapEntries.add(new MapField(dexFile, (short)ItemType.TYPE_HEADER_ITEM.getMapValue()));
        mapItem.mapEntries.add(new MapField(dexFile, (short)ItemType.TYPE_STRING_ID_ITEM.getMapValue()));
        mapItem.mapEntries.add(new MapField(dexFile, (short)ItemType.TYPE_TYPE_ID_ITEM.getMapValue()));
        mapItem.mapEntries.add(new MapField(dexFile, (short)ItemType.TYPE_PROTO_ID_ITEM.getMapValue()));
        mapItem.mapEntries.add(new MapField(dexFile, (short)ItemType.TYPE_FIELD_ID_ITEM.getMapValue()));
        mapItem.mapEntries.add(new MapField(dexFile, (short)ItemType.TYPE_METHOD_ID_ITEM.getMapValue()));
        mapItem.mapEntries.add(new MapField(dexFile, (short)ItemType.TYPE_CLASS_DEF_ITEM.getMapValue()));
        mapItem.mapEntries.add(new MapField(dexFile, (short)ItemType.TYPE_STRING_DATA_ITEM.getMapValue()));
        mapItem.mapEntries.add(new MapField(dexFile, (short)ItemType.TYPE_ENCODED_ARRAY_ITEM.getMapValue()));
        mapItem.mapEntries.add(new MapField(dexFile, (short)ItemType.TYPE_ANNOTATION_ITEM.getMapValue()));
        mapItem.mapEntries.add(new MapField(dexFile, (short)ItemType.TYPE_ANNOTATION_SET_ITEM.getMapValue()));
        mapItem.mapEntries.add(new MapField(dexFile, (short)ItemType.TYPE_ANNOTATION_SET_REF_LIST.getMapValue()));
        mapItem.mapEntries.add(new MapField(dexFile, (short)ItemType.TYPE_ANNOTATIONS_DIRECTORY_ITEM.getMapValue()));
        mapItem.mapEntries.add(new MapField(dexFile, (short)ItemType.TYPE_TYPE_LIST.getMapValue()));
        mapItem.mapEntries.add(new MapField(dexFile, (short)ItemType.TYPE_DEBUG_INFO_ITEM.getMapValue()));
        mapItem.mapEntries.add(new MapField(dexFile, (short)ItemType.TYPE_CODE_ITEM.getMapValue()));
        mapItem.mapEntries.add(new MapField(dexFile, (short)ItemType.TYPE_CLASS_DATA_ITEM.getMapValue()));
        mapItem.mapEntries.add(new MapField(dexFile, (short)ItemType.TYPE_MAP_LIST.getMapValue()));


        return mapItem;
    }

    public MapField[] getMapEntries() {
        return mapEntries.toArray(new MapField[1]);
    }

    protected int getAlignment() {
        return 4;
    }

    public ItemType getItemType() {
        return ItemType.TYPE_MAP_LIST;
    }

    public void copyTo(DexFile dexFile, MapItem copy) {
        //nothing to do
    }

    public int hashCode() {
        return 1;
    }

    public boolean equals(Object o) {
        return getClass() == o.getClass();
    }

    public String getConciseIdentity() {
        return "map_item";
    }

    public int compareTo(MapItem o) {
        //there is only 1 map item
        return 1;
    }
}
