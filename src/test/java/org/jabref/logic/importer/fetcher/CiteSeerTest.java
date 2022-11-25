package org.jabref.logic.importer.fetcher;

import java.util.Collections;
import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@FetcherTest
class CiteSeerTest {

    private CiteSeer fetcher = new CiteSeer();

    @Test
    @Disabled("CiteseerX currently has issues with ncites query")
    void searchByQueryFindsEntryRigorousDerivation() throws Exception {
        BibEntry expected = new BibEntry(StandardEntryType.Misc)
                .withField(StandardField.AUTHOR, "Wang Wei and Zhang Pingwen and Zhang Zhifei")
                .withField(StandardField.TITLE, "Rigorous Derivation from Landau-de Gennes Theory to Eericksen-leslie Theory")
                .withField(StandardField.DOI, "10.1.1.744.5780");

        List<BibEntry> fetchedEntries = fetcher.performSearch("title:Ericksen-Leslie AND venue:q AND ncites:[10 TO 15000]");
        assertEquals(Collections.singletonList(expected), fetchedEntries);
    }

    @Test
    void searchByQueryFindsEntryCopingTheoryAndResearch() throws Exception {
        BibEntry expected = new BibEntry(StandardEntryType.Misc)
                .withField(StandardField.AUTHOR, "Lazarus Richard S.")
                .withField(StandardField.TITLE, "Coping Theory and Research: Past Present and Future")
                .withField(StandardField.DOI, "10.1.1.115.9665")
                .withField(StandardField.YEAR, "1993")
                .withField(StandardField.JOURNALTITLE, "PSYCHOSOMATIC MEDICINE");

        List<BibEntry> fetchedEntries = fetcher.performSearch("doi:10.1.1.115.9665");
        assertEquals(Collections.singletonList(expected), fetchedEntries);
    }
}
