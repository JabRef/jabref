package org.jabref.logic.importer.fetcher;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.bibtex.FieldContentParserPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexEntryTypes;
import org.jabref.support.DisabledOnCIServer;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
class MathSciNetTest {

    MathSciNet fetcher;
    private BibEntry ratiuEntry;

    @BeforeEach
    void setUp() throws Exception {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class);
        when(importFormatPreferences.getFieldContentParserPreferences()).thenReturn(
                mock(FieldContentParserPreferences.class));
        fetcher = new MathSciNet(importFormatPreferences);

        ratiuEntry = new BibEntry();
        ratiuEntry.setType(BibtexEntryTypes.ARTICLE);
        ratiuEntry.setCiteKey("MR3537908");
        ratiuEntry.setField("author", "Chechkin, Gregory A. and Ratiu, Tudor S. and Romanov, Maxim S. and Samokhin, Vyacheslav N.");
        ratiuEntry.setField("title", "Existence and uniqueness theorems for the two-dimensional {E}ricksen-{L}eslie system");
        ratiuEntry.setField("journal", "Journal of Mathematical Fluid Mechanics");
        ratiuEntry.setField("volume", "18");
        ratiuEntry.setField("year", "2016");
        ratiuEntry.setField("number", "3");
        ratiuEntry.setField("pages", "571--589");
        ratiuEntry.setField("issn", "1422-6928");
        ratiuEntry.setField("keywords", "76A15 (35A01 35A02 35K61 82D30)");
        ratiuEntry.setField("mrnumber", "3537908");
        ratiuEntry.setField("doi", "10.1007/s00021-016-0250-0");
    }

    @Test
    void searchByEntryFindsEntry() throws Exception {
        BibEntry searchEntry = new BibEntry();
        searchEntry.setField("title", "existence");
        searchEntry.setField("author", "Ratiu");
        searchEntry.setField("journal", "fluid");

        List<BibEntry> fetchedEntries = fetcher.performSearch(searchEntry);
        assertFalse(fetchedEntries.isEmpty());
        assertEquals(ratiuEntry, fetchedEntries.get(0));
    }

    @Test
    @DisabledOnCIServer("CI server has no subscription to MathSciNet and thus gets 401 response")
    void searchByQueryFindsEntry() throws Exception {
        List<BibEntry> fetchedEntries = fetcher.performSearch("Existence and uniqueness theorems Two-Dimensional Ericksen Leslie System");
        assertFalse(fetchedEntries.isEmpty());
        assertEquals(ratiuEntry, fetchedEntries.get(1));
    }

    @Test
    @DisabledOnCIServer("CI server has no subscription to MathSciNet and thus gets 401 response")
    void searchByIdFindsEntry() throws Exception {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("3537908");
        assertEquals(Optional.of(ratiuEntry), fetchedEntry);
    }
}
