package org.jabref.logic.importer.fetcher;

import java.util.List;
import java.util.Set;

import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.journals.JournalInformation;
import org.jabref.logic.preferences.FetcherApiKey;

import kong.unirest.core.GetRequest;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.JsonNode;
import kong.unirest.core.Unirest;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JournalInformationFetcherKeyTest {

    @Test
    void usesConfiguredOpenAlexApiKey() {
        ImporterPreferences importerPreferences = ImporterPreferences.getDefault();
        importerPreferences.setApiKeys(Set.of(new FetcherApiKey(OpenAlex.FETCHER_NAME, true, "configured API key")));
        JournalInformationFetcher fetcher = new JournalInformationFetcher(importerPreferences);

        assertEquals("https://api.openalex.org/sources/issn:1545-4509?api_key=configured+API+key", fetcher.getOpenAlexUrl("1545-4509", ""));
    }

    @Test
    void doesNotUseDisabledOpenAlexApiKey() {
        ImporterPreferences importerPreferences = ImporterPreferences.getDefault();
        importerPreferences.setApiKeys(Set.of(new FetcherApiKey(OpenAlex.FETCHER_NAME, false, "configured API key")));
        JournalInformationFetcher fetcher = new JournalInformationFetcher(importerPreferences);

        assertEquals("https://api.openalex.org/sources/issn:1545-4509", fetcher.getOpenAlexUrl("1545-4509", ""));
    }

    @Test
    void matchesNormalizedJournalNames() {
        assertTrue(JournalInformationFetcher.haveMatchingNames("Journal of Molecular Biology", "Journal-of Molecular, Biology"));
        assertFalse(JournalInformationFetcher.haveMatchingNames("Journal of Molecular Biology", "Journal of Cell Biology"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsCrossrefInformationWhenOpenAlexIsUnavailable() throws Exception {
        GetRequest crossrefRequest = mock(GetRequest.class, Answers.RETURNS_DEEP_STUBS);
        HttpResponse<JsonNode> crossrefResponse = mock(HttpResponse.class);
        when(crossrefResponse.getStatus()).thenReturn(200);
        when(crossrefResponse.getBody()).thenReturn(new JsonNode("""
                {
                  "message": {
                    "title": "Crossref journal",
                    "publisher": "Crossref publisher",
                    "ISSN": ["1234-5678"]
                  }
                }
                """));
        when(crossrefRequest.header(anyString(), anyString()).asJson()).thenReturn(crossrefResponse);

        GetRequest openAlexRequest = mock(GetRequest.class, Answers.RETURNS_DEEP_STUBS);
        HttpResponse<JsonNode> openAlexResponse = mock(HttpResponse.class);
        when(openAlexResponse.getStatus()).thenReturn(503);
        when(openAlexRequest.header(anyString(), anyString()).asJson()).thenReturn(openAlexResponse);

        try (MockedStatic<Unirest> unirestMock = Mockito.mockStatic(Unirest.class)) {
            unirestMock.when(() -> Unirest.get(anyString()))
                       .thenAnswer(invocation -> invocation.getArgument(0, String.class).startsWith("https://api.crossref.org/") ? crossrefRequest : openAlexRequest);

            JournalInformation journalInformation = new JournalInformationFetcher().getJournalInformation("1234-5678", "").orElseThrow();

            assertEquals("Crossref journal", journalInformation.title());
            assertEquals("Crossref publisher", journalInformation.publisher());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void ignoresMalformedOpenAlexNumericValues() throws Exception {
        GetRequest crossrefRequest = mock(GetRequest.class, Answers.RETURNS_DEEP_STUBS);
        HttpResponse<JsonNode> crossrefResponse = mock(HttpResponse.class);
        when(crossrefResponse.getStatus()).thenReturn(200);
        when(crossrefResponse.getBody()).thenReturn(new JsonNode("""
                {
                  "message": {
                    "title": "Crossref journal",
                    "publisher": "Crossref publisher",
                    "ISSN": ["1234-5678"]
                  }
                }
                """));
        when(crossrefRequest.header(anyString(), anyString()).asJson()).thenReturn(crossrefResponse);

        GetRequest openAlexRequest = mock(GetRequest.class, Answers.RETURNS_DEEP_STUBS);
        HttpResponse<JsonNode> openAlexResponse = mock(HttpResponse.class);
        when(openAlexResponse.getStatus()).thenReturn(200);
        when(openAlexResponse.getBody()).thenReturn(new JsonNode("""
                {
                  "display_name": "OpenAlex journal",
                  "summary_stats": {"h_index": "unknown"},
                  "counts_by_year": [
                    {"year": "unknown", "works_count": "unknown", "cited_by_count": "unknown"}
                  ]
                }
                """));
        when(openAlexRequest.header(anyString(), anyString()).asJson()).thenReturn(openAlexResponse);

        try (MockedStatic<Unirest> unirestMock = Mockito.mockStatic(Unirest.class)) {
            unirestMock.when(() -> Unirest.get(anyString()))
                       .thenAnswer(invocation -> invocation.getArgument(0, String.class).startsWith("https://api.crossref.org/") ? crossrefRequest : openAlexRequest);

            JournalInformation journalInformation = new JournalInformationFetcher().getJournalInformation("1234-5678", "").orElseThrow();

            assertEquals("", journalInformation.hIndex());
            assertEquals(List.of(), journalInformation.worksCount());
            assertEquals(List.of(), journalInformation.citedByCount());
        }
    }
}
