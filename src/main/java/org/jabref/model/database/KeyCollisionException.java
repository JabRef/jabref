package org.jabref.model.database;

public class KeyCollisionException extends RuntimeException {

    private String id;

    public KeyCollisionException() {
        super();
    }

    public KeyCollisionException(String msg, String id) {
        super(msg);
        this.id = id;
    }

    public KeyCollisionException(String msg, Throwable exception) {
        super(msg, exception);
    }

    public KeyCollisionException(Throwable exception) {
        super(exception);
    }

    public String getId() {
        return id;
    }
}
