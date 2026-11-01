package org.jabref.logic.importer;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.util.Optional;
import java.util.Set;

import org.jabref.logic.importer.fetcher.TrustLevel;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
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
    void passesDiscoveredDoiToFulltextFetcher() throws Exception {
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
                        fulltextFetchers.findFullTextPDF(entry);

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
