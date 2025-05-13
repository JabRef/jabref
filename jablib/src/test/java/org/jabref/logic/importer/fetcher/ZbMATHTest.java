package org.jabref.logic.importer.fetcher;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
class ZbMATHTest {
    private ZbMATH fetcher;
    private BibEntry donaldsonEntry;

    @BeforeEach
    void setUp() {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');

        fetcher = new ZbMATH(importFormatPreferences);

        donaldsonEntry = new BibEntry();
        donaldsonEntry.setType(StandardEntryType.Article);
        donaldsonEntry.setCitationKey("zbMATH03800580");
        donaldsonEntry.setField(StandardField.AUTHOR, "Donaldson, S. K.");
        donaldsonEntry.setField(StandardField.JOURNAL, "Journal of Differential Geometry");
        donaldsonEntry.setField(StandardField.DOI, "10.4310/jdg/1214437665");
        donaldsonEntry.setField(StandardField.ISSN, "0022-040X");
        donaldsonEntry.setField(StandardField.LANGUAGE, "English");
        donaldsonEntry.setField(StandardField.KEYWORDS, "57N13,57R10,53C05,58J99,57R65");
        donaldsonEntry.setField(StandardField.PAGES, "279--315");
        donaldsonEntry.setField(StandardField.TITLE, "An application of gauge theory to four dimensional topology");
        donaldsonEntry.setField(StandardField.VOLUME, "18");
        donaldsonEntry.setField(StandardField.YEAR, "1983");
        donaldsonEntry.setField(StandardField.ZBL_NUMBER, "0507.57010");
        donaldsonEntry.setField(new UnknownField("zbmath"), "3800580");
    }

    @Test
    void searchByQueryFindsEntry() throws FetcherException {
        List<BibEntry> fetchedEntries = fetcher.performSearch("an:0507.57010");
        assertEquals(List.of(donaldsonEntry), fetchedEntries);
    }

    @Test
    void searchByIdFindsEntry() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("0507.57010");
        assertEquals(Optional.of(donaldsonEntry), fetchedEntry);
    }

    @Test
    void searchByEntryFindsEntry() throws FetcherException {
        BibEntry searchEntry = new BibEntry();
        searchEntry.setField(StandardField.TITLE, "An application of gauge theory to four dimensional topology");
        searchEntry.setField(StandardField.AUTHOR, "S. K. {Donaldson}");

        List<BibEntry> fetchedEntries = fetcher.performSearch(searchEntry);
        assertEquals(List.of(donaldsonEntry), fetchedEntries);
    }

    @Test
    void searchByNoneEntryFindsNothing() throws FetcherException {
        BibEntry searchEntry = new BibEntry();
        searchEntry.setField(StandardField.TITLE, "t");
        searchEntry.setField(StandardField.AUTHOR, "a");

        List<BibEntry> fetchedEntries = fetcher.performSearch(searchEntry);
        assertEquals(List.of(), fetchedEntries);
    }

    @Test
    void searchByIdInEntryFindsEntry() throws FetcherException {
        BibEntry searchEntry = new BibEntry();
        searchEntry.setField(StandardField.ZBL_NUMBER, "0507.57010");

        List<BibEntry> fetchedEntries = fetcher.performSearch(searchEntry);
        assertEquals(List.of(donaldsonEntry), fetchedEntries);
    }
}
