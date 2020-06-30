package org.jabref.logic.importer.fetcher;

import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MedraTest {

    private Medra fetcher;
    private BibEntry bibEntrySpileers2018;
    private BibEntry bibEntryIannarelli2019;
    private BibEntry bibEntryCisternino1999;

    @BeforeEach
    public void setUp() {
        fetcher = new Medra();

        bibEntrySpileers2018 = new BibEntry();
        bibEntrySpileers2018.setType(StandardEntryType.Article);
        bibEntrySpileers2018.setField(StandardField.AUTHOR, "SPILEERS, Steven ");
        bibEntrySpileers2018.setField(StandardField.PUBLISHER, "Peeters online journals");
        bibEntrySpileers2018.setField(StandardField.TITLE, "Algemene kroniek");
        bibEntrySpileers2018.setField(StandardField.YEAR, "2018");
        bibEntrySpileers2018.setField(StandardField.DOI, "10.2143/TVF.80.3.3285690");
        bibEntrySpileers2018.setField(StandardField.ISSN, "2031-8952");
        bibEntrySpileers2018.setField(StandardField.JOURNAL, "Tijdschrift voor Filosofie");
        bibEntrySpileers2018.setField(StandardField.PAGES, "625-629");
        bibEntrySpileers2018.setField(StandardField.URL, "http://doi.org/10.2143/TVF.80.3.3285690");

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

        bibEntryCisternino1999 = new BibEntry();
        bibEntryCisternino1999.setType(StandardEntryType.Article);
        bibEntryCisternino1999.setField(StandardField.AUTHOR, "Cisternino Paola ");
        bibEntryCisternino1999.setField(StandardField.PUBLISHER, "Edizioni Otto Novecento");
        bibEntryCisternino1999.setField(StandardField.TITLE, "Diagramma semantico dei lemmi : casa, parola, silenzio e attesa in È fatto giorno e Margherite e rosolacci di Rocco Scotellaro");
        bibEntryCisternino1999.setField(StandardField.YEAR, "1999");
        bibEntryCisternino1999.setField(StandardField.DOI, "10.1400/115378");
        bibEntryCisternino1999.setField(StandardField.JOURNAL, "Otto/Novecento : rivista quadrimestrale di critica e storia letteraria");
        bibEntryCisternino1999.setField(StandardField.URL, "http://doi.org/10.1400/115378");
    }

    @Test
    public void testGetName() {
        assertEquals("mEDRA", fetcher.getName());
    }

    @Test
    public void testPerformSearchSpileers2018() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("10.2143/TVF.80.3.3285690");
        assertEquals(Optional.of(bibEntrySpileers2018), fetchedEntry);
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

}
