package org.jabref.logic.bibtex.comparator;

import java.util.List;
import java.util.Optional;

import org.jabref.model.database.BibDatabaseContext;

public class ConflictDetector {

    public static Optional<BibDatabaseDiff> detectConflicts(BibDatabaseContext local, BibDatabaseContext remote) {
        BibDatabaseDiff diff = BibDatabaseDiff.compare(local, remote);

        if (!diff.getEntryDifferences().isEmpty() || diff.getMetaDataDifferences().isPresent()) {
            return Optional.of(diff);
        }
        return Optional.empty();
    }

    public static boolean hasConflicts(BibDatabaseContext local, BibDatabaseContext remote) {
        return detectConflicts(local, remote).isPresent();
    }

    public static List<BibEntryDiff> getEntryConflicts(BibDatabaseContext local, BibDatabaseContext remote) {
        return BibDatabaseDiff.compare(local, remote).getEntryDifferences();
    }
}
