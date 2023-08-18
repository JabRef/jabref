package org.jabref.logic.importer.fetcher;

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

        fetcher = new IssnFetcher(importPrefs);

        bibEntry = new BibEntry(BibEntry.DEFAULT_TYPE)
                .withField(StandardField.ISSN, "2579-5341")
                .withField(StandardField.TITLE, "Query: Jurnal Sistem Informasi")
                .withField(StandardField.INSTITUTION, "Univesitas Islam Negeri Sumatera Utara, Fakultas Sains dan Teknologi, Program Studi Sistem Informasi")
                .withField(StandardField.PUBLISHER, "Univesitas Islam Negeri Sumatera Utara")
                .withField(StandardField.LANGUAGE, "ID");
    }

    @Test
    void performSearchById() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("2579-5341");
        assertEquals(Optional.of(bibEntry), fetchedEntry);
    }

    @Test
    void findIdentifier() {
    }

    @Test
    void getIdentifierName() {
        assertEquals("ISSN", fetcher.getIdentifierName());
    }

    @Test
    void getName() {
        assertEquals("ISSN", fetcher.getName());
    }

    @Test
    void concatenateIssnWithId() {
        String modifiedIdentifier = fetcher.concatenateIssnWithId("2579-5341");
        assertEquals("issn:2579-5341", modifiedIdentifier);
    }
}
