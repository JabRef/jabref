package org.jabref.logic.importer;

import java.util.Collections;
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
     * This method is used to send complex queries using fielded search.
     *
     * @param complexSearchQuery the complex search query defining all fielded search parameters
     * @return a list of {@link BibEntry}, which are matched by the query (may be empty)
     */
    List<BibEntry> performSearch(ComplexSearchQuery complexSearchQuery) throws FetcherException;

    /**
     * Looks for hits which are matched by the given free-text query.
     *
     * @param complexSearchQuery query string that can be parsed into a complex search query
     * @return a list of {@link BibEntry}, which are matched by the query (may be empty)
     */
    default List<BibEntry> performSearch(String complexSearchQuery) throws FetcherException {
        if (complexSearchQuery.isBlank()) {
            return Collections.emptyList();
        }
        QueryParser queryParser = new QueryParser();
        Optional<ComplexSearchQuery> generatedQuery = queryParser.parseQueryStringIntoComplexQuery(complexSearchQuery);
        // Otherwise just use query as a default term
        return this.performSearch(generatedQuery.orElse(ComplexSearchQuery.builder().defaultFieldPhrase(complexSearchQuery).build()));
    }
}
