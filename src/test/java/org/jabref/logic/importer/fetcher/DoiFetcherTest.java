package org.jabref.logic.importer.fetcher;

import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

@FetcherTest
public class DoiFetcherTest {

    private DoiFetcher fetcher;
    private BibEntry bibEntryBurd2011;
    private BibEntry bibEntryDecker2007;
    private BibEntry bibEntryIannarelli2019;

    @BeforeEach
    public void setUp() {
        fetcher = new DoiFetcher(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS));

        bibEntryBurd2011 = new BibEntry();
        bibEntryBurd2011.setType(StandardEntryType.Book);
        bibEntryBurd2011.setCiteKey("Burd_2011");
        bibEntryBurd2011.setField(StandardField.TITLE, "Java{\\textregistered} For Dummies{\\textregistered}");
        bibEntryBurd2011.setField(StandardField.PUBLISHER, "Wiley Publishing, Inc.");
        bibEntryBurd2011.setField(StandardField.YEAR, "2011");
        bibEntryBurd2011.setField(StandardField.AUTHOR, "Barry Burd");
        bibEntryBurd2011.setField(StandardField.MONTH, "jul");
        bibEntryBurd2011.setField(StandardField.DOI, "10.1002/9781118257517");

        bibEntryDecker2007 = new BibEntry();
        bibEntryDecker2007.setType(StandardEntryType.InProceedings);
        bibEntryDecker2007.setCiteKey("Decker_2007");
        bibEntryDecker2007.setField(StandardField.AUTHOR, "Gero Decker and Oliver Kopp and Frank Leymann and Mathias Weske");
        bibEntryDecker2007.setField(StandardField.BOOKTITLE, "{IEEE} International Conference on Web Services ({ICWS} 2007)");
        bibEntryDecker2007.setField(StandardField.MONTH, "jul");
        bibEntryDecker2007.setField(StandardField.PUBLISHER, "{IEEE}");
        bibEntryDecker2007.setField(StandardField.TITLE, "{BPEL}4Chor: Extending {BPEL} for Modeling Choreographies");
        bibEntryDecker2007.setField(StandardField.YEAR, "2007");
        bibEntryDecker2007.setField(StandardField.DOI, "10.1109/icws.2007.59");

        // mEDRA BibEntry
        bibEntryIannarelli2019 = new BibEntry();
        bibEntryIannarelli2019.setType(StandardEntryType.Article);
        bibEntryIannarelli2019.setField(StandardField.AUTHOR,
                                        ""
                                                              + "Iannarelli Riccardo  and "
                                                              + "Novello Anna  and "
                                                              + "Stricker Damien  and "
                                                              + "Cisternino Marco  and "
                                                              + "Gallizio Federico  and "
                                                              + "Telib Haysam  and "
                                                              + "Meyer Thierry ");
        bibEntryIannarelli2019.setField(StandardField.PUBLISHER, "AIDIC: Italian Association of Chemical Engineering");
        bibEntryIannarelli2019.setField(StandardField.TITLE, "Safety in research institutions: how to better communicate the risks using numerical simulations");
        bibEntryIannarelli2019.setField(StandardField.YEAR, "2019");
        bibEntryIannarelli2019.setField(StandardField.DOI, "10.3303/CET1977146");
        bibEntryIannarelli2019.setField(StandardField.JOURNAL, "Chemical Engineering Transactions");
        bibEntryIannarelli2019.setField(StandardField.PAGES, "871-876");
        bibEntryIannarelli2019.setField(StandardField.URL, "http://doi.org/10.3303/CET1977146");
        bibEntryIannarelli2019.setField(StandardField.VOLUME, "77");
    }

    @Test
    public void testGetName() {
        assertEquals("DOI", fetcher.getName());
    }

    @Test
    public void testPerformSearchBurd2011() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("10.1002/9781118257517");
        assertEquals(Optional.of(bibEntryBurd2011), fetchedEntry);
    }

    @Test
    public void testPerformSearchDecker2007() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("10.1109/ICWS.2007.59");
        assertEquals(Optional.of(bibEntryDecker2007), fetchedEntry);
    }

    @Test
    public void testPerformSearchIannarelli2019() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("10.3303/CET1977146");
        assertEquals(Optional.of(bibEntryIannarelli2019), fetchedEntry);
    }

    @Test
    public void testPerformSearchEmptyDOI() {
        assertThrows(FetcherException.class, () -> fetcher.performSearchById(""));
    }

    @Test
    public void testPerformSearchInvalidDOI() {
        assertThrows(FetcherException.class, () -> fetcher.performSearchById("10.1002/9781118257517F"));
    }

    @Test
    public void testPerformSearchNonTrimmedDOI() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("http s://doi.org/ 10.1109 /ICWS .2007.59 ");
        assertEquals(Optional.of(bibEntryDecker2007), fetchedEntry);
    }
}
