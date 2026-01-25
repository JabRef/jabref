package org.jabref.model.database;

public class KeyCollisionException extends RuntimeException {

    private String id;

    public KeyCollisionException() {
        super();
        this.id = "";
    }

    public KeyCollisionException(String msg, String id) {
        super(msg);
        this.id = id;
    }

    public KeyCollisionException(String msg, Throwable exception) {
        super(msg, exception);
        this.id = "";
    }

    public KeyCollisionException(Throwable exception) {
        super(exception);
        this.id = "";
    }

    public String getId() {
        return id;
    }
}
