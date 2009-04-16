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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MapItem extends IndexedItem<MapItem> {
    private final Field[] fields;
    private ArrayList<MapField> mapEntries = new ArrayList<MapField>();

    public MapItem(final DexFile dexFile, int index) {
        super(index);

        fields = new Field[] {
                new ListSizeField(mapEntries, new IntegerField()),
                new FieldListField<MapField>(mapEntries) {
                    protected MapField make() {
                        return new MapField(dexFile);
                    }
                }
        };
    }

    public int place(int index, int offset) {
        for (int i=0; i<mapEntries.size(); i++) {
            MapField mapField = mapEntries.get(i);
            mapField.place(offset);
            if (mapField.getSectionSize() == 0 /*&& mapField.getSectionItemType().getMapValue() > 0x06*/) {
                mapEntries.remove(i--);
            }
        }

        return super.place(index, offset);
    }

    public void writeTo(Output out) {
        Collections.sort(mapEntries, new Comparator<MapField>() {

            public int compare(MapField o1, MapField o2) {
                return ((Integer)o1.getSectionOffset()).compareTo(o2.getSectionOffset());
            }
        });

        super.writeTo(out);
    }

    public static MapItem makeBlankMapItem(DexFile dexFile) {
        MapItem mapItem = new MapItem(dexFile, 0);

        mapItem.mapEntries.add(new MapField(dexFile, (short)0x0000));
        mapItem.mapEntries.add(new MapField(dexFile, (short)0x0001));
        mapItem.mapEntries.add(new MapField(dexFile, (short)0x0002));
        mapItem.mapEntries.add(new MapField(dexFile, (short)0x0003));
        mapItem.mapEntries.add(new MapField(dexFile, (short)0x0004));
        mapItem.mapEntries.add(new MapField(dexFile, (short)0x0005));
        mapItem.mapEntries.add(new MapField(dexFile, (short)0x0006));
        mapItem.mapEntries.add(new MapField(dexFile, (short)0x1003));
        mapItem.mapEntries.add(new MapField(dexFile, (short)0x2001));
        mapItem.mapEntries.add(new MapField(dexFile, (short)0x2006));
        mapItem.mapEntries.add(new MapField(dexFile, (short)0x1001));
        mapItem.mapEntries.add(new MapField(dexFile, (short)0x2002));
        mapItem.mapEntries.add(new MapField(dexFile, (short)0x2003));
        mapItem.mapEntries.add(new MapField(dexFile, (short)0x2004));
        mapItem.mapEntries.add(new MapField(dexFile, (short)0x2005));
        mapItem.mapEntries.add(new MapField(dexFile, (short)0x2000));
        mapItem.mapEntries.add(new MapField(dexFile, (short)0x1000));


        return mapItem;
    }

    public MapField[] getMapEntries() {
        return mapEntries.toArray(new MapField[1]);
    }

    protected int getAlignment() {
        return 4;
    }

    protected Field[] getFields() {
        return fields;
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

    public int compareTo(MapItem o) {
        //there is only 1 map item
        return 1;
    }
}
