package org.jabref.logic.importer.fetcher;

import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

@FetcherTest
public class DoiFetcherTest {

    private final DoiFetcher fetcher = new DoiFetcher(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS));

    private final BibEntry bibEntryBurd2011 = new BibEntry(StandardEntryType.Book)
            .withCitationKey("Burd_2011")
            .withField(StandardField.TITLE, "Java® For Dummies®")
            .withField(StandardField.PUBLISHER, "Wiley")
            .withField(StandardField.YEAR, "2011")
            .withField(StandardField.AUTHOR, "Burd, Barry")
            .withField(StandardField.MONTH, "#jul#")
            .withField(StandardField.DOI, "10.1002/9781118257517")
            .withField(StandardField.ISBN, "9781118257517");
    private final BibEntry bibEntryDecker2007 = new BibEntry(StandardEntryType.InProceedings)
            .withCitationKey("Decker_2007")
            .withField(StandardField.AUTHOR, "Decker, Gero and Kopp, Oliver and Leymann, Frank and Weske, Mathias")
            .withField(StandardField.BOOKTITLE, "IEEE International Conference on Web Services (ICWS 2007)")
            .withField(StandardField.MONTH, "#jul#")
            .withField(StandardField.PUBLISHER, "IEEE")
            .withField(StandardField.TITLE, "BPEL4Chor: Extending BPEL for Modeling Choreographies")
            .withField(StandardField.YEAR, "2007")
            .withField(StandardField.DOI, "10.1109/icws.2007.59");
    private final BibEntry bibEntryIannarelli2019 = new BibEntry(StandardEntryType.Article)
            .withField(StandardField.AUTHOR,
                    ""
                            + "Iannarelli Riccardo  and "
                            + "Novello Anna  and "
                            + "Stricker Damien  and "
                            + "Cisternino Marco  and "
                            + "Gallizio Federico  and "
                            + "Telib Haysam  and "
                            + "Meyer Thierry ")
            .withField(StandardField.PUBLISHER, "AIDIC: Italian Association of Chemical Engineering")
            .withField(StandardField.TITLE, "Safety in research institutions: how to better communicate the risks using numerical simulations")
            .withField(StandardField.YEAR, "2019")
            .withField(StandardField.DOI, "10.3303/CET1977146")
            .withField(StandardField.JOURNAL, "Chemical Engineering Transactions")
            .withField(StandardField.PAGES, "871-876")
            .withField(StandardField.VOLUME, "77");
    private final BibEntry bibEntryStenzel2020 = new BibEntry(StandardEntryType.Article)
            .withCitationKey("Stenzel_2020")
            .withField(StandardField.AUTHOR, "Stenzel, L. and Hayward, A. L. C. and Schollwöck, U. and Heidrich-Meisner, F.")
            .withField(StandardField.JOURNAL, "Physical Review A")
            .withField(StandardField.TITLE, "Topological phases in the Fermi-Hofstadter-Hubbard model on hybrid-space ladders")
            .withField(StandardField.YEAR, "2020")
            .withField(StandardField.MONTH, "#aug#")
            .withField(StandardField.VOLUME, "102")
            .withField(StandardField.DOI, "10.1103/physreva.102.023315")
            .withField(StandardField.ISSN, "2469-9934")
            .withField(StandardField.PUBLISHER, "American Physical Society (APS)")
            .withField(StandardField.PAGES, "023315")
            .withField(StandardField.NUMBER, "2");

    @Test
    public void getName() {
        assertEquals("DOI", fetcher.getName());
    }

    @Test
    public void performSearchBurd2011() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("10.1002/9781118257517");
        assertEquals(Optional.of(bibEntryBurd2011), fetchedEntry);
    }

    @Test
    public void performSearchDecker2007() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("10.1109/ICWS.2007.59");
        assertEquals(Optional.of(bibEntryDecker2007), fetchedEntry);
    }

    @Test
    public void performSearchIannarelli2019() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("10.3303/CET1977146");
        assertEquals(Optional.of(bibEntryIannarelli2019), fetchedEntry);
    }

    @Test
    public void performSearchEmptyDOI() {
        assertThrows(FetcherException.class, () -> fetcher.performSearchById(""));
    }

    @Test
    public void performSearchInvalidDOI() {
        assertThrows(FetcherException.class, () -> fetcher.performSearchById("10.1002/9781118257517F"));
    }

    @Test
    public void performSearchInvalidDOIClientResultsinFetcherClientException() {
        assertThrows(FetcherException.class, () -> fetcher.performSearchById("10.1002/9781118257517F"));
    }

    @Test
    public void performSearchInvalidDOIClientResultsinFetcherClientException2() {
        assertThrows(FetcherException.class, () -> fetcher.performSearchById("10.1002/9781517F"));
    }

    @Test
    public void performSearchNonTrimmedDOI() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("http s://doi.org/ 10.1109 /ICWS .2007.59 ");
        assertEquals(Optional.of(bibEntryDecker2007), fetchedEntry);
    }

    @Test
    public void aPSJournalCopiesArticleIdToPageField() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("10.1103/physreva.102.023315");
        assertEquals(Optional.of(bibEntryStenzel2020), fetchedEntry);
    }
}
