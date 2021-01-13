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
     * @param transformer transformer might be required to extract some parsing information
     * @return a list of {@link BibEntry}, which are matched by the query (may be empty)
     */
    List<BibEntry> performSearchForTransformedQuery(String transformedQuery, AbstractQueryTransformer transformer) throws FetcherException;

    /**
     * Looks for hits which are matched by the given free-text query.
     *
     * @param searchQuery query string that can be parsed into a complex search query
     * @return a list of {@link BibEntry}, which are matched by the query (may be empty)
     */
    default List<BibEntry> performSearch(String searchQuery) throws FetcherException {
        AbstractQueryTransformer transformer = getQueryTransformer();
        if (searchQuery.isBlank()) {
            return Collections.emptyList();
        }
        Optional<String> transformedQuery;
        try {
            transformedQuery = transformer.parseQueryStringIntoComplexQuery(searchQuery);
        } catch (JabRefException e) {
            throw new FetcherException("Error occured during query transformation", e);
        }
        // Otherwise just use query as a default term
        return this.performSearchForTransformedQuery(transformedQuery.orElse(""), transformer);
    }

    default AbstractQueryTransformer getQueryTransformer() {
        return new DefaultQueryTransformer();
    }
}
