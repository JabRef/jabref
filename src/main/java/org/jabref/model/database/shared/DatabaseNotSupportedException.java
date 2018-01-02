package org.jabref.model.database.shared;

/**
 * This exception is thrown in case that the SQL database structure is not compatible with the current shared database support mechanisms.
 */
public class DatabaseNotSupportedException extends Exception {

    public DatabaseNotSupportedException() {
        super("The structure of the SQL database is not supported.");
    }
}
