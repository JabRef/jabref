package net.sf.jabref.logic.importer.fetcher;

import java.util.Collections;
import java.util.List;

import net.sf.jabref.logic.bibtex.FieldContentParserPreferences;
import net.sf.jabref.logic.importer.ImportFormatPreferences;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexEntryTypes;
import net.sf.jabref.support.DevEnvironment;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class zbMATHTest {
    private zbMATH fetcher;
    private BibEntry donaldsonEntry;

    @Before
    public void setUp() throws Exception {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class);
        when(importFormatPreferences.getFieldContentParserPreferences()).thenReturn(
                mock(FieldContentParserPreferences.class));
        fetcher = new zbMATH(importFormatPreferences);

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
    public void searchByQueryFindsEntry() throws Exception {
        // CI has no subscription to zbMath and thus gets 401 response
        Assume.assumeFalse(DevEnvironment.isCIServer());

        List<BibEntry> fetchedEntries = fetcher.performSearch("an:0507.57010");
        assertEquals(Collections.singletonList(donaldsonEntry), fetchedEntries);
    }
}
