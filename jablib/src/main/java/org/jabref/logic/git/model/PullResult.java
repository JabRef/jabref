package org.jabref.logic.git.model;

import java.util.List;

import org.jabref.model.entry.BibEntry;

public final class PullResult implements GitOperationResult {
    private final boolean isSuccessful;
    private final boolean noop;
    private final List<BibEntry> mergedEntries;

    private PullResult(boolean isSuccessful, boolean noop, List<BibEntry> mergedEntries) {
        this.isSuccessful = isSuccessful;
        this.noop = noop;
        this.mergedEntries = mergedEntries == null ? List.of() : mergedEntries;
    }

    public static PullResult merged(List<BibEntry> mergedEntries) {
        return new PullResult(true, false, mergedEntries);
    }

    public static PullResult noopUpToDate() {
        return new PullResult(true, true, List.of());
    }

    public static PullResult noopAhead() {
        return new PullResult(true, true, List.of());
    }

    @Override
    public Operation operation() {
        return Operation.PULL;
    }

    @Override
    public boolean isSuccessful() {
        return isSuccessful;
    }

    @Override
    public boolean noop() {
        return noop;
    }

    public List<BibEntry> getMergedEntries() {
        return mergedEntries;
    }
}
