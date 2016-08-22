package net.sf.jabref.shared.exception;

import net.sf.jabref.shared.DBMSProcessor;

/**
 * This exception is thrown in case that the shared database is not compatible with the current shared database support mechanisms.
 * See JavaDoc of the field <code>VERSION</code> in {@link DBMSProcessor}.
 */
public class DatabaseNotSupportedException extends Exception {

    public DatabaseNotSupportedException() {
        super("The structure of the shared database is not longer supported.");
    }
}
