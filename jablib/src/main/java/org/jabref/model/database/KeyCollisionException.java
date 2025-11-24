package org.jabref.model.database;

public class KeyCollisionException extends RuntimeException {

    private String id;

    public KeyCollisionException() {
        super();
        this.id = null;
    }

    public KeyCollisionException(String msg, String id) {
        super(msg);
        this.id = id;
    }

    public KeyCollisionException(String msg, Throwable exception) {
        super(msg, exception);
        this.id = null;
    }

    public KeyCollisionException(Throwable exception) {
        super(exception);
        this.id = null;
    }

    public String getId() {
        return id;
    }
}
