package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.bibtex.FieldContentParserPreferences;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexEntryTypes;
import org.jabref.model.entry.FieldName;
import org.jabref.support.DisabledOnCIServer;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
class GoogleScholarTest {

    private GoogleScholar finder;
    private BibEntry entry;

    @BeforeEach
    void setUp() {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class);
        when(importFormatPreferences.getFieldContentParserPreferences()).thenReturn(
                mock(FieldContentParserPreferences.class));
        finder = new GoogleScholar(importFormatPreferences);
        entry = new BibEntry();
    }

    @Test
    @DisabledOnCIServer("CI server is blocked by Google")
    void linkFound() throws IOException, FetcherException {
        entry.setField("title", "Towards Application Portability in Platform as a Service");

        assertEquals(
                Optional.of(new URL("https://www.uni-bamberg.de/fileadmin/uni/fakultaeten/wiai_lehrstuehle/praktische_informatik/Dateien/Publikationen/sose14-towards-application-portability-in-paas.pdf")),
                finder.findFullText(entry)
        );
    }

    @Test
    @DisabledOnCIServer("CI server is blocked by Google")
    void noLinkFound() throws IOException, FetcherException {
        entry.setField("title", "Pro WF: Windows Workflow in NET 3.5");

        assertEquals(Optional.empty(), finder.findFullText(entry));
    }

    @Test
    @DisabledOnCIServer("CI server is blocked by Google")
    void findSingleEntry() throws FetcherException {
        entry.setType(BibtexEntryTypes.INPROCEEDINGS.getName());
        entry.setCiteKey("geiger2013detecting");
        entry.setField(FieldName.TITLE, "Detecting Interoperability and Correctness Issues in BPMN 2.0 Process Models.");
        entry.setField(FieldName.AUTHOR, "Geiger, Matthias and Wirtz, Guido");
        entry.setField(FieldName.BOOKTITLE, "ZEUS");
        entry.setField(FieldName.YEAR, "2013");
        entry.setField(FieldName.PAGES, "41--44");

        List<BibEntry> foundEntries = finder.performSearch("Detecting Interoperability and Correctness Issues in BPMN 2.0 Process Models");

        assertEquals(Collections.singletonList(entry), foundEntries);
    }

    @Test
    @DisabledOnCIServer("CI server is blocked by Google")
    void find20Entries() throws FetcherException {
        List<BibEntry> foundEntries = finder.performSearch("random test string");

        assertEquals(20, foundEntries.size());
    }
}
