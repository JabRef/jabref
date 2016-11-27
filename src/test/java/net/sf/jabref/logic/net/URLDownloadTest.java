package net.sf.jabref.logic.net;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.Assert;
import org.junit.Test;

public class URLDownloadTest {

    @Test
    public void testStringDownloadWithSetEncoding() throws IOException {
        URLDownload dl = new URLDownload(new URL("http://www.google.com"));

        Assert.assertTrue("google.com should contain google",
                dl.downloadToString(StandardCharsets.UTF_8).contains("Google"));
    }

    @Test
    public void testStringDownload() throws IOException {
        URLDownload dl = new URLDownload(new URL("http://www.google.com"));

        Assert.assertTrue("google.com should contain google",
                dl.downloadToString(JabRefPreferences.getInstance().getDefaultEncoding()).contains("Google"));
    }

    @Test
    public void testFileDownload() throws IOException {
        File destination = File.createTempFile("jabref-test", ".html");
        try {
            URLDownload dl = new URLDownload(new URL("http://www.google.com"));
            dl.downloadToFile(destination);
            Assert.assertTrue("file must exist", destination.exists());
        } finally {
            // cleanup
            if (!destination.delete()) {
                System.err.println("Cannot delete downloaded file");
            }
        }
    }

    @Test
    public void testDetermineMimeType() throws IOException {
        URLDownload dl = new URLDownload(new URL("http://www.google.com"));

        Assert.assertTrue(dl.determineMimeType().startsWith("text/html"));
    }

    @Test
    public void downloadToTemporaryFilePathWithoutFileSavesAsTmpFile() throws IOException {
        URLDownload google = new URLDownload(new URL("http://www.google.com"));

        String path = google.downloadToTemporaryFile().toString();
        Assert.assertTrue(path, path.endsWith(".tmp"));
    }

    @Test
    public void downloadToTemporaryFileKeepsName() throws IOException {
        URLDownload google = new URLDownload(new URL("https://github.com/JabRef/jabref/blob/master/LICENSE.md"));

        String path = google.downloadToTemporaryFile().toString();
        Assert.assertTrue(path, path.contains("LICENSE") && path.endsWith(".md"));
    }

}
