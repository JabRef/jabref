package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import javafx.collections.FXCollections;

import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.util.BuildInfo;
import org.jabref.logic.util.URLUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.support.DisabledOnCIServer;
import org.jabref.testutils.category.FetcherTest;

import kong.unirest.core.GetRequest;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.JsonNode;
import kong.unirest.core.Unirest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
class ScienceDirectTest {

    private final ImporterPreferences importerPreferences = mock(ImporterPreferences.class);
    private ScienceDirect finder;
    private BibEntry entry;

    @BeforeEach
    void setUp() {
        Optional<String> apiKey = Optional.of(new BuildInfo().scopusApiKey);
        when(importerPreferences.getApiKeys()).thenReturn(FXCollections.emptyObservableSet());
        when(importerPreferences.getApiKey(Scopus.FETCHER_NAME)).thenReturn(apiKey);
        finder = new ScienceDirect(importerPreferences);
        entry = new BibEntry();
    }

    @Test
    @DisabledOnCIServer("CI server is blocked")
    void findByDoiOldPage() throws IOException {
        entry.setField(StandardField.DOI, "10.1016/j.jrmge.2015.08.004");

        assertEquals(
                Optional.of(URLUtil.create("https://www.sciencedirect.com/science/article/pii/S1674775515001079/pdfft?md5=2b19b19a387cffbae237ca6a987279df&pid=1-s2.0-S1674775515001079-main.pdf")),
                finder.findFullText(entry)
        );
    }

    @Test
    @DisabledOnCIServer("CI server is blocked")
    void findByDoiNewPage() throws IOException {
        entry.setField(StandardField.DOI, "10.1016/j.aasri.2014.09.002");

        assertEquals(
                Optional.of(URLUtil.create("https://www.sciencedirect.com/science/article/pii/S2212671614001024/pdf?md5=4e2e9a369b4d5b3db5100aba599bef8b&pid=1-s2.0-S2212671614001024-main.pdf")),
                finder.findFullText(entry)
        );
    }

    @Test
    @DisabledOnCIServer("CI server is blocked")
    void findByDoiWorksForBoneArticle() throws IOException {
        // The DOI is an example by a user taken from https://github.com/JabRef/jabref/issues/5860
        entry.setField(StandardField.DOI, "https://doi.org/10.1016/j.bone.2020.115226");

        assertEquals(
                Optional.of(URLUtil.create("https://www.sciencedirect.com/science/article/pii/S8756328220300065/pdfft?md5=0ad75ff155637dec358e5c9fb8b90afd&pid=1-s2.0-S8756328220300065-main.pdf")),
                finder.findFullText(entry)
        );
    }

    @Test
    @DisabledOnCIServer("CI server is blocked")
    void notFoundByDoi() throws IOException {
        entry.setField(StandardField.DOI, "10.1016/j.aasri.2014.0559.002");

        assertEquals(Optional.empty(), finder.findFullText(entry));
    }

    @Test
    @SuppressWarnings("unchecked")
    void findsPdfDirectlyWhenSciDirPdfPresent() throws IOException {
        String jsonBody = """
                {"full-text-retrieval-response":{"coredata":{"link":[{"@href":"https://api.elsevier.com/content/article/pii/S0963869523002050","@rel":"self","@_fa":"true"},{"@href":"https://www.sciencedirect.com/science/article/pii/S0963869523002050","@rel":"scidir","@_fa":"true"},{"@href":"https://www.sciencedirect.com/science/article/pii/S0963869523002050/pdfft?isDTMRedir=true","@rel":"scidir-pdf","@_fa":"true"}]}}}""";

        entry = new BibEntry()
                .withField(StandardField.DOI, "10.1016/j.ndteint.2023.102990");

        GetRequest getRequest = mock(GetRequest.class, Answers.RETURNS_DEEP_STUBS);
        HttpResponse<JsonNode> httpResponse = mock(HttpResponse.class);
        when(httpResponse.getBody()).thenReturn(new JsonNode(jsonBody));
        when(getRequest.header(anyString(), anyString())
                       .queryString(anyString(), anyString())
                       .asJson()).thenReturn(httpResponse);

        try (MockedStatic<Unirest> unirestMock = Mockito.mockStatic(Unirest.class)) {
            unirestMock.when(() -> Unirest.get(anyString())).thenReturn(getRequest);

            Optional<URL> result = finder.findFullText(entry);

            assertEquals(
                    Optional.of(URLUtil.create("https://www.sciencedirect.com/science/article/pii/S0963869523002050/pdfft?isDTMRedir=true")),
                    result
            );
        }
    }
}
