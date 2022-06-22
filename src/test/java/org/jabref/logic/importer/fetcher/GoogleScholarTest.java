package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.bibtex.FieldContentFormatterPreferences;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.PagedSearchBasedFetcher;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.support.DisabledOnCIServer;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
@DisabledOnCIServer("CI server is blocked by Google")
class GoogleScholarTest implements SearchBasedFetcherCapabilityTest, PagedSearchFetcherTest {

    private GoogleScholar finder;
    private BibEntry entry;

    @BeforeEach
    void setUp() {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class);
        when(importFormatPreferences.getFieldContentFormatterPreferences()).thenReturn(
                mock(FieldContentFormatterPreferences.class));
        finder = new GoogleScholar(importFormatPreferences);
        entry = new BibEntry();
    }

    @Test
    void linkFound() throws IOException, FetcherException {
        entry.setField(StandardField.TITLE, "Towards Application Portability in Platform as a Service");

        assertEquals(
                Optional.of(new URL("https://www.uni-bamberg.de/fileadmin/uni/fakultaeten/wiai_lehrstuehle/praktische_informatik/Dateien/Publikationen/sose14-towards-application-portability-in-paas.pdf")),
                finder.findFullText(entry)
        );
    }

    @Test
    void noLinkFound() throws IOException, FetcherException {
        entry.setField(StandardField.TITLE, "Curriculum programme of career-oriented java specialty guided by principles of software engineering");

        assertEquals(Optional.empty(), finder.findFullText(entry));
    }

    @Test
    void findSingleEntry() throws FetcherException {
        entry.setType(StandardEntryType.InProceedings);
        entry.setCitationKey("geiger2013detecting");
        entry.setField(StandardField.TITLE, "Detecting Interoperability and Correctness Issues in BPMN 2.0 Process Models.");
        entry.setField(StandardField.AUTHOR, "Geiger, Matthias and Wirtz, Guido");
        entry.setField(StandardField.BOOKTITLE, "ZEUS");
        entry.setField(StandardField.YEAR, "2013");
        entry.setField(StandardField.PAGES, "41--44");

        List<BibEntry> foundEntries = finder.performSearch("Detecting Interoperability and Correctness Issues in BPMN 2.0 Process Models");

        assertEquals(Collections.singletonList(entry), foundEntries);
    }

    @Test
    void findManyEntries() throws FetcherException {
        List<BibEntry> foundEntries = finder.performSearch("random test string");

        assertEquals(20, foundEntries.size());
    }

    @Override
    public SearchBasedFetcher getFetcher() {
        return finder;
    }

    @Override
    public PagedSearchBasedFetcher getPagedFetcher() {
        return finder;
    }

    @Override
    public List<String> getTestAuthors() {
        return List.of("Mittermeier", "Myers");
    }

    @Override
    public String getTestJournal() {
        return "Nature";
    }
}
