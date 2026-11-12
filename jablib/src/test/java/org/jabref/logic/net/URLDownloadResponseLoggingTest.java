package org.jabref.logic.net;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.FetcherClientException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.fetcher.BibSonomyFetcher;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// The logger writes to the console in tests, so the log is read from `System.out`.
/// The remote side is a local server, because this tests the logging of [URLDownload] and not a remote API.
@ResourceLock(Resources.SYSTEM_OUT)
class URLDownloadResponseLoggingTest {

    private static final String RESPONSE_BODY = "Authentication failure: Wrong Authentication ('user'/'secret-key')";

    private final ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
    private final PrintStream originalOutput = System.out;

    private HttpServer server;
    private URL url;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            byte[] body = RESPONSE_BODY.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(401, body.length);
            try (OutputStream responseBody = exchange.getResponseBody()) {
                responseBody.write(body);
            }
        });
        server.start();
        url = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/").toURL();
        System.setOut(new PrintStream(capturedOutput, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOutput);
        server.stop(0);
    }

    @Test
    void responseBodyIsLoggedByDefault() {
        URLDownload download = new URLDownload(url);

        assertThrows(FetcherClientException.class, download::openConnection);

        assertEquals(List.of("SimpleHttpResponse{statusCode=401, responseMessage='Unauthorized', responseBody='" + RESPONSE_BODY + "'}"),
                loggedResponses());
    }

    @Test
    void omittedResponseBodyIsNotLogged() {
        URLDownload download = new URLDownload(url);
        download.omitResponseBodyFromLog();

        assertThrows(FetcherClientException.class, download::openConnection);

        assertEquals(List.of("SimpleHttpResponse{statusCode=401, responseMessage='Unauthorized', responseBody=''}"),
                loggedResponses());
    }

    @Test
    void bibSonomyDownloadDoesNotLogResponseBody() {
        ImporterPreferences importerPreferences = mock(ImporterPreferences.class);
        when(importerPreferences.getApiKey(BibSonomyFetcher.FETCHER_NAME)).thenReturn(Optional.of("user:secret-key"));
        BibSonomyFetcher fetcher = new BibSonomyFetcher(mock(ImportFormatPreferences.class), importerPreferences);
        URLDownload download = fetcher.getUrlDownload(url);

        assertThrows(FetcherClientException.class, download::openConnection);

        assertEquals(List.of("SimpleHttpResponse{statusCode=401, responseMessage='Unauthorized', responseBody=''}"),
                loggedResponses());
    }

    /// Other tests may print at the same time, so only the lines about this test's server are kept
    private List<String> loggedResponses() {
        return capturedOutput.toString(StandardCharsets.UTF_8).lines()
                             .filter(line -> line.contains(url.toString()))
                             .map(line -> line.substring(line.indexOf("SimpleHttpResponse")))
                             .toList();
    }
}
