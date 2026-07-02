package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrowserExtensionFulltextFetcherTest {

    private static final String EXAMPLE_DOI = "10.1109/SANER60148.2024.00014";
    private static final String EXAMPLE_URL = "https://ieeexplore.ieee.org/document/10589877";
    private static final Duration TEST_SOCKET_TIMEOUT = Duration.ofSeconds(5);

    private final List<MockWebServer> startedServers = new java.util.ArrayList<>();

    @AfterEach
    void stopServers() throws IOException {
        for (MockWebServer server : startedServers) {
            server.close();
        }
        startedServers.clear();
    }

    @Test
    void entryWithoutDoiOrUrlReturnsEmpty() throws IOException {
        BrowserExtensionFulltextFetcher fetcher = new BrowserExtensionFulltextFetcher(
                List::of, TEST_SOCKET_TIMEOUT);
        assertEquals(Optional.empty(), fetcher.findFullText(new BibEntry()));
    }

    @Test
    void noDiscoveredProvidersReturnsEmpty() throws IOException {
        BrowserExtensionFulltextFetcher fetcher = new BrowserExtensionFulltextFetcher(
                List::of, TEST_SOCKET_TIMEOUT);
        BibEntry entry = new BibEntry().withField(StandardField.DOI, EXAMPLE_DOI);
        assertEquals(Optional.empty(), fetcher.findFullText(entry));
    }

    @Test
    void trustLevelIsPublisher() {
        BrowserExtensionFulltextFetcher fetcher = new BrowserExtensionFulltextFetcher(
                List::of, TEST_SOCKET_TIMEOUT);
        assertEquals(TrustLevel.PUBLISHER, fetcher.getTrustLevel());
    }

    @Test
    void provider200ResponseReturnsFileUrl(@TempDir Path tempDir) throws IOException {
        Path pdfFile = writePlaceholderPdf(tempDir, "paper.pdf");
        Path tokenFile = writeTokenFile(tempDir, "secret");

        MockWebServer server = startServer();
        server.enqueue(new MockResponse.Builder()
                .code(200)
                .body("{\"id\":\"abc\",\"path\":\"" + escapePath(pdfFile) + "\","
                        + "\"sourceUrl\":\"https://example.test/pdf\"}")
                .build());

        BrowserExtensionFulltextFetcher fetcher = new BrowserExtensionFulltextFetcher(
                providerFor(server, tokenFile), TEST_SOCKET_TIMEOUT);
        BibEntry entry = new BibEntry().withField(StandardField.DOI, EXAMPLE_DOI);

        assertEquals(Optional.of(pdfFile.toUri().toURL()), fetcher.findFullText(entry));
    }

    @Test
    void providerNonAbsolutePathIsIgnored(@TempDir Path tempDir) throws IOException {
        Path tokenFile = writeTokenFile(tempDir, "secret");

        MockWebServer server = startServer();
        server.enqueue(new MockResponse.Builder()
                .code(200)
                .body("{\"id\":\"abc\",\"path\":\"relative/paper.pdf\"}")
                .build());

        BrowserExtensionFulltextFetcher fetcher = new BrowserExtensionFulltextFetcher(
                providerFor(server, tokenFile), TEST_SOCKET_TIMEOUT);
        BibEntry entry = new BibEntry().withField(StandardField.DOI, EXAMPLE_DOI);

        assertEquals(Optional.empty(), fetcher.findFullText(entry));
    }

    @Test
    void provider404ResponseIsTreatedAsSoftMiss(@TempDir Path tempDir) throws IOException {
        Path tokenFile = writeTokenFile(tempDir, "secret");

        MockWebServer server = startServer();
        server.enqueue(new MockResponse.Builder()
                .code(404)
                .body("{\"error\":\"no-pdf-found\"}")
                .build());

        BrowserExtensionFulltextFetcher fetcher = new BrowserExtensionFulltextFetcher(
                providerFor(server, tokenFile), TEST_SOCKET_TIMEOUT);
        BibEntry entry = new BibEntry().withField(StandardField.DOI, EXAMPLE_DOI);

        assertEquals(Optional.empty(), fetcher.findFullText(entry));
    }

    @Test
    void provider503ResponseIsTreatedAsSoftMissWithoutRetry(@TempDir Path tempDir) throws IOException, InterruptedException {
        Path tokenFile = writeTokenFile(tempDir, "secret");

        MockWebServer server = startServer();
        server.enqueue(new MockResponse.Builder()
                .code(503)
                .body("{\"error\":\"busy\"}")
                .build());

        BrowserExtensionFulltextFetcher fetcher = new BrowserExtensionFulltextFetcher(
                providerFor(server, tokenFile), TEST_SOCKET_TIMEOUT);
        BibEntry entry = new BibEntry().withField(StandardField.DOI, EXAMPLE_DOI);

        assertEquals(Optional.empty(), fetcher.findFullText(entry));
        assertEquals(1, server.getRequestCount(), "Fetcher must not retry on 503 busy");
    }

    @Test
    void raceTakesFirst200Response(@TempDir Path tempDir) throws IOException {
        Path tokenFile = writeTokenFile(tempDir, "secret");
        Path pdfFile = writePlaceholderPdf(tempDir, "winner.pdf");

        MockWebServer slow = startServer();
        slow.enqueue(new MockResponse.Builder()
                .code(404)
                .body("{\"error\":\"no-pdf-found\"}")
                .bodyDelay(300, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build());

        MockWebServer fast = startServer();
        fast.enqueue(new MockResponse.Builder()
                .code(200)
                .body("{\"id\":\"w\",\"path\":\"" + escapePath(pdfFile) + "\"}")
                .build());

        BrowserExtensionFulltextFetcher fetcher = new BrowserExtensionFulltextFetcher(
                () -> List.of(
                        new BrowserExtensionProvider("slow", "Slow", slow.getPort(), tokenFile, 1),
                        new BrowserExtensionProvider("fast", "Fast", fast.getPort(), tokenFile, 1)),
                TEST_SOCKET_TIMEOUT);
        BibEntry entry = new BibEntry().withField(StandardField.DOI, EXAMPLE_DOI);

        assertEquals(Optional.of(pdfFile.toUri().toURL()), fetcher.findFullText(entry));
    }

    @Test
    void requestCarriesBearerTokenAndDoi(@TempDir Path tempDir) throws IOException, InterruptedException {
        Path tokenFile = writeTokenFile(tempDir, "secret-token");
        Path pdfFile = writePlaceholderPdf(tempDir, "paper.pdf");

        MockWebServer server = startServer();
        server.enqueue(new MockResponse.Builder()
                .code(200)
                .body("{\"id\":\"x\",\"path\":\"" + escapePath(pdfFile) + "\"}")
                .build());

        BrowserExtensionFulltextFetcher fetcher = new BrowserExtensionFulltextFetcher(
                providerFor(server, tokenFile), TEST_SOCKET_TIMEOUT);
        BibEntry entry = new BibEntry().withField(StandardField.DOI, EXAMPLE_DOI);
        fetcher.findFullText(entry);

        RecordedRequest request = server.takeRequest();
        assertEquals("Bearer secret-token", request.getHeaders().get("Authorization"));
        String body = request.getBody().utf8();
        assertTrue(body.contains(EXAMPLE_DOI),
                "Request body must carry the DOI; was: " + body);
    }

    @Test
    void requestForwardsUrlWhenDoiAbsent(@TempDir Path tempDir) throws IOException, InterruptedException {
        Path tokenFile = writeTokenFile(tempDir, "secret");
        Path pdfFile = writePlaceholderPdf(tempDir, "paper.pdf");

        MockWebServer server = startServer();
        server.enqueue(new MockResponse.Builder()
                .code(200)
                .body("{\"id\":\"x\",\"path\":\"" + escapePath(pdfFile) + "\"}")
                .build());

        BrowserExtensionFulltextFetcher fetcher = new BrowserExtensionFulltextFetcher(
                providerFor(server, tokenFile), TEST_SOCKET_TIMEOUT);
        BibEntry entry = new BibEntry().withField(StandardField.URL, EXAMPLE_URL);
        fetcher.findFullText(entry);

        String body = server.takeRequest().getBody().utf8();
        assertTrue(body.contains("\"url\""),
                "Request body must carry the URL when DOI is absent; was: " + body);
    }

    @Test
    void unreachableTokenFileSkipsProvider(@TempDir Path tempDir) throws IOException {
        Path missingToken = tempDir.resolve("missing.token");
        Supplier<List<BrowserExtensionProvider>> providers = () -> List.of(
                new BrowserExtensionProvider("dead", "Dead", 1, missingToken, 1));

        BrowserExtensionFulltextFetcher fetcher = new BrowserExtensionFulltextFetcher(
                providers, TEST_SOCKET_TIMEOUT);
        BibEntry entry = new BibEntry().withField(StandardField.DOI, EXAMPLE_DOI);

        assertEquals(Optional.empty(), fetcher.findFullText(entry));
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private MockWebServer startServer() throws IOException {
        MockWebServer server = new MockWebServer();
        server.start();
        startedServers.add(server);
        return server;
    }

    private static Supplier<List<BrowserExtensionProvider>> providerFor(MockWebServer server, Path tokenFile) {
        BrowserExtensionProvider provider = new BrowserExtensionProvider(
                "test", "Test", server.getPort(), tokenFile, 1);
        return () -> List.of(provider);
    }

    private static Path writeTokenFile(Path tempDir, String token) throws IOException {
        Path file = tempDir.resolve("token");
        Files.writeString(file, token);
        return file;
    }

    private static Path writePlaceholderPdf(Path tempDir, String name) throws IOException {
        Path file = tempDir.resolve(name);
        Files.writeString(file, "%PDF-1.4\n%fake\n");
        return file;
    }

    private static String escapePath(Path path) {
        return path.toString().replace("\\", "\\\\");
    }
}
