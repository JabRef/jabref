package org.jabref.logic.shared.event;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

/**
 * A new {@link UpdateRefusedEvent} is fired, when the user tries to push changes of an obsolete {@link BibEntry} to the server.
 *
 * @param bibDatabaseContext Affected {@link BibDatabaseContext}
 * @param localBibEntry      Affected {@link BibEntry}
 */
public record UpdateRefusedEvent(
        BibDatabaseContext bibDatabaseContext,
        BibEntry localBibEntry,
        BibEntry sharedBibEntry) {
}
