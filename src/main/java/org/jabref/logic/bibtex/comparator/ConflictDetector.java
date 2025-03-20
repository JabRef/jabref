package org.jabref.logic.bibtex.comparator;

import java.util.List;
import java.util.Optional;

import org.jabref.model.database.BibDatabaseContext;

public class ConflictDetector {

    public static Optional<GitBibDatabaseDiff> detectGitConflicts(BibDatabaseContext base, BibDatabaseContext local, BibDatabaseContext remote) {
        GitBibDatabaseDiff diff = GitBibDatabaseDiff.compare(base, local, remote);

        boolean hasConflicts = diff.getEntryDifferences().stream().anyMatch(GitBibEntryDiff::hasConflicts);

        if (hasConflicts || diff.getMetaDataDifferences().isPresent()) {
            return Optional.of(diff);
        }
        return Optional.empty();
    }

    public static boolean hasGitConflicts(BibDatabaseContext base, BibDatabaseContext local, BibDatabaseContext remote) {
        return detectGitConflicts(base, local, remote).isPresent();
    }

    public static List<GitBibEntryDiff> getGitEntryConflicts(BibDatabaseContext base, BibDatabaseContext local, BibDatabaseContext remote) {
        return GitBibDatabaseDiff.compare(base, local, remote).getEntryDifferences();
    }
}
