package org.jf.dexlib2.builder;

import org.jf.dexlib2.base.BaseExceptionHandler;
import org.jf.dexlib2.iface.ExceptionHandler;
import org.jf.dexlib2.iface.reference.TypeReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

class BuilderExceptionHandler {
    static ExceptionHandler newExceptionHandler(@Nullable final TypeReference exceptionType,
                                                @Nonnull final LabelMethodItem handler) {
        if (exceptionType == null) {
            return newExceptionHandler(handler);
        }
        return new BaseExceptionHandler() {
            @Nullable @Override public String getExceptionType() {
                return exceptionType.getType();
            }

            @Override public int getHandlerCodeAddress() {
                return handler.getCodeAddress();
            }

            @Nullable @Override public TypeReference getExceptionTypeReference() {
                return exceptionType;
            }
        };
    }

    static ExceptionHandler newExceptionHandler(@Nonnull final LabelMethodItem handler) {
        return new BaseExceptionHandler() {
            @Nullable @Override public String getExceptionType() {
                return null;
            }

            @Override public int getHandlerCodeAddress() {
                return handler.getCodeAddress();
            }
        };
    }

    static ExceptionHandler newExceptionHandler(@Nullable final String exceptionType,
                                                @Nonnull final LabelMethodItem handler) {
        if (exceptionType == null) {
            return newExceptionHandler(handler);
        }
        return new BaseExceptionHandler() {
            @Nullable @Override public String getExceptionType() {
                return exceptionType;
            }

            @Override public int getHandlerCodeAddress() {
                return handler.getCodeAddress();
            }
        };
    }
}
