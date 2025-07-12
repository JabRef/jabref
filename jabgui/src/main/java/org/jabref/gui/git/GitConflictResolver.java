package org.jabref.gui.git;

import java.util.Optional;

import org.jabref.logic.git.conflicts.ThreeWayEntryConflict;
import org.jabref.model.entry.BibEntry;

public interface GitConflictResolver {
    Optional<BibEntry> resolveConflict(ThreeWayEntryConflict conflict);
}
