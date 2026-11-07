package org.jabref.logic.shared.event;

import org.jabref.model.database.BibDatabaseContext;

import org.jspecify.annotations.NullMarked;

/// Fired when a local change could not be written to the shared database although the connection
/// is still alive - the affected local changes are not synchronized.
///
/// @param bibDatabaseContext Affected [BibDatabaseContext]
@NullMarked
public record SharedWriteFailedEvent(
        BibDatabaseContext bibDatabaseContext) {
}
