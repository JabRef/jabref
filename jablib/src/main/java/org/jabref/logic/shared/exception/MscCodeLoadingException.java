package org.jabref.logic.shared.exception;

public class MscCodeLoadingException extends Exception {
    public MscCodeLoadingException(String message) {
        super(message);
    }

    public MscCodeLoadingException(String message, Throwable cause) {
        super(message, cause);
    }
}
