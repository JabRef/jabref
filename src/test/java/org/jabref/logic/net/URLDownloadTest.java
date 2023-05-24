package org.jabref.logic.net;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.jabref.logic.importer.FetcherClientException;
import org.jabref.logic.importer.FetcherServerException;
import org.jabref.support.DisabledOnCIServer;
import org.jabref.testutils.category.FetcherTest;

import kong.unirest.UnirestException;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@FetcherTest
public class URLDownloadTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(URLDownloadTest.class);

    @Test
    public void testStringDownloadWithSetEncoding() throws IOException {
        URLDownload dl = new URLDownload(new URL("http://www.google.com"));

        assertTrue(dl.asString().contains("Google"), "google.com should contain google");
    }

    @Test
    public void testStringDownload() throws IOException {
        URLDownload dl = new URLDownload(new URL("http://www.google.com"));

        assertTrue(dl.asString(StandardCharsets.UTF_8).contains("Google"), "google.com should contain google");
    }

    @Test
    public void testFileDownload() throws IOException {
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
    public void testDetermineMimeType() throws IOException {
        URLDownload dl = new URLDownload(new URL("http://www.google.com"));

        assertTrue(dl.getMimeType().startsWith("text/html"));
    }

    @Test
    public void downloadToTemporaryFilePathWithoutFileSavesAsTmpFile() throws IOException {
        URLDownload google = new URLDownload(new URL("http://www.google.com"));

        String path = google.toTemporaryFile().toString();
        assertTrue(path.endsWith(".tmp"), path);
    }

    @Test
    public void downloadToTemporaryFileKeepsName() throws IOException {
        URLDownload google = new URLDownload(new URL("https://github.com/JabRef/jabref/blob/main/LICENSE.md"));

        String path = google.toTemporaryFile().toString();
        assertTrue(path.contains("LICENSE") && path.endsWith(".md"), path);
    }

    @Test
    @DisabledOnCIServer("CI Server is apparently blocked")
    public void downloadOfFTPSucceeds() throws IOException {
        URLDownload ftp = new URLDownload(new URL("ftp://ftp.informatik.uni-stuttgart.de/pub/library/ncstrl.ustuttgart_fi/INPROC-2016-15/INPROC-2016-15.pdf"));

        Path path = ftp.toTemporaryFile();
        assertNotNull(path);
    }

    @Test
    public void downloadOfHttpSucceeds() throws IOException {
        URLDownload ftp = new URLDownload(new URL("http://www.jabref.org"));

        Path path = ftp.toTemporaryFile();
        assertNotNull(path);
    }

    @Test
    public void downloadOfHttpsSucceeds() throws IOException {
        URLDownload ftp = new URLDownload(new URL("https://www.jabref.org"));

        Path path = ftp.toTemporaryFile();
        assertNotNull(path);
    }

    @Test
    public void testCheckConnectionSuccess() throws MalformedURLException {
        URLDownload google = new URLDownload(new URL("http://www.google.com"));

        assertTrue(google.canBeReached());
    }

    @Test
    public void testCheckConnectionFail() throws MalformedURLException {
        URLDownload nonsense = new URLDownload(new URL("http://nonsenseadddress"));

        assertThrows(UnirestException.class, nonsense::canBeReached);
    }

    @Test
    public void connectTimeoutIsNeverNull() throws MalformedURLException {
        URLDownload urlDownload = new URLDownload(new URL("http://www.example.com"));
        assertNotNull(urlDownload.getConnectTimeout(), "there's a non-null default by the constructor");

        urlDownload.setConnectTimeout(null);
        assertNotNull(urlDownload.getConnectTimeout(), "no null value can be set");
    }

    @Test
    public void test503ErrorThrowsNestedIOExceptionWithFetcherServerException() throws Exception {
        URLDownload urlDownload = new URLDownload(new URL("http://httpstat.us/503"));

        Exception exception = assertThrows(IOException.class, urlDownload::asString);
        assertTrue(exception.getCause() instanceof FetcherServerException);
    }

    @Test
    public void test429ErrorThrowsNestedIOExceptionWithFetcherServerException() throws Exception {
        URLDownload urlDownload = new URLDownload(new URL("http://httpstat.us/429"));

        Exception exception = assertThrows(IOException.class, urlDownload::asString);
        assertTrue(exception.getCause() instanceof FetcherClientException);
    }
}
