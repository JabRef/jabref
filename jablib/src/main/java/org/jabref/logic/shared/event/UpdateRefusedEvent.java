package org.jabref.logic.shared.event;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;

/// A new [UpdateRefusedEvent] is fired, when the user tries to push changes of an obsolete [BibEntry] to the server.
///
/// @param bibDatabaseContext Affected [BibDatabaseContext]
/// @param localBibEntry      Affected [BibEntry]
@NullMarked
public record UpdateRefusedEvent(
        BibDatabaseContext bibDatabaseContext,
        BibEntry localBibEntry,
        BibEntry sharedBibEntry) {
}
