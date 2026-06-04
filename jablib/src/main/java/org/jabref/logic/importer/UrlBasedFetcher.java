package org.jabref.logic.importer;

import java.util.List;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NonNull;

/// Searches web resources for bibliographic information based on a URL.
public interface UrlBasedFetcher extends WebFetcher {

    /// Looks for bibliographic information associated to the given URL.
    ///
    /// @param url a string which contains information regarding one or more entries
    /// @return a {@link BibEntry} containing the bibliographic information (or an empty optional if no data was found)
    Optional<List<BibEntry>> performSearch(@NonNull String url) throws FetcherException;
}
