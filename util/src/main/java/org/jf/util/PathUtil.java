/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver
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

package org.jf.util;

import com.google.common.collect.Lists;

import java.io.*;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.List;

public class PathUtil {
    private PathUtil() {
    }

    public static File getRelativeFile(File baseFile, File fileToRelativize) throws IOException {
        if (baseFile.isFile()) {
            baseFile = baseFile.getParentFile();
        }

        return new File(getRelativeFileInternal(baseFile.getCanonicalFile(), fileToRelativize.getCanonicalFile()));
    }

    static String getRelativeFileInternal(File canonicalBaseFile, File canonicalFileToRelativize) {
        List<String> basePath = getPathComponents(canonicalBaseFile);
        List<String> pathToRelativize = getPathComponents(canonicalFileToRelativize);

        //if the roots aren't the same (i.e. different drives on a windows machine), we can't construct a relative
        //path from one to the other, so just return the canonical file
        if (!basePath.get(0).equals(pathToRelativize.get(0))) {
            return canonicalFileToRelativize.getPath();
        }

        int commonDirs;
        StringBuilder sb = new StringBuilder();

        for (commonDirs=1; commonDirs<basePath.size() && commonDirs<pathToRelativize.size(); commonDirs++) {
            if (!basePath.get(commonDirs).equals(pathToRelativize.get(commonDirs))) {
                break;
            }
        }

        boolean first = true;
        for (int i=commonDirs; i<basePath.size(); i++) {
            if (!first) {
                sb.append(File.separatorChar);
            } else {
                first = false;
            }

            sb.append("..");
        }

        first = true;
        for (int i=commonDirs; i<pathToRelativize.size(); i++) {
            if (first) {
                if (sb.length() != 0) {
                    sb.append(File.separatorChar);
                }
                first = false;
            } else {
                sb.append(File.separatorChar);
            }
            
            sb.append(pathToRelativize.get(i));
        }

        if (sb.length() == 0) {
            return ".";
        }

        return sb.toString();
    }

    private static List<String> getPathComponents(File file) {
        ArrayList<String> path = new ArrayList<String>();

        while (file != null) {
            File parentFile = file.getParentFile();

            if (parentFile == null) {
                path.add(file.getPath());
            } else {
                path.add(file.getName());
            }

            file = parentFile;
        }

        return Lists.reverse(path);
    }

    public static boolean testCaseSensitivity(File path) throws IOException {
        int num = 1;
        File f, f2;
        do {
            f = new File(path, "test." + num);
            f2 = new File(path, "TEST." + num++);
        } while(f.exists() || f2.exists());

        try {
            try {
                FileWriter writer = new FileWriter(f);
                writer.write("test");
                writer.flush();
                writer.close();
            } catch (IOException ex) {
                try {f.delete();} catch (Exception ex2) {}
                throw ex;
            }

            if (f2.exists()) {
                return false;
            }

            if (f2.createNewFile()) {
                return true;
            }

            //the above 2 tests should catch almost all cases. But maybe there was a failure while creating f2
            //that isn't related to case sensitivity. Let's see if we can open the file we just created using
            //f2
            try {
                CharBuffer buf = CharBuffer.allocate(32);
                FileReader reader = new FileReader(f2);

                while (reader.read(buf) != -1 && buf.length() < 4);
                if (buf.length() == 4 && buf.toString().equals("test")) {
                    return false;
                } else {
                    //we probably shouldn't get here. If the filesystem was case-sensetive, creating a new
                    //FileReader should have thrown a FileNotFoundException. Otherwise, we should have opened
                    //the file and read in the string "test". It's remotely possible that someone else modified
                    //the file after we created it. Let's be safe and return false here as well
                    assert(false);
                    return false;
                }
            } catch (FileNotFoundException ex) {
                return true;
            }
        } finally {
            try { f.delete(); } catch (Exception ex) {}
            try { f2.delete(); } catch (Exception ex) {}
        }
    }
}
