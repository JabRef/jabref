package org.jabref.logic.shared.event;

import org.jabref.model.entry.BibEntry;

import java.util.List;

/**
 * A new {@link SharedEntryNotPresentEvent} is fired, when the user tries to push changes of an obsolete
 * {@link BibEntry} to the server.
 */
public class SharedEntryNotPresentEvent {

    private final List<BibEntry> bibEntries;

    /**
     * @param bibEntry Affected {@link BibEntry}
     */
    public SharedEntryNotPresentEvent(List<BibEntry> bibEntries) {
        this.bibEntries = bibEntries;
    }

    public List<BibEntry> getBibEntries() {
        return this.bibEntries;
    }
}
