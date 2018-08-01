package org.jabref.logic.shared.event;

import org.jabref.model.entry.BibEntry;

/**
 * A new {@link SharedEntryNotPresentEvent} is fired, when the user tries to push changes of an obsolete
 * {@link BibEntry} to the server.
 */
public class SharedEntryNotPresentEvent {

    private final BibEntry bibEntry;

    /**
     * @param bibEntry Affected {@link BibEntry}
     */
    public SharedEntryNotPresentEvent(BibEntry bibEntry) {
        this.bibEntry = bibEntry;
    }

    public BibEntry getBibEntry() {
        return this.bibEntry;
    }
}
