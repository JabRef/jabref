package org.jabref.gui.externalfiles;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.jabref.gui.util.FileNodeViewModel;
import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;

/// @param relatedEntriesByFile keyed by [#normalizePath(Path)]
@NullMarked
record UnlinkedFilesSearchResult(FileNodeViewModel treeRoot, Map<Path, List<BibEntry>> relatedEntriesByFile) {
    List<BibEntry> relatedEntries(Path file) {
        return relatedEntriesByFile.getOrDefault(normalizePath(file), List.of());
    }

    /// Uses filesystem identity: lexical normalization would not match a crawled path below a symlinked search directory
    /// against the path found in the configured (target) file directory.
    static Path normalizePath(Path path) {
        try {
            return path.toRealPath();
        } catch (IOException e) {
            return path.toAbsolutePath().normalize();
        }
    }
}
