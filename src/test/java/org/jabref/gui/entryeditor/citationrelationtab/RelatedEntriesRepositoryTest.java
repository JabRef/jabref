package org.jabref.gui.entryeditor.citationrelationtab;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RelatedEntriesRepositoryTest {
    MockRelatedEntriesFetcher relatedEntriesFetcher;
    RelatedEntriesCache cache;
    RelatedEntriesRepository repository;
    @BeforeEach
    void setUp() {
        relatedEntriesFetcher = new MockRelatedEntriesFetcher();
        cache = new RelatedEntriesCache();
        repository = new RelatedEntriesRepository(relatedEntriesFetcher, cache);
    }

    @Test
    void testLookupRelatedEntries() {
        BibEntry someEntry = new BibEntry()
                .withField(StandardField.DOI, "10.1088/1367-2630/15/3/033026");

        BibEntry anotherEntry = new BibEntry()
                .withField(StandardField.DOI, "10.1364/cleo_si.2020.sm4e.4");

        repository.lookupRelatedEntries(someEntry);

        assertEquals(1, relatedEntriesFetcher.getFetchesCount());

        repository.lookupRelatedEntries(someEntry);

        // The related entries of 'someEntry' should be cached by now so the fetcher should not
        // be called again.
        assertEquals(1, relatedEntriesFetcher.getFetchesCount());
        assertEquals(1, relatedEntriesFetcher.getFetchesCount());
        assertEquals(1, relatedEntriesFetcher.getFetchesCount());

        repository.lookupRelatedEntries(anotherEntry);

        // Entries related to 'otherEntry' are not cached so a call to the fetcher
        // is expected
        assertEquals(2, relatedEntriesFetcher.getFetchesCount());
    }

    @Test
    void testRefreshCache() {
        BibEntry someEntry = new BibEntry()
                .withField(StandardField.DOI, "10.1088/1367-2630/15/3/033026");

        repository.lookupRelatedEntries(someEntry);

        assertEquals(1, relatedEntriesFetcher.getFetchesCount());

        repository.lookupRelatedEntries(someEntry);
        repository.refreshCache(someEntry);

        // Normally the related entries of 'someEntry' should be cached by now and the fetcher would not
        // be called again, but we are forcefully refreshing the cache which would call the fetcher again.
        assertEquals(2, relatedEntriesFetcher.getFetchesCount());
    }

}
