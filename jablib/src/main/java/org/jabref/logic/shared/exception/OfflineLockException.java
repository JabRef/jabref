package org.jabref.logic.shared.exception;

import org.jabref.model.entry.BibEntry;

/**
 * This exception is thrown if a BibEntry with smaller version number is going to be used for an update on the shared side.
 * The principle of optimistic offline lock forbids updating with obsolete objects.
 */
public class OfflineLockException extends Exception {

    private final BibEntry localBibEntry;
    private final BibEntry sharedBibEntry;

    public OfflineLockException(BibEntry localBibEntry, BibEntry sharedBibEntry) {
        super("Local BibEntry data is not up-to-date.");
        this.localBibEntry = localBibEntry;
        this.sharedBibEntry = sharedBibEntry;
    }

    public BibEntry getLocalBibEntry() {
        return localBibEntry;
    }

    public BibEntry getSharedBibEntry() {
        return sharedBibEntry;
    }
}
