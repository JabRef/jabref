package org.jabref.logic.importer.fetcher;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.net.ssl.TrustStoreManager;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
class ZbMATHTest {
    @TempDir
    static Path tempDir;

    private ZbMATH fetcher;
    private BibEntry donaldsonEntry;
    private BibEntry gareyJohnsonBookEntry;
    private BibEntry blackScholesCollectionEntry;

    @BeforeAll
    static void configureTrustStore() throws IOException {
        TrustStoreManager.createTruststoreFileIfNotExist(tempDir.resolve("truststore.jks"));
    }

    @BeforeEach
    void setUp() {
        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');

        fetcher = new ZbMATH(importFormatPreferences);

        donaldsonEntry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Donaldson, S. K.")
                .withField(StandardField.JOURNAL, "Journal of Differential Geometry")
                .withField(StandardField.DOI, "10.4310/jdg/1214437665")
                .withField(StandardField.ISSN, "0022-040X")
                .withField(StandardField.LANGUAGE, "English")
                .withField(StandardField.KEYWORDS, "57N13,57R10,53C05,58J99,57R65")
                .withField(StandardField.PAGES, "279--315")
                .withField(StandardField.TITLE, "An application of gauge theory to four dimensional topology")
                .withField(StandardField.VOLUME, "18")
                .withField(StandardField.YEAR, "1983")
                .withField(StandardField.ZBL_NUMBER, "0507.57010")
                .withField(new UnknownField("zbmath"), "3800580");

        gareyJohnsonBookEntry = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.AUTHOR, "Garey, Michael R. and Johnson, David S.")
                .withField(StandardField.LANGUAGE, "English")
                .withField(StandardField.KEYWORDS, "68Q25,68-02,03D15,68R10,94C15,94C30,05A17,68P20,68M20,90C99,90B35,91A99,68Q45,68T99,68N99")
                .withField(StandardField.TITLE, "Computers and intractability. A guide to the theory of NP-completeness")
                .withField(StandardField.YEAR, "1979")
                .withField(StandardField.ZBL_NUMBER, "0411.68039")
                .withField(new UnknownField("zbmath"), "3639144");

        blackScholesCollectionEntry = new BibEntry(StandardEntryType.InCollection)
                .withField(StandardField.AUTHOR, "Black, Fischer and Scholes, Myron")
                .withField(StandardField.LANGUAGE, "English")
                .withField(StandardField.KEYWORDS, "91G20,91G50")
                .withField(StandardField.PAGES, "40--57")
                .withField(StandardField.TITLE, "The pricing of options and corporate liabilities")
                .withField(StandardField.BOOKTITLE, "Semmler, Willi (ed.) et al., The foundations of credit risk analysis. The International Library of Critical Writings in Econometrics 211. Cheltenham: Edward Elgar Publishing. 40-57 (2007).")
                .withField(StandardField.PUBLISHER, "Cheltenham: Edward Elgar Publishing")
                .withField(StandardField.ISBN, "978-1-84720-148-5")
                .withField(StandardField.YEAR, "2007")
                .withField(StandardField.ZBL_NUMBER, "1418.91504")
                .withField(new UnknownField("zbmath"), "7088115");
    }

    @Test
    void getURLForEntryUsesDocumentSearchApi() throws Exception {
        URL urlForEntry = fetcher.getURLForEntry(getDonaldsonSearchEntry());

        String expectedUrl = "https://api.zbmath.org/v1/document/_search"
                + "?search_string=ti%3A%22An%20application%20of%20gauge%20theory"
                + "%20to%20four%20dimensional%20topology%22%20%26%20au%3ADonaldson"
                + "&page=0&results_per_page=1";
        assertEquals(expectedUrl, urlForEntry.toString());
    }

    @Test
    void searchByQueryFindsEntry() throws FetcherException {
        List<BibEntry> fetchedEntries = fetcher.performSearch("an:0507.57010");
        assertEquals(List.of(donaldsonEntry), fetchedEntries);
    }

    @Test
    void searchByIdFindsEntry() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("0507.57010");
        assertEquals(Optional.of(donaldsonEntry), fetchedEntry);
    }

    @Test
    void searchByEntryFindsEntry() throws FetcherException {
        List<BibEntry> fetchedEntries = fetcher.performSearch(getDonaldsonSearchEntry());
        assertEquals(List.of(donaldsonEntry), fetchedEntries);
    }

    @Test
    void searchByIdFindsBookEntry() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("0411.68039");
        assertEquals(Optional.of(gareyJohnsonBookEntry), fetchedEntry);
    }

    @Test
    void searchByIdFindsCollectionEntry() throws FetcherException {
        Optional<BibEntry> fetchedEntry = fetcher.performSearchById("1418.91504");
        assertEquals(Optional.of(blackScholesCollectionEntry), fetchedEntry);
    }

    @Test
    void searchByEmptyEntryFindsNothing() throws FetcherException {
        List<BibEntry> fetchedEntries = fetcher.performSearch(new BibEntry());
        assertEquals(List.of(), fetchedEntries);
    }

    @Test
    void parserMapsBookEntries() throws ParseException {
        String response = """
                {
                  "result": {
                    "document_type": {"code": "b", "description": "book / book article"},
                    "id": 3639144,
                    "identifier": "0411.68039",
                    "year": "1979",
                    "contributors": {
                      "authors": [
                        {"name": "Garey, Michael R."},
                        {"name": "Johnson, David S."}
                      ]
                    },
                    "language": {"languages": ["English"]},
                    "msc": [{"code": "68Q25"}],
                    "source": {
                      "book": [
                        {
                          "publisher": "San Francisco: W. H. Freeman and Company",
                          "isbn": [{"number": "0-7167-1045-5", "type": "print"}]
                        }
                      ]
                    },
                    "title": {"title": "Computers and intractability"}
                  },
                  "status": {"status_code": 200}
                }
                """;

        List<BibEntry> entries = fetcher.getParser().parseEntries(new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8)));

        BibEntry expected = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.AUTHOR, "Garey, Michael R. and Johnson, David S.")
                .withField(StandardField.TITLE, "Computers and intractability")
                .withField(StandardField.YEAR, "1979")
                .withField(StandardField.ZBL_NUMBER, "0411.68039")
                .withField(StandardField.LANGUAGE, "English")
                .withField(StandardField.KEYWORDS, "68Q25")
                .withField(StandardField.PUBLISHER, "San Francisco: W. H. Freeman and Company")
                .withField(StandardField.ISBN, "0-7167-1045-5")
                .withField(new UnknownField("zbmath"), "3639144");
        assertEquals(List.of(expected), entries);
    }

    @Test
    void parserMapsCollectionArticles() throws ParseException {
        String response = """
                {
                  "result": {
                    "document_type": {"code": "a", "description": "serial article"},
                    "id": 7088115,
                    "identifier": "1418.91504",
                    "year": "2007",
                    "contributors": {
                      "authors": [
                        {"name": "Black, Fischer"},
                        {"name": "Scholes, Myron"}
                      ]
                    },
                    "language": {"languages": ["English"]},
                    "source": {
                      "book": [
                        {
                          "publisher": "Cheltenham: Edward Elgar Publishing",
                          "isbn": [{"number": "978-1-84720-148-5", "type": "hbk"}]
                        }
                      ],
                      "pages": "40-57",
                      "source": "Semmler, Willi (ed.) et al., The foundations of credit risk analysis. The International Library of Critical Writings in Econometrics 211. Cheltenham: Edward Elgar Publishing. 40-57 (2007)."
                    },
                    "title": {"title": "The pricing of options and corporate liabilities"}
                  },
                  "status": {"status_code": 200}
                }
                """;

        List<BibEntry> entries = fetcher.getParser().parseEntries(new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8)));

        BibEntry expected = new BibEntry(StandardEntryType.InCollection)
                .withField(StandardField.AUTHOR, "Black, Fischer and Scholes, Myron")
                .withField(StandardField.TITLE, "The pricing of options and corporate liabilities")
                .withField(StandardField.YEAR, "2007")
                .withField(StandardField.ZBL_NUMBER, "1418.91504")
                .withField(StandardField.LANGUAGE, "English")
                .withField(StandardField.PAGES, "40--57")
                .withField(StandardField.BOOKTITLE, "Semmler, Willi (ed.) et al., The foundations of credit risk analysis. The International Library of Critical Writings in Econometrics 211. Cheltenham: Edward Elgar Publishing. 40-57 (2007).")
                .withField(StandardField.PUBLISHER, "Cheltenham: Edward Elgar Publishing")
                .withField(StandardField.ISBN, "978-1-84720-148-5")
                .withField(new UnknownField("zbmath"), "7088115");
        assertEquals(List.of(expected), entries);
    }

    private BibEntry getDonaldsonSearchEntry() {
        return new BibEntry()
                .withField(StandardField.TITLE, "An application of gauge theory to four dimensional topology")
                .withField(StandardField.AUTHOR, "S. K. {Donaldson}");
    }
}
