package org.jabref.logic.importer.fetcher;

import org.jabref.logic.importer.PagedSearchBasedFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.paging.Page;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * This interface provides general test methods for paged fetchers
 */
public interface PagedSearchFetcherTest {

    default String queryForUniqueResultsPerPage() {
        return "Software";
    }

    /**
     * Ensure that different page return different entries
     */
    @Test
    default void pageSearchReturnsUniqueResultsPerPage() throws Exception {
        String query = queryForUniqueResultsPerPage();
        Page<BibEntry> firstPage = getPagedFetcher().performSearchPaged(query, 0);
        Page<BibEntry> secondPage = getPagedFetcher().performSearchPaged(query, 1);

        // Both pages need to be filled with contents to be valid for checking containment
        assertEquals(20, firstPage.getSize());
        assertEquals(20, secondPage.getSize());

        for (BibEntry entry : firstPage.getContent()) {
            assertFalse(secondPage.getContent().contains(entry), "%s contained in %s".formatted(entry, secondPage.getContent()));
        }
    }

    PagedSearchBasedFetcher getPagedFetcher();
}
