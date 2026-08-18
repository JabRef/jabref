package org.jabref.logic.importer.fetcher;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import javafx.collections.FXCollections;

import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.search.query.SearchQueryNode;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
class BaseSearchFetcherTest {

    private BaseSearchFetcher fetcher;
    private ImporterPreferences importerPreferences;

    @BeforeEach
    void setUp() {
        importerPreferences = mock(ImporterPreferences.class);
        when(importerPreferences.getApiKeys()).thenReturn(FXCollections.emptyObservableSet());
        when(importerPreferences.getApiKey(BaseSearchFetcher.FETCHER_NAME)).thenReturn(Optional.of("dummy-test-key"));
        fetcher = new BaseSearchFetcher(importerPreferences);
    }

    @Test
    void getName() {
        assertEquals("Bielefeld Academic Search Engine", fetcher.getName());
    }

    @Test
    void getURLForQueryContainsApiKeyAndQuery() throws Exception {
        SearchQueryNode queryNode = new SearchQueryNode(Optional.empty(), "test query");
        URL url = fetcher.getURLForQuery(queryNode, 0);

        assertTrue(url.toString().contains("func=PerformSearch"));
        assertTrue(url.toString().contains("format=json"));
        assertTrue(url.toString().contains("apikey=dummy-test-key"));
    }

    @Test
    void getURLForQueryUsesCorrectOffsetForPage() throws Exception {
        SearchQueryNode queryNode = new SearchQueryNode(Optional.empty(), "test query");
        URL urlPageZero = fetcher.getURLForQuery(queryNode, 0);
        URL urlPageOne = fetcher.getURLForQuery(queryNode, 1);

        assertTrue(urlPageZero.toString().contains("offset=0"));
        assertTrue(urlPageOne.toString().contains("offset=" + fetcher.getPageSize()));
    }

    @Test
    void parserReturnsEmptyListOnErrorResponse() throws ParseException {
        String json = """
                {
                  "error": "invalid key"
                }
                """;
        InputStream inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        List<BibEntry> entries = fetcher.getParser().parseEntries(inputStream);

        assertEquals(List.of(), entries);
    }

    @Test
    void parserParsesArticleEntryFromSampleResponse() throws ParseException {
        String json = """
                {
                  "response": {
                    "result": {
                      "docs": [
                        {
                          "dctitle": "A Study on Reference Management",
                          "dcyear": "2023",
                          "dcpublisher": "Springer",
                          "dcdoi": "10.1000/example.doi",
                          "dclink": "https://example.org/paper",
                          "dccreator": ["Doe, Jane", "Smith, John"],
                          "dcsubject": ["bibliography", "software"],
                          "dctypenorm": ["121"]
                        }
                      ]
                    }
                  }
                }
                """;
        InputStream inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        List<BibEntry> entries = fetcher.getParser().parseEntries(inputStream);

        assertEquals(1, entries.size());
        BibEntry entry = entries.getFirst();
        assertEquals(StandardEntryType.Article, entry.getType());
        assertEquals(Optional.of("A Study on Reference Management"), entry.getField(StandardField.TITLE));
        assertEquals(Optional.of("2023"), entry.getField(StandardField.YEAR));
        assertEquals(Optional.of("Springer"), entry.getField(StandardField.PUBLISHER));
        assertEquals(Optional.of("10.1000/example.doi"), entry.getField(StandardField.DOI));
        assertEquals(Optional.of("https://example.org/paper"), entry.getField(StandardField.URL));
        assertEquals(Optional.of("Doe, Jane and Smith, John"), entry.getField(StandardField.AUTHOR));
    }

    @Test
    void parserDefaultsToMiscWhenNoTypeCode() throws ParseException {
        String json = """
                {
                  "response": {
                    "result": {
                      "docs": [
                        {
                          "dctitle": "Untyped Document",
                          "dcyear": "2020"
                        }
                      ]
                    }
                  }
                }
                """;
        InputStream inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        List<BibEntry> entries = fetcher.getParser().parseEntries(inputStream);

        assertEquals(1, entries.size());
        assertEquals(StandardEntryType.Misc, entries.getFirst().getType());
    }

    @Test
    void isValidKeyReturnsFalseForMalformedResponse() {
        assertEquals(false, fetcher.isValidKey("obviously-invalid-key"));
    }
}
