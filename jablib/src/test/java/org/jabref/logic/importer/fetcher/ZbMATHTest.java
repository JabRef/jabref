package org.jabref.logic.importer.fetcher;

import java.net.URL;
import java.util.List;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
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

    @BeforeEach
    void setUp() {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');

        fetcher = new ZbMATH(importFormatPreferences);
    }

    @Test
    void getURLForEntryUsesCitationMatchingApi() throws Exception {
        URL urlForEntry = fetcher.getURLForEntry(getDonaldsonSearchEntry());

        assertEquals("https://zbmath.org/bibtexoutput/?q=an%3A0507.57010&start=0&count=1", urlForEntry.toString());
    }

    @Test
    void searchByNoneEntryFindsNothing() throws FetcherException {
        BibEntry searchEntry = new BibEntry()
                .withField(StandardField.TITLE, "t")
                .withField(StandardField.AUTHOR, "a");

        List<BibEntry> fetchedEntries = fetcher.performSearch(searchEntry);
        assertEquals(List.of(), fetchedEntries);
    }

    private BibEntry getDonaldsonSearchEntry() {
        return new BibEntry()
                .withField(StandardField.TITLE, "An application of gauge theory to four dimensional topology")
                .withField(StandardField.AUTHOR, "S. K. {Donaldson}");
    }
}
