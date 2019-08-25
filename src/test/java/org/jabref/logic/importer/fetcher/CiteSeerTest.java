package org.jabref.logic.importer.fetcher;

import java.util.Collections;
import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@FetcherTest
class CiteSeerTest {

    CiteSeer fetcher;

    @BeforeEach
    void setUp() throws Exception {
        fetcher = new CiteSeer();
    }

    @Test
    void searchByQueryFindsEntry() throws Exception {
        BibEntry expected = new BibEntry();
        expected.setType(StandardEntryType.Misc);
        expected.setField(StandardField.AUTHOR, "Wang Wei and Zhang Pingwen and Zhang Zhifei");
        expected.setField(StandardField.TITLE, "Rigorous Derivation from Landau-de Gennes Theory to Eericksen-leslie Theory");
        expected.setField(StandardField.DOI, "10.1.1.744.5780");

        List<BibEntry> fetchedEntries = fetcher.performSearch("title:Ericksen-Leslie AND venue:q AND ncites:[10 TO 15000]");
        assertEquals(Collections.singletonList(expected), fetchedEntries);
    }

    @Test
    void searchByQueryFindsEntry2() throws Exception {
        BibEntry expected = new BibEntry();
        expected.setType(StandardEntryType.Misc);
        expected.setField(StandardField.AUTHOR, "Lazarus Richard S.");
        expected.setField(StandardField.TITLE, "Coping Theory and Research: Past Present and Future");
        expected.setField(StandardField.DOI, "10.1.1.115.9665");
        expected.setField(StandardField.YEAR, "1993");
        expected.setField(StandardField.JOURNALTITLE, "PSYCHOSOMATIC MEDICINE");

        List<BibEntry> fetchedEntries = fetcher.performSearch("JabRef");
        assertEquals(expected, fetchedEntries.get(4));
    }
}
