package org.jabref.logic.importer;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.paging.Page;

public interface PagedSearchBasedFetcher extends SearchBasedFetcher {

    /**
     * @param query      search query send to endpoint
     * @param pageNumber requested site number
     * @return Page with search results
     */
    Page<BibEntry> performSearchPaged(String query, int pageNumber) throws FetcherException;

    /**
     * @return default pageSize
     */
    default int getPageSize() {
        return 20;
    }
}
