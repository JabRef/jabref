package org.jabref.model.database;

public class KeyCollisionException extends RuntimeException {
    public KeyCollisionException() {
        super();
    }

    public KeyCollisionException(String msg) {
        super(msg);
    }
}
