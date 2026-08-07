package org.jabref.logic.git.conflicts;

import java.util.List;

import org.jabref.model.entry.BibEntry;

/// No-op implementation of GitConflictResolverStrategy for CLI use.
///
/// TODO: Implement CLI conflict resolution or integrate external merge tool.
public class CliGitConflictResolverStrategy implements GitConflictResolverStrategy {
    @Override
    public List<BibEntry> resolveConflicts(List<ThreeWayEntryConflict> conflicts) {
        return List.of();
    }
}
