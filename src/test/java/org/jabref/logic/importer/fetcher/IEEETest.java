package org.jabref.logic.importer.fetcher;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.PagedSearchBasedFetcher;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
class IEEETest implements SearchBasedFetcherCapabilityTest, PagedSearchFetcherTest {

    private final BibEntry IGOR_NEWCOMERS = new BibEntry(StandardEntryType.InProceedings)
            .withField(StandardField.AUTHOR, "Igor Steinmacher and Tayana Uchoa Conte and Christoph Treude and Marco Aurélio Gerosa")
            .withField(StandardField.DATE, "14-22 May 2016")
            .withField(StandardField.YEAR, "2016")
            .withField(StandardField.EVENTDATE, "14-22 May 2016")
            .withField(StandardField.EVENTTITLEADDON, "Austin, TX, USA")
            .withField(StandardField.LOCATION, "Austin, TX, USA")
            .withField(StandardField.DOI, "10.1145/2884781.2884806")
            .withField(StandardField.JOURNALTITLE, "2016 IEEE/ACM 38th International Conference on Software Engineering (ICSE)")
            .withField(StandardField.PAGES, "273--284")
            .withField(StandardField.ISBN, "978-1-5090-2071-3")
            .withField(StandardField.ISSN, "1558-1225")
            .withField(StandardField.PUBLISHER, "IEEE")
            .withField(StandardField.KEYWORDS, "Portals, Documentation, Computer bugs, Joining processes, Industries, Open source software, Newcomers, Newbies, Novices, Beginners, Open Source Software, Barriers, Obstacles, Onboarding, Joining Process")
            .withField(StandardField.TITLE, "Overcoming Open Source Project Entry Barriers with a Portal for Newcomers")
            .withField(StandardField.FILE, ":https\\://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=7886910:PDF");

    private IEEE fetcher;
    private BibEntry entry;

    @BeforeEach
    void setUp() {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class);
        when(importFormatPreferences.getKeywordSeparator()).thenReturn(',');
        fetcher = new IEEE(importFormatPreferences);
        entry = new BibEntry();
    }

    @Test
    void findByDOI() throws Exception {
        entry.setField(StandardField.DOI, "10.1109/ACCESS.2016.2535486");
        assertEquals(Optional.of(new URL("https://ieeexplore.ieee.org/ielx7/6287639/7419931/07421926.pdf?tp=&arnumber=7421926&isnumber=7419931&ref=")),
                fetcher.findFullText(entry));
    }

    @Test
    void findByDocumentUrl() throws Exception {
        entry.setField(StandardField.URL, "https://ieeexplore.ieee.org/document/7421926/");
        assertEquals(Optional.of(new URL("https://ieeexplore.ieee.org/ielx7/6287639/7419931/07421926.pdf?tp=&arnumber=7421926&isnumber=7419931&ref=")),
                fetcher.findFullText(entry));
    }

    @Test
    void findByURL() throws Exception {
        entry.setField(StandardField.URL, "https://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=7421926&ref=");
        assertEquals(Optional.of(new URL("https://ieeexplore.ieee.org/ielx7/6287639/7419931/07421926.pdf?tp=&arnumber=7421926&isnumber=7419931&ref=")),
                fetcher.findFullText(entry));
    }

    @Test
    void findByOldURL() throws Exception {
        entry.setField(StandardField.URL, "https://ieeexplore.ieee.org/stamp/stamp.jsp?arnumber=7421926");
        assertEquals(Optional.of(new URL("https://ieeexplore.ieee.org/ielx7/6287639/7419931/07421926.pdf?tp=&arnumber=7421926&isnumber=7419931&ref=")),
                fetcher.findFullText(entry));
    }

    @Test
    void findByDOIButNotURL() throws Exception {
        entry.setField(StandardField.DOI, "10.1109/ACCESS.2016.2535486");
        entry.setField(StandardField.URL, "http://dx.doi.org/10.1109/ACCESS.2016.2535486");
        assertEquals(Optional.of(new URL("https://ieeexplore.ieee.org/ielx7/6287639/7419931/07421926.pdf?tp=&arnumber=7421926&isnumber=7419931&ref=")),
                fetcher.findFullText(entry));
    }

    @Test
    void notFoundByURL() throws Exception {
        entry.setField(StandardField.URL, "http://dx.doi.org/10.1109/ACCESS.2016.2535486");
        assertEquals(Optional.empty(), fetcher.findFullText(entry));
    }

    @Test
    void notFoundByDOI() throws Exception {
        entry.setField(StandardField.DOI, "10.1021/bk-2006-WWW.ch014");
        assertEquals(Optional.empty(), fetcher.findFullText(entry));
    }

    @Test
    void searchResultHasNoKeywordTerms() throws Exception {
        BibEntry expected = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Shatakshi Sharma and Bhim Singh and Sukumar Mishra")
                .withField(StandardField.DATE, "April 2020")
                .withField(StandardField.YEAR, "2020")
                .withField(StandardField.DOI, "10.1109/TII.2019.2935531")
                .withField(StandardField.FILE, ":https\\://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=8801912:PDF")
                .withField(StandardField.ISSUE, "4")
                .withField(StandardField.ISSN, "1941-0050")
                .withField(StandardField.JOURNALTITLE, "IEEE Transactions on Industrial Informatics")
                .withField(StandardField.PAGES, "2346--2356")
                .withField(StandardField.PUBLISHER, "IEEE")
                .withField(StandardField.TITLE, "Economic Operation and Quality Control in PV-BES-DG-Based Autonomous System")
                .withField(StandardField.VOLUME, "16")
                .withField(StandardField.KEYWORDS, "Batteries, Generators, Economics, Power quality, State of charge, Harmonic analysis, Control systems, Battery, diesel generator (DG), distributed generation, power quality, photovoltaic (PV), voltage source converter (VSC)");

        List<BibEntry> fetchedEntries = fetcher.performSearch("article_number:8801912"); // article number
        fetchedEntries.forEach(entry -> entry.clearField(StandardField.ABSTRACT)); // Remove abstract due to copyright);
        assertEquals(Collections.singletonList(expected), fetchedEntries);
    }

    @Test
    void searchByPlainQueryFindsEntry() throws Exception {
        List<BibEntry> fetchedEntries = fetcher.performSearch("Overcoming Open Source Project Entry Barriers with a Portal for Newcomers");
        // Abstract should not be included in JabRef tests
        fetchedEntries.forEach(entry -> entry.clearField(StandardField.ABSTRACT));
        assertEquals(Collections.singletonList(IGOR_NEWCOMERS), fetchedEntries);
    }

    @Test
    void searchByQuotedQueryFindsEntry() throws Exception {
        List<BibEntry> fetchedEntries = fetcher.performSearch("\"Overcoming Open Source Project Entry Barriers with a Portal for Newcomers\"");
        // Abstract should not be included in JabRef tests
        fetchedEntries.forEach(entry -> entry.clearField(StandardField.ABSTRACT));
        assertEquals(Collections.singletonList(IGOR_NEWCOMERS), fetchedEntries);
    }

    @Override
    public SearchBasedFetcher getFetcher() {
        return fetcher;
    }

    @Override
    public List<String> getTestAuthors() {
        return List.of("Igor Steinmacher", "Tayana Uchoa Conte", "Christoph Treude", "Marco Aurélio Gerosa");
    }

    @Override
    public String getTestJournal() {
        return "IET Renewable Power Generation";
    }

    @Override
    public PagedSearchBasedFetcher getPagedFetcher() {
        return fetcher;
    }
}
