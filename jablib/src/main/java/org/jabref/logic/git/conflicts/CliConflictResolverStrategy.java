package org.jabref.logic.git.conflicts;

import java.util.List;
import java.util.Optional;

import org.jabref.model.database.BibDatabaseContext;

public class CliConflictResolverStrategy implements GitConflictResolverStrategy {

    @Override
    public Optional<BibDatabaseContext> resolveConflicts(List<ThreeWayEntryConflict> conflicts, BibDatabaseContext remote) {
        return Optional.empty();
    }
}
