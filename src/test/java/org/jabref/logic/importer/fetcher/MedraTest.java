package org.jabref.logic.importer.fetcher;

import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@FetcherTest
public class MedraTest {

    private final Medra fetcher = new Medra();

    private static Stream<Arguments> getDoiBibEntryPairs() {
        return Stream.of(
                Arguments.of("10.1016/j.bjoms.2007.08.004",
                        Optional.empty()),

                Arguments.of("10.2143/TVF.80.3.3285690",
                        Optional.of(
                                new BibEntry(StandardEntryType.Article)
                                        .withField(StandardField.AUTHOR, "SPILEERS, Steven ")
                                        .withField(StandardField.PUBLISHER, "Peeters online journals")
                                        .withField(StandardField.TITLE, "Algemene kroniek")
                                        .withField(StandardField.YEAR, "2018")
                                        .withField(StandardField.DOI, "10.2143/TVF.80.3.3285690")
                                        .withField(StandardField.ISSN, "2031-8952, 2031-8952")
                                        .withField(StandardField.JOURNAL, "Tijdschrift voor Filosofie")
                                        .withField(StandardField.PAGES, "625-629")
                        )),

                Arguments.of("10.3303/CET1977146",
                        Optional.of(
                                new BibEntry(StandardEntryType.Article)
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
                                        .withField(StandardField.VOLUME, "77"))),
                Arguments.of("10.1400/115378",
                        Optional.of(
                                new BibEntry(StandardEntryType.Article)
                                        .withField(StandardField.AUTHOR, "Paola Cisternino")
                                        .withField(StandardField.PUBLISHER, "Edizioni Otto Novecento")
                                        .withField(StandardField.TITLE, "Diagramma semantico dei lemmi : casa, parola, silenzio e attesa in È fatto giorno e Margherite e rosolacci di Rocco Scotellaro")
                                        .withField(StandardField.ISSN, "03912639")
                                        .withField(StandardField.YEAR, "1999")
                                        .withField(StandardField.DOI, "10.1400/115378")
                                        .withField(StandardField.JOURNAL, "Otto/Novecento : rivista quadrimestrale di critica e storia letteraria")
                        ))
        );
    }

    @Test
    public void testGetName() {
        assertEquals("mEDRA", fetcher.getName());
    }

    @Test
    public void testPerformSearchEmptyDOI() throws FetcherException {
        assertEquals(Optional.empty(), fetcher.performSearchById(""));
    }

    @ParameterizedTest
    @MethodSource("getDoiBibEntryPairs")
    public void testDoiBibEntryPairs(String identifier, Optional<BibEntry> expected) throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById(identifier);
        assertEquals(expected, fetchedEntry);
    }

}
