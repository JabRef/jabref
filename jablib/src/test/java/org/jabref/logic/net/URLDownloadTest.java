package org.jabref.logic.net;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jabref.logic.importer.FetcherClientException;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.FetcherServerException;
import org.jabref.logic.util.URLUtil;
import org.jabref.support.DisabledOnCIServer;
import org.jabref.testutils.category.FetcherTest;

import com.github.tomakehurst.wiremock.WireMockServer;
import kong.unirest.core.UnirestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@FetcherTest
class URLDownloadTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(URLDownloadTest.class);

    @Test
    void stringDownloadWithSetEncoding() throws MalformedURLException, FetcherException {
        URLDownload dl = new URLDownload(URLUtil.create("http://www.google.com"));

        assertTrue(dl.asString().contains("Google"), "google.com should contain google");
    }

    @Test
    void stringDownload() throws MalformedURLException, FetcherException {
        URLDownload dl = new URLDownload(URLUtil.create("http://www.google.com"));

        assertTrue(dl.asString(StandardCharsets.UTF_8).contains("Google"), "google.com should contain google");
    }

    @Test
    void fileDownload() throws IOException, FetcherException {
        File destination = File.createTempFile("jabref-test", ".html");
        try {
            URLDownload dl = new URLDownload(URLUtil.create("http://www.google.com"));
            dl.toFile(destination.toPath());
            assertTrue(destination.exists(), "file must exist");
        } finally {
            // cleanup
            if (!destination.delete()) {
                LOGGER.error("Cannot delete downloaded file");
            }
        }
    }

    @Test
    void determineMimeType() throws MalformedURLException {
        URLDownload dl = new URLDownload(URLUtil.create("http://www.google.com"));

        assertTrue(dl.getMimeType().get().startsWith("text/html"));
    }

    @Test
    void downloadToTemporaryFilePathWithoutFileSavesAsTmpFile() throws MalformedURLException, FetcherException {
        URLDownload google = new URLDownload(URLUtil.create("http://www.google.com"));

        String path = google.toTemporaryFile().toString();
        assertTrue(path.endsWith(".tmp"), path);
    }

    @Test
    void downloadToTemporaryFileKeepsName() throws MalformedURLException, FetcherException {
        URLDownload google = new URLDownload(URLUtil.create("https://github.com/JabRef/jabref/blob/main/LICENSE"));

        String path = google.toTemporaryFile().toString();
        assertTrue(path.contains("LICENSE"), path);
    }

    @Test
    @DisabledOnCIServer("CI Server is apparently blocked")
    void downloadOfFTPSucceeds() throws MalformedURLException, FetcherException {
        URLDownload ftp = new URLDownload(URLUtil.create("ftp://ftp.informatik.uni-stuttgart.de/pub/library/ncstrl.ustuttgart_fi/INPROC-2016-15/INPROC-2016-15.pdf"));

        Path path = ftp.toTemporaryFile();
        assertNotNull(path);
    }

    @Test
    void downloadOfHttpSucceeds() throws MalformedURLException, FetcherException {
        URLDownload ftp = new URLDownload(URLUtil.create("http://www.jabref.org"));

        Path path = ftp.toTemporaryFile();
        assertNotNull(path);
    }

    @Test
    void downloadOfHttpsSucceeds() throws MalformedURLException, FetcherException {
        URLDownload ftp = new URLDownload(URLUtil.create("https://www.jabref.org"));

        Path path = ftp.toTemporaryFile();
        assertNotNull(path);
    }

    @Test
    void checkConnectionSuccess() throws MalformedURLException {
        URLDownload google = new URLDownload(URLUtil.create("http://www.google.com"));

        assertTrue(google.canBeReached());
    }

    @Test
    void checkConnectionFail() throws MalformedURLException {
        URLDownload nonsense = new URLDownload(URLUtil.create("http://nonsenseadddress"));

        assertThrows(UnirestException.class, nonsense::canBeReached);
    }

    @Test
    void connectTimeoutIsNeverNull() throws MalformedURLException {
        URLDownload urlDownload = new URLDownload(URLUtil.create("http://www.example.com"));
        assertNotNull(urlDownload.getConnectTimeout(), "there's a non-null default by the constructor");

        urlDownload.setConnectTimeout(null);
        assertNotNull(urlDownload.getConnectTimeout(), "no null value can be set");
    }

    @Test
    void test503ErrorThrowsFetcherServerException() throws MalformedURLException {
        URLDownload urlDownload = new URLDownload(URLUtil.create("http://httpstat.us/503"));
        assertThrows(FetcherServerException.class, urlDownload::asString);
    }

    @Test
    void test429ErrorThrowsFetcherClientException() throws MalformedURLException {
        URLDownload urlDownload = new URLDownload(URLUtil.create("http://httpstat.us/429"));
        assertThrows(FetcherClientException.class, urlDownload::asString);
    }

    @Test
    void redirectWorks(@TempDir Path tempDir) throws IOException, FetcherException {
        WireMockServer wireMockServer = new WireMockServer(2222);
        wireMockServer.start();
        configureFor("localhost", 2222);
        stubFor(get("/redirect")
                .willReturn(aResponse()
                        .withStatus(302)
                        .withHeader("Location", "/final")));
        byte[] pdfContent = {0x00};
        stubFor(get(urlEqualTo("/final"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/pdf")
                        .withBody(pdfContent)));

        URLDownload urlDownload = new URLDownload(URLUtil.create("http://localhost:2222/redirect"));
        Path downloadedFile = tempDir.resolve("download.pdf");
        urlDownload.toFile(downloadedFile);
        byte[] actual = Files.readAllBytes(downloadedFile);
        assertArrayEquals(pdfContent, actual);

        wireMockServer.stop();
    }
}
