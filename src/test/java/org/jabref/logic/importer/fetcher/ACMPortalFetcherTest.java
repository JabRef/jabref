package org.jabref.logic.importer.fetcher;

import java.util.Collections;
import java.util.List;

import org.jabref.logic.bibtex.FieldContentFormatterPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
@Disabled("ACM endpoint does not work, thus the fetcher does not work at all. See https://github.com/JabRef/jabref/issues/5804 for details.")
class ACMPortalFetcherTest {
    ACMPortalFetcher fetcher;

    @BeforeEach
    void setUp() {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class);
        when(importFormatPreferences.getFieldContentFormatterPreferences()).thenReturn(mock(FieldContentFormatterPreferences.class));
        fetcher = new ACMPortalFetcher(importFormatPreferences);
    }

    @Test
    void searchByQueryFindsEntry() throws Exception {
        BibEntry expected = new BibEntry(StandardEntryType.InProceedings);
        expected.setCitationKey("Olsson:2017:RCC:3129790.3129810");
        expected.setField(new UnknownField("acmid"), "3129810");
        expected.setField(StandardField.ADDRESS, "New York, NY, USA");
        expected.setField(StandardField.AUTHOR, "Olsson, Tobias and Ericsson, Morgan and Wingkvist, Anna");
        expected.setField(StandardField.BOOKTITLE, "Proceedings of the 11th European Conference on Software Architecture: Companion Proceedings");
        expected.setField(StandardField.DOI, "10.1145/3129790.3129810");
        expected.setField(StandardField.ISBN, "978-1-4503-5217-8");
        expected.setField(StandardField.KEYWORDS, "conformance checking, repository data mining, software architecture");
        expected.setField(StandardField.LOCATION, "Canterbury, United Kingdom");
        expected.setField(new UnknownField("numpages"), "7");
        expected.setField(StandardField.PAGES, "152--158");
        expected.setField(StandardField.PUBLISHER, "ACM");
        expected.setField(StandardField.SERIES, "ECSA '17");
        expected.setField(StandardField.TITLE, "The Relationship of Code Churn and Architectural Violations in the Open Source Software JabRef");
        expected.setField(StandardField.URL, "http://doi.acm.org/10.1145/3129790.3129810");
        expected.setField(StandardField.YEAR, "2017");

        List<BibEntry> fetchedEntries = fetcher.performSearch("jabref architectural churn");
        assertEquals(Collections.singletonList(expected), fetchedEntries);
    }
}
