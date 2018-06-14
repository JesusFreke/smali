/*
 * Copyright 2016, Google Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 * Neither the name of Google Inc. nor the names of its
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

package org.jf.dexlib2;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jf.dexlib2.DexFileFactory.DexEntryFinder;
import org.jf.dexlib2.DexFileFactory.DexFileNotFoundException;
import org.jf.dexlib2.DexFileFactory.MultipleMatchingDexEntriesException;
import org.jf.dexlib2.DexFileFactory.UnsupportedFileTypeException;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.DexBackedDexFile.NotADexFile;
import org.jf.dexlib2.iface.MultiDexContainer;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static org.mockito.Mockito.mock;

public class DexEntryFinderTest {

    @Test
    public void testNormalStuff() throws Exception {
        Map<String, DexBackedDexFile> entries = Maps.newHashMap();
        DexBackedDexFile dexFile1 = mock(DexBackedDexFile.class);
        entries.put("/system/framework/framework.jar", dexFile1);
        DexBackedDexFile dexFile2 = mock(DexBackedDexFile.class);
        entries.put("/system/framework/framework.jar:classes2.dex", dexFile2);
        DexEntryFinder testFinder = new DexEntryFinder("blah.oat", new TestMultiDexContainer(entries));

        Assert.assertEquals(dexFile1, testFinder.findEntry("/system/framework/framework.jar", true));

        assertEntryNotFound(testFinder, "system/framework/framework.jar", true);
        assertEntryNotFound(testFinder, "/framework/framework.jar", true);
        assertEntryNotFound(testFinder, "framework/framework.jar", true);
        assertEntryNotFound(testFinder, "/framework.jar", true);
        assertEntryNotFound(testFinder, "framework.jar", true);

        Assert.assertEquals(dexFile1, testFinder.findEntry("system/framework/framework.jar", false));
        Assert.assertEquals(dexFile1, testFinder.findEntry("/framework/framework.jar", false));
        Assert.assertEquals(dexFile1, testFinder.findEntry("framework/framework.jar", false));
        Assert.assertEquals(dexFile1, testFinder.findEntry("/framework.jar", false));
        Assert.assertEquals(dexFile1, testFinder.findEntry("framework.jar", false));

        assertEntryNotFound(testFinder, "ystem/framework/framework.jar", false);
        assertEntryNotFound(testFinder, "ssystem/framework/framework.jar", false);
        assertEntryNotFound(testFinder, "ramework/framework.jar", false);
        assertEntryNotFound(testFinder, "ramework.jar", false);
        assertEntryNotFound(testFinder, "framework", false);

        Assert.assertEquals(dexFile2, testFinder.findEntry("/system/framework/framework.jar:classes2.dex", true));

        assertEntryNotFound(testFinder, "system/framework/framework.jar:classes2.dex", true);
        assertEntryNotFound(testFinder, "framework.jar:classes2.dex", true);
        assertEntryNotFound(testFinder, "classes2.dex", true);

        Assert.assertEquals(dexFile2, testFinder.findEntry("system/framework/framework.jar:classes2.dex", false));
        Assert.assertEquals(dexFile2, testFinder.findEntry("/framework/framework.jar:classes2.dex", false));
        Assert.assertEquals(dexFile2, testFinder.findEntry("framework/framework.jar:classes2.dex", false));
        Assert.assertEquals(dexFile2, testFinder.findEntry("/framework.jar:classes2.dex", false));
        Assert.assertEquals(dexFile2, testFinder.findEntry("framework.jar:classes2.dex", false));
        Assert.assertEquals(dexFile2, testFinder.findEntry(":classes2.dex", false));
        Assert.assertEquals(dexFile2, testFinder.findEntry("classes2.dex", false));

        assertEntryNotFound(testFinder, "ystem/framework/framework.jar:classes2.dex", false);
        assertEntryNotFound(testFinder, "ramework.jar:classes2.dex", false);
        assertEntryNotFound(testFinder, "lasses2.dex", false);
        assertEntryNotFound(testFinder, "classes2", false);
    }

    @Test
    public void testSimilarEntries() throws Exception {
        Map<String, DexBackedDexFile> entries = Maps.newHashMap();
        DexBackedDexFile dexFile1 = mock(DexBackedDexFile.class);
        entries.put("/system/framework/framework.jar", dexFile1);
        DexBackedDexFile dexFile2 = mock(DexBackedDexFile.class);
        entries.put("system/framework/framework.jar", dexFile2);
        DexEntryFinder testFinder = new DexEntryFinder("blah.oat", new TestMultiDexContainer(entries));

        Assert.assertEquals(dexFile1, testFinder.findEntry("/system/framework/framework.jar", true));
        Assert.assertEquals(dexFile2, testFinder.findEntry("system/framework/framework.jar", true));

        assertMultipleMatchingEntries(testFinder, "/system/framework/framework.jar");
        assertMultipleMatchingEntries(testFinder, "system/framework/framework.jar");

        assertMultipleMatchingEntries(testFinder, "/framework/framework.jar");
        assertMultipleMatchingEntries(testFinder, "framework/framework.jar");
        assertMultipleMatchingEntries(testFinder, "/framework.jar");
        assertMultipleMatchingEntries(testFinder, "framework.jar");
    }

    @Test
    public void testMatchingSuffix() throws Exception {
        Map<String, DexBackedDexFile> entries = Maps.newHashMap();
        DexBackedDexFile dexFile1 = mock(DexBackedDexFile.class);
        entries.put("/system/framework/framework.jar", dexFile1);
        DexBackedDexFile dexFile2 = mock(DexBackedDexFile.class);
        entries.put("/framework/framework.jar", dexFile2);
        DexEntryFinder testFinder = new DexEntryFinder("blah.oat", new TestMultiDexContainer(entries));

        Assert.assertEquals(dexFile1, testFinder.findEntry("/system/framework/framework.jar", true));
        Assert.assertEquals(dexFile2, testFinder.findEntry("/framework/framework.jar", true));

        Assert.assertEquals(dexFile2, testFinder.findEntry("/framework/framework.jar", false));
        Assert.assertEquals(dexFile2, testFinder.findEntry("framework/framework.jar", false));

        assertMultipleMatchingEntries(testFinder, "/framework.jar");
        assertMultipleMatchingEntries(testFinder, "framework.jar");
    }

    @Test
    public void testNonDexEntries() throws Exception {
        Map<String, DexBackedDexFile> entries = Maps.newHashMap();
        DexBackedDexFile dexFile1 = mock(DexBackedDexFile.class);
        entries.put("classes.dex", dexFile1);
        entries.put("/blah/classes.dex", null);
        DexEntryFinder testFinder = new DexEntryFinder("blah.oat", new TestMultiDexContainer(entries));

        Assert.assertEquals(dexFile1, testFinder.findEntry("classes.dex", true));
        Assert.assertEquals(dexFile1, testFinder.findEntry("classes.dex", false));

        assertUnsupportedFileType(testFinder, "/blah/classes.dex", true);
        assertDexFileNotFound(testFinder, "/blah/classes.dex", false);
    }

    private void assertEntryNotFound(DexEntryFinder finder, String entry, boolean exactMatch) throws IOException {
        try {
            finder.findEntry(entry, exactMatch);
            Assert.fail();
        } catch (DexFileNotFoundException ex) {
            // expected exception
        }
    }

    private void assertMultipleMatchingEntries(DexEntryFinder finder, String entry) throws IOException {
        try {
            finder.findEntry(entry, false);
            Assert.fail();
        } catch (MultipleMatchingDexEntriesException ex) {
            // expected exception
        }
    }

    private void assertUnsupportedFileType(DexEntryFinder finder, String entry, boolean exactMatch) throws IOException {
        try {
            finder.findEntry(entry, exactMatch);
            Assert.fail();
        } catch (UnsupportedFileTypeException ex) {
            // expected exception
        }
    }

    private void assertDexFileNotFound(DexEntryFinder finder, String entry, boolean exactMatch) throws IOException {
        try {
            finder.findEntry(entry, exactMatch);
            Assert.fail();
        } catch (DexFileNotFoundException ex) {
            // expected exception
        }
    }

    public static class TestMultiDexContainer implements MultiDexContainer<DexBackedDexFile> {
        @Nonnull private final Map<String, DexBackedDexFile> entries;

        public TestMultiDexContainer(@Nonnull Map<String, DexBackedDexFile> entries) {
            this.entries = entries;
        }

        @Nonnull @Override public List<String> getDexEntryNames() throws IOException {
            List<String> entryNames = Lists.newArrayList();

            for (Entry<String, DexBackedDexFile> entry: entries.entrySet()) {
                if (entry.getValue() != null) {
                    entryNames.add(entry.getKey());
                }
            }

            return entryNames;
        }

        @Nullable @Override public DexBackedDexFile getEntry(@Nonnull String entryName) throws IOException {
            if (entries.containsKey(entryName)) {
                DexBackedDexFile entry = entries.get(entryName);
                if (entry == null) {
                    throw new NotADexFile();
                }
                return entry;
            }
            return null;
        }
    }
}
