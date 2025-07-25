package org.jabref.logic.git.conflicts;

import java.util.List;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;

public class CliConflictResolverStrategy implements GitConflictResolverStrategy {
    @Override
    public Optional<List<BibEntry>> resolveConflicts(List<ThreeWayEntryConflict> conflicts) {
        return Optional.empty();
    }
}
