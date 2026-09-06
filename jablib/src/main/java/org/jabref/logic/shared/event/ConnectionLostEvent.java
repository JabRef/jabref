package org.jabref.logic.shared.event;

import org.jabref.model.database.BibDatabaseContext;

/// A new [ConnectionLostEvent] is fired, when the connection to the shared database gets lost.
///
/// @param bibDatabaseContext Affected [BibDatabaseContext]
public record ConnectionLostEvent(
        BibDatabaseContext bibDatabaseContext) {
}
