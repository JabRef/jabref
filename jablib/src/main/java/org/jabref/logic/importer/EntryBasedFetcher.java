package org.jabref.logic.importer;

import java.util.List;

import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;

/// Searches web resources for bibliographic information based on a [BibEntry].
/// Useful to **complete** an existing entry with fetched information.
/// May return multiple search hits.
@NullMarked
public interface EntryBasedFetcher extends WebFetcher {

    /// Looks for hits which are matched by the given [BibEntry].
    ///
    /// @param entry entry to search bibliographic information for
    /// @return a list of [BibEntry], which are matched by the query (may be empty)
    List<BibEntry> performSearch(BibEntry entry) throws FetcherException;
}
