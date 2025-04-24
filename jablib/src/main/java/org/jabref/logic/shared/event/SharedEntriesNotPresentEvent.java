package org.jabref.logic.shared.event;

import java.util.List;

import org.jabref.model.entry.BibEntry;

/**
 * This event is fired when the user tries to push changes of one or more obsolete
 * {@link BibEntry} to the server.
 *
 * @param bibEntries Affected {@link BibEntry}
 */
public record SharedEntriesNotPresentEvent(
        List<BibEntry> bibEntries) {
}
