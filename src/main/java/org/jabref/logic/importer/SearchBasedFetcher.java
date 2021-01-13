package org.jabref.logic.importer;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.JabRefException;
import org.jabref.logic.importer.fetcher.transformators.AbstractQueryTransformer;
import org.jabref.logic.importer.fetcher.transformators.DefaultQueryTransformer;
import org.jabref.model.entry.BibEntry;

/**
 * Searches web resources for bibliographic information based on a free-text query.
 * May return multiple search hits.
 */
public interface SearchBasedFetcher extends WebFetcher {

    /**
     * This method is used to send complex queries using fielded search.
     *
     * @param transformedQuery the search query defining all fielded search parameters
     * @return a list of {@link BibEntry}, which are matched by the query (may be empty)
     */
    List<BibEntry> performSearchForTransformedQuery(String transformedQuery) throws FetcherException;

    /**
     * Looks for hits which are matched by the given free-text query.
     *
     * @param searchQuery query string that can be parsed into a complex search query
     * @return a list of {@link BibEntry}, which are matched by the query (may be empty)
     */
    default List<BibEntry> performSearch(String searchQuery) throws FetcherException {
        if (searchQuery.isBlank()) {
            return Collections.emptyList();
        }
        Optional<String> transformedQuery;
        try {
            transformedQuery = getQueryTransformer().parseQueryStringIntoComplexQuery(searchQuery);
        } catch (JabRefException e) {
            throw new FetcherException("Error occured during query transformation", e);
        }
        // Otherwise just use query as a default term
        return this.performSearchForTransformedQuery(transformedQuery.orElse(searchQuery));
    }

    default AbstractQueryTransformer getQueryTransformer() {
        return new DefaultQueryTransformer();
    }
}
