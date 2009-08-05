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

import org.jf.dexlib.Util.Output;

/**
 * Interface for a binary output destination that may be augmented
 * with textual annotations.
 */
public interface AnnotatedOutput
        extends Output {
    /**
     * Get whether this instance will actually keep annotations.
     *
     * @return <code>true</code> iff annotations are being kept
     */
    public boolean annotates();

    /**
     * Get whether this instance is intended to keep verbose annotations.
     * Annotators may use the result of calling this method to inform their
     * annotation activity.
     *
     * @return <code>true</code> iff annotations are to be verbose
     */
    public boolean isVerbose();

    /**
     * Add an annotation for the subsequent output. Any previously
     * open annotation will be closed by this call, and the new
     * annotation marks all subsequent output until another annotation
     * call.
     *
     * @param msg non-null; the annotation message
     */
    public void annotate(String msg);

    /**
     * Add an annotation for a specified amount of subsequent
     * output. Any previously open annotation will be closed by this
     * call. If there is already pending annotation from one or more
     * previous calls to this method, the new call "consumes" output
     * after all the output covered by the previous calls.
     *
     * @param amt &gt;= 0; the amount of output for this annotation to
     * cover
     * @param msg non-null; the annotation message
     */
    public void annotate(int amt, String msg);

    /**
     * End the most recent annotation. Subsequent output will be unannotated,
     * until the next call to {@link #annotate}.
     */
    public void endAnnotation();

    /**
     * Get the maximum width of the annotated output. This is advisory:
     * Implementations of this interface are encouraged to deal with too-wide
     * output, but annotaters are encouraged to attempt to avoid exceeding
     * the indicated width.
     *
     * @return &gt;= 1; the maximum width
     */
    public int getAnnotationWidth();

    public void setIndentAmount(int indentAmount);
    public void indent();
    public void deindent();
}