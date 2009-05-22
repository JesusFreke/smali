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

package org.JesusFreke.dexlib.util;

import org.JesusFreke.dexlib.debug.*;
import org.JesusFreke.dexlib.DexFile;
import org.JesusFreke.dexlib.DebugInfoItem;
import org.JesusFreke.dexlib.StringIdItem;
import org.JesusFreke.dexlib.TypeIdItem;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is intended to provide an easy to use container to build up a method's debug info. You can easily add
 * an "event" at a specific address, where an event is something like a line number, start/end local, etc.
 * The events must be added such that the code addresses increase monotonically. This matches how a parser would
 * generally behave, and is intended to increase performance.
 */
public class DebugInfoBuilder
{
    //TODO: take a look at the debug bytecode generation logic in dx, and make sure that this does the same thing
    //(in the interest of being able to exactly reproduce a given dx-generated dex file) 

    private static final int LINE_BASE = -4;
    private static final int LINE_RANGE = 15;
    private static final int FIRST_SPECIAL = 0x0a;

    private int lineStart = 0;
    private ArrayList<String> parameterNames = new ArrayList<String>();
    private ArrayList<Event> events = new ArrayList<Event>();
    private int lastAddress = 0;

    private boolean hasData;

    private int currentAddress;
    private int currentLine;

    public DebugInfoBuilder() {
    }

    private void checkAddress(int address) {
        if (lastAddress > address) {
            throw new RuntimeException("Cannot add an event with an address before the address of the prior event");
        }
    }

    public void addParameterName(String parameterName) {
        hasData = true;

        parameterNames.add(parameterName);
    }

    public void addLine(int address, int line) {
        hasData = true;

        checkAddress(address);

        if (lineStart == 0) {
            lineStart = line;
        }

        events.add(new LineEvent(address, line));
    }

    public void addLocal(int address, int registerNumber, String localName, String localType) {
        hasData = true;

        checkAddress(address);

        events.add(new StartLocalEvent(address, registerNumber, localName, localType));
    }

    public int getParameterNameCount() {
        return parameterNames.size();
    }

    public DebugInfoItem encodeDebugInfo(DexFile dexFile) {
        if (!hasData) {
            return null;
        }
        
        ArrayList<DebugInstruction> debugInstructions = new ArrayList<DebugInstruction>();
        ArrayList<StringIdItem> parameterNameReferences = new ArrayList<StringIdItem>();

        if (lineStart == 0) {
            lineStart = 1;
        }

        currentLine = lineStart;

        for (Event event: events) {
            event.emit(dexFile, debugInstructions);
        }
        debugInstructions.add(new EndSequence());

        for (String parameterName: parameterNames) {
            if (parameterName == null) {
                parameterNameReferences.add(null);
            } else {
                parameterNameReferences.add(new StringIdItem(dexFile, parameterName));
            }
        }

        return new DebugInfoItem(dexFile, lineStart, parameterNameReferences, debugInstructions);
    }
    
    private interface Event
    {
        int getAddress();
        void emit(DexFile dexFile, List<DebugInstruction> debugInstructions);
    }

    private class LineEvent implements Event
    {
        private final int address;
        private final int line;

        public LineEvent(int address, int line) {
            this.address = address;
            this.line = line;
        }

        public int getAddress() {
            return address;
        }

        public void emit(DexFile dexFile, List<DebugInstruction> debugInstructions) {
            int lineDelta = line - currentLine;
            int addressDelta = address - currentAddress;

            if (lineDelta < -4 || lineDelta > 10) {
                debugInstructions.add(new AdvanceLine(lineDelta));
                lineDelta = 0;
            }
            if (lineDelta < 2 && addressDelta > 16 || lineDelta > 1 && addressDelta > 15) {
                debugInstructions.add(new AdvancePC(addressDelta));
                addressDelta = 0;
            }

            debugInstructions.add(new SpecialOpcode(calculateSpecialOpcode(lineDelta, addressDelta)));


            currentAddress = address;
            currentLine = line;
        }

        private byte calculateSpecialOpcode(int lineDelta, int addressDelta) {
            return (byte)(FIRST_SPECIAL + (addressDelta * LINE_RANGE) + (lineDelta - LINE_BASE));
        }
    }

    private class StartLocalEvent implements Event
    {
        private final int address;
        private final int registerNum;
        private final String localName;
        private final String localType;

        public StartLocalEvent(int address, int registerNum, String localName, String localType) {
            this.address = address;
            this.registerNum = registerNum;
            this.localName = localName;
            this.localType = localType;
        }

        public int getAddress()
        {
            return address;
        }

        public void emit(DexFile dexFile, List<DebugInstruction> debugInstructions)
        {
            int addressDelta = address-currentAddress;

            if (addressDelta > 0) {
                debugInstructions.add(new AdvancePC(addressDelta));
                currentAddress = address;
            }

            debugInstructions.add(new StartLocal(dexFile, registerNum, new StringIdItem(dexFile, localName),
                    new TypeIdItem(dexFile, localType)));
        }
    }
}
