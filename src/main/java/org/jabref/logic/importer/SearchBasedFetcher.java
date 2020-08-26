package org.jabref.logic.importer;

import java.util.List;
import java.util.Optional;

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
        // Default Implementation behaves like perform search using the default field phrases as query
        Optional<List<String>> defaultPhrases = complexSearchQuery.getDefaultFieldPhrases();
        return performSearch(defaultPhrases.map(strings -> String.join(" ", strings)).orElse(""));
    }
}
