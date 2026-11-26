package org.jabref.logic.openoffice.uno;

/// A Writer document or one of its required UNO objects could not be accessed.
public class WriterDocumentAccessException extends Exception {

    public WriterDocumentAccessException(String message) {
        super(message);
    }

    public WriterDocumentAccessException() {
        super("Not connected to a document");
    }
}
