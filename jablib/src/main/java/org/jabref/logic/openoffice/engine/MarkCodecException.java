package org.jabref.logic.openoffice.engine;

import org.jspecify.annotations.NullMarked;

/// Invalid canonical mark data or a name that is not in the canonical JabRef format.
@NullMarked
public final class MarkCodecException extends Exception {

    @NullMarked
    public enum Reason {
        EMPTY_KEYS,
        INVALID_KEY,
        INVALID_MARK_NAME,
        INVALID_UNIQUE_ID
    }

    private final Reason reason;

    public MarkCodecException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason reason() {
        return reason;
    }
}
