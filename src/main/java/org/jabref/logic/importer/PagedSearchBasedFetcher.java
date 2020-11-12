package org.jabref.logic.importer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.fetcher.ComplexSearchQuery;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.paging.Page;

public interface PagedSearchBasedFetcher extends SearchBasedFetcher {

    /**
     * @param query      search query send to endpoint
     * @param pageNumber requested site number indexed from 0
     * @return Page with search results
     */
    Page<BibEntry> performComplexSearchPaged(ComplexSearchQuery query, int pageNumber) throws FetcherException;

    /**
     * @param complexSearchQuery search query send to endpoint
     * @param pageNumber         requested site number indexed from 0
     * @return Page with search results
     */
    default Page<BibEntry> performComplexSearchPaged(String complexSearchQuery, int pageNumber) throws FetcherException {
        if (complexSearchQuery.isBlank()) {
            return new Page<>(complexSearchQuery, pageNumber, Collections.emptyList());
        }
        QueryParser queryParser = new QueryParser();
        Optional<ComplexSearchQuery> generatedQuery = queryParser.parseQueryStringIntoComplexQuery(complexSearchQuery);
        // Otherwise just use query as a default term
        return this.performComplexSearchPaged(generatedQuery.orElse(ComplexSearchQuery.builder().defaultFieldPhrase(complexSearchQuery).build()), pageNumber);
    }

    /**
     * @return default pageSize
     */
    default int getPageSize() {
        return 20;
    }

    @Override
    default List<BibEntry> performComplexSearch(ComplexSearchQuery complexSearchQuery) throws FetcherException {
        return new ArrayList<>(performComplexSearchPaged(complexSearchQuery, 0).getContent());
    }

    @Override
    default List<BibEntry> performComplexSearch(String complexSearchQuery) throws FetcherException {
        return new ArrayList<>(performComplexSearchPaged(complexSearchQuery, 0).getContent());
    }
}
