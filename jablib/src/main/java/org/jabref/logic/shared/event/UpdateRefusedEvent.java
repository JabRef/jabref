package org.jabref.logic.shared.event;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

/// A new [UpdateRefusedEvent] is fired, when the user tries to push changes of an obsolete [BibEntry] to the server.
///
/// @param bibDatabaseContext Affected [BibDatabaseContext]
/// @param localBibEntry      Affected [BibEntry]
public record UpdateRefusedEvent(
        BibDatabaseContext bibDatabaseContext,
        BibEntry localBibEntry,
        BibEntry sharedBibEntry) {
}
