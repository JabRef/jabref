package org.jabref.logic.openoffice;

public class NoDocumentFoundException extends Exception {

    public NoDocumentFoundException(String message) {
        super(message);
    }

    public NoDocumentFoundException() {
        super("No Writer documents found");
    }
}
