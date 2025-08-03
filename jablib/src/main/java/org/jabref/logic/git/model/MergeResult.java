package org.jabref.logic.git.model;

import java.util.List;

import org.jabref.logic.bibtex.comparator.BibEntryDiff;

public record MergeResult(boolean isSuccessful, List<BibEntryDiff> conflicts) {
    public static MergeResult withConflicts(List<BibEntryDiff> conflicts) {
        return new MergeResult(false, conflicts);
    }

    public static MergeResult success() {
        return new MergeResult(true, List.of());
    }

    public static MergeResult failure() {
        return new MergeResult(false, List.of());
    }

    public boolean hasConflicts() {
        return !conflicts.isEmpty();
    }
}
