package org.jabref.logic.shared.event;

import org.jabref.model.database.BibDatabaseContext;

/// Fired when the connection to the shared database is back after a [ConnectionLostEvent].
///
/// @param bibDatabaseContext Affected [BibDatabaseContext]
public record ConnectionRestoredEvent(
        BibDatabaseContext bibDatabaseContext) {
}
