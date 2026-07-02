package org.jabref.logic.importer;

import java.util.List;

import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;

/// Searches web resources for bibliographic information based on a URL.
///
/// In contrast to an [IdBasedFetcher], the user provides a complete URL (for instance the address of a
/// blog post or web page) instead of a normalized identifier such as a DOI or ISBN.
@NullMarked
public interface UrlBasedFetcher extends WebFetcher {

    /// Creates one (or more) [BibEntry] instances from the given URL.
    ///
    /// @param url the URL to create bibliographic information from
    /// @return a list of [BibEntry] (empty if nothing could be created)
    List<BibEntry> performSearch(String url) throws FetcherException;
}
