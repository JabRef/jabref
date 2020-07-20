package org.jabref.logic.importer;

import java.util.List;

import org.jabref.logic.importer.fetcher.ComplexSearchQuery;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;

/**
 * Searches web resources for bibliographic information based on a free-text query.
 * May return multiple search hits.
 */
public interface SearchBasedFetcher extends BibEntryFetcher {

    /**
     * Looks for hits which are matched by the given free-text query.
     *
     * @param query search string
     * @param targetBibEntryFormat the format the entries should be returned in
     * @return a list of {@link BibEntry}, which are matched by the query (may be empty) in the requested format
     */
    List<BibEntry> performSearch(String query, BibDatabaseMode targetBibEntryFormat) throws FetcherException;

    /**
     * This method is used to send complex queries using fielded search.
     *
     * @param complexSearchQuery the search query defining all fielded search parameters
     * @return a list of {@link BibEntry}, which are matched by the query (may be empty)
     */
    default List<BibEntry> performComplexSearch(ComplexSearchQuery complexSearchQuery, BibDatabaseMode targetBibEntryFormat) throws FetcherException {
        // Default Implementation behaves like perform search using the default field as query
        return performSearch(complexSearchQuery.getDefaultField().orElse(""), targetBibEntryFormat);
    }
}
