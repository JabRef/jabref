package org.jabref.model.database;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class KeyCollisionException extends RuntimeException {

    private final String id;

    public KeyCollisionException(String msg, String id) {
        super(msg);
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
