package org.jabref.logic.importer.fetcher;

import java.util.List;
import java.util.stream.Stream;

import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.support.DisabledOnCIServer;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@FetcherTest
@DisabledOnCIServer("Disable on CI Server to not hit the API call limit")
public class DOABFetcherTest {
    private DOABFetcher fetcher = new DOABFetcher();

    @Test
    public void testGetName() {
        assertEquals("DOAB", fetcher.getName());
    }

    public static Stream<Arguments> testPerformSearch() {
        return Stream.of(
                Arguments.of(
                        new BibEntry(StandardEntryType.Book)
                                .withField(StandardField.AUTHOR, "David Pol")
                                .withField(StandardField.TITLE, "I Open Fire")
                                .withField(StandardField.DOI, "10.21983/P3.0086.1.00")
                                .withField(StandardField.PAGES, "56")
                                .withField(StandardField.YEAR, "2014")
                                .withField(StandardField.URL, "http://library.oapen.org/handle/20.500.12657/25535")
                                .withField(StandardField.URI, "https://directory.doabooks.org/handle/20.500.12854/34739")
                                .withField(StandardField.LANGUAGE, "English")
                                .withField(StandardField.KEYWORDS, "poetry, love, warfare")
                                .withField(StandardField.PUBLISHER, "punctum books"),
                        "i open fire"
                ),
                Arguments.of(
                        new BibEntry(StandardEntryType.Book)
                                .withField(StandardField.AUTHOR, "Ronald Snijder")
                                .withField(StandardField.TITLE, "The deliverance of open access books")
                                .withField(StandardField.SUBTITLE, "Examining usage and dissemination")
                                .withField(StandardField.DOI, "10.26530/OAPEN_1004809")
                                .withField(StandardField.PAGES, "234")
                                .withField(StandardField.YEAR, "2019")
                                .withField(StandardField.URL, "http://library.oapen.org/handle/20.500.12657/25287")
                                .withField(StandardField.URI, "https://directory.doabooks.org/handle/20.500.12854/26303")
                                .withField(StandardField.LANGUAGE, "English")
                                .withField(StandardField.KEYWORDS, "Open Access, Monographs, OAPEN Library, " +
                                        "Directory of Open Access Books")
                                .withField(StandardField.PUBLISHER, "Amsterdam University Press"),
                        "the deliverance of open access books"
                ),
                Arguments.of(
                        new BibEntry(StandardEntryType.Book)
                                .withField(StandardField.EDITOR, "Andrew Perrin and Loren T. Stuckenbruck")
                                .withField(StandardField.TITLE, "Four Kingdom Motifs before and beyond the Book of Daniel")
                                .withField(StandardField.DOI, "10.1163/9789004443280")
                                .withField(StandardField.PAGES, "354")
                                .withField(StandardField.YEAR, "2020")
                                .withField(StandardField.URL, "https://library.oapen.org/handle/20.500.12657/48312")
                                .withField(StandardField.URI, "https://directory.doabooks.org/handle/20.500.12854/68086")
                                .withField(StandardField.LANGUAGE, "English")
                                .withField(StandardField.KEYWORDS, "Religion")
                                .withField(StandardField.PUBLISHER, "Brill"),
                        "Four Kingdom Motifs before and beyond the Book of Daniel"
                ),
                Arguments.of(
                        new BibEntry(StandardEntryType.Book)
                                .withField(StandardField.EDITOR, "Felipe Gonzalez Toro and Antonios Tsourdos")
                                .withField(StandardField.TITLE, "UAV Sensors for Environmental Monitoring")
                                .withField(StandardField.DOI, "10.3390/books978-3-03842-754-4")
                                .withField(StandardField.PAGES, "670")
                                .withField(StandardField.YEAR, "2018")
                                .withField(StandardField.URI, "https://directory.doabooks.org/handle/20.500.12854/39793")
                                .withField(StandardField.LANGUAGE, "English")
                                .withField(StandardField.KEYWORDS, "UAV sensors, Environmental Monitoring, drones, unmanned aerial vehicles")
                                .withField(StandardField.PUBLISHER, "MDPI - Multidisciplinary Digital Publishing Institute"),
                        "UAV Sensors for Environmental Monitoring"
                ),
                Arguments.of(
                        new BibEntry(StandardEntryType.Book)
                                .withField(StandardField.AUTHOR, "Carl Marnewick and Wikus Erasmus and Joseph Nazeer")
                                .withField(StandardField.TITLE, "The symbiosis between information system project complexity and information system project success")
                                .withField(StandardField.DOI, "10.4102/aosis.2017.itpsc45")
                                .withField(StandardField.PAGES, "184")
                                .withField(StandardField.YEAR, "2017")
                                .withField(StandardField.URL, "http://library.oapen.org/handle/20.500.12657/30652")
                                .withField(StandardField.URI, "https://directory.doabooks.org/handle/20.500.12854/38792")
                                .withField(StandardField.LANGUAGE, "English")
                                .withField(StandardField.KEYWORDS, "agile, structural equation modelling, information technology, success, models, strategic alignment, complexity, waterfall, project management, quantitative, Agile software development, Change management, Deliverable, Exploratory factor analysis, South Africa")
                                .withField(StandardField.PUBLISHER, "AOSIS"),
                        "The symbiosis between information system project complexity and information system project success"
                )
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testPerformSearch(BibEntry expected, String query) throws FetcherException {
        List<BibEntry> entries = fetcher.performSearch(query);
        // We must not contain abstracts in our code base; thus we remove the abstracts from the fetched results
        entries.stream().forEach(entry -> entry.clearField(StandardField.ABSTRACT));
        assertEquals(List.of(expected), entries);
    }
}
