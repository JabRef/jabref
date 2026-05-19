package org.jabref.logic.importer;

import java.util.List;

import org.jabref.model.entry.BibEntry;

public interface UrlBasedFetcher extends WebFetcher {
    /// Looks for bibliographic information associated to the given URL.

    List<BibEntry> performSearch(String url) throws FetcherException;
}
