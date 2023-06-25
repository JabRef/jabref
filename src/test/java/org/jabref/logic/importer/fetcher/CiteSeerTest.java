package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@FetcherTest
class CiteSeerTest {

    private CiteSeer fetcher = new CiteSeer();

    @Test
    void searchByQueryFindsEntryRigorousDerivation() throws Exception {
        String title = "RIGOROUS DERIVATION FROM LANDAU-DE GENNES THEORY TO ERICKSEN-LESLIE THEORY";
        BibEntry expected = new BibEntry(StandardEntryType.Misc)
                .withField(StandardField.DOI, "68b3fde1aa6354a34061f8811e2050e1b512af26")
                .withField(StandardField.AUTHOR, "Wang, Wei and Zhang, Pingwen and Zhang, Zhifei")
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
                .withField(StandardField.DOI, "c16e0888b17cb2c689e5dfa4e2be4fdffb23869e")
                .withField(StandardField.AUTHOR, "Lazarus, Richard S.")
                .withField(StandardField.TITLE, "Coping theory and research: Past, present, and future")
                .withField(StandardField.ABSTRACT, "In this essay in honor of Donald Oken, I emphasize coping as a key concept for theory and research on adaptation and health. My focus will be the contrasts between two approaches to coping, one that emphasizes")
                .withField(StandardField.YEAR, "1993")
                .withField(StandardField.VENUE, "Psychosomatic Medicine")
                .withField(StandardField.URL, "http://intl.psychosomaticmedicine.org/content/55/3/234.full.pdf");

        List<BibEntry> fetchedEntries = fetcher.performSearch("title:\"Coping Theory and Research: Past Present and Future\" AND pageSize:1");
        assertEquals(Collections.singletonList(expected), fetchedEntries);
    }

    /*
    * CiteSeer seems to only apply year ranges effectively when we search for entries
    * with associated pdfs, year values do not accurately reflect realistic values
    * */
    @Disabled
    @Test
    void searchWithSortingByYear() throws FetcherException {
        Optional<String> expected = Optional.of("1552");
        List<BibEntry> fetchedEntries = fetcher.performSearch("title:Theory AND year:1552 AND sortBy:Year");
        for (BibEntry actual: fetchedEntries) {
            if (actual.hasField(StandardField.YEAR)) {
                assertEquals(expected, actual.getField(StandardField.YEAR));
            }
        }
    }

    @Test
    void searchWithSortingByYearAndYearRange() throws FetcherException {
        List<BibEntry> fetchedEntries = fetcher.performSearch("title:Theory AND year-range:2002-2012 AND sortBy:Year");
        Iterator<BibEntry> fetchedEntriesIter = fetchedEntries.iterator();
        BibEntry recentEntry = fetchedEntriesIter.next();
        while (fetchedEntriesIter.hasNext()) {
            BibEntry laterEntry = fetchedEntriesIter.next();
            if (recentEntry.hasField(StandardField.YEAR) && laterEntry.hasField(StandardField.YEAR)) {
                Integer recentYear = Integer.parseInt(recentEntry.getField(StandardField.YEAR).orElse("0"));
                Integer laterYear = Integer.parseInt(laterEntry.getField(StandardField.YEAR).orElse("0"));
                assertFalse(recentYear < laterYear);
            }
            recentEntry = laterEntry;
        }
    }

    @Test
    void findByIdAsDOI() throws FetcherException, IOException {
        BibEntry entry = new BibEntry(StandardEntryType.Misc)
                .withField(StandardField.DOI, "c16e0888b17cb2c689e5dfa4e2be4fdffb23869e");
        Optional<URL> expected = Optional.of(new URL("https://citeseerx.ist.psu.edu/document?repid=rep1&type=pdf&doi=c16e0888b17cb2c689e5dfa4e2be4fdffb23869e"));
        assertEquals(expected, fetcher.findFullText(entry));
    }

    @Test
    void findBySourceURL() throws FetcherException, IOException {
        BibEntry entry = new BibEntry(StandardEntryType.Misc)
                .withField(StandardField.DOI, "")
                .withField(StandardField.URL, "http://intl.psychosomaticmedicine.org/content/55/3/234.full.pdf");
        Optional<URL> expected = Optional.of(new URL("http://intl.psychosomaticmedicine.org/content/55/3/234.full.pdf"));
        assertEquals(expected, fetcher.findFullText(entry));
    }

    @Test
    void notFoundByIdOrURL() throws FetcherException, IOException {
        assertEquals(Optional.empty(), fetcher.findFullText(new BibEntry()));
    }
}
