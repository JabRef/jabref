package org.jabref.logic.importer.fetcher;

import java.util.Collections;
import java.util.List;

import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.PagedSearchBasedFetcher;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@FetcherTest
class LOBIDFetcherTest implements SearchBasedFetcherCapabilityTest, PagedSearchFetcherTest {

    ImporterPreferences importerPreferences = mock(ImporterPreferences.class);
    LOBIDFetcher fetcher;

    @BeforeEach
    void setUp() {
        fetcher = new LOBIDFetcher(importerPreferences);
    }

    @Override
    public String queryForUniqueResultsPerPage() {
        // LOBID does not have unique entries per page for "Software";
        return "Discussion";
    }

    @Test
    void searchByQueryFindsEntry() throws Exception {
        BibEntry firstArticle = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.AUTHOR, "Nichols, Cathrine and Blume, Eugen and DruckVerlag Kettler GmbH")
                .withField(StandardField.PUBLISHER, "Verlag Kettler")
                .withField(StandardField.DATE, "2016")
                .withField(StandardField.ISBN, "9783862065752")
                .withField(StandardField.KEYWORDS, "(Produktform)Hardback, Cathrine Nichols, Eugen Blume, Staatliche Museen zu Berlin, (Produktgruppe)Ausst: Ausstellungskatalog, (VLB-WN)1580: Hardcover, Softcover / Kunst")
                .withField(StandardField.LANGUAGE, "Deutsch")
                .withField(StandardField.LOCATION, "Dortmund")
                .withField(StandardField.TITLE, "Das Kapital")
                .withField(StandardField.TITLEADDON, "Schuld, Territorium, Utopie")
                .withField(StandardField.TYPE, "BibliographicResource, Book")
                .withField(StandardField.URL, "http://lobid.org/resources/991002500969706485")
                .withField(StandardField.YEAR, "2016");

        BibEntry secondArticle = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Nielsen, Cathrin")
                .withField(StandardField.TITLE, "\"... und die Substanz ist natürlich allein schon ein seelischer Prozeß\"")
                .withField(StandardField.JOURNAL, "Beuys. Die Revolution sind wir")
                .withField(StandardField.DATE, "2008")
                .withField(StandardField.KEYWORDS, "Beuys, Joseph, Physis")
                .withField(StandardField.LANGUAGE, "Deutsch")
                .withField(StandardField.TYPE, "BibliographicResource, Article")
                .withField(StandardField.URL, "http://lobid.org/resources/990173112890206441")
                .withField(StandardField.YEAR, "2008");

        BibEntry thirdArticle = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.AUTHOR, "Nichols, Catherine and Blume, Eugen and Hamburger Bahnhof - Museum für Gegenwart - Berlin and Nationalgalerie (Berlin) and DruckVerlag Kettler GmbH")
                .withField(StandardField.ABSTRACT, "Impresum: \"Diese Publikation erscheint anlässlich der Ausstellung Das Kapital. Schuld-Territorium-Utopie. Eine Ausstellung der Nationalgalerie im Hamburger Bahnhof - Museum für Gegenwart - Berlin, 2. Juli-6. November 2016\"")
                .withField(StandardField.PUBLISHER, "Verlag Kettler")
                .withField(StandardField.DATE, "2016")
                .withField(StandardField.EDITION, "1. Auflage")
                .withField(StandardField.ISBN, "9783862065752")
                .withField(StandardField.KEYWORDS, "Cathrine Nichols, Eugen Blume, Staatliche Museen zu Berlin, Beuys, Joseph: Das Kapital Raum 1970-1977, Kunst, Kapitalismus (Motiv), Geschichte")
                .withField(StandardField.LANGUAGE, "Deutsch")
                .withField(StandardField.LOCATION, "Dortmund")
                .withField(StandardField.TITLE, "Das Kapital")
                .withField(StandardField.TITLEADDON, "Schuld - Territorium - Utopie")
                .withField(StandardField.TYPE, "BibliographicResource, Book")
                .withField(StandardField.URL, "http://lobid.org/resources/990212549810206441")
                .withField(StandardField.YEAR, "2016");

        List<BibEntry> fetchedEntries = fetcher.performSearch("Cathrine Nichols");
        assertEquals(List.of(firstArticle, secondArticle, thirdArticle), fetchedEntries);
    }

    @Test
    void searchByEmptyQueryFindsNothing() throws Exception {
        assertEquals(Collections.emptyList(), fetcher.performSearch(""));
    }

    @Test
    @Override
    @Disabled("Results returned contain a few incorrect years. The majority are accurate")
    public void supportsYearSearch() {
    }

    @Override
    public PagedSearchBasedFetcher getPagedFetcher() {
        return fetcher;
    }

    @Override
    public SearchBasedFetcher getFetcher() {
        return fetcher;
    }

    @Override
    public List<String> getTestAuthors() {
        return List.of("Nichols, Cathrine", "Blume, Eugen");
    }

    @Override
    public String getTestJournal() {
        return "Kontinuität und Diskontinuität";
    }
}
