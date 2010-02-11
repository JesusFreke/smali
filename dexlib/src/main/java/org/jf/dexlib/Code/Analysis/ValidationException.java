package org.jf.dexlib.Code.Analysis;

import org.jf.dexlib.Util.ExceptionWithContext;

public class ValidationException extends ExceptionWithContext {
    private int codeAddress;

    public ValidationException(int codeAddress, String errorMessage) {
        super(errorMessage);
        this.codeAddress = codeAddress;
    }

    public ValidationException(String errorMessage) {
        super(errorMessage);
    }

    public void setCodeAddress(int codeAddress) {
        this.codeAddress = codeAddress;
    }

    public int getCodeAddress() {
        return codeAddress;
    }
}
