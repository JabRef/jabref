package org.jabref.logic.importer.fetcher;

import java.net.URL;
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

        donaldsonEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Donaldson, S. K.")
                .withField(StandardField.JOURNAL, "Journal of Differential Geometry")
                .withField(StandardField.DOI, "10.4310/jdg/1214437665")
                .withField(StandardField.ISSN, "0022-040X")
                .withField(StandardField.LANGUAGE, "English")
                .withField(StandardField.KEYWORDS, "57N13,57R10,53C05,58J99,57R65")
                .withField(StandardField.PAGES, "279--315")
                .withField(StandardField.TITLE, "An application of gauge theory to four dimensional topology")
                .withField(StandardField.VOLUME, "18")
                .withField(StandardField.YEAR, "1983")
                .withField(StandardField.ZBL_NUMBER, "0507.57010")
                .withField(new UnknownField("zbmath"), "3800580");
    }

    @Test
    void getURLForEntryUsesDocumentSearchApi() throws Exception {
        URL urlForEntry = fetcher.getURLForEntry(getDonaldsonSearchEntry());

        String expectedUrl = "https://api.zbmath.org/v1/document/_search"
                + "?search_string=ti%3A%22An%20application%20of%20gauge%20theory"
                + "%20to%20four%20dimensional%20topology%22%20au%3A%22Donaldson%22"
                + "&page=0&results_per_page=1";
        assertEquals(expectedUrl, urlForEntry.toString());
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
        List<BibEntry> fetchedEntries = fetcher.performSearch(getDonaldsonSearchEntry());
        assertEquals(List.of(donaldsonEntry), fetchedEntries);
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
