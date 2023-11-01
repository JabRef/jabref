package org.jabref.logic.importer.fetcher;

import java.util.List;

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

        bibEntry = new BibEntry(BibEntry.DEFAULT_TYPE)
                .withField(StandardField.ISSN, "2193-1801")
                .withField(StandardField.TITLE, "Query: Jurnal Sistem Informasi")
                .withField(StandardField.INSTITUTION, "Univesitas Islam Negeri Sumatera Utara, Fakultas Sains dan Teknologi, Program Studi Sistem Informasi")
                .withField(StandardField.PUBLISHER, "Univesitas Islam Negeri Sumatera Utara")
                .withField(StandardField.LANGUAGE, "ID");
    }

    @Test
    void performSearchById() throws FetcherException {
        List<BibEntry> fetchedEntry = fetcher.performSearch(bibEntry);
        assertEquals(List.of(bibEntry), fetchedEntry);
    }

    @Test
    void getName() {
        assertEquals("ISSN", fetcher.getName());
    }

}
