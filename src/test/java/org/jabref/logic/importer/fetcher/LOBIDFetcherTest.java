package org.jabref.logic.importer.fetcher;

import java.util.List;

import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.PagedSearchBasedFetcher;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

class LOBIDFetcherTest implements SearchBasedFetcherCapabilityTest, PagedSearchFetcherTest {

    ImporterPreferences importerPreferences = mock(ImporterPreferences.class);
    LOBIDFetcher fetcher;

    @BeforeEach
    void setUp() {
        fetcher = new LOBIDFetcher(importerPreferences);
    }

    @Test
    void searchByQueryFindsEntry() throws Exception {
        List<BibEntry> fetchedEntries = fetcher.performSearch("kunst");
        System.out.println(fetchedEntries);
    }

    @Override
    public PagedSearchBasedFetcher getPagedFetcher() {
        return null;
    }

    @Override
    public SearchBasedFetcher getFetcher() {
        return null;
    }

    @Override
    public List<String> getTestAuthors() {
        return null;
    }

    @Override
    public String getTestJournal() {
        return null;
    }
}
