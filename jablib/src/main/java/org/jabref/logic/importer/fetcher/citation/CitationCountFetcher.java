package org.jabref.logic.importer.fetcher.citation;

import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface CitationCountFetcher {
    /// Get the paper details that includes citation count field for a given {@link BibEntry}.
    ///
    /// @param entry entry to search citation count field
    /// @return returns a {@link Integer} for citation count field (may be empty)
    Optional<Integer> getCitationCount(BibEntry entry) throws FetcherException;
}
