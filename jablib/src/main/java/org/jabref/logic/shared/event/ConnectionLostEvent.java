package org.jabref.logic.shared.event;

import org.jabref.model.database.BibDatabaseContext;

import org.jspecify.annotations.NullMarked;

/// A new [ConnectionLostEvent] is fired, when the connection to the shared database gets lost.
///
/// @param bibDatabaseContext Affected [BibDatabaseContext]
@NullMarked
public record ConnectionLostEvent(
        BibDatabaseContext bibDatabaseContext) {
}
