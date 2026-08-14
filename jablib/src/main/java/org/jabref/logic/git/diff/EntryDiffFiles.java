package org.jabref.logic.git.diff;

import java.util.List;

import org.jabref.logic.bibtex.comparator.BibEntryDiff;

public record EntryDiffFiles(String fileName, List<BibEntryDiff> entryDiffs) implements DiffFiles {
}
