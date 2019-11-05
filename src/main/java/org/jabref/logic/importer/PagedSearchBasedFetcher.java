package org.jabref.logic.importer;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.paging.Page;

public interface PagedSearchBasedFetcher extends SearchBasedFetcher {

    /**
     * Takes a Page with empty content and sets its content;
     *
     * @param searchPage page with search parameters
     * @return Page with search results
     * @apiNote implementations should call the setter of the given page and not create a new Page
     */
    Page<BibEntry> performSearchPaged(Page<BibEntry> searchPage) throws FetcherException;

    /**
     * @return default pageSize
     */
    default int getPageSize() {
        return 20;
    }

    /**
     * @param query      search query send to endpoint
     * @param pageNumber requested site number
     * @return Page with search results
     */
    default Page<BibEntry> performSearchPaged(String query, int pageNumber) throws FetcherException {
        return performSearchPaged(new Page<>(query, pageNumber));
    }

    @Override
    default boolean supportsPaging() {
        return true;
    }
}
