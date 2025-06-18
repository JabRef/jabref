package org.jabref.logic.git.util;

import java.util.List;

import org.jabref.logic.bibtex.comparator.BibEntryDiff;

public record MergeResult(boolean successful, List<BibEntryDiff> conflicts) {
    public static MergeResult conflictsFound(List<BibEntryDiff> conflicts) {
        return new MergeResult(false, conflicts);
    }

    public static MergeResult success() {
        return new MergeResult(true, List.of());
    }

    public boolean hasConflicts() {
        return !conflicts.isEmpty();
    }
}
