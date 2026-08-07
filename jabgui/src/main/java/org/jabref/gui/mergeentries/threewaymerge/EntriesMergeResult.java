package org.jabref.gui.mergeentries.threewaymerge;

import org.jabref.model.entry.BibEntry;

public record EntriesMergeResult(
        BibEntry originalLeftEntry, BibEntry originalRightEntry, BibEntry newLeftEntry, BibEntry newRightEntry, BibEntry mergedEntry
) {
}
