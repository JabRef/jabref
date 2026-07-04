package org.jabref.logic.importer.fetcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.testutils.category.FetcherTest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@FetcherTest
class ZbMATHTest {
    private static final String DONALDSON_BIBTEX = """
            @Article{zbMATH03800580,
              author     = {Donaldson, S. K.},
              journal    = {Journal of Differential Geometry},
              title      = {An application of gauge theory to four dimensional topology},
              year       = {1983},
              issn       = {0022-040X},
              pages      = {279--315},
              volume     = {18},
              doi        = {10.4310/jdg/1214437665},
              language   = {English},
              keywords   = {57N13,57R10,53C05,58J99,57R65},
              zbl        = {0507.57010},
              zbmath     = {3800580}
            }
            """;

    private static final String DONALDSON_MATCH_RESPONSE = """
            {"results":[{"zbl_id":"0507.57010"}]}
            """;

    private ZbMATH fetcher;
    private BibEntry donaldsonEntry;
    private ServerSocket serverSocket;
    private ExecutorService serverExecutor;
    private int citationMatchingStatus;
    private String citationMatchingResponse;

    @BeforeEach
    void setUp() throws IOException {
        citationMatchingStatus = 200;
        citationMatchingResponse = DONALDSON_MATCH_RESPONSE;

        serverSocket = new ServerSocket();
        InetAddress loopbackAddress = InetAddress.getLoopbackAddress();
        serverSocket.bind(new InetSocketAddress(loopbackAddress, 0));
        serverExecutor = Executors.newSingleThreadExecutor();
        serverExecutor.submit(this::handleRequests);

        ImportFormatPreferences importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');

        String hostAddress = loopbackAddress.getHostAddress();
        String urlHost = hostAddress.contains(":") ? "[%s]".formatted(hostAddress) : hostAddress;
        String baseUrl = "http://%s:%d".formatted(urlHost, serverSocket.getLocalPort());
        fetcher = new ZbMATH(importFormatPreferences, baseUrl + "/citationmatching/match", baseUrl + "/bibtexoutput/");

        donaldsonEntry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("zbMATH03800580")
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
    }

    @AfterEach
    void tearDown() throws IOException, InterruptedException {
        serverSocket.close();
        serverExecutor.shutdownNow();
        serverExecutor.awaitTermination(1, TimeUnit.SECONDS);
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
        BibEntry searchEntry = getDonaldsonSearchEntry();

        List<BibEntry> fetchedEntries = fetcher.performSearch(searchEntry);
        assertEquals(List.of(donaldsonEntry), fetchedEntries);
    }

    @Test
    void searchByNoneEntryFindsNothing() throws FetcherException {
        BibEntry searchEntry = new BibEntry()
                .withField(StandardField.TITLE, "t")
                .withField(StandardField.AUTHOR, "a");
        citationMatchingResponse = """
                {"results":[]}
                """;

        List<BibEntry> fetchedEntries = fetcher.performSearch(searchEntry);
        assertEquals(List.of(), fetchedEntries);
    }

    @Test
    void searchByIdInEntryFindsEntry() throws FetcherException {
        BibEntry searchEntry = new BibEntry().withField(StandardField.ZBL_NUMBER, "0507.57010");

        List<BibEntry> fetchedEntries = fetcher.performSearch(searchEntry);
        assertEquals(List.of(donaldsonEntry), fetchedEntries);
    }

    @Test
    void searchByEntryWithBlockedHtmlResponseThrowsFetcherException() {
        BibEntry searchEntry = getDonaldsonSearchEntry();
        citationMatchingResponse = """
                <html><title>Just a moment...</title></html>
                """;

        assertThrows(FetcherException.class, () -> fetcher.performSearch(searchEntry));
    }

    @Test
    void searchByEntryWithMissingResultsThrowsFetcherException() {
        BibEntry searchEntry = getDonaldsonSearchEntry();
        citationMatchingResponse = """
                {"unexpected":[]}
                """;

        assertThrows(FetcherException.class, () -> fetcher.performSearch(searchEntry));
    }

    @Test
    void searchByEntryWithMissingZblIdThrowsFetcherException() {
        BibEntry searchEntry = getDonaldsonSearchEntry();
        citationMatchingResponse = """
                {"results":[{"title":"An application of gauge theory to four dimensional topology"}]}
                """;

        assertThrows(FetcherException.class, () -> fetcher.performSearch(searchEntry));
    }

    @Test
    void searchByEntryWithInvalidResultThrowsFetcherException() {
        BibEntry searchEntry = getDonaldsonSearchEntry();
        citationMatchingResponse = """
                {"results":["0507.57010"]}
                """;

        assertThrows(FetcherException.class, () -> fetcher.performSearch(searchEntry));
    }

    @Test
    void searchByEntryWithFetchErrorThrowsFetcherException() {
        BibEntry searchEntry = getDonaldsonSearchEntry();
        citationMatchingStatus = 403;
        citationMatchingResponse = """
                <html><title>Just a moment...</title></html>
                """;

        assertThrows(FetcherException.class, () -> fetcher.performSearch(searchEntry));
    }

    private BibEntry getDonaldsonSearchEntry() {
        return new BibEntry()
                .withField(StandardField.TITLE, "An application of gauge theory to four dimensional topology")
                .withField(StandardField.AUTHOR, "S. K. {Donaldson}");
    }

    private void handleRequests() {
        while (!serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                handleRequest(socket);
            } catch (SocketException e) {
                return;
            } catch (IOException e) {
                return;
            }
        }
    }

    private void handleRequest(Socket socket) throws IOException {
        try (socket;
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             OutputStream outputStream = socket.getOutputStream()) {
            String requestLine = reader.readLine();
            String headerLine;
            while ((headerLine = reader.readLine()) != null) {
                if (headerLine.isEmpty()) {
                    break;
                }
            }
            if (requestLine != null && requestLine.contains("/citationmatching/match")) {
                sendResponse(outputStream, citationMatchingStatus, citationMatchingResponse);
            } else {
                sendResponse(outputStream, 200, DONALDSON_BIBTEX);
            }
        }
    }

    private void sendResponse(OutputStream outputStream, int statusCode, String body) throws IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        String statusMessage = statusCode == 200 ? "OK" : "Forbidden";
        String headers = "HTTP/1.1 %d %s\r\nContent-Type: text/plain; charset=UTF-8\r\nContent-Length: %d\r\nConnection: close\r\n\r\n"
                .formatted(statusCode, statusMessage, response.length);
        outputStream.write(headers.getBytes(StandardCharsets.UTF_8));
        outputStream.write(response);
    }
}
