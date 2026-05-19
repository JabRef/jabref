package org.jabref.logic.importer;

import org.jabref.model.entry.BibEntry;

import java.util.List;

public interface UrlBasedFetcher extends WebFetcher {
    /// Looks for bibliographic information associated to the given URL.

        List<BibEntry> performSearch(String url) throws FetcherException; }
