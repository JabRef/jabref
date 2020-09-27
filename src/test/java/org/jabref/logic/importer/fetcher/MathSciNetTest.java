package org.jabref.logic.importer.fetcher;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.bibtex.FieldContentFormatterPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
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
        when(importFormatPreferences.getFieldContentFormatterPreferences()).thenReturn(
                mock(FieldContentFormatterPreferences.class));
        fetcher = new MathSciNet(importFormatPreferences);

        ratiuEntry = new BibEntry();
        ratiuEntry.setType(StandardEntryType.Article);
        ratiuEntry.setCitationKey("MR3537908");
        ratiuEntry.setField(StandardField.AUTHOR, "Chechkin, Gregory A. and Ratiu, Tudor S. and Romanov, Maxim S. and Samokhin, Vyacheslav N.");
        ratiuEntry.setField(StandardField.TITLE, "Existence and uniqueness theorems for the two-dimensional {E}ricksen-{L}eslie system");
        ratiuEntry.setField(StandardField.JOURNAL, "Journal of Mathematical Fluid Mechanics");
        ratiuEntry.setField(StandardField.VOLUME, "18");
        ratiuEntry.setField(StandardField.YEAR, "2016");
        ratiuEntry.setField(StandardField.NUMBER, "3");
        ratiuEntry.setField(StandardField.PAGES, "571--589");
        ratiuEntry.setField(StandardField.ISSN, "1422-6928");
        ratiuEntry.setField(StandardField.KEYWORDS, "76A15 (35A01 35A02 35K61 82D30)");
        ratiuEntry.setField(StandardField.MR_NUMBER, "3537908");
        ratiuEntry.setField(StandardField.DOI, "10.1007/s00021-016-0250-0");
    }

    @Test
    void searchByEntryFindsEntry() throws Exception {
        BibEntry searchEntry = new BibEntry();
        searchEntry.setField(StandardField.TITLE, "existence");
        searchEntry.setField(StandardField.AUTHOR, "Ratiu");
        searchEntry.setField(StandardField.JOURNAL, "fluid");

        List<BibEntry> fetchedEntries = fetcher.performSearch(searchEntry);
        assertEquals(Collections.singletonList(ratiuEntry), fetchedEntries);
    }

    @Test
    @DisabledOnCIServer("CI server has no subscription to MathSciNet and thus gets 401 response")
    void searchByIdInEntryFindsEntry() throws Exception {
        BibEntry searchEntry = new BibEntry();
        searchEntry.setField(StandardField.MR_NUMBER, "3537908");

        List<BibEntry> fetchedEntries = fetcher.performSearch(searchEntry);
        assertEquals(Collections.singletonList(ratiuEntry), fetchedEntries);
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
