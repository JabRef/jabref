package org.jabref.logic.git.conflicts;

import java.util.List;

import org.jabref.model.entry.BibEntry;

/// Strategy interface for resolving semantic entry-level conflicts during Git merges.
///
/// Implementations decide how to resolve {@link ThreeWayEntryConflict}s, such as via GUI or CLI.
///
/// Used by {@link GitSyncService} to handle semantic conflicts after Git merge.
///
public interface GitConflictResolverStrategy {
    /**
     * Resolves all given entry-level semantic conflicts, and produces a new, resolved remote state.
     * <p>
     *
     * @param conflicts the list of detected three-way entry conflicts
     * @return the modified BibDatabaseContext containing resolved entries,
     * or empty if user canceled merge or CLI refuses to merge.
     */
    List<BibEntry> resolveConflicts(List<ThreeWayEntryConflict> conflicts);
}
