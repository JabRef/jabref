package org.jabref.logic.importer;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jabref.logic.importer.fetcher.TrustLevel;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FulltextFetchersDoiPropagationTest {

    @Test
    void passesExistingDoiToFulltextFetcher() throws Exception {
        String doi = "10.1000/existing";
        BibEntry entry = new BibEntry().withField(StandardField.DOI, doi);
        FulltextFetcher fetcher = mock(FulltextFetcher.class);
        when(fetcher.getTrustLevel()).thenReturn(TrustLevel.PUBLISHER);
        when(fetcher.getDownloadHeaders()).thenReturn(Map.of());

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        try {
            server.createContext("/paper.pdf", exchange -> {
                exchange.getResponseHeaders().set("Content-Type", "application/pdf");
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
            });
            server.start();
            URL pdfUrl = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/paper.pdf").toURL();
            when(fetcher.findFullText(any(BibEntry.class))).thenAnswer(invocation -> {
                BibEntry lookupEntry = invocation.getArgument(0);
                return lookupEntry.getField(StandardField.DOI).filter(doi::equals).map(_ -> pdfUrl);
            });

            Optional<FetcherResult> result = new FulltextFetchers(Set.of(fetcher)).findFullTextPDF(entry);

            assertEquals(Optional.of(pdfUrl), result.map(FetcherResult::source));
            ArgumentCaptor<BibEntry> entryCaptor = ArgumentCaptor.forClass(BibEntry.class);
            verify(fetcher).findFullText(entryCaptor.capture());
            assertEquals(Optional.of(doi), entryCaptor.getValue().getField(StandardField.DOI));
        } finally {
            server.stop(0);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void passesDiscoveredDoiToFulltextFetcher(boolean updateDoi) throws Exception {
        // Arrange: an entry with a title but no DOI
        BibEntry entry = new BibEntry()
                .withField(StandardField.TITLE, "Example research paper");

        DOI discoveredDoi = new DOI("10.1000/test");

        // Mock the DOI identifier fetcher
        @SuppressWarnings("unchecked")
        IdFetcher<DOI> doiFetcher = mock(IdFetcher.class);

        when(doiFetcher.findIdentifier(any(BibEntry.class)))
                .thenReturn(Optional.of(discoveredDoi));

        // Mock the full-text fetcher
        FulltextFetcher fulltextFetcher = mock(FulltextFetcher.class);

        when(fulltextFetcher.getTrustLevel())
                .thenReturn(TrustLevel.PUBLISHER);

        when(fulltextFetcher.getDownloadHeaders())
                .thenReturn(Map.of());

        // Start a local HTTP server to provide a PDF response
        HttpServer server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0), 0);

        try {
            server.createContext("/paper.pdf", exchange -> {
                exchange.getResponseHeaders()
                        .set("Content-Type", "application/pdf");
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
            });

            server.start();

            URL pdfUrl = URI.create(
                                    "http://127.0.0.1:" + server.getAddress().getPort() + "/paper.pdf")
                            .toURL();

            when(fulltextFetcher.findFullText(any(BibEntry.class)))
                    .thenReturn(Optional.of(pdfUrl));

            // Mock the static DOI fetcher factory
            try (MockedStatic<WebFetchers> webFetchers =
                         mockStatic(WebFetchers.class)) {
                webFetchers.when(() -> WebFetchers.getIdFetcherForIdentifier(
                                   eq(DOI.class), any(ImporterPreferences.class)))
                           .thenReturn(doiFetcher);

                // Act
                FulltextFetchers fulltextFetchers =
                        new FulltextFetchers(Set.of(fulltextFetcher));

                Optional<FetcherResult> result =
                        updateDoi ? fulltextFetchers.findFullTextPDFAndUpdateDoi(entry) : fulltextFetchers.findFullTextPDF(entry);

                assertEquals(updateDoi ? Optional.of(discoveredDoi.asString()) : Optional.empty(), entry.getField(StandardField.DOI));

                // Assert: a PDF result was found
                assertTrue(result.isPresent());

                // Assert: the full-text fetcher received the discovered DOI
                ArgumentCaptor<BibEntry> entryCaptor =
                        ArgumentCaptor.forClass(BibEntry.class);

                verify(fulltextFetcher).findFullText(entryCaptor.capture());

                assertEquals(
                        discoveredDoi.asString(),
                        entryCaptor.getValue()
                                   .getField(StandardField.DOI)
                                   .orElseThrow());

                // Assert: DOI discovery was attempted using the original title
                ArgumentCaptor<BibEntry> doiEntryCaptor =
                        ArgumentCaptor.forClass(BibEntry.class);

                verify(doiFetcher).findIdentifier(doiEntryCaptor.capture());

                assertEquals(
                        "Example research paper",
                        doiEntryCaptor.getValue()
                                      .getField(StandardField.TITLE)
                                      .orElseThrow());
            }
        } finally {
            server.stop(0);
        }
    }
}
