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
 * INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.dexlib.Util;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Exception which carries around structured context.
 */
public class ExceptionWithContext
        extends RuntimeException {
    /** non-null; human-oriented context of the exception */
    private StringBuffer context;

    /**
     * Augments the given exception with the given context, and return the
     * result. The result is either the given exception if it was an
     * {@link ExceptionWithContext}, or a newly-constructed exception if it
     * was not.
     *
     * @param ex non-null; the exception to augment
     * @param str non-null; context to add
     * @return non-null; an appropriate instance
     */
    public static ExceptionWithContext withContext(Throwable ex, String str) {
        ExceptionWithContext ewc;

        if (ex instanceof ExceptionWithContext) {
            ewc = (ExceptionWithContext) ex;
        } else {
            ewc = new ExceptionWithContext(ex);
        }

        ewc.addContext(str);
        return ewc;
    }

    /**
     * Constructs an instance.
     *
     * @param message human-oriented message
     */
    public ExceptionWithContext(String message) {
        this(message, null);
    }

    /**
     * Constructs an instance.
     *
     * @param cause null-ok; exception that caused this one
     */
    public ExceptionWithContext(Throwable cause) {
        this(null, cause);
    }

    /**
     * Constructs an instance.
     *
     * @param message human-oriented message
     * @param cause null-ok; exception that caused this one
     */
    public ExceptionWithContext(String message, Throwable cause) {
        super((message != null) ? message :
              (cause != null) ? cause.getMessage() : null,
              cause);

        if (cause instanceof ExceptionWithContext) {
            String ctx = ((ExceptionWithContext) cause).context.toString();
            context = new StringBuffer(ctx.length() + 200);
            context.append(ctx);
        } else {
            context = new StringBuffer(200);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void printStackTrace(PrintStream out) {
        super.printStackTrace(out);
        out.println(context);
    }

    /** {@inheritDoc} */
    @Override
    public void printStackTrace(PrintWriter out) {
        super.printStackTrace(out);
        out.println(context);
    }

    /**
     * Adds a line of context to this instance.
     *
     * @param str non-null; new context
     */
    public void addContext(String str) {
        if (str == null) {
            throw new NullPointerException("str == null");
        }

        context.append(str);
        if (!str.endsWith("\n")) {
            context.append('\n');
        }
    }

    /**
     * Gets the context.
     *
     * @return non-null; the context
     */
    public String getContext() {
        return context.toString();
    }

    /**
     * Prints the message and context.
     *
     * @param out non-null; where to print to
     */
    public void printContext(PrintStream out) {
        out.println(getMessage());
        out.print(context);
    }

    /**
     * Prints the message and context.
     *
     * @param out non-null; where to print to
     */
    public void printContext(PrintWriter out) {
        out.println(getMessage());
        out.print(context);
    }
}