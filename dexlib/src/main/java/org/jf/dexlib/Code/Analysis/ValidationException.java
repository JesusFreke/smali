package org.jf.dexlib.Code.Analysis;

import org.jf.dexlib.Util.ExceptionWithContext;

public class ValidationException extends ExceptionWithContext {
    public ValidationException(String errorMessage) {
        super(errorMessage);
    }
}
