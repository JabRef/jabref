package org.jabref.logic.importer.fetcher;

import java.util.Collections;
import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@FetcherTest
class CiteSeerTest {

    private CiteSeer fetcher = new CiteSeer();

    @Test
    void searchByQueryFindsEntryRigorousDerivation() throws Exception {
        String title = "RIGOROUS DERIVATION FROM LANDAU-DE GENNES THEORY TO ERICKSEN-LESLIE THEORY";
        BibEntry expected = new BibEntry(StandardEntryType.Misc)
                .withField(StandardField.AUTHOR, "Wang Wei and Zhang Pingwen and Zhang Zhifei")
                .withField(StandardField.TITLE, title)
                .withField(StandardField.ABSTRACT, "ar")
                .withField(StandardField.YEAR, "0")
                .withField(StandardField.URL, "http://arxiv.org/pdf/1307.0986.pdf");

        List<BibEntry> fetchedEntries = fetcher.performSearch("title:\"Rigorous Derivation from Landau-de Gennes Theory to Ericksen-leslie Theory\" AND pageSize:1");
        assertEquals(Collections.singletonList(expected), fetchedEntries);
    }

    @Test
    void searchByQueryFindsEntryCopingTheoryAndResearch() throws Exception {
        BibEntry expected = new BibEntry(StandardEntryType.Misc)
                .withField(StandardField.AUTHOR, "Lazarus Richard S.")
                .withField(StandardField.TITLE, "Coping theory and research: Past, present, and future")
                .withField(StandardField.ABSTRACT, "In this essay in honor of Donald Oken, I emphasize coping as a key concept for theory and research on adaptation and health. My focus will be the contrasts between two approaches to coping, one that emphasizes")
                .withField(StandardField.YEAR, "1993")
                .withField(StandardField.VENUE, "Psychosomatic Medicine")
                .withField(StandardField.URL, "http://intl.psychosomaticmedicine.org/content/55/3/234.full.pdf");

        List<BibEntry> fetchedEntries = fetcher.performSearch("title:\"Coping Theory and Research: Past Present and Future\" AND pageSize:1");
        assertEquals(Collections.singletonList(expected), fetchedEntries);
    }
}
