package org.jabref.logic.shared.event;

import java.util.List;

import org.jabref.model.entry.BibEntry;

/**
 * This event is fired when the user tries to push changes of one or more obsolete
 * {@link BibEntry} to the server.
 */
public class SharedEntriesNotPresentEvent {

    private final List<BibEntry> bibEntries;

    /**
     * @param bibEntries Affected {@link BibEntry}
     */
    public SharedEntriesNotPresentEvent(List<BibEntry> bibEntries) {
        this.bibEntries = bibEntries;
    }

    public List<BibEntry> getBibEntries() {
        return this.bibEntries;
    }
}
