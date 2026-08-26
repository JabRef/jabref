package org.jabref.gui.externalfiles;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.jabref.gui.util.FileNodeViewModel;
import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;

@NullMarked
record UnlinkedFilesSearchResult(FileNodeViewModel treeRoot, Map<Path, List<BibEntry>> relatedEntriesByFile) {
}
