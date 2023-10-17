package org.jabref.gui.externalfiles;

import org.jabref.gui.duplicationFinder.DuplicateResolverDialog;
import org.jabref.model.entry.BibEntry;

public class DuplicateDecisionResult {
    private final DuplicateResolverDialog.DuplicateResolverResult decision;
    private final BibEntry mergedEntry;

    public DuplicateDecisionResult(DuplicateResolverDialog.DuplicateResolverResult decision, BibEntry mergedEntry) {
        this.decision = decision;
        this.mergedEntry = mergedEntry;
    }

    public DuplicateResolverDialog.DuplicateResolverResult getDecision() {
        return decision;
    }

    public BibEntry getMergedEntry() {
        return mergedEntry;
    }
}

