package org.jabref.logic.net;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.jabref.logic.importer.FetcherClientException;
import org.jabref.logic.importer.FetcherServerException;
import org.jabref.support.DisabledOnCIServer;
import org.jabref.testutils.category.FetcherTest;

import kong.unirest.core.UnirestException;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@FetcherTest
class URLDownloadTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(URLDownloadTest.class);

    @Test
    void stringDownloadWithSetEncoding() throws Exception {
        URLDownload dl = new URLDownload(new URL("http://www.google.com"));

        assertTrue(dl.asString().contains("Google"), "google.com should contain google");
    }

    @Test
    void stringDownload() throws Exception {
        URLDownload dl = new URLDownload(new URL("http://www.google.com"));

        assertTrue(dl.asString(StandardCharsets.UTF_8).contains("Google"), "google.com should contain google");
    }

    @Test
    void fileDownload() throws Exception {
        File destination = File.createTempFile("jabref-test", ".html");
        try {
            URLDownload dl = new URLDownload(new URL("http://www.google.com"));
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
    void determineMimeType() throws Exception {
        URLDownload dl = new URLDownload(new URL("http://www.google.com"));

        assertTrue(dl.getMimeType().startsWith("text/html"));
    }

    @Test
    void downloadToTemporaryFilePathWithoutFileSavesAsTmpFile() throws Exception {
        URLDownload google = new URLDownload(new URL("http://www.google.com"));

        String path = google.toTemporaryFile().toString();
        assertTrue(path.endsWith(".tmp"), path);
    }

    @Test
    void downloadToTemporaryFileKeepsName() throws Exception {
        URLDownload google = new URLDownload(new URL("https://github.com/JabRef/jabref/blob/main/LICENSE"));

        String path = google.toTemporaryFile().toString();
        assertTrue(path.contains("LICENSE"), path);
    }

    @Test
    @DisabledOnCIServer("CI Server is apparently blocked")
    void downloadOfFTPSucceeds() throws Exception {
        URLDownload ftp = new URLDownload(new URL("ftp://ftp.informatik.uni-stuttgart.de/pub/library/ncstrl.ustuttgart_fi/INPROC-2016-15/INPROC-2016-15.pdf"));

        Path path = ftp.toTemporaryFile();
        assertNotNull(path);
    }

    @Test
    void downloadOfHttpSucceeds() throws Exception {
        URLDownload ftp = new URLDownload(new URL("http://www.jabref.org"));

        Path path = ftp.toTemporaryFile();
        assertNotNull(path);
    }

    @Test
    void downloadOfHttpsSucceeds() throws Exception {
        URLDownload ftp = new URLDownload(new URL("https://www.jabref.org"));

        Path path = ftp.toTemporaryFile();
        assertNotNull(path);
    }

    @Test
    void checkConnectionSuccess() throws MalformedURLException {
        URLDownload google = new URLDownload(new URL("http://www.google.com"));

        assertTrue(google.canBeReached());
    }

    @Test
    void checkConnectionFail() throws MalformedURLException {
        URLDownload nonsense = new URLDownload(new URL("http://nonsenseadddress"));

        assertThrows(UnirestException.class, nonsense::canBeReached);
    }

    @Test
    void connectTimeoutIsNeverNull() throws MalformedURLException {
        URLDownload urlDownload = new URLDownload(new URL("http://www.example.com"));
        assertNotNull(urlDownload.getConnectTimeout(), "there's a non-null default by the constructor");

        urlDownload.setConnectTimeout(null);
        assertNotNull(urlDownload.getConnectTimeout(), "no null value can be set");
    }

    @Test
    void test503ErrorThrowsFetcherServerException() throws Exception {
        URLDownload urlDownload = new URLDownload(new URL("http://httpstat.us/503"));
        assertThrows(FetcherServerException.class, urlDownload::asString);
    }

    @Test
    void test429ErrorThrowsFetcherClientException() throws Exception {
        URLDownload urlDownload = new URLDownload(new URL("http://httpstat.us/429"));
        Exception exception = assertThrows(FetcherClientException.class, urlDownload::asString);
    }
}
