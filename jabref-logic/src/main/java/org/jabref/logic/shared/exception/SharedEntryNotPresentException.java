package org.jabref.logic.shared.exception;

import org.jabref.model.entry.BibEntry;

/**
 * This exception is thrown if a BibEntry is going to be updated while it does not exist on the shared side.
 */
public class SharedEntryNotPresentException extends Exception {

    private final BibEntry nonPresentBibEntry;


    public SharedEntryNotPresentException(BibEntry nonPresentbibEntry) {
        super("Required BibEntry is not present on shared database.");
        this.nonPresentBibEntry = nonPresentbibEntry;
    }

    public BibEntry getNonPresentBibEntry() {
        return this.nonPresentBibEntry;
    }
}
