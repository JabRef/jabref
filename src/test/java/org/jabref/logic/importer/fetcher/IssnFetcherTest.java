package org.jabref.logic.importer.fetcher;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.preferences.BibEntryPreferences;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
class IssnFetcherTest {

    private IssnFetcher fetcher;
    private BibEntry bibEntry;

    @BeforeEach
    void setUp() {
        ImportFormatPreferences importPrefs = mock(ImportFormatPreferences.class);
        BibEntryPreferences bibEntryPrefs = mock(BibEntryPreferences.class);
        when(importPrefs.bibEntryPreferences()).thenReturn(bibEntryPrefs);

        fetcher = new IssnFetcher();

        bibEntry = new BibEntry()
                .withField(StandardField.ISSN, "15454509")
                .withField(StandardField.JOURNALTITLE, "Annual Review of Biochemistry")
                .withField(StandardField.PUBLISHER, "Annual Reviews Inc.");
    }

    @Test
    void performSearchByEntry() throws FetcherException {
        List<BibEntry> fetchedEntry = fetcher.performSearch(bibEntry);
        assertEquals(List.of(bibEntry), fetchedEntry);
    }

    @Test
    void performSearchById() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("15454509");
        assertEquals(Optional.of(bibEntry), fetchedEntry);
    }

    @Test
    void getName() {
        assertEquals("ISSN", fetcher.getName());
    }
}
