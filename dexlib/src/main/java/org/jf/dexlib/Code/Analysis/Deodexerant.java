/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver (JesusFreke)
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

package org.jf.dexlib.Code.Analysis;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

/**
 * This class handles communication with the deodexerant helper binary,
 * as well as caching the results of any deodexerant lookups
 */
public class Deodexerant {
    private final String host;
    private final int port;

    private Socket socket = null;
    private PrintWriter out = null;
    private BufferedReader in = null;

    public Deodexerant(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String[] getInlineMethods() {
        return sendMultilineCommand("I");
    }

    public String[] getVirtualMethods(String classType) {
        return sendMultilineCommand(String.format("V %s", classType));
    }

    public String[] getInstanceFields(String classType) {
        return sendMultilineCommand(String.format("F %s", classType));
    }


    private String sendCommand(String cmd) {
        try {
            connectIfNeeded();

            out.println(cmd);
            out.flush();
            String response = in.readLine();
            if (response.startsWith("err")) {
                String error = response.substring(5);
                throw new RuntimeException(error);
            }
            return response;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    //The command is still just a single line, but we're expecting a multi-line
    //response. The repsonse is considered finished when a line starting with "err"
    //or with "done" is encountered
    private String[] sendMultilineCommand(String cmd) {
        try {
            connectIfNeeded();

            out.println(cmd);
            out.flush();

            ArrayList<String> responseLines = new ArrayList<String>();
            String response = in.readLine();
            if (response == null) {
                throw new RuntimeException("Error talking to deodexerant");
            }
            while (!response.startsWith("done"))
            {
                if (response.startsWith("err")) {
                    throw new RuntimeException(response.substring(5));
                }

                int pos = response.indexOf(':') + 1;

                responseLines.add(response.substring(pos+1));
                response = in.readLine();
            }

            String[] lines = new String[responseLines.size()];

            for (int i=0; i<lines.length; i++) {
                lines[i] = responseLines.get(i);
            }

            return lines;

        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void connectIfNeeded() {
        try {
            if (socket != null) {
                return;
            }

            socket = new Socket(host, port);

            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
