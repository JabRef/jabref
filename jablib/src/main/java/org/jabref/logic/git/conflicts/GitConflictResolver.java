package org.jabref.logic.git.conflicts;

import java.util.Optional;

import org.jabref.model.entry.BibEntry;

public interface GitConflictResolver {
    Optional<BibEntry> resolveConflict(ThreeWayEntryConflict conflict);
}
