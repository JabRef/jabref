package org.jabref.logic.importer.fetcher;

import java.util.Collections;
import java.util.List;

import org.jabref.logic.bibtex.FieldContentParserPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexEntryTypes;
import org.jabref.support.DisabledOnCIServer;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
class ZbMATHTest {
    private ZbMATH fetcher;
    private BibEntry donaldsonEntry;

    @BeforeEach
    void setUp() throws Exception {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class);
        when(importFormatPreferences.getFieldContentParserPreferences()).thenReturn(
                mock(FieldContentParserPreferences.class));
        fetcher = new ZbMATH(importFormatPreferences);

        donaldsonEntry = new BibEntry();
        donaldsonEntry.setType(BibtexEntryTypes.ARTICLE);
        donaldsonEntry.setCiteKey("zbMATH03800580");
        donaldsonEntry.setField("author", "S.K. {Donaldson}");
        donaldsonEntry.setField("journal", "Journal of Differential Geometry");
        donaldsonEntry.setField("issn", "0022-040X; 1945-743X/e");
        donaldsonEntry.setField("language", "English");
        donaldsonEntry.setField("keywords", "57N13 57R10 53C05 58J99 57R65");
        donaldsonEntry.setField("pages", "279--315");
        donaldsonEntry.setField("publisher", "International Press of Boston, Somerville, MA");
        donaldsonEntry.setField("title", "An application of gauge theory to four dimensional topology.");
        donaldsonEntry.setField("volume", "18");
        donaldsonEntry.setField("year", "1983");
        donaldsonEntry.setField("zbl", "0507.57010");
    }

    @Test
    @DisabledOnCIServer("CI server has no subscription to zbMath and thus gets 401 response")
    void searchByQueryFindsEntry() throws Exception {
        List<BibEntry> fetchedEntries = fetcher.performSearch("an:0507.57010");
        assertEquals(Collections.singletonList(donaldsonEntry), fetchedEntries);
    }
}
