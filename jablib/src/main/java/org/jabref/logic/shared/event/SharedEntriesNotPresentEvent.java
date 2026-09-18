package org.jabref.logic.shared.event;

import java.util.List;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;

/// Fired when entries were removed locally because they no longer exist on the shared side.
///
/// @param bibDatabaseContext Affected [BibDatabaseContext]
/// @param bibEntries         The removed entries - they still hold their last local state
@NullMarked
public record SharedEntriesNotPresentEvent(
        BibDatabaseContext bibDatabaseContext,
        List<BibEntry> bibEntries) {
}
