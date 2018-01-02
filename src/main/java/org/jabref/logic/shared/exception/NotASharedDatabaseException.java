package org.jabref.logic.shared.exception;


/**
 * This exception is thrown when a shared database is required, but it actually isn't one.
 */
public class NotASharedDatabaseException extends Exception {

    public NotASharedDatabaseException() {
        super("Required database is not shared.");
    }
}
