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
        BibEntry expected = new BibEntry(BibtexEntryTypes.MASTERSTHESIS.getName());
        expected.setCiteKey("Diez:2014ppa");
        expected.setField("author", "Diez, Tobias");
        expected.setField("title", "Slice theorem for Fr\\'echet group actions and covariant symplectic field theory");
        expected.setField("school", "Leipzig U.");
        expected.setField("year", "2013");
        expected.setField("url", "https://inspirehep.net/record/1295621/files/arXiv:1405.2249.pdf");
        expected.setField("eprint", "1405.2249");
        expected.setField("archivePrefix", "arXiv");
        expected.setField("primaryClass", "math-ph");

        List<BibEntry> fetchedEntries = fetcher.performSearch("Fr\\'echet group actions field");
        assertEquals(Collections.singletonList(expected), fetchedEntries);
    }
}
