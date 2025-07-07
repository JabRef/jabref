package org.jabref.logic.git.model;

import java.util.List;

import org.jabref.logic.bibtex.comparator.BibEntryDiff;

public record MergeResult(boolean isSuccessful, List<BibEntryDiff> conflicts) {
    private static boolean SUCCESS = true;
    public static MergeResult withConflicts(List<BibEntryDiff> conflicts) {
        return new MergeResult(!SUCCESS, conflicts);
    }

    public static MergeResult success() {
        return new MergeResult(SUCCESS, List.of());
    }

    public boolean hasConflicts() {
        return !conflicts.isEmpty();
    }
}
