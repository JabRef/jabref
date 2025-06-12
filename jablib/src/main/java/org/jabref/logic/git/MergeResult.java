package org.jabref.logic.git;

import java.util.List;

import org.jabref.logic.bibtex.comparator.BibEntryDiff;

public class MergeResult {
    private final boolean success;
    private final List<BibEntryDiff> conflicts;

    private MergeResult(boolean success, List<BibEntryDiff> conflicts) {
        this.success = success;
        this.conflicts = conflicts;
    }

    public static MergeResult success() {
        return new MergeResult(true, List.of());
    }

    public static MergeResult conflictsFound(List<BibEntryDiff> conflicts) {
        return new MergeResult(false, conflicts);
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean hasConflicts() {
        return !conflicts.isEmpty();
    }

    public List<BibEntryDiff> getConflicts() {
        return conflicts;
    }
}
