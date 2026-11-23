package org.jabref.logic.openoffice.uno;

/// Checked failure while preparing or applying a Writer document operation.
public class WriterOperationException extends Exception {

    public WriterOperationException(String message) {
        super(message);
    }
}
