package org.jabref.logic.git.conflicts;

import java.util.List;
import java.util.Optional;

import org.jabref.model.database.BibDatabaseContext;

public interface GitConflictResolverStrategy {
    /**
     * Resolves all given entry-level semantic conflicts, and produces a new, resolved remote state.
     *
     * @param conflicts the list of detected three-way entry conflicts
     * @param remote the original remote state
     * @return the modified BibDatabaseContext containing resolved entries,
     *         or empty if user canceled merge or CLI refuses to merge.
     */
    Optional<BibDatabaseContext> resolveConflicts(List<ThreeWayEntryConflict> conflicts, BibDatabaseContext remote);
}
