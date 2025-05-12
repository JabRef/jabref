package org.jabref.gui.mergeentries;

import org.jabref.model.entry.BibEntry;

public record MergeResult(
        BibEntry leftEntry, BibEntry rightEntry, BibEntry mergedEntry
) {
}
