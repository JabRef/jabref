package org.jabref.logic.util.io;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jabref.model.entry.BibEntry;

import com.google.common.collect.Multimap;

public interface FileFinder {

    /**
     * Finds all files in the given directories that are probably associated with the given entries and have one of the
     * passed extensions.
     *
     * @param entries     The entries to search for.
     * @param directories The root directories to search.
     * @param extensions  The extensions that are acceptable.
     */
    Multimap<BibEntry, Path> findAssociatedFiles(List<BibEntry> entries, List<Path> directories, List<String> extensions);

    default Collection<Path> findAssociatedFiles(BibEntry entry, List<Path> directories, List<String> extensions) {
        return findAssociatedFiles(Collections.singletonList(entry), directories, extensions).get(entry);
    }
}
