package org.jabref.model.database;

import org.jspecify.annotations.Nullable;

public class KeyCollisionException extends RuntimeException {

    private @Nullable String id;

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

    public @Nullable String getId() {
        return id;
    }
}
