package org.jabref.logic.util.io;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jabref.model.entry.BibEntry;

public interface FileFinder {

    /**
     * Finds all files in the given directories that are probably associated with the given entries and have one of the
     * passed extensions.
     *
     * @param entries     The entries to search for.
     * @param directories The root directories to search.
     * @param extensions  The extensions that are acceptable.
     */
    Map<BibEntry, List<Path>> findAssociatedFiles(List<BibEntry> entries, List<Path> directories, List<String> extensions);

    default List<Path> findAssociatedFiles(BibEntry entry, List<Path> directories, List<String> extensions) {
        Map<BibEntry, List<Path>> associatedFiles = findAssociatedFiles(Collections.singletonList(entry), directories, extensions);
        return associatedFiles.getOrDefault(entry, Collections.emptyList());
    }
}
