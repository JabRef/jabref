package org.jabref.logic.importer.fetcher;

import java.util.Collections;
import java.util.List;

import org.jabref.logic.bibtex.FieldContentParserPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
class INSPIREFetcherTest {
    private INSPIREFetcher fetcher;

    @BeforeEach
    void setUp() {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class);
        when(importFormatPreferences.getFieldContentParserPreferences()).thenReturn(mock(FieldContentParserPreferences.class));
        fetcher = new INSPIREFetcher(importFormatPreferences);
    }

    @Test
    void searchByQueryFindsEntry() throws Exception {
        BibEntry expected = new BibEntry(StandardEntryType.MastersThesis);
        expected.setCiteKey("Diez:2014ppa");
        expected.setField(StandardField.AUTHOR, "Diez, Tobias");
        expected.setField(StandardField.TITLE, "Slice theorem for Fr\\'echet group actions and covariant symplectic field theory");
        expected.setField(StandardField.SCHOOL, "Leipzig U.");
        expected.setField(StandardField.YEAR, "2013");
        expected.setField(StandardField.URL, "https://inspirehep.net/record/1295621/files/arXiv:1405.2249.pdf");
        expected.setField(StandardField.EPRINT, "1405.2249");
        expected.setField(StandardField.ARCHIVEPREFIX, "arXiv");
        expected.setField(new UnknownField("primaryClass"), "math-ph");

        List<BibEntry> fetchedEntries = fetcher.performSearch("Fr\\'echet group actions field");
        assertEquals(Collections.singletonList(expected), fetchedEntries);
    }
}
