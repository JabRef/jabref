package org.jabref.logic.importer;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.paging.Page;

public interface PagedSearchBasedFetcher extends SearchBasedFetcher {

    Page<BibEntry> performSearchPaged(String query, int size);

    Page<BibEntry> performSearchForNextPage(Page<BibEntry> currentPage);

    Page<BibEntry> performSearchForPreviousPage(Page<BibEntry> currentPage);

    Page<BibEntry> performSearchForJumpToPage(Page<BibEntry> currentPage, int jumptTo);

    @Override
    default boolean supportsPaging() {
        return true;
    }
}
