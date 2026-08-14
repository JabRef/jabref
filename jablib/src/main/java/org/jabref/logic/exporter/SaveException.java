package org.jabref.logic.exporter;

import java.util.Optional;

import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.Nullable;

/// Exception thrown if saving goes wrong. If caused by a specific
/// entry, keeps track of which entry caused the problem.
public class SaveException extends Exception {
    private @Nullable BibEntry entry;

    public SaveException(String message) {
        super(message);
        entry = null;
    }

    public SaveException(String message, Throwable exception) {
        super(message, exception);
        entry = null;
    }

    public SaveException(String message, @Nullable BibEntry entry) {
        super(message);
        this.entry = entry;
    }

    public SaveException(String message, @Nullable BibEntry entry, Throwable base) {
        super(message, base);
        this.entry = entry;
    }

    public SaveException(Throwable base) {
        super(base.getMessage(), base);
    }

    public SaveException(Throwable base, BibEntry entry) {
        this(base.getMessage(), entry, base);
    }

    public Optional<BibEntry> getEntry() {
        return Optional.ofNullable(entry);
    }
}
