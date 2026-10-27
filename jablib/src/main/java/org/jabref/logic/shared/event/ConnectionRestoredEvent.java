package org.jabref.logic.shared.event;

import org.jabref.model.database.BibDatabaseContext;

import org.jspecify.annotations.NullMarked;

/// Fired when the connection to the shared database is back after a [ConnectionLostEvent].
///
/// @param bibDatabaseContext Affected [BibDatabaseContext]
@NullMarked
public record ConnectionRestoredEvent(
        BibDatabaseContext bibDatabaseContext) {
}
