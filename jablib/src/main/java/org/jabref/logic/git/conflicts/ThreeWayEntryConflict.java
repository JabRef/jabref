package org.jabref.logic.git.conflicts;

import org.jabref.model.entry.BibEntry;

public record ThreeWayEntryConflict(
    BibEntry base,
    BibEntry local,
    BibEntry remote
) {
    public ThreeWayEntryConflict {
        if (local == null && remote == null) {
            throw new IllegalArgumentException("Both local and remote are null: conflict must involve at least one side.");
        }
    }
}
