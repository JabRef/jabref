package org.jabref.logic.importer;

import java.util.ArrayList;
import java.util.List;

import org.jabref.logic.importer.fetcher.ComplexSearchQuery;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.paging.Page;

public interface PagedSearchBasedFetcher extends SearchBasedFetcher {

    /**
     * @param query      search query send to endpoint
     * @param pageNumber requested site number indexed from 0
     * @return Page with search results
     */
    Page<BibEntry> performSearchPaged(String query, int pageNumber) throws FetcherException;

    /**
     * @param query      search query send to endpoint
     * @param pageNumber requested site number indexed from 0
     * @return Page with search results
     */
    default Page<BibEntry> performComplexSearchPaged(ComplexSearchQuery query, int pageNumber) throws FetcherException {
        return performSearchPaged(query.toString(), pageNumber);
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
}
