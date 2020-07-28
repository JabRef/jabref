package org.jabref.logic.importer.fetcher;

import java.util.Collections;
import java.util.List;

import org.jabref.logic.bibtex.FieldContentFormatterPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;
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
        when(importFormatPreferences.getFieldContentFormatterPreferences()).thenReturn(
                mock(FieldContentFormatterPreferences.class));
        fetcher = new ZbMATH(importFormatPreferences);

        donaldsonEntry = new BibEntry();
        donaldsonEntry.setType(StandardEntryType.Article);
        donaldsonEntry.setCiteKey("zbMATH03800580");
        donaldsonEntry.setField(StandardField.AUTHOR, "S. K. {Donaldson}");
        donaldsonEntry.setField(StandardField.JOURNAL, "Journal of Differential Geometry");
        donaldsonEntry.setField(StandardField.ISSN, "0022-040X; 1945-743X/e");
        donaldsonEntry.setField(StandardField.LANGUAGE, "English");
        donaldsonEntry.setField(StandardField.KEYWORDS, "57N13 57R10 53C05 58J99 57R65");
        donaldsonEntry.setField(StandardField.PAGES, "279--315");
        donaldsonEntry.setField(StandardField.PUBLISHER, "International Press of Boston, Somerville, MA");
        donaldsonEntry.setField(StandardField.TITLE, "An application of gauge theory to four dimensional topology.");
        donaldsonEntry.setField(StandardField.VOLUME, "18");
        donaldsonEntry.setField(StandardField.YEAR, "1983");
        donaldsonEntry.setField(new UnknownField("zbl"), "0507.57010");
    }

    @Test
    @DisabledOnCIServer("CI server has no subscription to zbMath and thus gets 401 response")
    void searchByQueryFindsEntry() throws Exception {
        List<BibEntry> fetchedEntries = fetcher.performSearch("an:0507.57010");
        assertEquals(Collections.singletonList(donaldsonEntry), fetchedEntries);
    }
}
