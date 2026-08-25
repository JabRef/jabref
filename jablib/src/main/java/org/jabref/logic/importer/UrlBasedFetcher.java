package org.jabref.logic.importer;

import java.util.List;

import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;

/// Searches web resources for bibliographic information based on a URL pointing to the resource.
/// May return multiple search hits, since a single URL can represent several works
/// (e.g. an author's publication list or a conference proceedings page).
@NullMarked
public interface UrlBasedFetcher extends WebFetcher {

    /// Looks for bibliographic information for the resource located at the given URL.
    ///
    /// Implementations must validate the given string themselves (it is typically raw user input,
    /// e.g. pasted from the clipboard) and throw a [FetcherException] if it is not a URL they can handle.
    /// Failures while gathering optional data (such as an unreachable host) should not lead to an exception,
    /// implementations should instead return the best entry they can construct from the URL alone.
    ///
    /// @param url a string containing the URL of the resource
    /// @return a list of [BibEntry], one entry per hit found for the URL (may be empty, but never 'null')
    List<BibEntry> performSearch(String url) throws FetcherException;
}
