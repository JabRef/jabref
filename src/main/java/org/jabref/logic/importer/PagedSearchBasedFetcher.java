package org.jabref.logic.importer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.JabRefException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.paging.Page;

public interface PagedSearchBasedFetcher extends SearchBasedFetcher {

    /**
     * @param transformedQuery the complex query defining all fielded search parameters
     * @param pageNumber       requested site number indexed from 0
     * @return Page with search results
     */
    Page<BibEntry> performSearchPagedForTransformedQuery(String transformedQuery, int pageNumber) throws FetcherException;

    /**
     * @param searchQuery query string that can be parsed into a complex search query
     * @param pageNumber  requested site number indexed from 0
     * @return Page with search results
     */
    default Page<BibEntry> performSearchPaged(String searchQuery, int pageNumber) throws JabRefException {
        if (searchQuery.isBlank()) {
            return new Page<>(searchQuery, pageNumber, Collections.emptyList());
        }
        Optional<String> transformedQuery = getQueryTransformer().parseQueryStringIntoComplexQuery(searchQuery);
        // Otherwise just use query as a default term
        return this.performSearchPagedForTransformedQuery(transformedQuery.orElse(searchQuery), pageNumber);
    }

    /**
     * @return default pageSize
     */
    default int getPageSize() {
        return 20;
    }

    @Override
    default List<BibEntry> performSearchForTransformedQuery(String transformedQuery) throws FetcherException {
        return new ArrayList<>(performSearchPagedForTransformedQuery(transformedQuery, 0).getContent());
    }
}
