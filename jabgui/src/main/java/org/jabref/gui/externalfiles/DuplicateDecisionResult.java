package org.jabref.gui.externalfiles;

import org.jabref.gui.duplicationFinder.DuplicateResolverDialog;
import org.jabref.model.entry.BibEntry;

public record DuplicateDecisionResult(
        DuplicateResolverDialog.DuplicateResolverResult decision,
        BibEntry mergedEntry) {
}


