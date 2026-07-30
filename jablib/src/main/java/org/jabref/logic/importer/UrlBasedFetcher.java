package org.jabref.logic.importer;

import java.util.List;

import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;

/// Searches web resources for bibliographic information based on a URL pointing to the resource.
@NullMarked
public interface UrlBasedFetcher extends WebFetcher {

    /// Looks for bibliographic information for the resource located at the given URL.
    ///
    /// @param url a string containing the URL of the resource
    /// @return a list of [BibEntry], one entry per hit found for the URL
    List<BibEntry> performSearch(String url) throws FetcherException;
}
