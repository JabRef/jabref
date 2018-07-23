package org.jabref.logic.importer.fetcher;

import java.util.Collections;
import java.util.List;

import org.jabref.logic.bibtex.FieldContentParserPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexEntryTypes;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
class ACMPortalFetcherTest {
    ACMPortalFetcher fetcher;

    @BeforeEach
    void setUp() {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class);
        when(importFormatPreferences.getFieldContentParserPreferences()).thenReturn(mock(FieldContentParserPreferences.class));
        fetcher = new ACMPortalFetcher(importFormatPreferences);
    }

    @Test
    void searchByQueryFindsEntry() throws Exception {
        BibEntry expected = new BibEntry(BibtexEntryTypes.INPROCEEDINGS.getName());
        expected.setCiteKey("Olsson:2017:RCC:3129790.3129810");
        expected.setField("acmid", "3129810");
        expected.setField("address", "New York, NY, USA");
        expected.setField("author", "Olsson, Tobias and Ericsson, Morgan and Wingkvist, Anna");
        expected.setField("booktitle", "Proceedings of the 11th European Conference on Software Architecture: Companion Proceedings");
        expected.setField("doi", "10.1145/3129790.3129810");
        expected.setField("isbn", "978-1-4503-5217-8");
        expected.setField("keywords", "conformance checking, repository data mining, software architecture");
        expected.setField("location", "Canterbury, United Kingdom");
        expected.setField("numpages", "7");
        expected.setField("pages", "152--158");
        expected.setField("publisher", "ACM");
        expected.setField("series", "ECSA '17");
        expected.setField("title", "The Relationship of Code Churn and Architectural Violations in the Open Source Software JabRef");
        expected.setField("url", "http://doi.acm.org/10.1145/3129790.3129810");
        expected.setField("year", "2017");

        List<BibEntry> fetchedEntries = fetcher.performSearch("jabref architectural churn");
        assertEquals(Collections.singletonList(expected), fetchedEntries);
    }
}
