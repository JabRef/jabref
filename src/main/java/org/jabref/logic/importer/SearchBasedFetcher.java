package org.jabref.logic.importer;

import java.util.List;

import org.jabref.logic.importer.fetcher.ComplexSearchQuery;
import org.jabref.model.entry.BibEntry;

/**
 * Searches web resources for bibliographic information based on a free-text query.
 * May return multiple search hits.
 */
public interface SearchBasedFetcher extends WebFetcher {

    /**
     * Looks for hits which are matched by the given free-text query.
     *
     * @param query search string
     * @return a list of {@link BibEntry}, which are matched by the query (may be empty)
     */
    List<BibEntry> performSearch(String query) throws FetcherException;

    /**
     * This method is used to send complex queries using fielded search.
     *
     * @param complexSearchQuery the search query defining all fielded search parameters
     * @return a list of {@link BibEntry}, which are matched by the query (may be empty)
     */
    default List<BibEntry> performComplexSearch(ComplexSearchQuery complexSearchQuery) throws FetcherException {
        // Default implementation behaves as perform search on all fields concatenated as query
        return performSearch(complexSearchQuery.toString());
    }
}
