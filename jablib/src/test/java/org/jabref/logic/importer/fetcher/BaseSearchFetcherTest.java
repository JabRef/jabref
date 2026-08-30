package org.jabref.logic.importer.fetcher;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;

import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.fetcher.transformers.BaseSearchQueryTransformer;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.search.query.OperatorNode;
import org.jabref.model.search.query.SearchQueryNode;
import org.jabref.testutils.category.FetcherTest;

import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@NullMarked
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

        Map<String, String> params = new URIBuilder(url.toURI())
                .getQueryParams()
                .stream()
                .collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue, (v1, v2) -> v1));

        assertEquals("PerformSearch", params.get("func"));
        assertEquals("json", params.get("format"));
        assertEquals("dummy-test-key", params.get("apikey"));
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
    void queryTransformerTransformsIndividualFieldsCorrectly() {
        BaseSearchQueryTransformer transformer = new BaseSearchQueryTransformer();

        SearchQueryNode authorNode = new SearchQueryNode(Optional.of(StandardField.AUTHOR), "Knuth");
        SearchQueryNode titleNode = new SearchQueryNode(Optional.of(StandardField.TITLE), "Algorithms");
        SearchQueryNode journalNode = new SearchQueryNode(Optional.of(StandardField.JOURNAL), "ACM");
        SearchQueryNode yearNode = new SearchQueryNode(Optional.of(StandardField.YEAR), "2020");

        assertEquals(Optional.of("dccreator:Knuth"), transformer.transformSearchQuery(authorNode));
        assertEquals(Optional.of("dctitle:Algorithms"), transformer.transformSearchQuery(titleNode));
        assertEquals(Optional.of("dcsource:ACM"), transformer.transformSearchQuery(journalNode));
        assertEquals(Optional.of("dcyear:2020"), transformer.transformSearchQuery(yearNode));
    }

    @Test
    void queryTransformerParenthesizesYearRangeInComplexQuery() {
        BaseSearchQueryTransformer transformer = new BaseSearchQueryTransformer();

        SearchQueryNode yearRangeNode = new SearchQueryNode(Optional.of(new UnknownField("year-range")), "2020-2022");
        SearchQueryNode titleNode = new SearchQueryNode(Optional.of(StandardField.TITLE), "quantum");

        OperatorNode andNode = new OperatorNode(
                OperatorNode.Operator.AND,
                List.of(yearRangeNode, titleNode));

        assertEquals(
                Optional.of("(dcyear:2020 OR dcyear:2021 OR dcyear:2022) AND dctitle:quantum"),
                transformer.transformSearchQuery(andNode));
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
    void parserReturnsEmptyListOnMissingResponseOrResultOrDocs() throws ParseException {
        String jsonEmptyResponse = "{}";
        String jsonMissingResult = "{\"response\": {}}";
        String jsonMissingDocs = "{\"response\": {\"result\": {}}}";

        for (String json : List.of(jsonEmptyResponse, jsonMissingResult, jsonMissingDocs)) {
            InputStream inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
            List<BibEntry> entries = fetcher.getParser().parseEntries(inputStream);
            assertEquals(List.of(), entries);
        }
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

    @ParameterizedTest
    @CsvSource({
            "11,  Book",
            "121, Article",
            "13,  InProceedings",
            "14,  TechReport",
            "18,  PhdThesis"
    })
    void parserMapsTypeCodeToCorrectEntryType(String typeCode, StandardEntryType expectedType) throws ParseException {
        String json = """
            {
              "response": {
                "result": {
                  "docs": [
                    {
                      "dctitle": "Sample Title",
                      "dctypenorm": ["%s"]
                    }
                  ]
                }
              }
            }
            """.formatted(typeCode);

        InputStream inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        List<BibEntry> entries = fetcher.getParser().parseEntries(inputStream);

        assertEquals(1, entries.size());
        assertEquals(expectedType, entries.getFirst().getType());
    }

    @Test
    void isValidKeyReturnsFalseForMalformedResponse() {
        assertFalse(fetcher.isValidKey("obviously-invalid-key"));
    }
}
