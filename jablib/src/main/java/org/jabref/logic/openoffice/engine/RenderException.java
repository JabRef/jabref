package org.jabref.logic.openoffice.engine;

import java.util.Optional;

import org.jspecify.annotations.NullMarked;

/// A rendering failure that must be handled before any document text is changed.
@NullMarked
public class RenderException extends Exception {

    public RenderException(String message) {
        super(message);
    }

    public RenderException(String message, Throwable cause) {
        super(message, cause);
    }

    /// Localized corrective guidance that is safe to present to the user, unlike raw diagnostics.
    public Optional<String> localizedMessage() {
        return Optional.empty();
    }
}
