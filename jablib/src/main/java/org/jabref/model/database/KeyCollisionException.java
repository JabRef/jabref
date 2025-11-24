package org.jabref.model.database;

import java.util.Objects;

import org.jspecify.annotations.NonNull;

public class KeyCollisionException extends RuntimeException {

    private static final String UNKNOWN_ID = "<unknown>";

    private final @NonNull String id;

    public KeyCollisionException() {
        super();
        this.id = UNKNOWN_ID;
    }

    public KeyCollisionException(String msg, String id) {
        super(msg);
        this.id = Objects.requireNonNullElse(id, UNKNOWN_ID);
    }

    public KeyCollisionException(String msg, Throwable exception) {
        super(msg, exception);
        this.id = UNKNOWN_ID;
    }

    public KeyCollisionException(Throwable exception) {
        super(exception);
        this.id = UNKNOWN_ID;
    }

    public @NonNull String getId() {
        return id;
    }
}
