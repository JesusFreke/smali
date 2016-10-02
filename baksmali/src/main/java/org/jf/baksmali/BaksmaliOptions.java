/*
 * Copyright 2013, Google Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.baksmali;

import org.jf.dexlib2.analysis.ClassPath;
import org.jf.dexlib2.analysis.InlineMethodResolver;
import org.jf.dexlib2.util.SyntheticAccessorResolver;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class BaksmaliOptions {
    public int apiLevel = 15;

    public boolean parameterRegisters = true;
    public boolean localsDirective = false;
    public boolean sequentialLabels = false;
    public boolean debugInfo = true;
    public boolean codeOffsets = false;
    public boolean accessorComments = true;
    public boolean allowOdex = false;
    public boolean deodex = false;
    public boolean implicitReferences = false;
    public boolean normalizeVirtualMethods = false;

    // register info values
    public static final int ALL = 1;
    public static final int ALLPRE = 2;
    public static final int ALLPOST = 4;
    public static final int ARGS = 8;
    public static final int DEST = 16;
    public static final int MERGE = 32;
    public static final int FULLMERGE = 64;

    public int registerInfo = 0;

    public Map<Integer,String> resourceIds = new HashMap<Integer,String>();
    public InlineMethodResolver inlineResolver = null;
    public ClassPath classPath = null;
    public SyntheticAccessorResolver syntheticAccessorResolver = null;

    /**
     * Load the resource ids from a set of public.xml files.
     *
     * @param resourceFiles A map of resource prefixes -> public.xml files
     */
    public void loadResourceIds(Map<String, File> resourceFiles) throws SAXException, IOException {
        for (Map.Entry<String, File> entry: resourceFiles.entrySet()) {
            try {
                SAXParser saxp = SAXParserFactory.newInstance().newSAXParser();
                final String prefix = entry.getKey();
                saxp.parse(entry.getValue(), new DefaultHandler() {
                    @Override
                    public void startElement(String uri, String localName, String qName,
                                             Attributes attr) throws SAXException {
                        if (qName.equals("public")) {
                            String resourceType = attr.getValue("type");
                            String resourceName = attr.getValue("name").replace('.', '_');
                            Integer resourceId = Integer.decode(attr.getValue("id"));
                            String qualifiedResourceName =
                                    String.format("%s.%s.%s", prefix, resourceType, resourceName);
                            resourceIds.put(resourceId, qualifiedResourceName);
                        }
                    }
                });
            } catch (ParserConfigurationException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
